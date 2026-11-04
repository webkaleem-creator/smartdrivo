package com.example.engine

import com.example.auth.OwnerClaimLease
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class Build401OfferSafetyTest {
    private fun offer(drop: String = "Synthetic destination", booking: String? = null) =
        RideCandidate(45f, .9f, 2.2f, "Address unavailable", drop, drop, Platform.RAPIDO, VehicleType.AUTO, booking)

    @Test fun proseIsNeverBookingIdentityAndGenuineLabelsRemainSupported() {
        for (text in listOf("Ride details", "Ride with confidence", "Paid online", "ID unavailable", "Order details"))
            assertNull(RapidoVisibleOffer.bookingId(text))
        assertEquals("RX12345", RapidoVisibleOffer.bookingId("Booking ID: RX12345"))
        assertEquals("123456", RapidoVisibleOffer.bookingId("ID: 123456"))
    }
    @Test fun tenRapidDestinationsWithIdenticalMetricsStaySeparateWhileRepeatsReuseIdentity() {
        val offers = RapidoVisibleOffer()
        val ids = (1..10).map { n ->
            val ride = offer("Synthetic destination $n")
            val first = offers.observe(ride)
            assertTrue(first.created)
            repeat(20) { assertEquals(first.session.id, offers.observe(ride).session.id) }
            first.session.id
        }
        assertEquals(10, ids.toSet().size)
        assertFalse(offers.isCurrent(ids.first(), offer("Synthetic destination 1")))
        assertFalse(offers.owns(ids.first()))
        assertTrue(offers.owns(ids.last()))
    }
    @Test fun positiveTransitionDistinguishesIdenticalAnonymousOffersAndPartialIdsCannotHideReplacement() {
        val offers = RapidoVisibleOffer()
        val first = offers.observe(offer()).session
        offers.disappeared()
        assertNotEquals(first.id, offers.observe(offer()).session.id)
        val identified = offers.observe(offer(booking = "RX111")).session
        offers.observe(offer())
        assertNotEquals(identified.id, offers.observe(offer(booking = "RX222")).session.id)
    }
    @Test fun labelledPickupRejectsMixedDropOrAmbiguousValuesWithoutGuessingOrdering() {
        fun f(k: Int, text: String, trusted: Boolean = true, id: String? = null) =
            PickupShieldEvidence.Field(k, 0, text, null, id, trusted)
        val label = f(1, "Pickup")
        assertEquals("Apartment 4B, 2nd Cross, M Road", PickupShieldEvidence.labelledPickup(listOf(label,
            f(2, "Apartment 4B, 2nd Cross, M Road", id = "pkg:id/address"))))
        assertNull(PickupShieldEvidence.labelledPickup(listOf(label, f(2, "Synthetic pickup"), f(3, "Drop"), f(4, "Blocked destination"))))
        assertNull(PickupShieldEvidence.labelledPickup(listOf(label, f(2, "Blocked destination", id = "pkg:id/drop_address"))))
        assertNull(PickupShieldEvidence.labelledPickup(listOf(label, f(2, "Wrong package", false))))
        assertNull(PickupShieldEvidence.labelledPickup(listOf(label, f(2, "One address"), f(3, "Second address"))))
    }
    @Test fun acknowledgementMissingRootsAndReplacementCannotBecomeRejectedOrAccepted() {
        assertFalse(PickupShieldEvidence.rejectLabel("−", null, null))
        assertFalse(PickupShieldEvidence.rejectLabel("Passenger", null, null))
        assertFalse(PickupShieldEvidence.rejectLabel("Accept", "Skip", null))
        assertTrue(PickupShieldEvidence.rejectLabel("Skip", null, null))
        val watch = PickupShieldEvidence.Watch()
        repeat(8) { assertEquals(PickupShieldEvidence.Dismissal.PENDING, watch.observe(false, null, false)) }
        assertEquals(OrderStatus.UNCONFIRMED, PickupShieldEvidence.outcome(true, PickupShieldEvidence.Dismissal.UNKNOWN))
        assertEquals(OrderStatus.FAILED, PickupShieldEvidence.outcome(false, PickupShieldEvidence.Dismissal.UNKNOWN))
        assertEquals(PickupShieldEvidence.Dismissal.PENDING, watch.observe(true, null, false))
        watch.observe(false, true, false)
        assertEquals(PickupShieldEvidence.Dismissal.PENDING, watch.observe(true, null, false))
        assertEquals(PickupShieldEvidence.Dismissal.DISMISSED, watch.observe(true, null, false))
        assertEquals(OrderStatus.SKIPPED, PickupShieldEvidence.outcome(true, PickupShieldEvidence.Dismissal.DISMISSED))
        assertEquals(OrderStatus.UNCONFIRMED, PickupShieldEvidence.outcome(true, PickupShieldEvidence.Dismissal.REPLACED))
    }
    @Test fun backgroundRenewalPreservesTtlAndStaleCallbacksCannotRevokeNewOwner() {
        val lease = OwnerClaimLease(); val now = 1_000_000L
        val first = lease.begin("first", now)!!
        assertTrue(lease.complete(first, true, "first", now))
        assertTrue(lease.valid("first", now+299_999L))
        assertNull(lease.begin("first", now+299_999L))
        val renewal = lease.begin("first", now+300_000L)!!
        assertTrue(lease.complete(renewal, true, "first", now+300_001L))
        assertTrue(lease.valid("first", now+600_001L))
        assertFalse(lease.valid("first", now+900_001L))
        val old = lease.begin("first", now+900_002L)!!
        val fresh = lease.begin("second", now+900_003L)!!
        assertTrue(lease.complete(fresh, true, "second", now+900_004L))
        assertFalse(lease.complete(old, false, "second", now+900_005L))
        assertTrue(lease.valid("second", now+900_006L))
        lease.clear()
        assertFalse(lease.valid("second", now+900_007L))
    }
}
