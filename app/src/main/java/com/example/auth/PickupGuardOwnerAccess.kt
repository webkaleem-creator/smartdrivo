package com.example.auth

import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseAuth

/**
 * Owner-only Firebase custom claim: pickupGuardOwner=true.
 * This claim must be assigned by a privileged Firebase Admin SDK, never by the app.
 * No valid claim, no current auth session or expired verification => fail closed.
 */
object PickupGuardOwnerAccess {
    @Volatile private var verifiedUid: String? = null
    @Volatile private var verifiedAtMs = 0L
    private const val VALID_MS = 10 * 60 * 1000L

    fun authorized(profile: UserProfile): Boolean {
        val current = FirebaseAuth.getInstance().currentUser?.uid
        val now = System.currentTimeMillis()
        return profile.isAdmin && profile.isActive && !profile.isBlocked &&
            !profile.isDeviceBlacklisted && profile.uid.isNotBlank() &&
            profile.uid == current && profile.uid == verifiedUid &&
            verifiedAtMs > 0L && now >= verifiedAtMs && now - verifiedAtMs < VALID_MS
    }

    fun refresh(profile: UserProfile, onResult: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (!profile.isAdmin || !profile.isActive || profile.isBlocked ||
            profile.isDeviceBlacklisted || user == null || user.uid != profile.uid) {
            clear()
            onResult(false)
            return
        }
        user.getIdToken(true)
            .addOnSuccessListener { token ->
                val verified = token.claims["pickupGuardOwner"] == true &&
                    FirebaseAuth.getInstance().currentUser?.uid == profile.uid
                if (verified) {
                    verifiedUid = profile.uid
                    verifiedAtMs = System.currentTimeMillis()
                } else clear()
                onResult(verified && authorized(profile))
            }
            .addOnFailureListener {
                clear()
                onResult(false)
            }
    }

    fun clear() {
        verifiedUid = null
        verifiedAtMs = 0L
    }
}
