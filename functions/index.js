"use strict";

/**
 * STAGING ONLY. NOT deployed. Do not add secrets to Git.
 * Secure server-side Razorpay checkout + captured payment verification.
 * Razorpay checkout callbacks never directly grant premium access.
 */
const admin = require("firebase-admin");
const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { logger } = require("firebase-functions");
const {
  verifyWebhookSignature, verifyCheckoutSignature, resolvePlan,
  newExpiryMillis, validCapturedPayment
} = require("./razorpaySecurity");

admin.initializeApp();
const db = admin.firestore();

const keyId = defineSecret("RAZORPAY_KEY_ID");
const keySecret = defineSecret("RAZORPAY_KEY_SECRET");
const webhookSecret = defineSecret("RAZORPAY_WEBHOOK_SECRET");
const paymentOptions = {
  region: "asia-south1",
  secrets: [keyId, keySecret, webhookSecret],
  memory: "256MiB",
  timeoutSeconds: 30
};

async function api(path, method, body) {
  const key = keyId.value();
  const secret = keySecret.value();
  if (!key || !secret) throw new Error("MERCHANT_KEYS_NOT_CONFIGURED");
  const response = await fetch("https://api.razorpay.com/v1/" + path, {
    method,
    headers: {
      "Authorization": "Basic " + Buffer.from(key + ":" + secret).toString("base64"),
      "Content-Type": "application/json"
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(15000)
  });
  const result = await response.json();
  if (!response.ok) {
    logger.error("Razorpay backend request failed", { path, status: response.status });
    throw new Error("RAZORPAY_API_FAILURE");
  }
  return result;
}

async function requireEnabled() {
  const doc = await db.doc("settings/global").get();
  const config = doc.exists ? doc.data() : {};
  // Fail closed until owner explicitly enables server-side integration.
  if (!config || config.razorpayEnabled !== true)
    throw new HttpsError("failed-precondition", "Online payments are not activated yet");
  return config;
}

exports.createSmartDrivoOrder = onCall(paymentOptions, async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in required");
  const planId = request.data && request.data.planId;
  if (typeof planId !== "string")
    throw new HttpsError("invalid-argument", "Invalid plan");
  const config = await requireEnabled();
  const plan = resolvePlan(planId, config.planPrices);
  const userDoc = await db.doc("users/" + uid).get();
  if (!userDoc.exists) throw new HttpsError("permission-denied", "Account not found");
  const user = userDoc.data();
  if (user.isAdmin || user.isActive === false ||
      user.isBlocked === true || user.isDeviceBlacklisted === true)
    throw new HttpsError("permission-denied", "Account is not eligible");

  // Do not trust a client-supplied price, duration, user UID or payment ID.
  const receipt = ("sd_" + uid.slice(0, 12) + "_" + Date.now().toString(36)).slice(0, 40);
  const created = await api("orders", "POST", {
    amount: plan.amountPaise, currency: "INR", receipt,
    notes: { source: "smartdrivo", plan: plan.planId }
  });
  if (!created || !/^order_[A-Za-z0-9]+$/.test(created.id || "") ||
      created.amount !== plan.amountPaise || created.currency !== "INR")
    throw new Error("UNEXPECTED_RAZORPAY_ORDER");

  await db.doc("razorpayOrders/" + created.id).create({
    uid, planId: plan.planId, days: plan.days,
    priceRupees: plan.priceRupees, amountPaise: plan.amountPaise,
    currency: "INR", status: "CREATED",
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });
  return {
    keyId: keyId.value(), orderId: created.id, amount: plan.amountPaise,
    currency: "INR", planId: plan.planId, days: plan.days
  };
});

/** One transaction per order and payment, safe under retries and duplicate webhooks. */
async function fulfillCapturedPayment(orderId, paymentId, expectedUid) {
  if (!/^order_[A-Za-z0-9]+$/.test(orderId || "") ||
      !/^pay_[A-Za-z0-9]+$/.test(paymentId || "")) throw new Error("INVALID_IDS");

  const orderRef = db.doc("razorpayOrders/" + orderId);
  const initialOrder = await orderRef.get();
  if (!initialOrder.exists) throw new Error("UNKNOWN_ORDER");
  const data = initialOrder.data();
  if (expectedUid && data.uid !== expectedUid)
    throw new Error("PAYMENT_OWNER_MISMATCH");

  // Confirm captured status with Razorpay itself, never a client claim.
  const payment = await api("payments/" + encodeURIComponent(paymentId), "GET");
  if (!validCapturedPayment(payment, { id: orderId, amountPaise: data.amountPaise }))
    throw new Error("NOT_CAPTURED_OR_MISMATCH");

  const userRef = db.doc("users/" + data.uid);
  const paidRef = db.doc("razorpayPayments/" + paymentId);
  return db.runTransaction(async (tx) => {
    // All reads before writes.
    const [orderSnap, userSnap, paidSnap] =
      await Promise.all([tx.get(orderRef), tx.get(userRef), tx.get(paidRef)]);
    const order = orderSnap.data();
    if (!order || !userSnap.exists) throw new Error("MISSING_ORDER_OR_USER");
    if (order.uid !== data.uid || order.amountPaise !== data.amountPaise ||
        order.planId !== data.planId || order.days !== data.days)
      throw new Error("ORDER_CHANGED");
    if (paidSnap.exists) {
      if (paidSnap.data().orderId !== orderId) throw new Error("PAYMENT_REUSED");
      return { status: "ALREADY_PROCESSED" };
    }
    if (order.status === "PAID" || order.status === "REVIEW_REQUIRED")
      return { status: "ALREADY_PROCESSED" };
    if (order.status !== "CREATED") throw new Error("INVALID_ORDER_STATE");

    const user = userSnap.data();
    const eligible = !user.isAdmin && user.isActive !== false &&
      user.isBlocked !== true && user.isDeviceBlacklisted !== true;
    const now = Date.now();
    const marker = admin.firestore.FieldValue.serverTimestamp();
    tx.create(paidRef, {
      uid: data.uid, orderId, paymentId, amountPaise: data.amountPaise,
      recordedAt: marker, status: eligible ? "ACTIVATED" : "REVIEW_REQUIRED"
    });
    if (!eligible) {
      tx.update(orderRef, { status: "REVIEW_REQUIRED", paymentId, verifiedAt: marker });
      return { status: "REVIEW_REQUIRED" };
    }
    const until = newExpiryMillis(user.planExpireMillis, now, data.days);
    tx.update(userRef, {
      plan: data.planId, planPrice: data.priceRupees,
      planExpireMillis: until, isApproved: true,
      paymentProvider: "RAZORPAY", lastRazorpayPaymentId: paymentId,
      updatedAt: now
    });
    tx.update(orderRef, {
      status: "PAID", paymentId, activatedUntilMillis: until, verifiedAt: marker
    });
    return { status: "ACTIVATED", untilMillis: until };
  });
}

/** Faster client confirmation, backed by BOTH checkout HMAC and Razorpay captured-status API. */
exports.confirmSmartDrivoPayment = onCall(paymentOptions, async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in required");
  const { orderId, paymentId, signature } = request.data || {};
  if (!verifyCheckoutSignature(orderId, paymentId, signature, keySecret.value()))
    throw new HttpsError("permission-denied", "Payment signature failed");
  try {
    return await fulfillCapturedPayment(orderId, paymentId, uid);
  } catch (error) {
    logger.warn("Payment pending verification", { message: error.message });
    throw new HttpsError("failed-precondition", "Payment verification pending");
  }
});

/** Webhook catches success even if app closes immediately after paying. */
exports.smartDrivoRazorpayWebhook = onRequest(paymentOptions, async (req, res) => {
  if (req.method !== "POST") return res.status(405).end();
  if (!Buffer.isBuffer(req.rawBody) || req.rawBody.length > 1024 * 1024)
    return res.status(400).end();
  const header = req.get("X-Razorpay-Signature");
  if (!verifyWebhookSignature(req.rawBody, header, webhookSecret.value()))
    return res.status(401).end();
  let event;
  try {
    event = JSON.parse(req.rawBody.toString("utf8"));
  } catch {
    return res.status(400).end();
  }
  if (event.event !== "payment.captured") return res.status(204).end();
  const payment = event.payload && event.payload.payment &&
    event.payload.payment.entity;
  if (!payment || payment.status !== "captured")
    return res.status(400).end();
  try {
    await fulfillCapturedPayment(payment.order_id, payment.id, null);
    return res.status(200).json({ ok: true });
  } catch (error) {
    // Razorpay retries failures. Do not acknowledge a missing database update.
    logger.error("Razorpay capture webhook not yet fulfilled", {
      message: error.message
    });
    return res.status(503).json({ ok: false });
  }
});
