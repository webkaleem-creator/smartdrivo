package com.example.engine

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/** Only a self-clickable, explicitly identified Reject/Skip control; no geometry guesses or ancestor substitution. */
object PickupShieldRejectReader {
    fun read(root: AccessibilityNodeInfo, diagnostic: (String) -> Unit = {}): AccessibilityNodeInfo? {
        if (root.packageName?.toString() !in RapidoAdapter.RAPIDO_PACKAGES) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>(); queue.add(root)
        var visited = 0; var ambiguous = 0
        val rootBounds = Rect(); root.getBoundsInScreen(rootBounds)
        while (queue.isNotEmpty() && visited++ < 350) {
            val node = queue.removeFirst()
            if (node.packageName?.toString() != root.packageName?.toString() ||
                node.windowId != root.windowId || !node.isVisibleToUser) continue
            val text = node.text?.toString(); val description = node.contentDescription?.toString()
            if (text?.trim() in setOf("-", "−", "–", "—")) ambiguous++
            if (PickupShieldEvidence.rejectLabel(text, description, node.viewIdResourceName) &&
                node.isEnabled && node.isClickable && node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }) {
                val bounds = Rect(); node.getBoundsInScreen(bounds)
                if (!bounds.isEmpty && !rootBounds.isEmpty && rootBounds.contains(bounds) && bounds != rootBounds) return node
            }
            for (i in 0 until node.childCount.coerceAtMost(45)) node.getChild(i)?.let(queue::addLast)
        }
        diagnostic("stage=reject_missing nodes=$visited ambiguousMinus=$ambiguous window=${root.windowId}")
        return null
    }
}
