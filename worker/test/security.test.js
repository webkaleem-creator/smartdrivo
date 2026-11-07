import test from "node:test";
import assert from "node:assert/strict";
import {createHmac,generateKeyPairSync,webcrypto} from "node:crypto";
import {plan,fields,expiry,eligible,isCaptured,verifyHmac} from "../src/policy.js";
import worker from "../src/index.js";

// Node 20 already exposes the WebCrypto binding as a read-only global.
const fake=values=>({name:"projects/smartdrivo/databases/(default)/documents/test/item",updateTime:"2026-11-06T00:00:00.000000Z",fields:fields(values)});
test("server sets plan prices and duration; client amount cannot be used",()=>{
  const settings={fields:{planPrices:{mapValue:{fields:{plan7Days:{integerValue:"199"}}}}}};
  assert.deepEqual(plan("7DAYS",settings),{id:"7DAYS",days:7,price:199,amount:19900});
  assert.throws(()=>plan("evil",settings),/INVALID_PLAN/);
  assert.throws(()=>plan("7DAYS",{fields:{planPrices:{mapValue:{fields:{plan7Days:{integerValue:"-1"}}}}}}),/INVALID_SERVER_PRICE/);
});
test("membership renewals append days and blocked users never activate",()=>{
 assert.equal(expiry(999999,111111,7),999999+7*86400000);
 assert.equal(expiry(0,111111,7),111111+7*86400000);
 assert.throws(()=>expiry(0,111111,0),/INVALID_EXPIRY/);
 assert.equal(eligible(fake({isActive:true,isAdmin:false})),true);
 for(const k of["isAdmin","isBlocked","isDeviceBlacklisted"])
  assert.equal(eligible(fake({[k]:true})),false);
 assert.equal(eligible(fake({isActive:false})),false);
});
test("captured status, amount, currency and original order must all match",()=>{
 const p={id:"pay_ABCDE",order_id:"order_ABCDEF",status:"captured",currency:"INR",amount:12900},o=fake({amountPaise:12900,currency:"INR"});
 assert.equal(isCaptured(p,"order_ABCDEF",o),true);
 assert.equal(isCaptured({...p,status:"authorized"},"order_ABCDEF",o),false);
 assert.equal(isCaptured({...p,amount:1},"order_ABCDEF",o),false);
 assert.equal(isCaptured({...p,currency:"USD"},"order_ABCDEF",o),false);
 assert.equal(isCaptured({...p,order_id:"order_OTHER"},"order_ABCDEF",o),false);
});
test("webhook signature HMAC rejects fake/modified payload",async()=>{
 const secret="test-secret",raw=new TextEncoder().encode('{"event":"payment.captured"}');
 const signature=createHmac("sha256",secret).update(raw).digest("hex");
 assert.equal(await verifyHmac(secret,raw,signature),true);
 assert.equal(await verifyHmac(secret,new TextEncoder().encode("{}"),signature),false);
 assert.equal(await verifyHmac(secret,raw,"00".repeat(32)),false);
 assert.equal(await verifyHmac(secret,raw,"not-a-hmac"),false);
});
test("disabled payments don't access external services or activate membership",async()=>{
 const r=await worker.fetch(new Request("https://payments.example/v1/orders",{method:"POST",body:"{}"}),{PAYMENTS_ENABLED:"false"});
 assert.equal(r.status,503);assert.equal((await r.json()).status,"NOT_ENABLED");
 const health=await worker.fetch(new Request("https://payments.example/health"),{});
 assert.deepEqual(await health.json(),{status:"OK",billing:"FREE_ONLY",live:false});
});
test("end-to-end server-captured payment credits once; callback replay is idempotent",async()=>{
 const keys=generateKeyPairSync("rsa",{modulusLength:2048});
 const sa={project_id:"smartdrivo",client_email:"sandbox@smartdrivo.iam.gserviceaccount.com",private_key:keys.privateKey.export({format:"pem",type:"pkcs8"})};
 const env={PAYMENTS_ENABLED:"true",RAZORPAY_KEY_ID:"rzp_test_fake",RAZORPAY_KEY_SECRET:"fakeSecret",
 RAZORPAY_WEBHOOK_SECRET:"webhookSecret",FIREBASE_PROJECT_ID:"smartdrivo",
 FIREBASE_WEB_API_KEY:"firebasePublicTestKey",FIREBASE_SERVICE_ACCOUNT_JSON:JSON.stringify(sa),TEST_FIREBASE_UID:"testUser_123"};
 const prefix="projects/smartdrivo/databases/(default)/documents/";
 const docs=new Map();
 const make=(path,data)=>({name:prefix+path,updateTime:"2026-11-06T00:00:00.000000Z",fields:fields(data)});
 docs.set("settings/global",{name:prefix+"settings/global",updateTime:"2026-11-06T00:00:00.000000Z",
 fields:{razorpayEnabled:{booleanValue:true},planPrices:{mapValue:{fields:{plan7Days:{integerValue:"129"}}}}}});
 docs.set("users/testUser_123",make("users/testUser_123",{isAdmin:false,isActive:true,isApproved:false,planExpireMillis:0}));
 let credits=0,commits=0;
 const originalFetch=globalThis.fetch;
 globalThis.fetch=async(url,init={})=>{
  const u=new URL(url),path=u.pathname;const method=init.method??"GET";
  if(u.hostname==="identitytoolkit.googleapis.com")return Response.json({users:[{localId:"testUser_123"}]});
  if(u.hostname==="oauth2.googleapis.com")return Response.json({access_token:"test-google-access",expires_in:3600});
  if(u.hostname==="api.razorpay.com"&&path==="/v1/orders")
   return Response.json({id:"order_ABC123",amount:12900,currency:"INR"});
  if(u.hostname==="api.razorpay.com"&&path==="/v1/payments/pay_ABC123")
   return Response.json({id:"pay_ABC123",order_id:"order_ABC123",amount:12900,currency:"INR",status:"captured"});
  if(u.hostname==="firestore.googleapis.com"){
   const tail=decodeURIComponent(path.split("/documents")[1]||"");
   const docpath=tail.replace(/^\//,"");
   if(method==="GET")return docs.has(docpath)?Response.json(docs.get(docpath)):Response.json({error:"NOT_FOUND"},{status:404});
   if(method==="POST"&&tail.includes("?"))throw Error("unexpected path");
   if(method==="POST"&&u.searchParams.has("documentId")){
    const id=u.searchParams.get("documentId"),data=JSON.parse(init.body);
    docs.set(docpath+"/"+id,{name:prefix+docpath+"/"+id,updateTime:"2026-11-06T00:00:00.000000Z",fields:data.fields});
    return Response.json(docs.get(docpath+"/"+id));
   }
   if(method==="POST"&&tail===":commit"){
    const writes=JSON.parse(init.body).writes;commits++;
    for(const w of writes){
     const p=w.update.name.slice(prefix.length);
     if(w.currentDocument?.exists===false&&docs.has(p))return Response.json({error:"exists"},{status:409});
    }
    for(const w of writes){
     const p=w.update.name.slice(prefix.length),old=docs.get(p);
     docs.set(p,{name:w.update.name,updateTime:"2026-11-06T00:00:01.000000Z",fields:{...(old?.fields||{}),...w.update.fields}});
     if(p.startsWith("users/"))credits++;
    }
    return Response.json({writeResults:writes.map(()=>({}))});
   }
  }
  throw Error("Unexpected mock API: "+method+" "+url);
 };
 try{
  const auth={"Authorization":"Bearer "+"X".repeat(200),"Content-Type":"application/json"};
  const created=await worker.fetch(new Request("https://payments.example/v1/orders",{method:"POST",headers:auth,body:JSON.stringify({planId:"7DAYS",amount:1})}),env);
  assert.equal(created.status,200);const order=await created.json();assert.equal(order.amount,12900);
  const payload={orderId:"order_ABC123",paymentId:"pay_ABC123"};
  payload.signature=createHmac("sha256",env.RAZORPAY_KEY_SECRET)
    .update(payload.orderId+"|"+payload.paymentId).digest("hex");
  const fakeSig=await worker.fetch(new Request("https://payments.example/v1/confirm",{method:"POST",headers:auth,body:JSON.stringify({...payload,signature:"0".repeat(64)})}),env);
  assert.equal(fakeSig.status,403);assert.equal(credits,0);
  for(let i=0;i<2;i++){
   const confirmed=await worker.fetch(new Request("https://payments.example/v1/confirm",{method:"POST",headers:auth,body:JSON.stringify(payload)}),env);
   assert.equal(confirmed.status,200);
   assert.ok(["ACTIVATED","ALREADY_PAID"].includes((await confirmed.json()).status));
  }
  assert.equal(credits,1);assert.equal(commits,1);
  assert.equal(Number(docs.get("users/testUser_123").fields.planExpireMillis.integerValue)>Date.now(),true);
 }finally{globalThis.fetch=originalFetch}
});
