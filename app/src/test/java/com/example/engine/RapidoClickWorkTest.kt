package com.example.engine

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class RapidoClickWorkTest {
    private fun ride(pickup: Float? = 0.9f, drop: String = "Pune Station") = RideCandidate(
        120f, pickup, 2.2f, "Address unavailable", drop, drop, Platform.RAPIDO,
        VehicleType.AUTO, tipAmount = 30f)
    private val normal = AppSettings(isFastestModeEnabled = false, minFare = 50f,
        maxPickupDistanceKm = 2f, maxDropDistanceKm = 20f, isNoGoEnabled = true)
    private val noGo = listOf(AreaGroup(name = "Avoid", type = AreaType.NO_GO,
        keywords = listOf("Blocked", "पुणे", "কলকাতা")))

    @Test fun identicalMandatoryAndCanonicalChecksEvaluateExactlyOnce() {
        var count = 0; var formatted = 0
        val work = RapidoClickWork({ count++; DecisionResult.Accept("matched") },
            { formatted++; "same History reason" })
        assertEquals(0, count)
        assertTrue(work.decision is DecisionResult.Accept)
        assertTrue(work.decision is DecisionResult.Accept)
        assertEquals(1, count)
        assertEquals(0, formatted)
        assertEquals("same History reason", work.afterDispatch())
        assertEquals("same History reason", work.afterDispatch())
        assertEquals(1, formatted)
    }
    @Test fun everyNewSnapshotReevaluatesAndNeverTurnsRejectionIntoAcceptance() {
        val candidate = ride()
        fun work(settings: AppSettings) = RapidoClickWork({ AreaRulesEngine.evaluateRide(
            candidate, settings = settings) }, { null })
        assertTrue(work(normal).decision is DecisionResult.Accept)
        assertFalse(work(normal.copy(maxPickupDistanceKm = 0.5f)).decision is DecisionResult.Accept)
        val reject = RapidoClickWork({ DecisionResult.Reject("No-Go") }, { null })
        assertEquals(DecisionResult.Reject("No-Go"), reject.decision)
    }
    @Test fun differentialFilterMatrixKeepsAllCanonicalDecisionsAndReasons() {
        val settings = listOf(normal, normal.copy(isFastestModeEnabled = true),
            normal.copy(isBundleOrderEnabled = false), normal.copy(isNoGoEnabled = false),
            normal.copy(isGoToEnabled = true), normal.copy(isPlusAmountFilterEnabled = true))
        val goTo = listOf(AreaGroup(name = "Home", type = AreaType.GO_TO,
            areas = listOf(AreaEntry(name = "Pune Station", minFare = 50f, maxPickupKm = 2f, maxDropKm = 10f))))
        for (saved in settings) for (pickup in listOf(null, 0f, 0.9f, 4f, Float.NaN)) {
            for (drop in listOf("Pune Station", "Blocked", "पुणे", "কলকাতা")) for (bundle in listOf(false,true)) {
                val candidate = ride(pickup, drop).copy(isBundledOrder = bundle)
                fun evaluate() = AreaRulesEngine.evaluateRide(candidate = candidate,
                    settings = saved, areas = goTo + noGo, goToAreas = goTo, noGoAreas = noGo,
                    pickupLocationTextOverride = "", isGoToEnabled = saved.isGoToEnabled,
                    isNoGoEnabled = saved.isNoGoEnabled)
                val expected = evaluate()
                val work = RapidoClickWork({ evaluate() }, { null })
                assertEquals(expected, work.decision)
                assertEquals(expected, work.decision)
            }
        }
    }
    @Test fun presentationFailureAfterDispatchCannotAbortVerificationOrReevaluateFilters() {
        var count = 0
        val work = RapidoClickWork({ DecisionResult.Accept("matched") },
            { count++; error("format unavailable") })
        assertTrue(work.decision is DecisionResult.Accept)
        assertEquals("Saved filters matched (details unavailable)", work.afterDispatch())
        assertEquals("Saved filters matched (details unavailable)", work.afterDispatch())
        assertEquals(1, count)
    }

    @Test fun jvmPreDispatchSliceP50P95Experiment() {
        val candidate = ride()
        fun evaluate() = AreaRulesEngine.evaluateRide(candidate, settings = normal,
            noGoAreas = noGo, pickupLocationTextOverride = "", isNoGoEnabled = true)
        fun formatReason() = String.format(java.util.Locale.ENGLISH,
            "Fare %.1f Pickup %.1f Trip %.1f",candidate.fare,candidate.pickupDistKm,candidate.dropDistKm)
        repeat(1000) { evaluate(); formatReason() }
        val before = mutableListOf<Long>(); val after = mutableListOf<Long>()
        repeat(1000) { index ->
            fun baseline() {
                val start = System.nanoTime(); evaluate(); evaluate(); formatReason()
                before += System.nanoTime()-start
            }
            fun optimized() {
                val start = System.nanoTime()
                val work = RapidoClickWork({ evaluate() }, { formatReason() })
                assertTrue(work.decision is DecisionResult.Accept)
                assertTrue(work.decision is DecisionResult.Accept)
                after += System.nanoTime()-start // Simulated dispatch boundary; no Android click.
                work.afterDispatch()
            }
            if (index % 2 == 0) { baseline(); optimized() } else { optimized(); baseline() }
        }
        fun percentile(values: List<Long>, p: Double) = values.sorted()[kotlin.math.ceil(values.size*p).toInt()-1]/1e6
        println("RAPIDO_JVM_PRE_DISPATCH_SLICE n=1000 beforeP50Ms=${percentile(before,0.5)} beforeP95Ms=${percentile(before,0.95)} afterP50Ms=${percentile(after,0.5)} afterP95Ms=${percentile(after,0.95)} excludesAndroidParsingBinderAndNetwork=true")
    }
}
