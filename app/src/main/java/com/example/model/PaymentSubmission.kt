package com.example.model

import androidx.compose.runtime.Immutable

enum class PaymentStatus {
    PENDING,
    APPROVED,
    REJECTED
}

@Immutable
data class PaymentSubmission(
    val paymentId: String = "",
    val uid: String = "",
    val userName: String = "",
    val utrNumber: String = "",
    val planSelected: String = "7DAYS",
    val amount: Int = 129,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val submittedAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null,
    val note: String = ""
) {
    val timestamp: Long get() = submittedAt

    constructor(
        paymentId: String,
        uid: String,
        userName: String,
        utrNumber: String,
        planSelected: String,
        amount: Int,
        timestamp: Long = System.currentTimeMillis(),
        status: PaymentStatus = PaymentStatus.PENDING
    ) : this(
        paymentId = paymentId,
        uid = uid,
        userName = userName,
        utrNumber = utrNumber,
        planSelected = planSelected,
        amount = amount,
        status = status,
        submittedAt = timestamp
    )
}
