package com.example.engine

import com.example.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderPopupStatusPolicyTest {
    @Test fun aVerifiedAcceptedResultCannotBeDowngradedByStaleCallbacks() {
        for (stale in listOf(OrderStatus.UNCONFIRMED, OrderStatus.PROCESSING,
            OrderStatus.IGNORED, OrderStatus.REJECTED, OrderStatus.MISSED,
            OrderStatus.FAILED, OrderStatus.SKIPPED)) {
            assertFalse(OrderPopupStatusPolicy.mayReplace(OrderStatus.ACCEPTED, stale))
        }
    }

    @Test fun aLateVerifiedAcceptanceRepairsUnconfirmedPresentation() {
        assertTrue(OrderPopupStatusPolicy.mayReplace(OrderStatus.UNCONFIRMED, OrderStatus.ACCEPTED))
        assertTrue(OrderPopupStatusPolicy.mayReplace(null, OrderStatus.ACCEPTED))
    }

    @Test fun aDefiniteMissedResultIsNotOverwrittenByUnknown() {
        assertFalse(OrderPopupStatusPolicy.mayReplace(OrderStatus.MISSED, OrderStatus.UNCONFIRMED))
    }

    @Test fun uncertaintyIsDescribedWithoutFalselyClaimingRejection() {
        assertEquals("COULD NOT VERIFY YET",
            OrderPopupStatusPolicy.statusTitle(OrderStatus.UNCONFIRMED))
        assertEquals("ACCEPTED", OrderPopupStatusPolicy.statusTitle(OrderStatus.ACCEPTED))
    }
}
