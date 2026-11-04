package com.example.auth

import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseAuth

/**
 * Owner-only Firebase custom claim: pickupGuardOwner=true.
 * This claim must be assigned by a privileged Firebase Admin SDK, never by the app.
 * No valid claim, no current auth session or expired verification => fail closed.
 */
object PickupGuardOwnerAccess {
    private val lease = OwnerClaimLease()

    fun authorized(profile: UserProfile): Boolean {
        if (!profile.isAdmin || !profile.isActive || profile.isBlocked ||
            profile.isDeviceBlacklisted || profile.uid.isBlank()) return false
        val current = FirebaseAuth.getInstance().currentUser?.uid
        val now = System.currentTimeMillis()
        return profile.isAdmin && profile.isActive && !profile.isBlocked &&
            !profile.isDeviceBlacklisted && profile.uid.isNotBlank() &&
            profile.uid == current && lease.valid(current, now)
    }

    fun refresh(profile: UserProfile, onResult: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (!profile.isAdmin || !profile.isActive || profile.isBlocked ||
            profile.isDeviceBlacklisted || user == null || user.uid != profile.uid) {
            clear()
            onResult(false)
            return
        }
        val request = lease.begin(profile.uid, System.currentTimeMillis())
        if (request == null) { onResult(authorized(profile)); return }
        user.getIdToken(true)
            .addOnSuccessListener { token ->
                lease.complete(request, token.claims["pickupGuardOwner"] == true,
                    FirebaseAuth.getInstance().currentUser?.uid, System.currentTimeMillis())
                onResult(authorized(profile))
            }
            .addOnFailureListener {
                lease.complete(request, false, FirebaseAuth.getInstance().currentUser?.uid, System.currentTimeMillis())
                onResult(false)
            }
    }

    fun clear() { lease.clear() }
}
