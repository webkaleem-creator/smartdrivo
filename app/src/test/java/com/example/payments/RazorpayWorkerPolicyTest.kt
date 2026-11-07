package com.example.payments

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RazorpayWorkerPolicyTest {
    @Test fun onlySecureWorkerUrlsAllowed() {
        assertTrue(RazorpayWorkerPolicy.allowedEndpoint("https://smartdrivo-razorpay.example.workers.dev"))
        assertFalse(RazorpayWorkerPolicy.allowedEndpoint("http://smartdrivo-razorpay.example.workers.dev"))
        assertFalse(RazorpayWorkerPolicy.allowedEndpoint("https://evil.example.org"))
        assertFalse(RazorpayWorkerPolicy.allowedEndpoint("https://workers.dev.attacker.com"))
        assertFalse(RazorpayWorkerPolicy.allowedEndpoint(""))
    }
    @Test fun onlyKnownPlanAndTestKeys() {
        assertTrue(RazorpayWorkerPolicy.allowedPlan("7DAYS"))
        assertFalse(RazorpayWorkerPolicy.allowedPlan("UNLIMITED"))
        assertTrue(RazorpayWorkerPolicy.allowedTestKey("rzp_test_ABC123"))
        assertFalse(RazorpayWorkerPolicy.allowedTestKey("rzp_live_ABC123"))
    }
    @Test fun paymentIdsAndCallbackSignaturesMustBeStructured() {
        assertTrue(RazorpayWorkerPolicy.allowedOrder("order_ABCD1234"))
        assertTrue(RazorpayWorkerPolicy.allowedPayment("pay_ABCD1234"))
        assertTrue(RazorpayWorkerPolicy.allowedSignature("0".repeat(64)))
        assertFalse(RazorpayWorkerPolicy.allowedOrder("bad"))
        assertFalse(RazorpayWorkerPolicy.allowedSignature("not_verified"))
    }
}
