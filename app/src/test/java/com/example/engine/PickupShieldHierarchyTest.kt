package com.example.engine

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PickupShieldHierarchyTest {
    private val pkg = "com.rapido.passenger"
    private fun node(text: String? = null, id: String? = null, click: Boolean = false,
        owner: String = pkg, rect: Rect = Rect(10,10,400,80)) = AccessibilityNodeInfo.obtain().apply {
        packageName = owner; this.text = text; viewIdResourceName = id
        isVisibleToUser = true; isEnabled = true; isClickable = click
        setBoundsInScreen(rect)
        if (click) addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
    }
    private fun add(parent: AccessibilityNodeInfo, child: AccessibilityNodeInfo) { shadowOf(parent).addChild(child) }
    @Test fun genericAddressSiblingInsidePickupSectionDoesNotRequirePickupViewId() {
        val root = node(rect = Rect(0,0,1080,2200)); val section = node()
        add(root,section); add(section,node("Pickup")); add(section,node("Flat 4B, 123 M Road", "$pkg:id/address"))
        val drop = node(); add(root,drop); add(drop,node("Drop")); add(drop,node("Blocked destination", "$pkg:id/drop_address"))
        val pickup = AdminPickupAddressReader.read(root, Platform.RAPIDO)
        assertEquals("Flat 4B, 123 M Road", pickup)
        assertEquals(AdminPickupBlockPolicy.Decision.Allow,
            AdminPickupBlockPolicy.evaluate(AdminPickupBlockSettings(true,listOf("blocked")),pickup))
    }
    @Test fun wrongPackageSiblingAndMixedContainerFailClosedWithNoAddressDiagnostics() {
        val root = node(rect = Rect(0,0,1080,2200)); add(root,node("Pickup"))
        add(root,node("PRIVATE_ADDRESS_SENTINEL", owner = "com.other.app"))
        val logs = mutableListOf<String>()
        assertNull(AdminPickupAddressReader.read(root, Platform.RAPIDO, logs::add))
        assertTrue(logs.isNotEmpty()); assertFalse(logs.joinToString().contains("PRIVATE_ADDRESS_SENTINEL"))
    }
    @Test fun nestedReadOnlyPickupValueIsSupportedButCamelCaseDropIdIsNotPickupEvidence() {
        val root=node(rect=Rect(0,0,1080,2200)); val section=node(); val nested=node()
        add(root,section); add(section,node("Pickup")); add(section,nested)
        val address=node("TadBun Falaknuma","$pkg:id/address"); add(nested,address)
        assertEquals("TadBun Falaknuma",AdminPickupAddressReader.read(root,Platform.RAPIDO))
        address.viewIdResourceName="$pkg:id/tvDropAddress"
        assertNull(AdminPickupAddressReader.read(root,Platform.RAPIDO))
    }
    @Test fun strictRejectRequiresActualSemanticEvidenceAndNeverSubstitutesParent() {
        val root = node(rect = Rect(0,0,1080,2200))
        val minus = node("−", click = true); add(root,minus)
        val accept = node("Accept", click = true); add(root,accept)
        assertNull(PickupShieldRejectReader.read(root))
        val parent = node(click = true); add(root,parent); add(parent,node("Skip"))
        assertNull(PickupShieldRejectReader.read(root))
        val skip = node("Skip", click = true); add(root,skip)
        assertEquals(skip, PickupShieldRejectReader.read(root))
        skip.isEnabled = false
        assertNull(PickupShieldRejectReader.read(root))
        skip.isEnabled = true; skip.packageName = "com.other.app"
        assertNull(PickupShieldRejectReader.read(root))
    }
}
