package com.example.payments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Dedicated checkout Activity: existing ride processing and manual UPI remain untouched.
 * This is TEST-KEY-ONLY until reviewed production rollout.
 * Razorpay callback never grants the plan locally; only verified Worker + Firestore can.
 */
class RazorpayCheckoutActivity : Activity(), PaymentResultWithDataListener {
    companion object {
        const val EXTRA_PLAN_ID = "smartdrivo_payment_plan_id"
        const val EXTRA_EXPECTED_RUPEES = "smartdrivo_payment_expected_rupees"
        const val RESULT_STATUS = "smartdrivo_verified_payment_status"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val http = OkHttpClient.Builder()
        .callTimeout(25, TimeUnit.SECONDS).build()
    private var originalOrderId: String = ""
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusText = TextView(this).apply {
            text = "Preparing secure Razorpay checkout..."
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        setContentView(statusText)

        val base = BuildConfig.RAZORPAY_WORKER_URL
        val plan = intent.getStringExtra(EXTRA_PLAN_ID).orEmpty()
        val expected = intent.getIntExtra(EXTRA_EXPECTED_RUPEES, 0)
        if (!RazorpayWorkerPolicy.allowedEndpoint(base) ||
            !RazorpayWorkerPolicy.allowedPlan(plan) || expected <= 0) {
            finishWithStatus("NOT_CONFIGURED")
            return
        }

        withFirebaseToken { token ->
            scope.launch {
                try {
                    val data = withContext(Dispatchers.IO) {
                        post(base, "/v1/orders", token, JSONObject().put("planId", plan))
                    }
                    val id = data.optString("orderId")
                    val key = data.optString("keyId")
                    val amount = data.optInt("amount")
                    if (data.optString("status") != "READY" ||
                        !RazorpayWorkerPolicy.allowedOrder(id) ||
                        !RazorpayWorkerPolicy.allowedTestKey(key) ||
                        amount != expected * 100 || data.optString("currency") != "INR") {
                        finishWithStatus("PRICE_OR_ORDER_MISMATCH")
                        return@launch
                    }
                    originalOrderId = id
                    statusText.text = "Opening Razorpay secure checkout..."
                    val checkout = Checkout()
                    checkout.setKeyID(key)
                    val options = JSONObject().apply {
                        put("name", "SmartDrivo")
                        put("description", "SmartDrivo membership: $plan")
                        put("order_id", id)
                        put("amount", amount)
                        put("currency", "INR")
                        put("theme", JSONObject().put("color", "#1677F2"))
                    }
                    checkout.open(this@RazorpayCheckoutActivity, options)
                } catch (_: Exception) {
                    finishWithStatus("CHECKOUT_UNAVAILABLE")
                }
            }
        }
    }

    private fun post(base: String, path: String, token: String, payload: JSONObject): JSONObject {
        val req = Request.Builder()
            .url(base + path)
            .header("Authorization", "Bearer $token")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { response ->
            val json = JSONObject(response.body?.string().orEmpty().ifBlank { "{}" })
            if (!response.isSuccessful) throw IllegalStateException("Verification not ready")
            return json
        }
    }

    private fun withFirebaseToken(onReady: (String) -> Unit) {
        val current = FirebaseAuth.getInstance().currentUser
        if (current == null) {
            finishWithStatus("LOGIN_REQUIRED")
            return
        }
        current.getIdToken(false).addOnCompleteListener(this) { result ->
            val token = if (result.isSuccessful) result.result?.token else null
            if (token.isNullOrBlank()) finishWithStatus("LOGIN_REQUIRED")
            else onReady(token)
        }
    }

    override fun onPaymentSuccess(paymentId: String?, paymentData: PaymentData?) {
        val receipt = try { JSONObject(paymentData?.data?.toString() ?: "{}") }
        catch (_: Exception) { JSONObject() }
        val signedOrder = receipt.optString("razorpay_order_id")
        val signature = receipt.optString("razorpay_signature")
        val verifiedPaymentId = receipt.optString("razorpay_payment_id", paymentId.orEmpty())
        if (signedOrder != originalOrderId ||
            !RazorpayWorkerPolicy.allowedOrder(signedOrder) ||
            !RazorpayWorkerPolicy.allowedPayment(verifiedPaymentId) ||
            !RazorpayWorkerPolicy.allowedSignature(signature)) {
            finishWithStatus("VERIFICATION_PENDING")
            return
        }
        statusText.text = "Payment received. Waiting for server verification..."
        withFirebaseToken { token ->
            scope.launch {
                val verified = try {
                    val data = withContext(Dispatchers.IO) {
                        post(BuildConfig.RAZORPAY_WORKER_URL, "/v1/confirm", token,
                            JSONObject().put("orderId", originalOrderId)
                                .put("paymentId", verifiedPaymentId)
                                .put("signature", signature))
                    }
                    data.optString("status")
                } catch (_: Exception) {
                    "VERIFICATION_PENDING"
                }
                // No local membership state modification. Server-side Firestore is
                // authoritative; a delayed webhook can still activate the plan.
                finishWithStatus(
                    if (verified == "ACTIVATED" || verified == "ALREADY_PAID")
                        "VERIFIED" else "VERIFICATION_PENDING"
                )
            }
        }
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        finishWithStatus("CHECKOUT_CLOSED")
    }

    private fun finishWithStatus(status: String) {
        setResult(RESULT_OK, Intent().putExtra(RESULT_STATUS, status))
        finish()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
