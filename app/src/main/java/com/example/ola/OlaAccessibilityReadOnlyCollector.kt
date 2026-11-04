package com.example.ola

import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/**
 * Bounded accessibility tree traversal for Ola only. Never clicks, gestures,
 * accesses pickup address text, or scans other apps' trees.
 */
object OlaAccessibilityReadOnlyCollector {
    private const val MAX_VISITED = 190
    private const val MAX_DEPTH = 10
    private const val MAX_CHILDREN_PER_NODE = 24

    fun collect(root: AccessibilityNodeInfo?): List<OlaReadOnlyOfferParser.Field> {
        if (root?.packageName?.toString() != OlaReadOnlyCapturePolicy.OLA_DRIVER_PACKAGE) {
            return emptyList()
        }
        val result = ArrayList<OlaReadOnlyOfferParser.Field>(4)
        val work = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        work.add(root to 0)
        var count = 0
        while (work.isNotEmpty() && count < MAX_VISITED && result.size < 12) {
            val (node, depth) = work.removeFirst()
            count++
            try {
                if (node.packageName?.toString() != OlaReadOnlyCapturePolicy.OLA_DRIVER_PACKAGE) {
                    continue
                }
                val id = node.viewIdResourceName.orEmpty()
                // Check ID before accessing text, including explicit exclusion of
                // pickup address and all unrelated text nodes.
                if (OlaReadOnlyOfferParser.classify(id) != null) {
                    val value = (node.text ?: node.contentDescription)?.toString().orEmpty()
                    if (value.isNotBlank()) {
                        result.add(OlaReadOnlyOfferParser.Field(id, value.take(220)))
                    }
                }
                if (depth < MAX_DEPTH) {
                    for (i in 0 until node.childCount.coerceAtMost(MAX_CHILDREN_PER_NODE)) {
                        node.getChild(i)?.let { work.add(it to depth + 1) }
                    }
                }
            } catch (_: Exception) {
                // A recycled or rapidly changing Ola window is not actionable.
            }
        }
        return result
    }
}
