package com.example.ola

import org.junit.Assert.*
import org.junit.Test

class OlaReadOnlyOfferParserTest {
    @Test fun parsesShownOlaOfferWithoutPickupAddress() {
        val fields = listOf(
            OlaReadOnlyOfferParser.Field("com.olacabs.oladriver:id/tv_compact_price", "₹63 | ₹32/km"),
            OlaReadOnlyOfferParser.Field("com.olacabs.oladriver:id/tv_pickup_distance_time", "1.2 km • 6 mins"),
            OlaReadOnlyOfferParser.Field("com.olacabs.oladriver:id/tv_drop_distance_time", "1.5 km • 7 mins"),
            OlaReadOnlyOfferParser.Field("com.olacabs.oladriver:id/tv_detail_drop_address",
                "New Idgah Rd, Alibagh, Ramnaspura"),
            OlaReadOnlyOfferParser.Field("com.olacabs.oladriver:id/tv_detail_pickup_address",
                "PRIVATE PICKUP ADDRESS MUST NOT BE READ")
        )
        val offer = OlaReadOnlyOfferParser.parse(fields)!!
        assertTrue(offer.isComplete)
        assertEquals(63.0, offer.fareRupees!!, 0.001)
        assertEquals(1.2, offer.pickupKm!!, 0.001)
        assertEquals(1.5, offer.tripKm!!, 0.001)
        assertEquals("New Idgah Rd, Alibagh, Ramnaspura", offer.dropAddress)
        assertEquals(4, offer.matchedFieldCount)
    }

    @Test fun pickupAddressNeverClassifiedOrRetained() {
        assertNull(OlaReadOnlyOfferParser.classify("com.olacabs.oladriver:id/tv_detail_pickup_address"))
        assertNull(OlaReadOnlyOfferParser.parse(listOf(
            OlaReadOnlyOfferParser.Field("id/tv_detail_pickup_address", "Do not read")
        )))
    }

    @Test fun unknownIdsAreIgnoredFailClosed() {
        assertNull(OlaReadOnlyOfferParser.parse(listOf(
            OlaReadOnlyOfferParser.Field("com.unknown:id/tv_price", "₹400"),
            OlaReadOnlyOfferParser.Field("com.unknown:id/accept", "Accept")
        )))
    }

    @Test fun refusesToUsePickupFeeAsFare() {
        val offer = OlaReadOnlyOfferParser.parse(listOf(
            OlaReadOnlyOfferParser.Field("id/tv_pickup_fee", "inc. ₹15 Pickup fee"),
            OlaReadOnlyOfferParser.Field("id/tv_pickup_distance_time", "0 km • 1 min")
        ))!!
        assertNull(offer.fareRupees)
        assertEquals(0.0, offer.pickupKm!!, 0.001)
        assertFalse(offer.isComplete)
    }

    @Test fun zeroDistanceAndIncompleteOfferAreReadOnlyPartial() {
        val offer = OlaReadOnlyOfferParser.parse(listOf(
            OlaReadOnlyOfferParser.Field("id/tv_compact_price", "₹100"),
            OlaReadOnlyOfferParser.Field("id/tv_pickup_distance_time", "0 km"),
            OlaReadOnlyOfferParser.Field("id/tv_drop_distance_time", "2.5 km")
        ))!!
        assertEquals(0.0, offer.pickupKm!!, 0.001)
        assertNull(offer.dropAddress)
        assertFalse(offer.isComplete)
    }

    @Test fun invalidDistanceDoesNotProduceFakeZero() {
        val offer = OlaReadOnlyOfferParser.parse(listOf(
            OlaReadOnlyOfferParser.Field("id/tv_pickup_distance_time", "nearby"),
            OlaReadOnlyOfferParser.Field("id/tv_drop_distance_time", "11 mins")
        ))!!
        assertNull(offer.pickupKm)
        assertNull(offer.tripKm)
    }
}
