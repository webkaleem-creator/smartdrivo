package com.example

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.data.db.*
import com.example.engine.*
import com.example.model.*
import com.example.service.SmartDrivoAccessibilityService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import android.database.sqlite.SQLiteException
import android.os.Looper
import com.example.data.db.dao.RideHistoryDao
import com.example.service.OrderDecisionPopup
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSettings
import java.lang.reflect.Proxy
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicInteger

/** Exercises the production handler/queues with already-validated synthetic offer snapshots, not phone UI. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RapidoLiveHistoryDetectionTest {
    private lateinit var db: AppDatabase
    private lateinit var history: RideHistoryRepository
    private lateinit var prefs: PreferencesManager
    private lateinit var service: SmartDrivoAccessibilityService
    private lateinit var oldSettings: AppSettings
    private lateinit var oldProfile: UserProfile
    private val io = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val handler = SmartDrivoAccessibilityService::class.java.getDeclaredMethod("handleRapidoOrder",
        AccessibilityNodeInfo::class.java, RapidoPopupValidation::class.java, java.lang.Long.TYPE).apply { isAccessible = true }

    private fun set(name: String, value: Any) = SmartDrivoAccessibilityService::class.java.getDeclaredField(name).apply {
        isAccessible = true; set(service,value)
    }
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        history = RideHistoryRepository(db.rideHistoryDao(), io)
        prefs = PreferencesManager.getInstance(context)
        oldSettings = prefs.appSettings.value; oldProfile = prefs.userProfile.value
        prefs.saveAppSettings(oldSettings.copy(isAutoAcceptActive = true, showOrderReasonPopups = false,
            isAutoRejectBadFaresEnabled = false))
        prefs.saveUserProfile(UserProfile(uid = "synthetic-driver", isApproved = true,
            planExpireMillis = System.currentTimeMillis()+86_400_000L))
        service = Robolectric.buildService(SmartDrivoAccessibilityService::class.java).create().get()
        OrderDecisionPopup.pauseForBankingMode(); shadowOf(Looper.getMainLooper()).idle(); OrderDecisionPopup.resumeAfterBanking()
        set("prefs",prefs); set("rideHistoryRepository\$delegate",lazyOf(history))
        val remote = FirebaseRepository(context,prefs)
        // Explicitly disconnect all remote handles; these tests must never write production data.
        for (name in listOf("firestore","realtimeDb","auth")) FirebaseRepository::class.java.getDeclaredField(name).apply {
            isAccessible = true; set(remote,null)
        }
        set("repository",remote); set("isProcessing",true)
    }
    @After fun close() {
        val field = SmartDrivoAccessibilityService::class.java.getDeclaredField("serviceScope").apply { isAccessible = true }
        (field.get(service) as CoroutineScope).cancel()
        OrderDecisionPopup.pauseForBankingMode(); shadowOf(Looper.getMainLooper()).idle(); OrderDecisionPopup.resumeAfterBanking()
        io.close(); db.close(); prefs.saveAppSettings(oldSettings); prefs.saveUserProfile(oldProfile)
    }
    private fun detect(index: Int, same: Boolean = false) {
        val root = AccessibilityNodeInfo.obtain().apply {
            packageName = "com.rapido.passenger"; isVisibleToUser = true
            setBoundsInScreen(Rect(0,0,1080,2200))
        }
        val data = RapidoOrderData(totalFare=45f,baseFare=45f,pickupKm=.9f,dropKm=2.2f,
            dropAddress="Synthetic destination ${if (same) 1 else index}")
        handler.invoke(service,root,RapidoPopupValidation(true,fare=45f,pickupDistKm=.9f,orderData=data),1L)
    }
    private fun end() = SmartDrivoAccessibilityService::class.java.getDeclaredMethod("endRapidoPresentation",String::class.java)
        .apply { isAccessible = true }.invoke(service,"TEST_OFFER_DISAPPEARED")

    private fun legacy(candidate:RideCandidate, reason:String) = SmartDrivoAccessibilityService::class.java.getDeclaredMethod(
        "logOrderEvent",RideCandidate::class.java,OrderStatus::class.java,String::class.java,java.lang.Long.TYPE,java.lang.Integer.TYPE
    ).apply { isAccessible=true }.invoke(service,candidate,OrderStatus.IGNORED,reason,System.currentTimeMillis(),0)
    private fun register(candidate:RideCandidate):RapidoVisibleOffer.Session = SmartDrivoAccessibilityService::class.java.getDeclaredMethod(
        "detectRapidoOffer",RideCandidate::class.java,java.lang.Long.TYPE).apply { isAccessible=true }
        .invoke(service,candidate,1L) as RapidoVisibleOffer.Session
    private fun synthetic(index:Int)=RideCandidate(45f,.9f,2.2f,"Address unavailable","Synthetic legacy $index","Area",
        Platform.RAPIDO,VehicleType.AUTO,detectionTimeMs=System.currentTimeMillis())
    private fun activePopup() = OrderDecisionPopup::class.java.getDeclaredField("activeView").apply { isAccessible=true }.get(null)
    private fun failInserts(failCount:Int):AtomicInteger {
        val calls=AtomicInteger(); val real=db.rideHistoryDao()
        val proxy=Proxy.newProxyInstance(RideHistoryDao::class.java.classLoader,arrayOf(RideHistoryDao::class.java)) { _,method,args ->
            if(method.name=="insert" && calls.incrementAndGet()<=failCount) throw SQLiteException("Synthetic failure")
            try { method.invoke(real,*(args?:emptyArray())) } catch(error:InvocationTargetException) { throw error.cause!! }
        } as RideHistoryDao
        history=RideHistoryRepository(proxy,io); set("rideHistoryRepository\$delegate",lazyOf(history))
        return calls
    }
    @Test fun legacyFinalWaitsThroughTransientInsertFailureAndPublishesOnlyPersistedRow() = runBlocking {
        ShadowSettings.setCanDrawOverlays(true)
        prefs.saveAppSettings(prefs.appSettings.value.copy(showOrderReasonPopups=true))
        val calls=failInserts(1); val candidate=synthetic(1)
        register(candidate); legacy(candidate,"Persisted synthetic decision")
        withTimeout(5000) { while(calls.get()==0) yield() }
        shadowOf(Looper.getMainLooper()).idle(); assertNull(activePopup())
        val rows=withTimeout(5000) { history.allHistory.first { it.size==1 && it.single().status==OrderStatus.IGNORED.name } }
        withTimeout(5000) { while(activePopup()==null) { shadowOf(Looper.getMainLooper()).idle(); yield() } }
        assertEquals("Persisted synthetic decision",rows.single().decisionReasonText)
        assertEquals(2,calls.get())
    }
    @Test fun permanentInsertFailureNeverShowsLegacyDecisionPopupOrInventsARow() = runBlocking {
        ShadowSettings.setCanDrawOverlays(true)
        prefs.saveAppSettings(prefs.appSettings.value.copy(showOrderReasonPopups=true))
        val calls=failInserts(Int.MAX_VALUE); val candidate=synthetic(1)
        val session=register(candidate); legacy(candidate,"Must never display")
        val gate=SmartDrivoAccessibilityService::class.java.getDeclaredField("historyInserts").apply { isAccessible=true }.get(service) as HistoryInsertGate
        assertFalse(withTimeout(5000) { gate.await(session.id) })
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(activePopup()); assertEquals(3,calls.get()); assertTrue(db.rideHistoryDao().getAllHistoryList().isEmpty())
    }
    @Test fun legacyCallbackUpdatesOriginalSnapshotInsteadOfNewPlatformActiveRecord() = runBlocking {
        val first=synthetic(1); val old=register(first)
        val second=synthetic(2); val fresh=register(second)
        legacy(first,"Old snapshot final")
        val rows=withTimeout(5000) { history.allHistory.first { list -> list.size==2 &&
            list.any { it.id==old.id && it.decisionReasonText=="Old snapshot final" } } }
        assertEquals(OrderStatus.PROCESSING.name,rows.single { it.id==fresh.id }.status)
        end()
        withTimeout(5000) { history.allHistory.first { it.none { row -> row.status==OrderStatus.PROCESSING.name } } }
        Unit
    }
    @Test fun shieldRunsBeforeTripCompletionProcessingAndPickupGuardWithoutAcceptingUnreadableInput() = runBlocking {
        val state=PreferencesManager::class.java.getDeclaredField("_adminPickupBlockSettings").apply { isAccessible=true }
            .get(prefs) as kotlinx.coroutines.flow.MutableStateFlow<AdminPickupBlockSettings>
        val previous=state.value
        val guardState=PreferencesManager::class.java.getDeclaredField("_pickupGuardSettings").apply { isAccessible=true }
            .get(prefs) as kotlinx.coroutines.flow.MutableStateFlow<PickupGuardSettings>
        val previousGuard=guardState.value
        try {
            state.value=AdminPickupBlockSettings(true,listOf("tadbun"))
            guardState.value=previousGuard.copy(enabled=true)
            set("currentOlaState",SmartDrivoAccessibilityService.OlaState.OLA_DETECTED)
            val root=AccessibilityNodeInfo.obtain().apply { packageName="com.rapido.passenger";isVisibleToUser=true }
            val partial=RapidoOrderData(totalFare=45f,baseFare=45f,pickupKm=.9f,dropKm=null,dropAddress="Synthetic drop")
            handler.invoke(service,root,RapidoPopupValidation(true,fare=45f,pickupDistKm=.9f,orderData=partial),1L)
            val row=withTimeout(5000) { history.allHistory.first { it.size==1 && it.single().status==OrderStatus.IGNORED.name } }.single()
            assertEquals("ADMIN_PICKUP_OWNER_UNAVAILABLE",row.decisionReasonCode)
            assertEquals(0,row.timesClicked)
            val busy=SmartDrivoAccessibilityService::class.java.getDeclaredField("isProcessing").apply { isAccessible=true }
            val ola=SmartDrivoAccessibilityService::class.java.getDeclaredField("currentOlaState").apply { isAccessible=true }
            assertTrue(busy.getBoolean(service))
            assertEquals(SmartDrivoAccessibilityService.OlaState.OLA_DETECTED,ola.get(service))
        } finally { state.value=previous; guardState.value=previousGuard }
    }

    @Test fun tenRapidOffersSurviveProcessingLockAndDelayedInsertWithoutBlockingCaller() = runBlocking {
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        withContext(io) { } // ensure dispatcher is live
        val hold = CoroutineScope(io).launch { entered.countDown(); check(release.await(5,TimeUnit.SECONDS)) }
        assertTrue(entered.await(2,TimeUnit.SECONDS))
        try {
            repeat(10) { detect(it) }
            // Handler returned for all ten while inserts are still deliberately held.
            assertEquals(0,db.rideHistoryDao().getAllHistoryList().size)
            end()
        } finally { release.countDown() }
        hold.join()
        val rows = withTimeout(5000) { history.allHistory.first { rows ->
            rows.size == 10 && rows.all { it.status != OrderStatus.PROCESSING.name }
        } }
        assertEquals(10,rows.map { it.id }.toSet().size)
        assertEquals(10,rows.map { it.dropAddress }.toSet().size)
        assertTrue(rows.all { it.timesClicked == 0 && it.status == OrderStatus.IGNORED.name })
    }
    @Test fun twentyRepeatedEventsStillProduceOneRowAndOneFinalization() = runBlocking {
        repeat(20) { detect(1,true) }
        end()
        val rows = withTimeout(5000) { history.allHistory.first { it.size == 1 && it.single().status == OrderStatus.IGNORED.name } }
        assertEquals(0,rows.single().timesClicked)
    }
    @Test fun previousSessionCleanupCannotReleaseANewerOrdersProcessingLock() = runBlocking {
        detect(1); detect(2)
        val rows = withTimeout(5000) { history.allHistory.first { it.size==2 } }
        val old = rows.single { it.dropAddress == "Synthetic destination 1" }
        val fresh = rows.single { it.dropAddress == "Synthetic destination 2" }
        val reset = SmartDrivoAccessibilityService::class.java.getDeclaredMethod("resetRapidoProcessing",String::class.java)
            .apply { isAccessible = true }
        val busy = SmartDrivoAccessibilityService::class.java.getDeclaredField("isProcessing").apply { isAccessible = true }
        reset.invoke(service,old.id)
        assertTrue(busy.getBoolean(service))
        reset.invoke(service,fresh.id)
        assertFalse(busy.getBoolean(service))
        end()
        withTimeout(5000) { history.allHistory.first { it.none { row -> row.status==OrderStatus.PROCESSING.name } } }
        Unit
    }
    @Test fun positiveEndThenIdenticalNewOfferCannotReusePreviousIgnoreDecision() = runBlocking {
        detect(1,true)
        val first = withTimeout(5000) { history.allHistory.first { it.size==1 } }.single()
        val guard = SmartDrivoAccessibilityService::class.java.getDeclaredField("rapidoIgnoredVisibleOfferGuard")
            .apply { isAccessible = true }.get(service) as RapidoIgnoredVisibleOfferGuard
        val candidate = RideCandidate(45f,.9f,2.2f,"Address unavailable",first.dropAddress,first.dropAddress,
            Platform.RAPIDO,VehicleType.AUTO)
        guard.rememberIgnored(candidate,"old filter context")
        end()
        detect(1,true)
        assertFalse(guard.isDuplicateIgnored(candidate))
        end()
        val rows = withTimeout(5000) { history.allHistory.first { it.size==2 && it.none { r -> r.status==OrderStatus.PROCESSING.name } } }
        assertEquals(2,rows.map { it.id }.distinct().size)
    }
    @Test fun exceptionAfterAttemptIsUnconfirmedAndCannotFinalizeReplacement() = runBlocking {
        detect(1)
        val first = withTimeout(5000) { history.allHistory.first { it.size==1 } }.single()
        SmartDrivoAccessibilityService::class.java.getDeclaredMethod("onOrderActionAttemptFast",String::class.java)
            .apply { isAccessible = true }.invoke(service,first.id)
        detect(2)
        val candidate = RideCandidate(45f,.9f,2.2f,"Address unavailable",first.dropAddress,first.dropAddress,
            Platform.RAPIDO,VehicleType.AUTO)
        SmartDrivoAccessibilityService::class.java.getDeclaredMethod("finishRapidoReadError",String::class.java,
            RideCandidate::class.java,java.lang.Boolean.TYPE).apply { isAccessible = true }
            .invoke(service,first.id,candidate,true)
        val rows = withTimeout(5000) { history.allHistory.first { rows ->
            rows.size==2 && rows.any { it.id==first.id && it.status==OrderStatus.UNCONFIRMED.name }
        } }
        assertEquals(OrderStatus.PROCESSING.name,rows.single { it.id!=first.id }.status)
        assertTrue(rows.single { it.id==first.id }.actionAttemptAt>0)
        end()
        withTimeout(5000) { history.allHistory.first { it.none { row -> row.status==OrderStatus.PROCESSING.name } } }
        Unit
    }
    @Test fun genuineBookingRemainsOneRowAcrossSessionCacheLossAndLateCallbacks() = runBlocking {
        val candidate = RideCandidate(45f,.9f,2.2f,"Address unavailable","Synthetic destination","Area",
            Platform.RAPIDO,VehicleType.AUTO,"RX12345")
        val first = history.onOrderDetected(candidate,rapidoRecordId="rapido_offer_first")
        history.onOrderActionCompleted(first.id,OrderStatus.ACCEPTED,"CONFIRMED","Synthetic confirmed UI",true)
        val reused = history.onOrderDetected(candidate,rapidoRecordId="rapido_offer_after_cache_loss")
        assertEquals(first.id,reused.id)
        history.onOrderActionCompleted("rapido_offer_after_cache_loss",OrderStatus.UNCONFIRMED,"LATE","Late callback",false)
        assertEquals(1,history.getAllHistoryDirect().size)
        assertEquals(OrderStatus.ACCEPTED.name,history.getById(first.id)!!.status)
    }
    @Test fun realRoomFlowLatencyWithHundredRetainedRowsAndConcurrentStaleSweeps() = runBlocking {
        repeat(100) { n ->
            val candidate = RideCandidate(50f,1f,3f,"Unavailable","Synthetic seed $n","Seed",
                Platform.UBER,VehicleType.AUTO,"SEED$n")
            val row = history.onOrderDetected(candidate)
            history.onOrderDecision(row.id,OrderStatus.IGNORED,"TEST","Synthetic retained row")
        }
        val sweeper = launch(Dispatchers.Default) { repeat(30) { history.finalizeAllStaleProcessing(); yield() } }
        val samples = mutableListOf<Double>()
        repeat(30) { index ->
            val before = System.nanoTime()
            detect(index)
            withTimeout(5000) { history.allHistory.first { rows -> rows.any { it.dropAddress == "Synthetic destination $index" } } }
            samples += (System.nanoTime()-before)/1e6
        }
        end(); sweeper.join()
        fun p(q: Double) = samples.sorted()[kotlin.math.ceil(samples.size*q).toInt()-1]
        println("HISTORY_SERVICE_ROOM_JVM n=30 retainedRows=100 concurrentStaleSweeps=30 p50Ms=${p(.5)} p95Ms=${p(.95)} excludesPhoneEventsAndFrames=true")
        val rows = withTimeout(5000) { history.allHistory.first { it.size==130 && it.none { row -> row.status==OrderStatus.PROCESSING.name } } }
        assertEquals(30,rows.count { it.platform==Platform.RAPIDO.name })
    }
}
