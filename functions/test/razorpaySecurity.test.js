"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const crypto = require("node:crypto");
const {
  resolvePlan, verifyWebhookSignature, verifyCheckoutSignature,
  newExpiryMillis, validCapturedPayment
} = require("../razorpaySecurity");

test("Razorpay valid raw-body webhook and tamper detection", () => {
  const body = Buffer.from('{"event":"payment.captured"}');
  const signature = crypto.createHmac("sha256", "demo-webhook-secret")
    .update(body).digest("hex");
  assert.equal(verifyWebhookSignature(body, signature, "demo-webhook-secret"), true);
  assert.equal(verifyWebhookSignature(Buffer.from('{"event":"order.paid"}'),
    signature, "demo-webhook-secret"), false);
  assert.equal(verifyWebhookSignature(body, "a".repeat(64), "demo-webhook-secret"), false);
  assert.equal(verifyWebhookSignature(body, "invalid", "demo-webhook-secret"), false);
});

test("Android checkout callback signature is NOT sufficient without capture", () => {
  const sig = crypto.createHmac("sha256", "server-only-secret")
    .update("order_ABC123|pay_XYZ123").digest("hex");
  assert.equal(verifyCheckoutSignature("order_ABC123", "pay_XYZ123",
    sig, "server-only-secret"), true);
  assert.equal(verifyCheckoutSignature("order_OTHER", "pay_XYZ123",
    sig, "server-only-secret"), false);
  assert.equal(verifyCheckoutSignature("order_ABC123", "pay_XYZ123",
    "0".repeat(64), "server-only-secret"), false);
});

test("Only server-configured plan amounts and days may be accepted", () => {
  const plan = resolvePlan("7DAYS", { plan7Days: 199 });
  assert.deepEqual(plan, { planId:"7DAYS", days:7, priceRupees:199, amountPaise:19900 });
  assert.throws(() => resolvePlan("CUSTOM", {}), /UNSUPPORTED_PLAN/);
  assert.throws(() => resolvePlan("7DAYS", { plan7Days:-100 }), /INVALID_SERVER/);
  assert.throws(() => resolvePlan("7DAYS", { plan7Days:0 }), /INVALID_SERVER/);
});

test("Renewals append from existing expiry and never subtract days", () => {
  assert.equal(newExpiryMillis(0, 100000, 3), 100000+3*86400000);
  assert.equal(newExpiryMillis(200000, 100000, 3), 200000+3*86400000);
  assert.throws(() => newExpiryMillis(0,100000,0), /INVALID_EXPIRY/);
});

test("A successful-looking callback cannot bypass captured amount/currency/order", () => {
  const order = { id:"order_ABC123", amountPaise:19900 };
  const payment = {id:"pay_ABC123",order_id:"order_ABC123",
    amount:19900,currency:"INR",status:"captured"};
  assert.equal(validCapturedPayment(payment,order), true);
  assert.equal(validCapturedPayment({...payment,status:"authorized"},order), false);
  assert.equal(validCapturedPayment({...payment,amount:1},order), false);
  assert.equal(validCapturedPayment({...payment,order_id:"order_other"},order), false);
  assert.equal(validCapturedPayment({...payment,currency:"USD"},order), false);
});
