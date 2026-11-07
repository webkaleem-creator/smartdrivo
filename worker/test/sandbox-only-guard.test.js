import test from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.js";

// The staging Worker must never accept LIVE Razorpay credentials or an unscoped user.
// These tests make NO external requests and perform NO Firestore writes.
const env={
  PAYMENTS_ENABLED:"true",FIREBASE_PROJECT_ID:"smartdrivo",
  RAZORPAY_KEY_ID:"rzp_test_fakeOnly",RAZORPAY_KEY_SECRET:"testOnlySecret",
  RAZORPAY_WEBHOOK_SECRET:"webhookTestSecret",FIREBASE_WEB_API_KEY:"firebasePublicMock",
  FIREBASE_SERVICE_ACCOUNT_JSON:"{}",TEST_FIREBASE_UID:"testUser_123"
};
const req=()=>new Request("https://test.invalid/v1/orders",{method:"POST",
  headers:{"Authorization":"Bearer "+"x".repeat(150),"Content-Type":"application/json"},
  body:JSON.stringify({planId:"7DAYS"})});
test("even when enabled, LIVE keys are rejected before network access",async()=>{
  const old=globalThis.fetch;let called=0;
  globalThis.fetch=async()=>{called++;throw Error("unexpected external request");};
  try{
    const r=await worker.fetch(req(),{...env,RAZORPAY_KEY_ID:"rzp_live_shouldNeverPass"});
    assert.equal(r.status,503);assert.equal(called,0);
  }finally{globalThis.fetch=old;}
});
test("enabled staging requires a specific Firebase test UID",async()=>{
  const old=globalThis.fetch;let called=0;
  globalThis.fetch=async()=>{called++;throw Error("unexpected external request");};
  try{
    const r=await worker.fetch(req(),{...env,TEST_FIREBASE_UID:""});
    assert.equal(r.status,503);assert.equal(called,0);
  }finally{globalThis.fetch=old;}
});
test("authenticated non-test Firebase user cannot create any Razorpay order",async()=>{
  const old=globalThis.fetch;let called=0;
  globalThis.fetch=async(url)=>{
    called++;
    assert.equal(new URL(url).hostname,"identitytoolkit.googleapis.com");
    return Response.json({users:[{localId:"otherUser_999"}]});
  };
  try{
    const r=await worker.fetch(req(),env);
    assert.equal(r.status,403);
    assert.equal((await r.json()).status,"TEST_USER_ONLY");
    assert.equal(called,1); // identity lookup only; no Firestore or Razorpay
  }finally{globalThis.fetch=old;}
});
