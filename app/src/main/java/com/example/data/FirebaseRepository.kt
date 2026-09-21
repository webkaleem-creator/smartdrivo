package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.AreaGroup
import com.example.model.AreaType
import com.example.model.MembershipPlan
import com.example.model.OrderHistoryItem
import com.example.model.PaymentStatus
import com.example.model.PaymentSubmission
import com.example.model.UserProfile
import com.example.model.VehicleType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    private var ownUserListener: ListenerRegistration? = null
    private var ownPaymentsListener: ListenerRegistration? = null
    private var adminUsersListener: ListenerRegistration? = null
    private var adminPaymentsListener: ListenerRegistration? = null

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

    private fun userFromDocument(doc: DocumentSnapshot): UserProfile {
        val vehicle = try {
            com.example.model.VehicleType.valueOf(
                (doc.getString("vehicleType") ?: "AUTO").uppercase()
            )
        } catch (_: Exception) {
            com.example.model.VehicleType.AUTO
        }

        return UserProfile(
            uid = doc.id,
            name = doc.getString("name") ?: "",
            email = doc.getString("email") ?: "",
            phone = doc.getString("phone")
                ?: doc.getString("mobile")
                ?: doc.getString("mobileNumber")
                ?: "",
            city = doc.getString("city") ?: "",
            state = doc.getString("state") ?: "",
            vehicleType = vehicle,
            plan = doc.getString("plan") ?: "NONE",
            planPrice = doc.getLong("planPrice")?.toInt() ?: 0,
            planExpireMillis = doc.getLong("planExpireMillis") ?: 0L,
            isApproved = doc.getBoolean("isApproved") ?: false,
            isAdmin = doc.getBoolean("isAdmin") ?: false,
            isActive = doc.getBoolean("isActive") ?: true,
            referralCode = doc.getString("referralCode") ?: "",
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun paymentFromDocument(doc: DocumentSnapshot): PaymentSubmission {
        val status = try {
            com.example.model.PaymentStatus.valueOf(
                (doc.getString("status") ?: "PENDING").uppercase()
            )
        } catch (_: Exception) {
            com.example.model.PaymentStatus.PENDING
        }

        return PaymentSubmission(
            paymentId = doc.id,
            uid = doc.getString("uid") ?: "",
            userName = doc.getString("userName") ?: "",
            utrNumber = doc.getString("utrNumber") ?: "",
            planSelected = doc.getString("planSelected") ?: "7DAYS",
            amount = doc.getLong("amount")?.toInt() ?: 0,
            status = status,
            submittedAt = doc.getLong("submittedAt") ?: 0L,
            approvedAt = doc.getLong("approvedAt"),
            note = doc.getString("note") ?: ""
        )
    }

    fun startOwnMembershipSync() {
        val uid = auth?.currentUser?.uid ?: return
        val fs = firestore ?: return

        ownUserListener?.remove()
        ownPaymentsListener?.remove()

        ownUserListener = fs.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseRepo", "Own membership listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val remote = userFromDocument(snapshot)
                    val local = prefs.userProfile.value

                    val merged = remote.copy(
                        name = remote.name.ifBlank { local.name },
                        email = remote.email.ifBlank { local.email },
                        phone = remote.phone.ifBlank { local.phone },
                        city = remote.city.ifBlank { local.city },
                        state = remote.state.ifBlank { local.state },
                        referralCode = remote.referralCode.ifBlank { local.referralCode }
                    )

                    prefs.saveUserProfile(merged)

                    if (!merged.isPlanValid && !merged.isAdmin) {
                        prefs.setAutoAcceptActive(false)
                    }
                }
            }

        ownPaymentsListener = fs.collection("payments")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseRepo", "Own payments listener error: ${error.message}")
                    return@addSnapshotListener
                }

                prefs.setPaymentSubmissions(
                    snapshot?.documents
                        ?.map { paymentFromDocument(it) }
                        ?.sortedByDescending { it.submittedAt }
                        ?: emptyList()
                )
            }
    }

    fun stopOwnMembershipSync() {
        ownUserListener?.remove()
        ownPaymentsListener?.remove()
        ownUserListener = null
        ownPaymentsListener = null
    }

    fun startAdminBackendSync() {
        val fs = firestore ?: return
        if (!prefs.userProfile.value.isAdmin) return

        adminUsersListener?.remove()
        adminPaymentsListener?.remove()

        adminUsersListener = fs.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseRepo", "Admin users listener error: ${error.message}")
                    return@addSnapshotListener
                }

                prefs.setAllUsers(
                    snapshot?.documents
                        ?.map { userFromDocument(it) }
                        ?.sortedByDescending { it.createdAt }
                        ?: emptyList()
                )
            }

        adminPaymentsListener = fs.collection("payments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseRepo", "Admin payments listener error: ${error.message}")
                    return@addSnapshotListener
                }

                prefs.setPaymentSubmissions(
                    snapshot?.documents
                        ?.map { paymentFromDocument(it) }
                        ?.sortedByDescending { it.submittedAt }
                        ?: emptyList()
                )
            }
    }

    fun stopAdminBackendSync() {
        adminUsersListener?.remove()
        adminPaymentsListener?.remove()
        adminUsersListener = null
        adminPaymentsListener = null
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
                val isActive = doc.getBoolean("isActive") ?: true
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
        val fs = firestore
        val authUid = auth?.currentUser?.uid

        if (fs == null || authUid.isNullOrBlank()) {
            Log.e("FirebaseRepo", "Payment submit blocked: Firebase/Auth unavailable")
            onComplete(false)
            return
        }

        val cleanUtr = submission.utrNumber.trim()
        if (cleanUtr.length != 12 || !cleanUtr.all { it.isDigit() }) {
            Log.e("FirebaseRepo", "Payment submit blocked: invalid 12-digit UTR")
            onComplete(false)
            return
        }

        val normalized = submission.copy(
            uid = authUid,
            utrNumber = cleanUtr,
            status = com.example.model.PaymentStatus.PENDING,
            approvedAt = null
        )

        scope.launch {
            try {
                fs.collection("payments").document(normalized.paymentId).set(
                    mapOf(
                        "paymentId" to normalized.paymentId,
                        "uid" to normalized.uid,
                        "userName" to normalized.userName,
                        "utrNumber" to normalized.utrNumber,
                        "planSelected" to normalized.planSelected,
                        "amount" to normalized.amount,
                        "status" to "PENDING",
                        "submittedAt" to normalized.submittedAt,
                        "approvedAt" to null,
                        "note" to normalized.note
                    )
                ).await()

                prefs.addPaymentSubmission(normalized)

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                Log.e("FirebaseRepo", "Payment submission FAILED: ${e.message}")
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun adminApprovePayment(
        submission: PaymentSubmission,
        onComplete: (Boolean, String) -> Unit
    ) {
        val fs = firestore
        val adminUid = auth?.currentUser?.uid

        if (fs == null || adminUid.isNullOrBlank() || !prefs.userProfile.value.isAdmin) {
            onComplete(false, "Admin authorization unavailable")
            return
        }

        val cleanUtr = submission.utrNumber.trim()
        if (cleanUtr.length != 12 || !cleanUtr.all { it.isDigit() }) {
            onComplete(false, "UTR must be exactly 12 numeric digits")
            return
        }

        val daysToAdd = when (submission.planSelected.uppercase()) {
            "3DAYS" -> 3
            "7DAYS" -> 7
            "15DAYS" -> 15
            "1MONTH", "30DAYS" -> 30
            else -> 7
        }

        scope.launch {
            try {
                val paymentRef = fs.collection("payments").document(submission.paymentId)
                val userRef = fs.collection("users").document(submission.uid)
                val utrRef = fs.collection("paymentUtrs").document(cleanUtr)
                val now = System.currentTimeMillis()

                fs.runTransaction { tx ->
                    val paymentDoc = tx.get(paymentRef)
                    if (!paymentDoc.exists()) {
                        throw IllegalStateException("Payment record not found")
                    }

                    if ((paymentDoc.getString("status") ?: "PENDING") == "APPROVED") {
                        throw IllegalStateException("Payment already approved")
                    }

                    val utrDoc = tx.get(utrRef)
                    if (utrDoc.exists()) {
                        throw IllegalStateException("This UTR was already approved")
                    }

                    val userDoc = tx.get(userRef)
                    if (!userDoc.exists()) {
                        throw IllegalStateException("Driver account not found")
                    }

                    val oldExpiry = userDoc.getLong("planExpireMillis") ?: 0L
                    val base = if (oldExpiry > now) oldExpiry else now
                    val newExpiry = base + (daysToAdd * 86400000L)

                    tx.update(
                        paymentRef,
                        mapOf(
                            "status" to "APPROVED",
                            "approvedAt" to now,
                            "approvedBy" to adminUid
                        )
                    )

                    tx.set(
                        userRef,
                        mapOf(
                            "plan" to submission.planSelected,
                            "planPrice" to submission.amount,
                            "planExpireMillis" to newExpiry,
                            "isApproved" to true,
                            "isActive" to true,
                            "updatedAt" to now
                        ),
                        SetOptions.merge()
                    )

                    tx.set(
                        utrRef,
                        mapOf(
                            "utrNumber" to cleanUtr,
                            "paymentId" to submission.paymentId,
                            "uid" to submission.uid,
                            "approvedBy" to adminUid,
                            "approvedAt" to now
                        )
                    )
                }.await()

                prefs.updatePaymentStatus(
                    submission.paymentId,
                    com.example.model.PaymentStatus.APPROVED
                )

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(true, "Membership activated for $daysToAdd days")
                }
            } catch (e: Exception) {
                Log.e("FirebaseRepo", "Admin approval failed: ${e.message}")
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(false, e.message ?: "Approval failed")
                }
            }
        }
    }

    fun adminRejectPayment(
        submission: PaymentSubmission,
        onComplete: (Boolean, String) -> Unit
    ) {
        val fs = firestore
        val adminUid = auth?.currentUser?.uid

        if (fs == null || adminUid.isNullOrBlank() || !prefs.userProfile.value.isAdmin) {
            onComplete(false, "Admin authorization unavailable")
            return
        }

        scope.launch {
            try {
                fs.collection("payments").document(submission.paymentId).update(
                    mapOf(
                        "status" to "REJECTED",
                        "approvedAt" to null,
                        "rejectedAt" to System.currentTimeMillis(),
                        "rejectedBy" to adminUid
                    )
                ).await()

                prefs.updatePaymentStatus(
                    submission.paymentId,
                    com.example.model.PaymentStatus.REJECTED
                )

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(true, "Payment rejected")
                }
            } catch (e: Exception) {
                Log.e("FirebaseRepo", "Admin rejection failed: ${e.message}")
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(false, e.message ?: "Rejection failed")
                }
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
                        "minFare" to a.minFare,
                        // Keep legacy field for older data readers.
                        "maxFare" to a.maxFare,
                        "minPickupKm" to a.minPickupKm,
                        "maxPickupKm" to a.maxPickupKm,
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

    // --- Realtime Firestore Sync for Admin (Users & Payments) ---
    fun listenToAllUsers(onUsersChanged: ((List<UserProfile>) -> Unit)? = null): ListenerRegistration? {
        val fs = firestore ?: return null
        return try {
            fs.collection("users").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseRepo", "Realtime listen to /users error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<UserProfile>()
                    for (doc in snapshot.documents) {
                        try {
                            val uid = doc.id
                            val name = doc.getString("name") ?: ""
                            val email = doc.getString("email") ?: ""
                            val phone = doc.getString("phone") ?: doc.getString("mobile") ?: ""
                            val city = doc.getString("city") ?: ""
                            val state = doc.getString("state") ?: ""
                            val vehicleStr = doc.getString("vehicleType") ?: "AUTO"
                            val plan = doc.getString("plan") ?: "7DAYS"
                            val planPrice = doc.getLong("planPrice")?.toInt() ?: 129
                            val planExpireMillis = doc.getLong("planExpireMillis") ?: System.currentTimeMillis()
                            val isApproved = doc.getBoolean("isApproved") ?: false
                            val isAdmin = doc.getBoolean("isAdmin") ?: false
                            val isActive = doc.getBoolean("isActive") ?: true
                            val referralCode = doc.getString("referralCode") ?: "SMART50"
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                            list.add(
                                UserProfile(
                                    uid = uid,
                                    name = name,
                                    email = email,
                                    phone = phone,
                                    city = city,
                                    state = state,
                                    vehicleType = VehicleType.fromString(vehicleStr),
                                    plan = plan,
                                    planPrice = planPrice,
                                    planExpireMillis = planExpireMillis,
                                    isApproved = isApproved,
                                    isAdmin = isAdmin,
                                    isActive = isActive,
                                    referralCode = referralCode,
                                    createdAt = createdAt
                                )
                            )
                        } catch (e: Exception) {
                            Log.w("FirebaseRepo", "Error parsing user doc ${doc.id}: ${e.message}")
                        }
                    }
                    prefs.setAllUsers(list)
                    onUsersChanged?.invoke(list)
                }
            }
        } catch (e: Exception) {
            Log.w("FirebaseRepo", "Failed to attach users listener: ${e.message}")
            null
        }
    }

    fun listenToAllPayments(onPaymentsChanged: ((List<PaymentSubmission>) -> Unit)? = null): ListenerRegistration? {
        val fs = firestore ?: return null
        return try {
            fs.collection("payments").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseRepo", "Realtime listen to /payments error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<PaymentSubmission>()
                    for (doc in snapshot.documents) {
                        try {
                            val paymentId = doc.getString("paymentId") ?: doc.id
                            val uid = doc.getString("uid") ?: ""
                            val userName = doc.getString("userName") ?: ""
                            val utrNumber = doc.getString("utrNumber") ?: ""
                            val planSelected = doc.getString("planSelected") ?: "7DAYS"
                            val amount = doc.getLong("amount")?.toInt() ?: 129
                            val statusStr = doc.getString("status") ?: "PENDING"
                            val status = try { PaymentStatus.valueOf(statusStr) } catch (e: Exception) { PaymentStatus.PENDING }
                            val submittedAt = doc.getLong("submittedAt") ?: System.currentTimeMillis()
                            val approvedAt = doc.getLong("approvedAt")
                            val note = doc.getString("note") ?: ""

                            list.add(
                                PaymentSubmission(
                                    paymentId = paymentId,
                                    uid = uid,
                                    userName = userName,
                                    utrNumber = utrNumber,
                                    planSelected = planSelected,
                                    amount = amount,
                                    status = status,
                                    submittedAt = submittedAt,
                                    approvedAt = approvedAt,
                                    note = note
                                )
                            )
                        } catch (e: Exception) {
                            Log.w("FirebaseRepo", "Error parsing payment doc ${doc.id}: ${e.message}")
                        }
                    }
                    val sorted = list.sortedByDescending { it.submittedAt }
                    prefs.setPaymentSubmissions(sorted)
                    onPaymentsChanged?.invoke(sorted)
                }
            }
        } catch (e: Exception) {
            Log.w("FirebaseRepo", "Failed to attach payments listener: ${e.message}")
            null
        }
    }

    fun adminToggleUserActiveStatus(uid: String, currentActive: Boolean, onComplete: ((Boolean) -> Unit)? = null) {
        val newActive = !currentActive
        prefs.toggleUserActiveStatus(uid)
        scope.launch {
            try {
                firestore?.collection("users")?.document(uid)?.update(
                    mapOf(
                        "isActive" to newActive,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )?.await()
                onComplete?.invoke(true)
            } catch (e: Exception) {
                Log.w("FirebaseRepo", "Firestore toggleUserActiveStatus failed: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    fun adminApprovePayment(submission: PaymentSubmission, daysToAdd: Int, onComplete: ((Boolean) -> Unit)? = null) {
        prefs.updatePaymentStatus(submission.paymentId, PaymentStatus.APPROVED)
        prefs.extendUserPlan(submission.uid, daysToAdd, submission.planSelected, submission.amount)
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val fs = firestore ?: return@launch

                // 1. Update payments collection in Firestore
                fs.collection("payments").document(submission.paymentId).update(
                    mapOf(
                        "status" to PaymentStatus.APPROVED.name,
                        "approvedAt" to now
                    )
                ).await()

                // 2. Extend driver plan in users collection in Firestore
                if (submission.uid.isNotBlank()) {
                    val userDoc = fs.collection("users").document(submission.uid).get().await()
                    val currentExpiry = userDoc.getLong("planExpireMillis") ?: 0L
                    val baseTime = if (currentExpiry > now) currentExpiry else now
                    val newExpiry = baseTime + (daysToAdd * 86400000L)

                    fs.collection("users").document(submission.uid).update(
                        mapOf(
                            "plan" to submission.planSelected,
                            "planPrice" to submission.amount,
                            "planExpireMillis" to newExpiry,
                            "isApproved" to true,
                            "isActive" to true,
                            "updatedAt" to now
                        )
                    ).await()
                }
                onComplete?.invoke(true)
            } catch (e: Exception) {
                Log.w("FirebaseRepo", "Firestore adminApprovePayment failed: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    fun adminRejectPayment(paymentId: String, onComplete: ((Boolean) -> Unit)? = null) {
        prefs.updatePaymentStatus(paymentId, PaymentStatus.REJECTED)
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                firestore?.collection("payments")?.document(paymentId)?.update(
                    mapOf(
                        "status" to PaymentStatus.REJECTED.name,
                        "rejectedAt" to now
                    )
                )?.await()
                onComplete?.invoke(true)
            } catch (e: Exception) {
                Log.w("FirebaseRepo", "Firestore adminRejectPayment failed: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    fun adminExtendUserPlan(uid: String, daysToAdd: Int, planLabel: String, price: Int, onComplete: ((Boolean) -> Unit)? = null) {
        prefs.extendUserPlan(uid, daysToAdd, planLabel, price)
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val fs = firestore ?: return@launch
                val userDoc = fs.collection("users").document(uid).get().await()
                val currentExpiry = userDoc.getLong("planExpireMillis") ?: 0L
                val baseTime = if (currentExpiry > now) currentExpiry else now
                val newExpiry = baseTime + (daysToAdd * 86400000L)

                fs.collection("users").document(uid).update(
                    mapOf(
                        "plan" to planLabel,
                        "planPrice" to price,
                        "planExpireMillis" to newExpiry,
                        "isApproved" to true,
                        "isActive" to true,
                        "updatedAt" to now
                    )
                ).await()
                onComplete?.invoke(true)
            } catch (e: Exception) {
                Log.w("FirebaseRepo", "Firestore adminExtendUserPlan failed: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }
}
