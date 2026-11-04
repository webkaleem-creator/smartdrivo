package com.example.engine

import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.Platform
import java.util.ArrayDeque

/**
 * Runs ONLY while the owner pickup veto is enabled.
 * No broad text scanning to infer pickup from drop; only explicit fields/labels.
 * A missing or ambiguous pickup is unreadable (fail closed, never auto-accept).
 */
object AdminPickupAddressReader {
    fun read(root: AccessibilityNodeInfo, platform: Platform): String? {
        val rootPackage = root.packageName?.toString().orEmpty()
        val rootWindow = root.windowId
        fun trusted(node: AccessibilityNodeInfo) = PickupOnlyLabel.trustedNode(
            rootPackage, node.packageName?.toString().orEmpty(), platform,
            rootWindow, node.windowId, node.isVisibleToUser)
        if (!trusted(root)) return null
        val pending: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
        pending.add(root)
        var visited = 0
        while (pending.isNotEmpty() && visited++ < 350) {
            val node = pending.removeFirst()
            if (!trusted(node)) continue

            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val id = node.viewIdResourceName
            PickupOnlyLabel.fromField(text, id)?.let { return it }
            PickupOnlyLabel.fromField(desc, id)?.let { return it }

            // If an exact pickup label is followed by an address field inside
            // its own parent, inspect only that sibling, never the drop block.
            if (PickupOnlyLabel.isLabel(text) || PickupOnlyLabel.isLabel(desc)) {
                val parent = node.parent
                if (parent != null) {
                    val siblingCount = parent.childCount.coerceAtMost(12)
                    for (i in 0 until siblingCount) {
                        val sibling = parent.getChild(i) ?: continue
                        if (sibling == node || !trusted(sibling)) continue
                        val t = sibling.text?.toString()?.trim()
                        val candidate = t?.takeIf(PickupOnlyLabel::plausible)
                        if (candidate != null && !PickupOnlyLabel.isLabel(candidate)) {
                            // Only a sibling with pickup-related view ID is trustworthy.
                            val siblingId = sibling.viewIdResourceName.orEmpty()
                            if (PickupOnlyLabel.fromField(candidate, siblingId) != null) return candidate
                        }
                    }
                }
            }
            for (i in 0 until node.childCount.coerceAtMost(45)) {
                node.getChild(i)?.let(pending::addLast)
            }
        }
        return null
    }

}
