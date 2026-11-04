package com.example.auth

import com.example.model.UserProfile

/** Local defense in depth. Firestore must independently authorize every remote operation. */
object AdminAccessPolicy {
    fun canManage(profile: UserProfile, authenticatedUid: String?, ownerVerified: Boolean): Boolean =
        !authenticatedUid.isNullOrBlank() && profile.uid == authenticatedUid &&
            profile.isAdmin && profile.isActive && !profile.isBlocked &&
            !profile.isDeviceBlacklisted && profile.isDeviceAuthorized && ownerVerified
}
