package com.example.engine

import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.Platform
import java.util.ArrayDeque

/** Enabled Shield only. Explicit fields or a unique value in a trusted labelled pickup section. */
object AdminPickupAddressReader {
    fun read(root: AccessibilityNodeInfo, platform: Platform, diagnostics: ((String) -> Unit)? = null): String? {
        val rootPackage = root.packageName?.toString().orEmpty()
        val rootWindow = root.windowId
        fun trusted(node: AccessibilityNodeInfo) = PickupOnlyLabel.trustedNode(
            rootPackage, node.packageName?.toString().orEmpty(), platform,
            rootWindow, node.windowId, node.isVisibleToUser)
        if (!trusted(root)) { diagnostics?.invoke("stage=pickup_structure reason=ROOT_UNTRUSTED"); return null }
        val pending = ArrayDeque<Triple<AccessibilityNodeInfo, Int, Int?>>()
        pending.add(Triple(root, 0, null))
        val fields = mutableListOf<PickupShieldEvidence.Field>()
        var key = 0
        while (pending.isNotEmpty() && fields.size < 350) {
            val (node, ownKey, parentKey) = pending.removeFirst()
            val safe = trusted(node)
            val id = node.viewIdResourceName
            val text = if (safe) node.text?.toString()?.trim() else null
            val desc = if (safe) node.contentDescription?.toString()?.trim() else null
            fields += PickupShieldEvidence.Field(ownKey, parentKey, text, desc, id, safe,
                node.isClickable || node.isEditable, node.className?.toString())
            if (!safe) continue
            PickupOnlyLabel.fromField(text, id)?.let { return it }
            PickupOnlyLabel.fromField(desc, id)?.let { return it }
            for (i in 0 until node.childCount.coerceAtMost(45)) {
                node.getChild(i)?.let { pending.add(Triple(it, ++key, ownKey)) }
            }
        }
        val value = PickupShieldEvidence.labelledPickup(fields)
        if (value == null) {
            val ids = fields.mapNotNull { it.resourceId?.substringAfterLast('/') }
                .filter { it.matches(Regex("[A-Za-z_]{1,64}")) }.distinct().take(8)
            diagnostics?.invoke("stage=pickup_structure reason=NO_TRUSTED_PICKUP nodes=${fields.size} " +
                "labels=${fields.count { PickupOnlyLabel.isLabel(it.text) || PickupOnlyLabel.isLabel(it.description) }} " +
                "untrusted=${fields.count { !it.trusted }} ids=${ids.joinToString(",")}")
            diagnostics?.invoke("stage=pickup_shape tree=${PickupShieldEvidence.structure(fields)}")
        }
        return value
    }
}
