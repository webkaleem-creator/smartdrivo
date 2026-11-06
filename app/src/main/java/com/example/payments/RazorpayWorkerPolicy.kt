package com.example.payments

/**
 * TEST integration only; URLs are validated before passing Firebase auth ID tokens.
 * No Razorpay secret is embedded in Android or read from user input.
 */
internal object RazorpayWorkerPolicy {
    private val endpointRegex = Regex("^https://[a-z0-9-]+\\.[a-z0-9-]+\\.workers\\.dev$")
    private val orderRegex = Regex("^order_[a-zA-Z0-9]{4,80}$")
    private val paymentRegex = Regex("^pay_[a-zA-Z0-9]{4,80}$")
    private val signatureRegex = Regex("^[0-9a-fA-F]{64}$")
    private val validPlans = setOf("3DAYS", "7DAYS", "15DAYS", "1MONTH")

    fun allowedEndpoint(value: String): Boolean = endpointRegex.matches(value)
    fun allowedPlan(value: String): Boolean = value in validPlans
    fun allowedOrder(value: String): Boolean = orderRegex.matches(value)
    fun allowedPayment(value: String): Boolean = paymentRegex.matches(value)
    fun allowedSignature(value: String): Boolean = signatureRegex.matches(value)
    fun allowedTestKey(value: String): Boolean = value.startsWith("rzp_test_") && value.length > 10
}
