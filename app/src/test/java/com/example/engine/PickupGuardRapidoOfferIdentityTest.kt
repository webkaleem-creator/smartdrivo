package com.example.engine

import com.example.model.Platform
import com.example.model.RideCandidate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PickupGuardRapidoOfferIdentityTest {
    private fun offer(
        pickup: Float = 0.9f,
        fare: Float = 101f,
        drop: String? = "Attapur - Germanten Hospitals, Hyderabad",
        bookingId: String? = null
    ) = RideCandidate(
        fare = fare,
        pickupDistKm = pickup,
        dropDistKm = 5.7f,
        pickupAddress = "Address unavailable",
        dropAddress = drop,
        dropArea = drop,
        platform = Platform.RAPIDO,
        bookingId = bookingId,
        baseFare = 100f,
        tipAmount = 1f
    )

    @Test fun sameRapidoPopupSurvivesPickupDriftAndWhitespace() {
        assertTrue(PickupGuardRapidoOfferIdentity.sameVisibleOffer(
            offer(), offer(pickup = 0.8f, drop = "Attapur  -  Germanten Hospitals, Hyderabad")
        ))
    }

    @Test fun changedFareOrDestinationCannotInheritRejectDecision() {
        assertFalse(PickupGuardRapidoOfferIdentity.sameVisibleOffer(offer(), offer(fare = 102f)))
        assertFalse(PickupGuardRapidoOfferIdentity.sameVisibleOffer(
            offer(), offer(drop = "Nehru Zoological Park, Hyderabad")
        ))
    }

    @Test fun noIdAndMissingDestinationFailsClosed() {
        assertFalse(PickupGuardRapidoOfferIdentity.sameVisibleOffer(offer(drop = null), offer(drop = null)))
    }

    @Test fun changedBookingIdFailsClosed() {
        assertFalse(PickupGuardRapidoOfferIdentity.sameVisibleOffer(
            offer(bookingId = "ride-A"), offer(bookingId = "ride-B")
        ))
    }

    @Test fun matchingBookingIdPermitsSameOffer() {
        assertTrue(PickupGuardRapidoOfferIdentity.sameVisibleOffer(
            offer(bookingId = "ride-A"), offer(pickup = 1.0f, bookingId = "ride-A")
        ))
    }
}
