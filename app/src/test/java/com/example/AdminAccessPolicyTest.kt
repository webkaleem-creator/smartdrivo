package com.example

import com.example.auth.AdminAccessPolicy
import com.example.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

class AdminAccessPolicyTest {
    private val owner = UserProfile(uid = "owner", isAdmin = true)
    @Test fun onlyCurrentActiveVerifiedOwnerCanManage() {
        assertTrue(AdminAccessPolicy.canManage(owner, "owner", true))
        assertFalse(AdminAccessPolicy.canManage(owner, "owner", false))
        assertFalse(AdminAccessPolicy.canManage(owner, "different", true))
        assertFalse(AdminAccessPolicy.canManage(owner, null, true))
        for (denied in listOf(owner.copy(isAdmin = false), owner.copy(isActive = false),
            owner.copy(isBlocked = true), owner.copy(isDeviceBlacklisted = true),
            owner.copy(isDeviceAuthorized = false))) {
            assertFalse(AdminAccessPolicy.canManage(denied, "owner", true))
        }
    }
}
