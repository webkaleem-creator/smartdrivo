package com.example.engine

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern

object OlaAdapter {

    private const val TAG = "OlaAdapter"

    val OLA_PACKAGES = listOf(
        "com.olacabs.oladriver",
        "com.olacabs.driver",
        "com.olacabs.consumer",
        "com.olacabs.customer",
        "ola.cabs"
    )

    // Method A: Resource IDs for Ola accept
    val ACCEPT_IDS = listOf(
        "com.olacabs.oladriver:id/btn_accept",
        "com.olacabs.oladriver:id/accept_btn",
        "com.olacabs.oladriver:id/accept_button",
        "com.olacabs.oladriver:id/accept_ride_button",
        "com.olacabs.oladriver:id/accept_order",
        "com.olacabs.oladriver:id/acceptOrderBtn",
        "com.olacabs.oladriver:id/btnAccept",
        "com.olacabs.oladriver:id/layout_accept",
        "com.olacabs.oladriver:id/action_accept",
        "com.olacabs.oladriver:id/button_accept",
        "com.olacabs.oladriver:id/buttonAccept",
        "com.olacabs.oladriver:id/slide_to_accept",
        "com.olacabs.oladriver:id/swipe_to_accept",
        "com.olacabs.oladriver:id/swipe_button",
        "com.olacabs.oladriver:id/swipe_btn",
        "com.olacabs.oladriver:id/slider_button",
        "com.olacabs.oladriver:id/slider",
        "com.olacabs.oladriver:id/slide_view",
        "com.olacabs.oladriver:id/fl_accept",
        "com.olacabs.oladriver:id/accept_ride",
        "com.olacabs.oladriver:id/accept_trip",
        "com.olacabs.oladriver:id/btn_accept_ride",
        "com.olacabs.oladriver:id/btn_accept_booking",
        "com.olacabs.oladriver:id/accept_booking",
        "com.olacabs.oladriver:id/accept_request",
        "com.olacabs.oladriver:id/accept_duty",
        "com.olacabs.oladriver:id/tv_accept",
        "com.olacabs.oladriver:id/tvAccept",
        "com.olacabs.oladriver:id/acceptTextView",
        "com.olacabs.oladriver:id/accept_container",
        "com.olacabs.oladriver:id/accept_card",
        "com.olacabs.driver:id/btn_accept",
        "com.olacabs.driver:id/accept_btn",
        "com.olacabs.driver:id/accept_button",
        "com.olacabs.driver:id/accept_ride_button",
        "com.olacabs.driver:id/accept_order",
        "com.olacabs.driver:id/acceptOrderBtn",
        "com.olacabs.driver:id/btnAccept",
        "com.olacabs.driver:id/layout_accept",
        "com.olacabs.driver:id/action_accept",
        "com.olacabs.driver:id/button_accept",
        "com.olacabs.driver:id/buttonAccept",
        "com.olacabs.driver:id/slide_to_accept",
        "com.olacabs.driver:id/swipe_to_accept",
        "com.olacabs.driver:id/swipe_button",
        "com.olacabs.driver:id/swipe_btn",
        "com.olacabs.driver:id/slider_button",
        "com.olacabs.driver:id/slider",
        "com.olacabs.driver:id/slide_view",
        "com.olacabs.driver:id/fl_accept",
        "com.olacabs.driver:id/accept_ride",
        "com.olacabs.driver:id/accept_trip",
        "com.olacabs.driver:id/btn_accept_ride",
        "com.olacabs.driver:id/btn_accept_booking",
        "com.olacabs.driver:id/accept_booking",
        "com.olacabs.driver:id/accept_request",
        "com.olacabs.driver:id/accept_duty",
        // Bare IDs
        "btn_accept",
        "accept_btn",
        "accept_button",
        "accept_ride_button",
        "accept_order",
        "acceptOrderBtn",
        "btnAccept",
        "layout_accept",
        "action_accept",
        "button_accept",
        "buttonAccept",
        "slide_to_accept",
        "swipe_to_accept",
        "swipe_button",
        "swipe_btn",
        "slider_button",
        "slider",
        "slide_view",
        "fl_accept",
        "accept_ride",
        "accept_trip",
        "btn_accept_ride",
        "btn_accept_booking",
        "accept_booking",
        "accept_request",
        "accept_duty",
        "tv_accept",
        "tvAccept",
        "acceptTextView",
        "accept_container",
        "accept_card"
    )

    val ACCEPT_TEXTS = listOf(
        "Accept",
        "ACCEPT",
        "Accept Ride",
        "Accept Order",
        "Accept Booking",
        "Accept Trip",
        "Accept Duty",
        "Tap to Accept",
        "Swipe to Accept",
        "Slide to Accept",
        "SLIDE TO ACCEPT",
        "SWIPE TO ACCEPT",
        "TAP TO ACCEPT",
        "Slide to accept",
        "Swipe to accept",
        "Tap to accept",
        // Hindi
        "स्वीकार करें",
        "स्वीकार",
        "राइड स्वीकार करें",
        "ट्रिप स्वीकार करें",
        // Regional
        "ಸ್ವೀಕರಿಸಿ",
        "ஏற்றுக்கொள்",
        "ஏற்கவும்",
        "అంగీకరించు",
        "స్వీకరించు",
        "स्वीकारा",
        "গ্রহণ করুন"
    )

    val ACCEPT_CONTENT_DESCRIPTIONS = listOf(
        "Accept ride",
        "Accept order",
        "Accept booking",
        "Accept trip",
        "Accept",
        "ACCEPT",
        "Slide to accept",
        "Swipe to accept",
        "Tap to accept",
        "स्वीकार करें",
        "स्वीकार",
        "ಸ್ವೀಕರಿಸಿ",
        "ஏற்றுக்கொள்",
        "అంగీకరించు"
    )

    private val ACCEPT_REGEX = Pattern.compile(
        "(?:tap\\s*to\\s*|swipe\\s*to\\s*|slide\\s*to\\s*)?accept(?:\\s*(?:ride|order|booking|trip|duty))?",
        Pattern.CASE_INSENSITIVE
    )
    private val REJECT_REGEX = Pattern.compile(
        "^(?:tap\\s*to\\s*|swipe\\s*to\\s*)?(?:reject|decline|pass|skip)(?:\\s*(?:ride|order|booking|trip))?$",
        Pattern.CASE_INSENSITIVE
    )

    fun isAcceptText(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.trim().lowercase()
        if (lower.contains("don't accept") || lower.contains("do not accept") || lower.contains("cannot accept")) return false
        if (lower.contains("reject") || lower.contains("decline") || lower.contains("cancel") || lower.contains("skip")) return false

        if (lower.contains("accept") || lower.contains("slide to") || lower.contains("swipe to") || lower.contains("tap to accept")) {
            return true
        }
        if (ACCEPT_TEXTS.any { lower.contains(it.lowercase()) }) return true
        if (text.contains("स्वीकार") || text.contains("ಸ್ವೀಕರಿಸಿ") || text.contains("ஏற்க") || text.contains("అంగీకరించు") || text.contains("स्वीकारा") || text.contains("গ্রহণ")) return true
        if (ACCEPT_REGEX.matcher(text.trim()).find()) return true
        return false
    }

    fun isAcceptDesc(desc: String?): Boolean {
        if (desc.isNullOrBlank()) return false
        val lower = desc.trim().lowercase()
        if (lower.contains("don't accept") || lower.contains("do not accept") || lower.contains("cannot accept")) return false
        if (lower.contains("reject") || lower.contains("decline") || lower.contains("cancel") || lower.contains("skip")) return false

        if (lower.contains("accept") || lower.contains("slide to") || lower.contains("swipe to") || lower.contains("tap to accept")) {
            return true
        }
        if (ACCEPT_CONTENT_DESCRIPTIONS.any { lower.contains(it.lowercase()) }) return true
        if (desc.contains("स्वीकार") || desc.contains("ಸ್ವೀಕರಿಸಿ") || desc.contains("ஏற்க") || desc.contains("అంగీకరించు") || desc.contains("स्वीकारा")) return true
        return false
    }

    fun isSlideOrSwipe(node: AccessibilityNodeInfo): Boolean {
        val txt = node.text?.toString().orEmpty()
        val desc = node.contentDescription?.toString().orEmpty()
        val id = node.viewIdResourceName.orEmpty()
        val combined = "$txt $desc $id".lowercase()
        return combined.contains("slide") || combined.contains("swipe") || combined.contains("slider")
    }

    fun resolveClickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        if (node.isClickable) return node
        var curr: AccessibilityNodeInfo? = node.parent
        while (curr != null) {
            if (curr.isClickable) return curr
            val parent = curr.parent
            curr = parent
        }
        return node
    }

    fun findAcceptNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // 1. Direct text search queries for accept
        for (q in listOf(
            "Accept", "ACCEPT", "Accept Ride", "Accept Booking", "Accept Order", "Accept Trip",
            "Tap to Accept", "Swipe to Accept", "Slide to Accept",
            "स्वीकार करें", "स्वीकार", "राइड स्वीकार करें"
        )) {
            val list = root.findAccessibilityNodeInfosByText(q)
            if (!list.isNullOrEmpty()) {
                val target = resolveClickableTarget(list[0])
                for (i in 1 until list.size) list[i].recycle()
                Log.i(TAG, "✓ Ola Accept detected via text query '$q'")
                return target
            }
        }

        // 2. Resource IDs
        for (resId in ACCEPT_IDS) {
            val list = root.findAccessibilityNodeInfosByViewId(resId)
            if (!list.isNullOrEmpty()) {
                val target = resolveClickableTarget(list[0])
                for (i in 1 until list.size) list[i].recycle()
                Log.i(TAG, "✓ Ola Accept detected via Resource ID: '$resId'")
                return target
            }
        }

        // 3. Scan clickable nodes for accept text in self or any subtree child
        val clickableAccept = scanClickableNodesForAccept(root)
        if (clickableAccept != null) {
            Log.i(TAG, "✓ Ola Accept detected via Clickable Node Scan")
            return clickableAccept
        }

        // 4. Recursive text match across all nodes
        val textMatched = searchNode(root) { isAcceptText(it) }
        if (textMatched != null) {
            val target = resolveClickableTarget(textMatched)
            Log.i(TAG, "✓ Ola Accept detected via Text Match: '${textMatched.text}'")
            return target
        }

        // 5. Recursive content description match across all nodes
        val descMatched = searchNodeByDesc(root) { isAcceptDesc(it) }
        if (descMatched != null) {
            val target = resolveClickableTarget(descMatched)
            Log.i(TAG, "✓ Ola Accept detected via Content Desc: '${descMatched.contentDescription}'")
            return target
        }

        return null
    }

    private fun scanClickableNodesForAccept(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val clickableList = mutableListOf<AccessibilityNodeInfo>()
        collectClickableNodes(root, clickableList)

        for (node in clickableList) {
            val txt = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            if (isAcceptText(txt) || isAcceptDesc(desc)) {
                return node
            }
            val childTexts = mutableListOf<String>()
            collectChildTexts(node, childTexts)
            val combined = childTexts.joinToString(" ")
            if (isAcceptText(combined)) {
                return node
            }
        }
        return null
    }

    private fun collectChildTexts(node: AccessibilityNodeInfo?, outList: MutableList<String>) {
        if (node == null) return
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            child.text?.toString()?.trim()?.let { if (it.isNotEmpty()) outList.add(it) }
            child.contentDescription?.toString()?.trim()?.let { if (it.isNotEmpty()) outList.add(it) }
            collectChildTexts(child, outList)
            child.recycle()
        }
    }

    fun findRejectNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return searchNode(root) { text -> REJECT_REGEX.matcher(text).find() }
    }

    private fun collectClickableNodes(node: AccessibilityNodeInfo?, outList: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.isClickable) outList.add(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) collectClickableNodes(child, outList)
        }
    }

    private fun searchNode(node: AccessibilityNodeInfo?, predicate: (String) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        val txt = node.text?.toString()?.trim().orEmpty()
        if (txt.isNotEmpty() && predicate(txt)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val matched = searchNode(child, predicate)
            if (matched != null) return matched
            child?.recycle()
        }
        return null
    }

    private fun searchNodeByDesc(node: AccessibilityNodeInfo?, predicate: (String) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        val desc = node.contentDescription?.toString()?.trim().orEmpty()
        if (desc.isNotEmpty() && predicate(desc)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val matched = searchNodeByDesc(child, predicate)
            if (matched != null) return matched
            child?.recycle()
        }
        return null
    }
}

