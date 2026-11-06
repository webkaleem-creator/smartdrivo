"use strict";

const crypto = require("node:crypto");

const PLANS = Object.freeze({
  "3DAYS": { days: 3, key: "plan3Days", defaultRupees: 59 },
  "7DAYS": { days: 7, key: "plan7Days", defaultRupees: 129 },
  "15DAYS": { days: 15, key: "plan15Days", defaultRupees: 199 },
  "1MONTH": { days: 30, key: "plan1Month", defaultRupees: 329 }
});

function safeHexCompare(actual, expected) {
  if (typeof actual !== "string" || !/^[a-f0-9]{64}$/i.test(actual)) return false;
  const a = Buffer.from(actual, "hex");
  const b = Buffer.from(expected, "hex");
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

function verifyWebhookSignature(rawBody, header, webhookSecret) {
  if (!Buffer.isBuffer(rawBody) || !webhookSecret) return false;
  const expected = crypto.createHmac("sha256", webhookSecret)
    .update(rawBody).digest("hex");
  return safeHexCompare(header, expected);
}

function verifyCheckoutSignature(orderId, paymentId, signature, apiSecret) {
  if (!/^order_[A-Za-z0-9]+$/.test(orderId || "") ||
      !/^pay_[A-Za-z0-9]+$/.test(paymentId || "") || !apiSecret) return false;
  const expected = crypto.createHmac("sha256", apiSecret)
    .update(orderId + "|" + paymentId).digest("hex");
  return safeHexCompare(signature, expected);
}

function resolvePlan(planId, planPrices) {
  const spec = PLANS[planId];
  if (!spec) throw new Error("UNSUPPORTED_PLAN");
  const price = planPrices && Object.prototype.hasOwnProperty.call(planPrices, spec.key)
    ? Number(planPrices[spec.key]) : spec.defaultRupees;
  if (!Number.isSafeInteger(price) || price < 1 || price > 100000)
    throw new Error("INVALID_SERVER_PLAN_PRICE");
  return { planId, days: spec.days, priceRupees: price, amountPaise: price * 100 };
}

function newExpiryMillis(currentExpiryMillis, nowMillis, days) {
  if (!Number.isSafeInteger(nowMillis) || nowMillis <= 0 ||
      !Number.isSafeInteger(days) || days < 1 || days > 365)
    throw new Error("INVALID_EXPIRY_INPUT");
  const old = Number.isSafeInteger(currentExpiryMillis) && currentExpiryMillis > nowMillis
    ? currentExpiryMillis : nowMillis;
  return old + days * 86400000;
}

function validCapturedPayment(payment, order) {
  return Boolean(payment && order &&
    payment.status === "captured" &&
    payment.order_id === order.id &&
    payment.currency === "INR" &&
    payment.amount === order.amountPaise &&
    /^pay_[A-Za-z0-9]+$/.test(payment.id || ""));
}

module.exports = {
  PLANS, verifyWebhookSignature, verifyCheckoutSignature,
  resolvePlan, newExpiryMillis, validCapturedPayment
};
