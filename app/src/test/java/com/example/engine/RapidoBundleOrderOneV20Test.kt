package com.example.engine

import org.junit.Assert.*
import com.example.model.AppSettings
import com.example.model.Platform
import com.example.model.RideCandidate
import org.junit.Test

class RapidoBundleOrderOneV20Test {
    @Test fun bundle98UsesOnlyOrder1() {
        val data = RapidoAdapter.extractOrderDataFromTexts(listOf(
            "Bundle Order", "₹98+", "Order 1", "₹49", "0.4 Km",
            "TadBun Falaknuma - Bushrath Nagar", "0.5 Km",
            "TadBun Falaknuma - Dr M A Bari Clinic, Nawab Saheb Kunta Road",
            "Order 2", "₹49+", "0.3 km", "~3 km", "Falaknuma", "Accept"
        ), skipPickupAddress = true)
        assertTrue(data.isBundledOrder)
        assertEquals(49f, data.totalFare, 0.001f)
        assertEquals(0.4f, data.pickupKm!!, 0.001f)
        assertEquals(0.5f, data.dropKm!!, 0.001f)
        assertTrue(data.dropAddress.orEmpty().contains("Bari Clinic"))
    }

    @Test fun bundle114UsesOnlyOrder1() {
        val data = RapidoAdapter.extractOrderDataFromTexts(listOf(
            "Bundle Order", "₹114+", "Order 1", "₹57", "0.2 Km",
            "TadBun Falaknuma - Model Town Colony", "2.1 Km",
            "Shalibanda Charminar - India Post Shah Ali Banda, Falaknuma Road",
            "Order 2", "₹57+", "0.3 km", "~3 Km", "Falaknuma"
        ), skipPickupAddress = true)
        assertTrue(data.isBundledOrder)
        assertEquals(57f, data.totalFare, 0.001f)
        assertEquals(0.2f, data.pickupKm!!, 0.001f)
        assertEquals(2.1f, data.dropKm!!, 0.001f)
        assertTrue(data.dropAddress.orEmpty().contains("India Post"))
    }

    @Test fun missingOrder2MarkerMustNotReadCombinedFare() {
        val data = RapidoAdapter.extractOrderDataFromTexts(listOf(
            "Bundle Order", "₹114+", "Order 1", "₹57", "0.2 Km", "2.1 Km"
        ))
        assertTrue(data.isBundledOrder)
        assertEquals(0f, data.totalFare, 0.001f)
        assertNull(data.pickupKm)
    }

    @Test fun flattenedCardKeepsOnlyOrderOne() {
        val parts = RapidoBundleOrderOneV20.selectParts(listOf(
            "Bundle Order ₹114+ Order 1 ₹57 0.2 km 2.1 km Order 2 ₹57+ 0.3 km ~3 km"
        ))!!.map { it.text }.joinToString(" ")
        assertTrue(parts.contains("₹57"))
        assertFalse(parts.contains("₹114"))
        assertFalse(parts.contains("0.3 km"))
    }

    @Test fun noSectionMarkersFailClosed() {
        assertNull(RapidoBundleOrderOneV20.selectParts(listOf("Bundle Order", "₹98+", "₹49", "₹49+")))
    }

    @Test fun combined98CannotBypassMinimumFare50() {
        val d = RapidoAdapter.extractOrderDataFromTexts(listOf(
            "Bundle Order", "₹98+", "Order 1", "₹49", "0.4 km",
            "TadBun pickup", "0.5 km", "Dr M A Bari Clinic, Falaknuma",
            "Order 2", "₹49+", "0.3 km", "~3 km"
        ), true)
        val candidate = RideCandidate(
            fare = d.totalFare, pickupDistKm = d.pickupKm,
            dropDistKm = d.dropKm, pickupAddress = null,
            dropAddress = d.dropAddress, dropArea = d.dropAddress,
            platform = Platform.RAPIDO, isBundledOrder = d.isBundledOrder
        )
        val settings = AppSettings(
            isBundleOrderEnabled = true, minFare = 50f,
            isNoGoEnabled = false
        )
        assertTrue(AreaRulesEngine.evaluateRide(candidate, settings = settings)
            is DecisionResult.Reject)
    }

    @Test fun orderOne57PassesMinimum50OnlyWhenBundleEnabled() {
        val d = RapidoAdapter.extractOrderDataFromTexts(listOf(
            "Bundle Order", "₹114+", "Order 1", "₹57", "0.2 km",
            "TadBun pickup", "2.1 km", "India Post Shah Ali Banda, Falaknuma Road",
            "Order 2", "₹57+", "0.3 km", "~3 km"
        ), true)
        val candidate = RideCandidate(
            fare = d.totalFare, pickupDistKm = d.pickupKm,
            dropDistKm = d.dropKm, pickupAddress = null,
            dropAddress = d.dropAddress, dropArea = d.dropAddress,
            platform = Platform.RAPIDO, isBundledOrder = d.isBundledOrder
        )
        val settings = AppSettings(isBundleOrderEnabled = true, minFare = 50f,
            isNoGoEnabled = false)
        assertTrue(AreaRulesEngine.evaluateRide(candidate, settings = settings)
            is DecisionResult.Accept)
        assertTrue(AreaRulesEngine.evaluateRide(candidate,
            settings = settings.copy(isBundleOrderEnabled = false))
            is DecisionResult.Ignore)
    }
}
