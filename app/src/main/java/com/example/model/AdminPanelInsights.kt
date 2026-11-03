package com.example.model

/**
 * Read-only admin dashboard metrics. Account enablement and membership validity
 * are independent; never equate isActive with a currently paid subscription.
 * Admin accounts are excluded from paid-plan metrics.
 */
object AdminPanelInsights {
    data class Totals(
        val totalUsers: Int,
        val enabledAccounts: Int,
        val disabledAccounts: Int,
        val validPlans: Int,
        val expiredPlans: Int,
        val awaitingApproval: Int,
        val admins: Int
    )

    enum class UserFilter { ALL, VALID_PLAN, EXPIRED_PLAN, AWAITING, DISABLED }
    enum class SortOrder { NEWEST, EXPIRING_SOON, NAME }

    fun calculate(users: List<UserProfile>, now: Long = System.currentTimeMillis()): Totals =
        Totals(
            totalUsers = users.size,
            enabledAccounts = users.count { it.isActive },
            disabledAccounts = users.count { !it.isActive },
            validPlans = users.count { !it.isAdmin && MembershipDisplay.of(it, now).active },
            expiredPlans = users.count { !it.isAdmin && MembershipDisplay.of(it, now).status == "Expired" },
            awaitingApproval = users.count { !it.isAdmin && isAwaitingApproval(it, now) },
            admins = users.count { it.isAdmin }
        )

    fun isAwaitingApproval(user: UserProfile, now: Long): Boolean =
        !user.isAdmin && !user.isFreeTrial && !user.isApproved &&
            user.planExpireMillis > now

    fun matches(user: UserProfile, filter: UserFilter, now: Long): Boolean =
        when (filter) {
            UserFilter.ALL -> true
            UserFilter.VALID_PLAN -> !user.isAdmin && MembershipDisplay.of(user, now).active
            UserFilter.EXPIRED_PLAN -> !user.isAdmin && MembershipDisplay.of(user, now).status == "Expired"
            UserFilter.AWAITING -> isAwaitingApproval(user, now)
            UserFilter.DISABLED -> !user.isActive
        }

    fun sorted(users: List<UserProfile>, sort: SortOrder, now: Long): List<UserProfile> =
        when (sort) {
            SortOrder.NEWEST -> users.sortedWith(compareByDescending<UserProfile> { it.createdAt }.thenBy { it.name })
            SortOrder.NAME -> users.sortedWith(compareBy<UserProfile> { it.name.lowercase() }.thenByDescending { it.createdAt })
            SortOrder.EXPIRING_SOON -> users.sortedWith(
                compareBy<UserProfile> {
                    if (!it.isAdmin && MembershipDisplay.of(it, now).active)
                        it.planExpireMillis else Long.MAX_VALUE
                }.thenByDescending { it.createdAt }
            )
        }
}
