package com.example

import com.example.engine.PickupGuardRejectHistoryPolicy
import com.example.model.OrderStatus
import org.junit.Assert.*
import org.junit.Test

class PickupGuardRejectHistoryPolicyTest {
    @Test fun skipRequestIsShownImmediatelyButNeverClaimedRejected() {
        val event = PickupGuardRejectHistoryPolicy.forPhase(
            PickupGuardRejectHistoryPolicy.Phase.SKIP_SENT)
        assertEquals(OrderStatus.IGNORED, event.status)
        assertTrue(event.showPopup)
        assertTrue(event.suffix.contains("not yet confirmed"))
    }

    @Test fun onlyObservedRapidoHomeCanConfirmReject() {
        val home = PickupGuardRejectHistoryPolicy.forPhase(
            PickupGuardRejectHistoryPolicy.Phase.RAPIDO_HOME_CONFIRMED)
        assertEquals(OrderStatus.REJECTED, home.status)
        assertFalse(home.showPopup)
    }

    @Test fun timeoutKeepsUnverifiedOutcomeAndAvoidsSecondPopup() {
        val timeout = PickupGuardRejectHistoryPolicy.forPhase(
            PickupGuardRejectHistoryPolicy.Phase.DISMISSAL_UNVERIFIED)
        assertEquals(OrderStatus.IGNORED, timeout.status)
        assertFalse(timeout.showPopup)
        assertTrue(timeout.reasonCode.endsWith("UNCONFIRMED"))
    }
}
