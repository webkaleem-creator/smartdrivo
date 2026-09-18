package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.AreaGroup
import com.example.model.AreaType
import com.example.model.MembershipPlan
import com.example.model.OrderHistoryItem
import com.example.model.PaymentSubmission
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseRepository(
    private val context: Context,
    private val prefs: PreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null
    private var realtimeDb: FirebaseDatabase? = null
    private var auth: FirebaseAuth? = null

    init {
        try {
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            realtimeDb = FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            Log.w("FirebaseRepo", "Firebase not initialized or missing configuration: ${e.message}")
        }
    }

    fun getCurrentUid(): String {
        return auth?.currentUser?.uid ?: prefs.userProfile.value.uid.ifEmpty {
            val generated = UUID.randomUUID().toString()
            prefs.saveUserProfile(prefs.userProfile.value.copy(uid = generated))
            generated
        }
    }

    // --- User Profile Sync ---
    fun saveUserProfile(profile: UserProfile, onComplete: ((Boolean) -> Unit)? = null) {
        prefs.saveUserProfile(profile)
        scope.launch {
            try {
                // Client updates MUST NOT write or overwrite privileged authority fields:
                // isAdmin, isApproved, isActive, plan, planPrice, planExpireMillis
                val safeClientMap = mapOf(
                    "uid" to profile.uid,
                    "name" to profile.name,
                    "email" to profile.email,
                    "phone" to profile.phone,
                    "mobile" to profile.phone,
                    "mobileNumber" to profile.phone,
                    "city" to profile.city,
                    "state" to profile.state,
                    "vehicleType" to profile.vehicleType.name,
                    "referralCode" to profile.referralCode,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore?.collection("users")?.document(profile.uid)?.set(
                    safeClientMap,
                    SetOptions.merge()
                )?.await()
                onComplete?.invoke(true)
            } catch (e: Exception) {
                Log.e("FirebaseRepo", "Failed to sync user: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    // Privileged update: used when verified admin updates users from AdminPanel
    fun adminUpdateUserProfile(profile: UserProfile, onComplete: ((Boolean) -> Unit)? = null) {
        val currentCaller = prefs.userProfile.value
        if (!currentCaller.isAdmin) {
            Log.e("FirebaseRepo", "Unauthorized attempt to write privileged fields")
            onComplete?.invoke(false)
            return
        }
        scope.launch {
            try {
                firestore?.collection("users")?.document(profile.uid)?.set(
                    mapOf(
                        "uid" to profile.uid,
                        "name" to profile.name,
                        "email" to profile.email,
                        "phone" to profile.phone,
                        "city" to profile.city,
                        "state" to profile.state,
                        "vehicleType" to profile.vehicleType.name,
                        "plan" to profile.plan,
                        "planPrice" to profile.planPrice,
                        "planExpireMillis" to profile.planExpireMillis,
                        "isApproved" to profile.isApproved,
                        "isAdmin" to profile.isAdmin,
                        "isActive" to profile.isActive,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )?.await()
                onComplete?.invoke(true)
            } catch (e: Exception) {
                Log.e("FirebaseRepo", "Admin user update failed: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    suspend fun fetchFirestoreUserProfile(uid: String, email: String = ""): UserProfile? {
        val fs = firestore ?: return null
        return try {
            var doc = if (uid.isNotBlank()) fs.collection("users").document(uid).get().await() else null
            if ((doc == null || !doc.exists()) && email.isNotBlank()) {
                val query = fs.collection("users").whereEqualTo("email", email).limit(1).get().await()
                if (!query.isEmpty) {
                    doc = query.documents.first()
                }
            }
            if (doc != null && doc.exists()) {
                val phone = doc.getString("phone")
                    ?: doc.getString("mobile")
                    ?: doc.getString("mobileNumber")
                    ?: ""
                val city = doc.getString("city") ?: ""
                val state = doc.getString("state") ?: ""
                val name = doc.getString("name") ?: ""
                val userEmail = doc.getString("email") ?: email
                val plan = doc.getString("plan") ?: "NONE"
                val planPrice = doc.getLong("planPrice")?.toInt() ?: 0
                val planExpireMillis = doc.getLong("planExpireMillis") ?: 0L
                val isApproved = doc.getBoolean("isApproved") ?: false
                val isAdmin = doc.getBoolean("isAdmin") ?: false
                val isActive = doc.getBoolean("isActive") ?: false
                val referralCode = doc.getString("referralCode") ?: ""
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val vehicleTypeStr = doc.getString("vehicleType") ?: "AUTO"
                val vehicleType = try {
                    com.example.model.VehicleType.valueOf(vehicleTypeStr)
                } catch (_: Exception) {
                    com.example.model.VehicleType.AUTO
                }
                UserProfile(
                    uid = doc.id.ifEmpty { uid },
                    name = name,
                    email = userEmail,
                    phone = phone,
                    city = city,
                    state = state,
                    vehicleType = vehicleType,
                    plan = plan,
                    planPrice = planPrice,
                    planExpireMillis = planExpireMillis,
                    isApproved = isApproved,
                    isAdmin = isAdmin,
                    isActive = isActive,
                    referralCode = referralCode,
                    createdAt = createdAt
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("FirebaseRepo", "fetchFirestoreUserProfile failed: ${e.message}")
            null
        }
    }

    fun checkUserProfileFromFirestore(
        uid: String,
        email: String,
        onResult: (UserProfile?) -> Unit
    ) {
        scope.launch {
            val profile = fetchFirestoreUserProfile(uid, email)
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onResult(profile)
            }
        }
    }

    // --- Order History Sync ---
    fun recordOrderHistory(item: OrderHistoryItem) {
        prefs.addOrderHistory(item)
        val uid = getCurrentUid()
        scope.launch {
            try {
                val id = item.id.ifEmpty { UUID.randomUUID().toString() }
                firestore?.collection("orders")?.document(uid)?.collection("history")
                    ?.document(id)?.set(
                        mapOf(
                            "timestamp" to item.timestamp,
                            "dateStr" to item.dateStr,
                            "timeStr" to item.timeStr,
                            "status" to item.status.name,
                            "platform" to item.platform.name,
                            "vehicleType" to item.vehicleType.name,
                            "pickupDistKm" to item.pickupDistKm,
                            "dropDistKm" to item.dropDistKm,
                            "pickupAddress" to item.pickupAddress,
                            "dropAddress" to item.dropAddress,
                            "dropArea" to item.dropArea,
                            "amount" to item.amount,
                            "bookingId" to item.bookingId,
                            "detectionTimeMs" to item.detectionTimeMs,
                            "clickTimeMs" to item.clickTimeMs,
                            "baseFare" to item.baseFare,
                            "tipAmount" to item.tipAmount,
                            "timesClicked" to item.timesClicked
                        )
                    )?.await()
            } catch (e: Exception) {
                Log.w("FirebaseRepo", "Order history firestore write skipped: ${e.message}")
            }
        }
    }

    // --- Payment Submission Sync ---
    fun submitPayment(submission: PaymentSubmission, onComplete: (Boolean) -> Unit) {
        val accepted = prefs.addPaymentSubmission(submission)
        if (!accepted) {
            onComplete(false)
            return
        }
        scope.launch {
            try {
                firestore?.collection("payments")?.document(submission.paymentId)?.set(
                    mapOf(
                        "paymentId" to submission.paymentId,
                        "uid" to submission.uid,
                        "userName" to submission.userName,
                        "utrNumber" to submission.utrNumber,
                        "planSelected" to submission.planSelected,
                        "amount" to submission.amount,
                        "status" to submission.status.name,
                        "submittedAt" to submission.submittedAt,
                        "approvedAt" to submission.approvedAt
                    )
                )?.await()
                onComplete(true)
            } catch (e: Exception) {
                Log.w("FirebaseRepo", "Payment submission firestore fallback: ${e.message}")
                // Still notify success locally
                onComplete(true)
            }
        }
    }

    // --- Area Groups Sync ---
    fun syncAreas(areas: List<AreaGroup>) {
        prefs.saveAreaGroups(areas)
        val uid = getCurrentUid()
        scope.launch {
            try {
                val db = realtimeDb ?: return@launch
                val goToRef = db.getReference("areas/$uid/goTo")
                val noGoRef = db.getReference("areas/$uid/noGo")

                val goToMap = mutableMapOf<String, Any>()
                val noGoMap = mutableMapOf<String, Any>()

                for (a in areas) {
                    val map = mapOf(
                        "id" to a.id,
                        "name" to a.name,
                        "isEnabled" to a.isEnabled,
                        "minPickupKm" to a.minPickupKm,
                        "maxDropKm" to a.maxDropKm,
                        "keywords" to a.keywords
                    )
                    if (a.type == AreaType.GO_TO) {
                        goToMap[a.id] = map
                    } else {
                        noGoMap[a.id] = map
                    }
                }
                goToRef.setValue(goToMap)
                noGoRef.setValue(noGoMap)
            } catch (e: Exception) {
                Log.w("FirebaseRepo", "Realtime DB area sync skipped: ${e.message}")
            }
        }
    }

    // --- Realtime / Remote Plan & Settings Sync ---
    fun fetchGlobalSettings() {
        scope.launch {
            try {
                val doc = firestore?.collection("settings")?.document("global")?.get()?.await()
                if (doc != null && doc.exists()) {
                    doc.getString("upiId")?.let { if (it.isNotEmpty()) prefs.updateUpiId(it) }
                    doc.getString("qrImageUrl")?.let { prefs.updateQrImageUrl(it) }
                    val wa = doc.getString("whatsapp") ?: "https://chat.whatsapp.com/smartdrivo"
                    val tg = doc.getString("telegram") ?: "https://t.me/smartdrivo_riders"
                    val ig = doc.getString("instagram") ?: "https://instagram.com/smartdrivo"
                    prefs.updateCommunityLinks(wa, tg, ig)
                }
            } catch (e: Exception) {
                Log.w("FirebaseRepo", "Global settings fetch skipped: ${e.message}")
            }
        }
    }

    fun writeDebugLog(message: String) {
        val uid = getCurrentUid()
        scope.launch {
            try {
                val logRef = realtimeDb?.getReference("logs/$uid")?.push()
                logRef?.setValue(
                    mapOf(
                        "timestamp" to System.currentTimeMillis(),
                        "message" to message
                    )
                )
            } catch (e: Exception) {
                // Ignore log failures
            }
        }
    }
}
