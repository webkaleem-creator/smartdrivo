package com.example.model

import java.util.Locale
import kotlin.math.ceil

/** Read-only, role-aware presentation of the account's saved membership. */
object MembershipDisplay {
    data class Summary(val plan: String, val status: String, val days: String, val active: Boolean)

    fun of(
        user: UserProfile,
        now: Long = System.currentTimeMillis(),
        specialOffer: SpecialOccasionOffer = SpecialOccasionOffer()
    ): Summary {
        if (user.isAdmin) return Summary("Admin Access", "Active", "Unlimited", true)
        val stored = user.plan.trim()
        val code = stored.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]"), "")
        val label = when (code) {
            "", "NONE", "NOPLAN" -> "No Plan"
            "FREETRIAL" -> "1-Day Free Trial"
            "3DAYS", "3DAY" -> "3 Days Plan"
            "7DAYS", "7DAY" -> "7 Days Plan"
            "15DAYS", "15DAY" -> "15 Days Plan"
            "1MONTH", "30DAYS", "30DAY" -> "1 Month Plan"
            "1YEAR", "365DAYS", "365DAY" -> "1 Year Plan"
            else -> stored.replace('_', ' ').replace('-', ' ')
                .replace(Regex("\\s+"), " ").trim().ifBlank { "No Plan" }
        }
        val expiry = user.planExpireMillis
        val active = expiry > now && (user.isApproved || code == "FREETRIAL")
        // The paid/free-trial plan always takes precedence while valid.
        // A separate, server-verified occasion overlay is display-only and
        // must not alter any persisted subscription record.
        val offerActive = !active && user.isActive && !user.isBlocked &&
            !user.isDeviceBlacklisted && user.specialOfferUntilMillis > now &&
            specialOffer.isLive(now)
        if (offerActive) {
            val offerTitle = specialOffer.title.trim()
                .ifBlank { "Special Occasion Free" }
            val daysText = if (specialOffer.days == 0) "Unlimited"
                else {
                    val d = ceil(
                        (user.specialOfferUntilMillis - now) / 86_400_000.0
                    ).toLong().coerceAtLeast(1L)
                    d.toString() + " " +
                        (if (d == 1L) "day" else "days") + " left"
                }
            return Summary(offerTitle, "Active", daysText, true)
        }
        val status = when {
            active -> "Active"
            expiry > 0L && expiry <= now -> "Expired"
            else -> "Inactive"
        }
        val days = when {
            expiry <= 0L -> "Not activated"
            expiry > now && !active -> "Awaiting approval"
            expiry > now -> {
                val d = ceil((expiry-now)/86_400_000.0).toLong().coerceAtLeast(1L)
                d.toString()+" "+(if (d==1L) "day" else "days")+" left"
            }
            else -> {
                val d = ceil((now-expiry)/86_400_000.0).toLong().coerceAtLeast(1L)
                "Expired "+d+" "+(if (d==1L) "day" else "days")+" ago"
            }
        }
        return Summary(label,status,days,active)
    }
}
