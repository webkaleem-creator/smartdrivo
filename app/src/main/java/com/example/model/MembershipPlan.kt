package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
data class MembershipPlan(
    val id: String,
    val days: Int,
    val price: Int,
    val label: String,
    val description: String = ""
) {
    val name: String get() = label
    val durationDays: Int get() = days

    companion object {
        val DEFAULT_PLANS = listOf(
            MembershipPlan("3DAYS", 3, 59, "3 Days Pass", "Quick trial pass for new drivers"),
            MembershipPlan("7DAYS", 7, 129, "7 Days Pass", "Most popular weekly pass"),
            MembershipPlan("15DAYS", 15, 199, "15 Days Pass", "Best bi-weekly discount pack"),
            MembershipPlan("1MONTH", 30, 329, "1 Month Pass", "Full monthly unlimited pass")
        )
    }
}
