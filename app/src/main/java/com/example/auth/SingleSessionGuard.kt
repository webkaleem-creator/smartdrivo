package com.example.auth

import com.example.data.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.tasks.await
import java.util.UUID

// SECURITY_V2A_ONE_ACTIVE_PHONE
object SingleSessionGuard {
    private var listener: ListenerRegistration? = null
    private var watchedUid: String = ""
    private var kicked = false

    fun stop() {
        listener?.remove()
        listener = null
        watchedUid = ""
        kicked = false
    }

    private fun newSessionId() =
        "SDS-" + UUID.randomUUID().toString()

    fun claimFresh(
        prefs: PreferencesManager,
        callback: (Boolean) -> Unit
    ) {
        stop()
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            callback(false)
            return
        }

        val sessionId = newSessionId()
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .update(
                mapOf(
                    "activeSessionId" to sessionId,
                    "activeSessionAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnCompleteListener { task ->
                val sameUid =
                    FirebaseAuth.getInstance().currentUser?.uid == uid

                if (task.isSuccessful && sameUid) {
                    prefs.markLoginSessionActive(sessionId)
                    callback(true)
                } else {
                    callback(false)
                }
            }
    }

    suspend fun restoreExisting(prefs: PreferencesManager): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank() || prefs.userProfile.value.uid != uid) {
            return false
        }

        if (prefs.currentLoginSessionId.isNotBlank()) {
            return true
        }

        // One-time upgrade: claim only if no session exists yet.
        val sessionId = newSessionId()
        val ref = FirebaseFirestore.getInstance()
            .collection("users").document(uid)

        return try {
            val claimed = FirebaseFirestore.getInstance()
                .runTransaction { tx ->
                    val snap = tx.get(ref)
                    if (!snap.exists() ||
                        !snap.getString("activeSessionId").isNullOrBlank()) {
                        false
                    } else {
                        tx.update(
                            ref,
                            mapOf(
                                "activeSessionId" to sessionId,
                                "activeSessionAt" to FieldValue.serverTimestamp()
                            )
                        )
                        true
                    }
                }.await()

            if (claimed &&
                FirebaseAuth.getInstance().currentUser?.uid == uid) {
                prefs.markLoginSessionActive(sessionId)
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun watch(prefs: PreferencesManager, onReplaced: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val localId = prefs.currentLoginSessionId
        if (uid.isBlank() || localId.isBlank()) {
            prefs.setAutoAcceptActive(false)
            onReplaced()
            return
        }

        if (listener != null && watchedUid == uid) return

        stop()
        watchedUid = uid

        listener = FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
                if (watchedUid != uid || kicked) {
                    return@addSnapshotListener
                }

                if (error != null || snap == null ||
                    snap.metadata.isFromCache) {
                    return@addSnapshotListener
                }

                if (!snap.exists() ||
                    snap.getString("activeSessionId") != localId) {
                    kicked = true
                    prefs.setAutoAcceptActive(false)
                    prefs.invalidateLoginSession(
                        "This Gmail was signed in on another phone."
                    )
                    stop()
                    onReplaced()
                }
            }
    }
}