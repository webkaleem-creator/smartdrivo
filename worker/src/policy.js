export const PLANS=Object.freeze({
"3DAYS":{days:3,key:"plan3Days",fallback:59},
"7DAYS":{days:7,key:"plan7Days",fallback:129},
"15DAYS":{days:15,key:"plan15Days",fallback:199},
"1MONTH":{days:30,key:"plan1Month",fallback:329}});
export function value(doc,k,otherwise=null){const v=doc?.fields?.[k];if(!v)return otherwise;if("integerValue" in v)return Number(v.integerValue);if("stringValue" in v)return v.stringValue;if("booleanValue" in v)return v.booleanValue;if("mapValue" in v)return v.mapValue;return otherwise}
export function fields(obj){return Object.fromEntries(Object.entries(obj).map(([k,v])=>[k,typeof v==="boolean"?{booleanValue:v}:typeof v==="number"&&Number.isSafeInteger(v)?{integerValue:String(v)}:typeof v==="string"?{stringValue:v}:(()=>{throw Error("INVALID_FIELD")})()]))}
export function plan(id,settings){const p=PLANS[id];if(!p)throw Error("INVALID_PLAN");const map=value(settings,"planPrices",{fields:{}})?.fields||{};const x=p.key in map?Number(map[p.key].integerValue??map[p.key].stringValue):p.fallback;if(!Number.isSafeInteger(x)||x<=0||x>100000)throw Error("INVALID_SERVER_PRICE");return{id,days:p.days,price:x,amount:x*100}}
export function expiry(old,now,days){if(!Number.isSafeInteger(now)||now<=0||!Number.isSafeInteger(days)||days<1||days>365)throw Error("INVALID_EXPIRY");return(Math.max(Number.isSafeInteger(old)?old:0,now))+days*86400000}
export function eligible(user){return!!user&&value(user,"isAdmin",false)!==true&&value(user,"isActive",true)!==false&&value(user,"isBlocked",false)!==true&&value(user,"isDeviceBlacklisted",false)!==true}
export function isCaptured(payment,orderId,order){return payment?.status==="captured"&&/^pay_[A-Za-z0-9]+$/.test(payment?.id||"")&&payment?.order_id===orderId&&payment?.currency==="INR"&&payment?.amount===value(order,"amountPaise",-1)&&value(order,"currency")==="INR"}
export function b64(bytes){let s="";for(const x of bytes)s+=String.fromCharCode(x);return btoa(s).replace(/\+/g,"-").replace(/\//g,"_").replace(/=/g,"")}
export async function verifyHmac(secret,bytes,signature){if(!secret||typeof signature!=="string"||!/^[a-fA-F0-9]{64}$/.test(signature))return false;const s=Uint8Array.from(signature.match(/.{2}/g),x=>parseInt(x,16));const key=await crypto.subtle.importKey("raw",new TextEncoder().encode(secret),{name:"HMAC",hash:"SHA-256"},false,["verify"]);return crypto.subtle.verify("HMAC",key,s,bytes)}
