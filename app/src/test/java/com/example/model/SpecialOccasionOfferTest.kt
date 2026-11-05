package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecialOccasionOfferTest {
    @Test fun disabledIsNeverAvailable() {
        val offer = SpecialOccasionOffer(id = "diwali", enabled = false, title = "Diwali", days = 7, startsAtMillis = 1000L)
        assertFalse(offer.isLive(2000L))
    }
    @Test fun finiteCampaignExpiresAtConfiguredDays() {
        val offer = SpecialOccasionOffer(id = "diwali", enabled = true, title = "Diwali", days = 3, startsAtMillis = 1000L)
        assertEquals(1000L + 3L * 86_400_000L, offer.endAtMillis())
        assertTrue(offer.isLive(1000L + 100L))
        assertFalse(offer.isLive(offer.endAtMillis()))
    }
    @Test fun unlimitedRequiresOnSwitch() {
        val offer = SpecialOccasionOffer(id = "festival", enabled = true, title = "Festival", days = 0, startsAtMillis = 1000L)
        assertEquals(Long.MAX_VALUE, offer.endAtMillis())
        assertTrue(offer.isLive(2000L))
        assertFalse(offer.copy(enabled = false).isLive(2000L))
    }
    @Test fun malformedPayloadFailsClosed() {
        assertFalse(SpecialOccasionOffer.fromMap(mapOf(
            "id" to "bad", "enabled" to true, "title" to "Test", "days" to 99, "startsAtMillis" to 1000L
        )).isLive())
    }
}
