package com.example.engine

import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.Platform
import java.util.ArrayDeque
import java.util.Locale

/**
 * Runs ONLY while the owner pickup veto is enabled.
 * No broad text scanning to infer pickup from drop; only explicit fields/labels.
 * A missing or ambiguous pickup is unreadable (fail closed, never auto-accept).
 */
object AdminPickupAddressReader {
    fun read(root: AccessibilityNodeInfo, platform: Platform): String? {
        val rootPackage = root.packageName?.toString().orEmpty()
        if (!belongs(rootPackage, platform)) return null
        val pending: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
        pending.add(root)
        var visited = 0
        while (pending.isNotEmpty() && visited++ < 350) {
            val node = pending.removeFirst()
            val pkg = node.packageName?.toString().orEmpty()
            if (pkg.isNotBlank() && !belongs(pkg, platform)) continue

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
                        if (sibling == node) continue
                        val t = sibling.text?.toString()?.trim()
                        val candidate = t?.takeIf(PickupOnlyLabel::plausible)
                        if (candidate != null && !PickupOnlyLabel.isLabel(candidate)) {
                            // Only a sibling with pickup-related view ID is trustworthy.
                            val siblingId = sibling.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
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

    private fun belongs(pkg: String, platform: Platform): Boolean = when (platform) {
        Platform.RAPIDO -> pkg.startsWith("com.rapido.", ignoreCase = true)
        Platform.OLA -> pkg.startsWith("com.olacabs.", ignoreCase = true) ||
            pkg.equals("ola.cabs", ignoreCase = true)
        Platform.UBER -> pkg.startsWith("com.ubercab", ignoreCase = true) ||
            pkg.startsWith("com.uber.driver", ignoreCase = true)
    }
}
