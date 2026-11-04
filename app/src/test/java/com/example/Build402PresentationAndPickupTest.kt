package com.example

import com.example.engine.*
import com.example.model.*
import com.example.ui.screens.HistoryPendingPresentation
import org.junit.Assert.*
import org.junit.Test

class Build402PresentationAndPickupTest {
    @Test fun pendingPresentationNeverExposesDiagnosticReasonOrChangesTruthfulState() {
        val pending=OrderHistoryItem(id="pending",decisionReasonText="Evaluating ride filters...",reason="Processing Order")
        val visible=HistoryPendingPresentation.forDisplay(pending)
        assertEquals(OrderStatus.PROCESSING,visible.status)
        assertEquals("",visible.reason); assertEquals("",visible.decisionReasonText)
        assertEquals("Evaluating ride filters...",pending.decisionReasonText)
        for(status in OrderStatus.entries.filter { it!=OrderStatus.PROCESSING }) {
            val final=pending.copy(status=status,reason="Actual final decision")
            assertSame(final,HistoryPendingPresentation.forDisplay(final))
        }
    }
    @Test fun nestedPickupMatchesTadbunCaseInsensitivelyButDropAndMixedSectionsNeverMatch() {
        fun f(key:Int,parent:Int?,text:String?,id:String?=null,interactive:Boolean=false)=
            PickupShieldEvidence.Field(key,parent,text,null,id,true,interactive)
        val fields=listOf(f(0,null,null),f(1,0,"Pickup"),f(2,0,null),f(3,2,"TadBun Falaknuma"))
        val pickup=PickupShieldEvidence.labelledPickup(fields)
        assertEquals("TadBun Falaknuma",pickup)
        val settings=AdminPickupBlockSettings(true,AdminPickupBlockPolicy.parse("tadbun, other"))
        assertEquals(AdminPickupBlockPolicy.Decision.Blocked("tadbun"),AdminPickupBlockPolicy.evaluate(settings,pickup))
        assertEquals(AdminPickupBlockPolicy.Decision.Allow,AdminPickupBlockPolicy.evaluate(settings,"NotTadbun Street"))
        assertNull(PickupShieldEvidence.labelledPickup(fields.map { if(it.key==3) it.copy(resourceId="pkg:id/tvDropAddress") else it }))
        assertNull(PickupShieldEvidence.labelledPickup(fields+f(4,2,"Destination")))
        assertNull(PickupShieldEvidence.labelledPickup(fields.map { if(it.key==3) it.copy(interactive=true) else it }))
        assertFalse(PickupShieldEvidence.structure(fields).contains("TadBun Falaknuma"))
    }
    @Test fun unrelatedIdentityLabelsCannotMergeDistinctOffers() {
        for(text in listOf("Driver ID: 12345","Customer ID: 12345","Vehicle ID: 12345","Paid for ID: 12345"))
            assertNull(RapidoVisibleOffer.bookingId(text))
        assertEquals("RX12345",RapidoVisibleOffer.bookingId("Booking ID: RX12345"))
        assertEquals("RX12345",RapidoVisibleOffer.bookingId("ID: RX12345"))
    }
    @Test fun homeBehindAnOfferOrIncompleteWindowSnapshotCannotProveDismissal() {
        val watch=PickupShieldEvidence.Watch()
        repeat(4) { assertEquals(PickupShieldEvidence.Dismissal.PENDING,
            watch.observe(true,null,false,otherOfferVisible=true)) }
        repeat(4) { assertEquals(PickupShieldEvidence.Dismissal.PENDING,
            watch.observe(true,null,false,windowsComplete=false)) }
        assertEquals(PickupShieldEvidence.Dismissal.PENDING,watch.observe(true,null,false))
        assertEquals(PickupShieldEvidence.Dismissal.DISMISSED,watch.observe(true,null,false))
    }
}
