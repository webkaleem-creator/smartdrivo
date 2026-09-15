package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val vehicleType: VehicleType = VehicleType.AUTO,
    val plan: String = "7DAYS",
    val planPrice: Int = 129,
    val planExpireMillis: Long = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000),
    val isApproved: Boolean = false,
    val isAdmin: Boolean = false,
    val isActive: Boolean = true,
    val referralCode: String = "SMART50",
    val createdAt: Long = System.currentTimeMillis(),
    val mobile: String = "",
    val city: String = "",
    val state: String = ""
) {
    val isPlanValid: Boolean
        get() = isApproved && (planExpireMillis > System.currentTimeMillis())

    val effectiveMobile: String
        get() = mobile.ifEmpty { phone }
}
