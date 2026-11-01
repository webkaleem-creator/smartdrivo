package com.example.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OlaOfferSafetyTest {
    @Test fun onlyOlaDriverPackagesCanReachOlaEngine() {
        assertTrue(OlaOfferSafety.isDriverPackage("com.olacabs.oladriver"))
        assertTrue(OlaOfferSafety.isDriverPackage("com.ola.partner"))
        assertFalse(OlaOfferSafety.isDriverPackage("com.olacabs.consumer"))
        assertFalse(OlaOfferSafety.isDriverPackage("com.olacabs.customer"))
        assertFalse(OlaOfferSafety.isDriverPackage("com.rapido.captain"))
        assertFalse(OlaOfferSafety.isDriverPackage("com.smartdrivo.app"))
    }

    @Test fun acceptCountdownIsDetectableButNotClickReady() {
        assertTrue(OlaOfferSafety.isAcceptLabel("Accept in 5"))
        assertFalse(OlaOfferSafety.isReadyLabel("Accept in 5"))
        assertTrue(OlaOfferSafety.isAcceptLabel("Swipe to Accept"))
        assertTrue(OlaOfferSafety.isReadyLabel("Accept Booking"))
        assertFalse(OlaOfferSafety.isAcceptLabel("Booking accepted"))
        assertFalse(OlaOfferSafety.isAcceptLabel("Accepting booking"))
        assertFalse(OlaOfferSafety.isReadyLabel("Cancel ride"))
        assertFalse(OlaOfferSafety.isReadyLabel("Confirm payment"))
    }

    @Test fun acceptanceMustBeConfirmedByStrongOlaUi() {
        assertTrue(OlaOfferSafety.hasAcceptedUi("Booking accepted"))
        assertTrue(OlaOfferSafety.hasAcceptedUi("Navigate to pickup"))
        assertFalse(OlaOfferSafety.hasAcceptedUi("Cancel ride"))
        assertFalse(OlaOfferSafety.hasAcceptedUi("Call customer"))
        assertFalse(OlaOfferSafety.hasAcceptedUi("Accepted by another driver"))
        assertFalse(OlaOfferSafety.hasAcceptedUi("Order no longer available"))
        assertFalse(OlaOfferSafety.hasAcceptedUi("Accept in 5"))
    }

    @Test fun incompleteOffersAreNotRecordedAsReliableHistory() {
        assertFalse(OlaOfferSafety.hasUsableOffer(null, 2f, 5f, "A", "B"))
        assertFalse(OlaOfferSafety.hasUsableOffer(125f, null, null, "Address unavailable", "Drop Location"))
        assertFalse(OlaOfferSafety.hasUsableOffer(Float.NaN, 1f, 2f, "A", "B"))
        assertTrue(OlaOfferSafety.hasUsableOffer(125f, 1.2f, null, null, null))
        assertTrue(OlaOfferSafety.hasUsableOffer(125f, null, null, "Charminar, Hyderabad", null))
    }

    @Test fun changedOfferRequiresNewDecisionBeforeClick() {
        assertFalse(OlaOfferSafety.hasChanged(120f, 120f, 1.5f, 1.5f))
        assertTrue(OlaOfferSafety.hasChanged(120f, 145f, 1.5f, 1.5f))
        assertTrue(OlaOfferSafety.hasChanged(120f, 120f, 1.5f, 2.1f))
        assertFalse(OlaOfferSafety.hasChanged(120f, null, 1.5f, null))
    }
}
