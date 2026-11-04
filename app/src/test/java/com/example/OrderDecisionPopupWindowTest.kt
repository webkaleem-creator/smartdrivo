package com.example

import android.content.Context
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.model.OrderStatus
import com.example.service.OrderDecisionPopup
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36])
class OrderDecisionPopupWindowTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val looper get() = shadowOf(Looper.getMainLooper())
    private fun active() = OrderDecisionPopup::class.java.getDeclaredField("activeView").apply { isAccessible=true }.get(null) as View?
    private fun text(v: View?): String = when(v) {
        is TextView -> v.text.toString()
        is ViewGroup -> (0 until v.childCount).joinToString(" ") { text(v.getChildAt(it)) }
        else -> ""
    }
    @Before fun setup() { OrderDecisionPopup.pauseForBankingMode(); looper.idle(); OrderDecisionPopup.resumeAfterBanking(); ShadowSettings.setCanDrawOverlays(true) }
    @After fun cleanup() { OrderDecisionPopup.pauseForBankingMode(); looper.idle(); OrderDecisionPopup.resumeAfterBanking() }
    private fun show(key:String,status:OrderStatus=OrderStatus.IGNORED,reason:String="Synthetic decision") {
        OrderDecisionPopup.show(context,key,status,"Rapido",45f,reason,.9f,2.2f,"Synthetic area")
        looper.idle()
    }
    @Test fun duplicateAfterSixSecondsDoesNotRecreateOrExtendThirteenSecondPopup() {
        OrderDecisionPopup.onNewOrderDetected("timing"); show("timing")
        val root=active(); assertNotNull(root)
        looper.idleFor(Duration.ofSeconds(6)); show("timing"); assertSame(root,active())
        looper.idleFor(Duration.ofMillis(7100)); assertNull(active())
        show("timing"); assertNull(active())
    }
    @Test fun newFinalDecisionAndVerifiedUpgradeUpdateSameWindowWithoutOldResurrection() {
        OrderDecisionPopup.onNewOrderDetected("first"); show("first")
        val root=active()
        OrderDecisionPopup.onNewOrderDetected("next"); looper.idle(); assertSame(root,active())
        show("next",OrderStatus.UNCONFIRMED); assertSame(root,active())
        show("first",reason="Obsolete sentinel"); assertFalse(text(active()).contains("Obsolete sentinel"))
        show("next",OrderStatus.ACCEPTED,"Verified outcome"); assertSame(root,active()); assertTrue(text(active()).contains("ACCEPTED"))
        show("next",OrderStatus.UNCONFIRMED); assertTrue(text(active()).contains("ACCEPTED"))
    }
    @Test fun aliasUsesPersistedOwnershipAndPendingNeverRenders() {
        OrderDecisionPopup.onNewOrderDetected("provisional")
        OrderDecisionPopup.onHistoryResolved("provisional","persisted")
        show("persisted",OrderStatus.PROCESSING,"Evaluating ride filters..."); assertNull(active())
        show("persisted"); val root=active(); assertNotNull(root)
        OrderDecisionPopup.onNewOrderDetected("duplicate")
        OrderDecisionPopup.onHistoryResolved("duplicate","persisted")
        show("persisted"); assertSame(root,active())
    }
}
