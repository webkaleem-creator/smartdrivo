package com.example.engine

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern

data class DetectedButton(
    val node: AccessibilityNodeInfo,
    val buttonId: String,
    val packageName: String,
    val method: String = "Unknown"
)

data class RapidoOrderData(
    val baseFare: Float = 0f,
    val tipAmount: Float = 0f,
    val totalFare: Float = 0f,
    val pickupKm: Float? = null,
    val dropKm: Float? = null,
    val pickupAddress: String? = null,
    val dropAddress: String? = null,
    val bookingId: String? = null
)

object RapidoAdapter {

    private const val TAG = "RapidoAdapter"

    private fun logI(tag: String, msg: String) {
        try {
            Log.i(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }

    private fun logD(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }

    val KM_REGEX = Regex("""([0-9]+(?:\.[0-9]+)?)\s*km(?!\s*/\s*h|\s*ph|\s*/\s*hr)""", RegexOption.IGNORE_CASE)
    val RUPEE_AMOUNT_REGEX = Pattern.compile("₹\\s*([0-9]+(?:\\.[0-9]+)?)")

    val FORBIDDEN_ADDRESS_TEXTS = setOf(
        "auto", "services", "nearby",
        "view", "go to", "home", "credit", "orders", "surge",
        "accept", "decline", "reject", "pass", "skip",
        "ride", "trip", "captain", "rapido", "bike", "cab",
        "cash", "online", "payment", "collect cash", "collect",
        "drop", "pickup", "pick up", "from", "to", "destination",
        "driver", "captain app", "earning", "today"
    )

    fun isInvalidAddress(text: String?): Boolean {
        if (text.isNullOrBlank()) return true
        val clean = text.trim()
        if (clean.length < 2) return true
        val lower = clean.lowercase()

        // Requirement: Do NOT use "Auto", "Services", "Nearby" as addresses
        if (lower == "auto" || lower == "services" || lower == "nearby") return true
        if (lower.startsWith("auto ") || lower.startsWith("services ") || lower.startsWith("nearby ")) return true

        if (FORBIDDEN_ADDRESS_TEXTS.contains(lower)) return true

        // Cannot contain currency symbol
        if (clean.contains("₹") || lower.contains("rs.") || lower.contains("inr")) return true

        // Cannot contain km distance
        if (KM_REGEX.containsMatchIn(clean)) return true

        // Cannot be just numbers or rating
        if (clean.matches(Regex("^[0-9]+(?:\\.[0-9]+)?$"))) return true

        // Cannot be time duration e.g. "4 mins away", "5 min", "2 mins"
        if (clean.matches(Regex("^[0-9.]+\\s*(?:min|mins|minute|minutes|sec|secs|hr|hrs)(?:\\s+away)?$", RegexOption.IGNORE_CASE))) return true

        return false
    }

    fun cleanRapidoAddress(text: String?): String {
        if (text.isNullOrBlank()) return ""
        var res = text.trim()
        res = res.replace(KM_REGEX, "").trim()
        res = res.replace(RUPEE_AMOUNT_REGEX.toRegex(), "").trim()
        res = res.replace(Regex("""^[•\-\s:]+"""), "").trim()
        res = res.replace(Regex("""^(?:pickup|drop|pick\s*up|destination|from|to)[\s:]+""", RegexOption.IGNORE_CASE), "").trim()
        res = res.replace(Regex("""^[•\-\s:]+"""), "").trim()
        return res
    }

    data class TextNodeEntry(
        val text: String,
        val bounds: Rect,
        val viewId: String?
    )

    private fun collectEntries(node: AccessibilityNodeInfo?, list: MutableList<TextNodeEntry>) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val content = when {
            !text.isNullOrEmpty() -> text
            !desc.isNullOrEmpty() -> desc
            else -> null
        }
        if (!content.isNullOrEmpty()) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            list.add(TextNodeEntry(content, bounds, node.viewIdResourceName))
        }
        for (i in 0 until node.childCount) {
            collectEntries(node.getChild(i), list)
        }
    }

    /**
     * FIX 2 - Stats showing wrong data:
     * In RapidoAdapter.kt, extract fare correctly:
     * - Find node containing "₹" followed by numbers
     * - Format "₹83 +₹26" → base=83, tip=26, total=109
     * - Extract pickup km from node with pattern "[0-9.]+\\s*km" (first occurrence = pickup distance)
     * - Extract drop km (second occurrence = trip distance)
     * - Extract pickup address (text under first km node)
     * - Extract drop address (text under second km node)
     * - Do NOT use "Auto", "Services", "Nearby" as addresses
     */
    fun extractOrderData(root: AccessibilityNodeInfo): RapidoOrderData {
        val entries = mutableListOf<TextNodeEntry>()
        collectEntries(root, entries)
        val textList = entries.map { it.text }
        val data = extractOrderDataFromTexts(textList)

        // View ID based address extraction from node tree
        var pickupFromId: String? = null
        var dropFromId: String? = null
        for (entry in entries) {
            val id = entry.viewId?.lowercase().orEmpty()
            if (pickupFromId == null && (id.contains("pickup") || id.contains("source") || id.contains("start_loc") || id.contains("tv_pickup"))) {
                val clean = cleanRapidoAddress(entry.text)
                if (clean.length >= 3 && !isInvalidAddress(clean)) pickupFromId = clean
            }
            if (dropFromId == null && (id.contains("drop") || id.contains("dest") || id.contains("tv_drop"))) {
                val clean = cleanRapidoAddress(entry.text)
                if (clean.length >= 3 && !isInvalidAddress(clean)) dropFromId = clean
            }
        }

        // Booking ID extraction from text entries
        var bookingId: String? = null
        val bookingRegex = Regex("(?:CRN|ID|#|Booking\\s*ID)[:\\s-]*([A-Za-z0-9-]+)", RegexOption.IGNORE_CASE)
        for (entry in entries) {
            val match = bookingRegex.find(entry.text)
            if (match != null) {
                bookingId = match.groupValues[1].trim()
                break
            }
        }

        val finalPickup = pickupFromId ?: data.pickupAddress
        val finalDrop = dropFromId ?: data.dropAddress

        return data.copy(
            pickupAddress = finalPickup,
            dropAddress = finalDrop,
            bookingId = bookingId
        )
    }

    /**
     * Pure text-based extraction for unit testing and reliable order parsing.
     */
    fun extractOrderDataFromTexts(texts: List<String>): RapidoOrderData {
        // 1. Fare extraction:
        // - Base fare = FIRST ₹[number] found only
        // - Tip = FIRST +₹[number] immediately after base only
        // - Total = base + tip
        // - STOP after first base+tip pair found
        // - Ignore ALL other ₹ values from other order cards
        // - If no tip, total = base only
        var baseFare = 0f
        var tipAmount = 0f
        var totalFare = 0f

        val rupeeMatcher = Pattern.compile("₹\\s*([0-9]+(?:\\.[0-9]+)?)")
        val tipInSameStringRegex = Pattern.compile("^\\s*\\+\\s*₹?\\s*([0-9]+(?:\\.[0-9]+)?)")
        val tipNextStringRegex = Pattern.compile("^\\+?\\s*₹?\\s*([0-9]+(?:\\.[0-9]+)?)")

        for (i in texts.indices) {
            val text = texts[i]
            val matcher = rupeeMatcher.matcher(text)
            if (matcher.find()) {
                val parsedBase = matcher.group(1)?.toFloatOrNull()
                if (parsedBase != null) {
                    baseFare = parsedBase
                    val remainder = text.substring(matcher.end())
                    val sameStringTipMatcher = tipInSameStringRegex.matcher(remainder)
                    if (sameStringTipMatcher.find()) {
                        tipAmount = sameStringTipMatcher.group(1)?.toFloatOrNull() ?: 0f
                    } else if (remainder.trim().isEmpty() && i + 1 < texts.size) {
                        val nextText = texts[i + 1].trim()
                        if (nextText == "+" && i + 2 < texts.size) {
                            val nextRupeeMatcher = rupeeMatcher.matcher(texts[i + 2])
                            if (nextRupeeMatcher.find()) {
                                tipAmount = nextRupeeMatcher.group(1)?.toFloatOrNull() ?: 0f
                            }
                        } else if (nextText.startsWith("+")) {
                            val nextTipMatcher = tipNextStringRegex.matcher(nextText)
                            if (nextTipMatcher.find()) {
                                tipAmount = nextTipMatcher.group(1)?.toFloatOrNull() ?: 0f
                            }
                        }
                    }
                    totalFare = baseFare + tipAmount
                    logI(TAG, "💰 Rapido fare parsed: base=₹$baseFare, tip=₹$tipAmount, total=₹$totalFare (ignoring further ₹ values)")
                    break // STOP after first base+tip pair found! Ignore ALL other ₹ values from other order cards
                }
            }
        }

        // 2. Distance extraction: pattern [0-9.]+\s*km (excluding speed indicators)
        val kmMatches = mutableListOf<Pair<Int, Float>>()
        for (i in texts.indices) {
            val line = texts[i]
            val sanitized = line.replace(Regex("""[0-9]+(?:\.[0-9]+)?\s*(?:km/h|kmph|km\s*/\s*h|km/hr)\b""", RegexOption.IGNORE_CASE), " ")
            KM_REGEX.findAll(sanitized).forEach { m ->
                val kmVal = m.groupValues[1].toFloatOrNull()
                if (kmVal != null && kmVal > 0f) {
                    kmMatches.add(Pair(i, kmVal))
                }
            }
        }

        // Explicit drop/trip distance detection from labels or text
        var explicitDropKm: Float? = null
        for (line in texts) {
            val sanitized = line.replace(Regex("""[0-9]+(?:\.[0-9]+)?\s*(?:km/h|kmph|km\s*/\s*h|km/hr)\b""", RegexOption.IGNORE_CASE), " ")
            val dropMatch = Regex("""(?:(?:drop|trip|distance|destination)[^\n0-9]*([0-9]+(?:\.[0-9]+)?)\s*km)|(?:([0-9]+(?:\.[0-9]+)?)\s*km[^\n]*(?:drop|trip|distance|destination))""", RegexOption.IGNORE_CASE).find(sanitized)
            if (dropMatch != null) {
                val valStr = dropMatch.groupValues[1].ifEmpty { dropMatch.groupValues[2] }
                val parsed = valStr.toFloatOrNull()
                if (parsed != null && parsed > 0f) {
                    explicitDropKm = parsed
                    break
                }
            }
        }

        // BUG 1: If pickup distance is "Nearby" or unknown, do NOT assign a numeric value to pickupKm!
        val hasNearby = texts.any { it.trim().equals("nearby", ignoreCase = true) || it.contains("nearby", ignoreCase = true) }
        val pickupKm: Float?
        var dropKm: Float?

        if (explicitDropKm != null) {
            dropKm = explicitDropKm
            pickupKm = if (hasNearby) null else kmMatches.map { it.second }.firstOrNull { it != explicitDropKm }
        } else if (hasNearby) {
            // Pickup is explicitly "Nearby", so pickup distance is NOT a numeric value
            pickupKm = null
            // Any km match on screen belongs to drop/trip distance
            dropKm = kmMatches.firstOrNull()?.second
        } else {
            pickupKm = kmMatches.getOrNull(0)?.second
            dropKm = kmMatches.getOrNull(1)?.second
        }

        if (dropKm == null) {
            dropKm = OrderDataExtractor.extractDropDistance(texts, texts.joinToString(" "))
        }

        // 3. Address extraction:
        var pickupAddress: String? = null
        var dropAddress: String? = null

        // Step 3a: Explicit label prefix checks (e.g. "Pickup: Indiranagar", "Drop: Koramangala")
        for (i in texts.indices) {
            val raw = texts[i].trim()
            val lower = raw.lowercase()
            if (pickupAddress == null && (lower.startsWith("pickup:") || lower.startsWith("from:") || lower.startsWith("pick up:"))) {
                val clean = cleanRapidoAddress(raw.substringAfter(":"))
                if (clean.length >= 3 && !isInvalidAddress(clean)) pickupAddress = clean
            } else if (pickupAddress == null && (lower == "pickup" || lower == "from" || lower == "pick up")) {
                if (i + 1 < texts.size) {
                    val clean = cleanRapidoAddress(texts[i + 1])
                    if (clean.length >= 3 && !isInvalidAddress(clean)) pickupAddress = clean
                }
            }

            if (dropAddress == null && (lower.startsWith("drop:") || lower.startsWith("to:") || lower.startsWith("destination:"))) {
                val clean = cleanRapidoAddress(raw.substringAfter(":"))
                if (clean.length >= 3 && !isInvalidAddress(clean)) dropAddress = clean
            } else if (dropAddress == null && (lower == "drop" || lower == "to" || lower == "destination")) {
                if (i + 1 < texts.size) {
                    val clean = cleanRapidoAddress(texts[i + 1])
                    if (clean.length >= 3 && !isInvalidAddress(clean)) dropAddress = clean
                }
            }
        }

        // Step 3b: Proximity to km markers
        val firstKmIdx = kmMatches.getOrNull(0)?.first
        val secondKmIdx = kmMatches.getOrNull(1)?.first

        if (pickupAddress == null && firstKmIdx != null && !hasNearby) {
            val endSearch = secondKmIdx ?: texts.size
            for (i in (firstKmIdx + 1) until endSearch) {
                val clean = cleanRapidoAddress(texts[i])
                if (clean.length >= 3 && !isInvalidAddress(clean)) {
                    pickupAddress = clean
                    break
                }
            }
        }

        if (dropAddress == null && secondKmIdx != null) {
            for (i in (secondKmIdx + 1) until texts.size) {
                val clean = cleanRapidoAddress(texts[i])
                if (clean.length >= 3 && !isInvalidAddress(clean)) {
                    dropAddress = clean
                    break
                }
            }
        }

        // Step 3c: Filter candidate address strings from all texts
        val candidateLines = mutableListOf<String>()
        for (text in texts) {
            val clean = cleanRapidoAddress(text)
            if (clean.length >= 3 && !isInvalidAddress(clean) && !clean.contains("₹") && !KM_REGEX.containsMatchIn(clean)) {
                candidateLines.add(clean)
            }
        }

        if (pickupAddress == null && candidateLines.isNotEmpty()) {
            pickupAddress = candidateLines[0]
        }
        if (dropAddress == null && candidateLines.size >= 2) {
            dropAddress = candidateLines.firstOrNull { it != pickupAddress } ?: candidateLines[1]
        }

        // Step 3d (BUG 2): If address cannot be detected, save raw text from accessibility node instead of generic placeholder
        if (pickupAddress == null) {
            pickupAddress = texts.map { cleanRapidoAddress(it) }.firstOrNull { it.isNotBlank() && !it.contains("₹") && !it.matches(Regex("^[0-9.]+$")) }
                ?: texts.firstOrNull { it.isNotBlank() }
        }
        if (dropAddress == null) {
            dropAddress = texts.map { cleanRapidoAddress(it) }.filter { it != pickupAddress }.firstOrNull { it.isNotBlank() && !it.contains("₹") && !it.matches(Regex("^[0-9.]+$")) }
                ?: texts.drop(1).firstOrNull { it.isNotBlank() }
                ?: pickupAddress
        }

        return RapidoOrderData(
            baseFare = baseFare,
            tipAmount = tipAmount,
            totalFare = totalFare,
            pickupKm = pickupKm,
            dropKm = dropKm,
            pickupAddress = pickupAddress,
            dropAddress = dropAddress
        )
    }

    // 1. Check ALL these Rapido package names
    val RAPIDO_PACKAGES = listOf(
        "com.rapido.captain",
        "com.rapido.rider",
        "com.rapido.passenger"
    )

    // FIX 1: Rapido Accept Resource IDs (exact order from specification)
    val RAPIDO_ACCEPT_RESOURCE_IDS = listOf(
        "com.rapido.rider:id/accept_order",
        "com.rapido.rider:id/accept_button",
        "com.rapido.rider:id/acceptOrderBtn",
        "com.rapido.rider:id/btn_accept_ride",
        "com.rapido.rider:id/accept_ride_button",
        "com.rapido.rider:id/btn_accept",
        "com.rapido.rider:id/cta_accept",
        "com.rapido.rider:id/layout_accept",
        "com.rapido.rider:id/tv_accept",
        "com.rapido.rider:id/action_accept"
    )

    /**
     * STEP 1 - Find Accept node by Resource ID:
     * Try each ID using findAccessibilityNodeInfosByViewId():
     * Stop at first non-null result.
     */
    fun findAcceptNodeByResourceId(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (resId in RAPIDO_ACCEPT_RESOURCE_IDS) {
            val nodes = root.findAccessibilityNodeInfosByViewId(resId)
            if (!nodes.isNullOrEmpty()) {
                val node = nodes[0]
                for (i in 1 until nodes.size) nodes[i].recycle()
                Log.i(TAG, "✓ Rapido Accept found via Resource ID: $resId")
                return node
            }
        }
        val currentPkg = root.packageName?.toString().orEmpty()
        for (resId in RAPIDO_ACCEPT_RESOURCE_IDS) {
            val bareId = resId.substringAfter(":id/")
            if (currentPkg.isNotEmpty() && currentPkg != "com.rapido.rider") {
                val qualified = "$currentPkg:id/$bareId"
                val nodes = root.findAccessibilityNodeInfosByViewId(qualified)
                if (!nodes.isNullOrEmpty()) {
                    val node = nodes[0]
                    for (i in 1 until nodes.size) nodes[i].recycle()
                    Log.i(TAG, "✓ Rapido Accept found via Resource ID: $qualified")
                    return node
                }
            }
            val bareNodes = root.findAccessibilityNodeInfosByViewId(bareId)
            if (!bareNodes.isNullOrEmpty()) {
                val node = bareNodes[0]
                for (i in 1 until bareNodes.size) bareNodes[i].recycle()
                Log.i(TAG, "✓ Rapido Accept found via bare Resource ID: $bareId")
                return node
            }
        }
        return null
    }

    // METHOD A: Resource IDs to check (bare IDs requested and known variants)
    val BARE_ACCEPT_IDS = listOf(
        "accept_order",
        "accept_button",
        "acceptOrderBtn",
        "btn_accept_ride",
        "accept_ride_button",
        "btn_accept",
        "cta_accept",
        "layout_accept",
        "tv_accept",
        "action_accept",
        "btnAccept",
        "btn_accept_order",
        "button_accept",
        "accept_btn"
    )

    // METHOD B: Texts to match (Exact & keywords)
    val ACCEPT_TEXTS = listOf(
        "Accept",
        "ACCEPT",
        "Accept Ride",
        "Accept Order",
        "स्वीकार करें",
        "स्वीकार",
        "Tap to Accept",
        "Swipe to Accept"
    )

    // METHOD C: Content descriptions to match
    val ACCEPT_CONTENT_DESCRIPTIONS = listOf(
        "Accept ride",
        "Accept order",
        "Accept",
        "ACCEPT",
        "स्वीकार करें",
        "स्वीकार"
    )

    // Reject / Decline button IDs
    val BARE_REJECT_IDS = listOf(
        "declineOrderBtn",
        "btn_decline",
        "btnReject",
        "rejectOrderBtn",
        "btnDecline",
        "btn_reject",
        "button_decline",
        "button_reject",
        "decline_button",
        "layout_decline",
        "layout_reject",
        "reject_button",
        "tv_decline",
        "tv_reject"
    )

    val REJECT_TEXTS = listOf(
        "Decline",
        "DECLINE",
        "Reject",
        "REJECT",
        "Pass",
        "Skip",
        "अस्वीकार करें",
        "अस्वीकार"
    )

    fun isRapidoPackage(pkgName: String?): Boolean {
        if (pkgName == null) return false
        return RAPIDO_PACKAGES.any { pkgName.equals(it, ignoreCase = true) } ||
                pkgName.contains("rapido", ignoreCase = true)
    }

    // Forbidden node texts: Never click nodes with text: "View", "Services", "Go To", "Home", "Credit", "Orders", "Surge"
    val FORBIDDEN_TEXTS = listOf(
        "View",
        "Services",
        "Go To",
        "Home",
        "Credit",
        "Orders",
        "Surge"
    )

    fun isForbiddenText(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val trimmed = text.trim()
        return FORBIDDEN_TEXTS.any { forbidden ->
            trimmed.equals(forbidden, ignoreCase = true) ||
            trimmed.contains(forbidden, ignoreCase = true)
        }
    }

    /**
     * Checks if a string matches any of the accept text keywords (case-insensitive for English).
     */
    fun isAcceptText(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val trimmed = text.trim()
        if (isForbiddenText(trimmed)) return false
        val lower = trimmed.lowercase()
        if (lower.contains("don't accept") || lower.contains("do not accept") || lower.contains("cannot accept")) {
            return false
        }
        if (trimmed.equals("Accept", ignoreCase = true)) return true
        if (ACCEPT_TEXTS.any { lower.contains(it.lowercase()) }) return true
        if (trimmed.contains("स्वीकार करें") || trimmed.contains("स्वीकार")) return true
        return false
    }

    /**
     * Checks if a string matches any of the accept content descriptions.
     */
    fun isAcceptDescription(desc: String?): Boolean {
        if (desc.isNullOrBlank()) return false
        val trimmed = desc.trim()
        if (isForbiddenText(trimmed)) return false
        val lower = trimmed.lowercase()
        if (trimmed.equals("Accept", ignoreCase = true)) return true
        if (ACCEPT_CONTENT_DESCRIPTIONS.any { lower.contains(it.lowercase()) }) return true
        if (trimmed.contains("स्वीकार करें") || trimmed.contains("स्वीकार")) return true
        return false
    }

    /**
     * Brings Rapido Captain / Rider to the foreground using launch intent flags before clicking.
     */
    fun bringRapidoToForeground(context: Context, targetPkg: String? = null) {
        val packages = listOfNotNull(
            targetPkg?.takeIf { it.isNotEmpty() },
            "com.rapido.rider",
            "com.rapido.captain",
            "com.rapido.passenger"
        ).distinct()

        for (pkg in packages) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
                    context.startActivity(intent)
                    Log.i(TAG, "🚀 [RapidoAdapter] Brought Rapido to foreground: $pkg")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bring Rapido ($pkg) to foreground: ${e.message}")
            }
        }
    }

    /**
     * Finds node with exact or case-insensitive text/description "Accept" or "ACCEPT",
     * resolving to a clickable target (node or clickable ancestor).
     */
    fun findAcceptTextNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null

        // 1. Fast system text query for "Accept" and "ACCEPT"
        for (query in listOf("Accept", "ACCEPT", "accept")) {
            val list = root.findAccessibilityNodeInfosByText(query)
            if (!list.isNullOrEmpty()) {
                for (node in list) {
                    val text = node.text?.toString()?.trim()
                    val desc = node.contentDescription?.toString()?.trim()
                    if (isStrictAcceptMatch(text) || isStrictAcceptMatch(desc)) {
                        val target = resolveClickableTarget(node)
                        Log.i(TAG, "✓ findAcceptTextNode matched via system text query: text='$text', desc='$desc', id='${target.viewIdResourceName}', clickable=${target.isClickable}")
                        return target
                    }
                }
            }
        }

        // 2. Full recursive tree search for node with text or desc == "Accept" / "ACCEPT"
        val found = findNodeByPredicate(root) { node ->
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            isStrictAcceptMatch(text) || isStrictAcceptMatch(desc)
        }
        if (found != null) {
            val target = resolveClickableTarget(found)
            Log.i(TAG, "✓ findAcceptTextNode matched via tree search: text='${found.text}', id='${target.viewIdResourceName}', clickable=${target.isClickable}")
            return target
        }

        // 3. Clickable node scan with children having "Accept" / "ACCEPT"
        val clickableAccept = scanAllClickableNodesForAccept(root, root.packageName?.toString().orEmpty())
        if (clickableAccept != null) {
            Log.i(TAG, "✓ findAcceptTextNode matched via clickable scan: ${clickableAccept.buttonId}")
            return clickableAccept.node
        }

        return null
    }

    fun isStrictAcceptMatch(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val trimmed = value.trim()
        if (isForbiddenText(trimmed)) return false
        val lower = trimmed.lowercase()
        if (lower.contains("don't") || lower.contains("do not") || lower.contains("cannot") || lower.contains("cancel") || lower.contains("decline") || lower.contains("reject")) {
            return false
        }
        return trimmed.equals("Accept", ignoreCase = true) ||
               trimmed.equals("ACCEPT", ignoreCase = true) ||
               trimmed.contains("स्वीकार करें") ||
               trimmed.contains("स्वीकार") ||
               lower == "accept order" ||
               lower == "accept ride"
    }

    private fun findNodeByPredicate(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        if (predicate(node)) return node
        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i)
            val res = findNodeByPredicate(child, predicate)
            if (res != null) return res
        }
        return null
    }

    /**
     * Resolves a clickable target for a matching node.
     * If the node itself is clickable, returns it. Otherwise walks up the ancestor chain
     * to find the nearest clickable parent or container. If none is clickable, returns the node.
     */
    fun resolveClickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        if (node.isClickable) return node
        var curr: AccessibilityNodeInfo? = node.parent
        while (curr != null) {
            if (curr.isClickable) {
                return curr
            }
            val parent = curr.parent
            curr = parent
        }
        return node
    }

    /**
     * Scans and finds the accept button using MULTIPLE methods:
     * 1. METHOD 1: Direct "Accept" / "ACCEPT" Text Node Search
     * 2. METHOD 2: Scan ALL Clickable Nodes on Screen for "Accept" text or children text
     * 3. METHOD 3: By Resource ID
     * 4. METHOD 4: By Content Description
     * 5. Built-in findAccessibilityNodeInfosByText fallback
     */
    fun findAcceptButton(root: AccessibilityNodeInfo, currentPackage: String? = null): DetectedButton? {
        val rootPkg = currentPackage?.takeIf { it.isNotEmpty() } ?: root.packageName?.toString().orEmpty()

        // -------------------------------------------------------------
        // STEP 1 - Primary "Accept" / "ACCEPT" Text Node Search
        // -------------------------------------------------------------
        val acceptTextNode = findAcceptTextNode(root)
        if (acceptTextNode != null) {
            val id = acceptTextNode.viewIdResourceName ?: "accept_text_node"
            val text = acceptTextNode.text?.toString() ?: acceptTextNode.contentDescription?.toString() ?: "Accept"
            Log.i(TAG, "✓ Rapido Accept DETECTED via [STEP 1: Text 'Accept'/'ACCEPT'] -> ID: '$id', Text: '$text', Package: '$rootPkg'")
            return DetectedButton(
                node = acceptTextNode,
                buttonId = "$id (Text: '$text')",
                packageName = rootPkg,
                method = "ACCEPT_TEXT_NODE"
            )
        }

        // -------------------------------------------------------------
        // STEP 2 - Resource ID search
        // -------------------------------------------------------------
        val resIdNode = findAcceptNodeByResourceId(root)
        if (resIdNode != null) {
            val target = resolveClickableTarget(resIdNode)
            val id = resIdNode.viewIdResourceName ?: "accept_resource_id"
            val detectedPkg = target.packageName?.toString()?.takeIf { it.isNotEmpty() } ?: rootPkg
            Log.i(TAG, "✓ Rapido Accept DETECTED via [STEP 2: Resource ID] -> ID: '$id', Package: '$detectedPkg'")
            return DetectedButton(
                node = target,
                buttonId = id,
                packageName = detectedPkg,
                method = "RESOURCE_ID"
            )
        }

        // -------------------------------------------------------------
        // METHOD 1 (Scan ALL clickable nodes on screen)
        // -------------------------------------------------------------
        val clickableAccept = scanAllClickableNodesForAccept(root, rootPkg)
        if (clickableAccept != null) {
            Log.i(
                TAG,
                "✓ Rapido Accept DETECTED via [Method: Clickable Node Scan] -> Text: '${clickableAccept.buttonId}', Package: '${clickableAccept.packageName}'"
            )
            return clickableAccept
        }

        // -------------------------------------------------------------
        // METHOD B: By TEXT content (Deep tree scan across all nodes)
        // -------------------------------------------------------------
        val textMatchedNode = findNodeByTextPredicate(root) { isAcceptText(it) }
        if (textMatchedNode != null) {
            val target = resolveClickableTarget(textMatchedNode)
            val matchedText = textMatchedNode.text?.toString()?.trim() ?: "Accept"
            val id = target.viewIdResourceName ?: textMatchedNode.viewIdResourceName ?: "text_accept"
            val detectedPkg = target.packageName?.toString()?.takeIf { it.isNotEmpty() } ?: rootPkg
            Log.i(
                TAG,
                "✓ Rapido Accept DETECTED via [Method B: Text Content Match] -> Text: '$matchedText', Target ID: '$id', Package: '$detectedPkg'"
            )
            return DetectedButton(
                node = target,
                buttonId = "$id (Text: '$matchedText')",
                packageName = detectedPkg,
                method = "TEXT_CONTENT"
            )
        }

        // -------------------------------------------------------------
        // METHOD C: By Content Description
        // -------------------------------------------------------------
        val descMatchedNode = findNodeByContentDescPredicate(root) { isAcceptDescription(it) }
        if (descMatchedNode != null) {
            val target = resolveClickableTarget(descMatchedNode)
            val matchedDesc = descMatchedNode.contentDescription?.toString()?.trim() ?: "Accept"
            val id = target.viewIdResourceName ?: descMatchedNode.viewIdResourceName ?: "desc_accept"
            val detectedPkg = target.packageName?.toString()?.takeIf { it.isNotEmpty() } ?: rootPkg
            Log.i(
                TAG,
                "✓ Rapido Accept DETECTED via [Method C: Content Description Match] -> Desc: '$matchedDesc', Target ID: '$id', Package: '$detectedPkg'"
            )
            return DetectedButton(
                node = target,
                buttonId = "$id (Desc: '$matchedDesc')",
                packageName = detectedPkg,
                method = "CONTENT_DESC"
            )
        }

        // -------------------------------------------------------------
        // METHOD A: By Resource ID (Check all requested & known IDs)
        // -------------------------------------------------------------
        val packagesToCheck = (listOf(rootPkg) + RAPIDO_PACKAGES).filter { it.isNotEmpty() }.distinct()
        for (bareId in BARE_ACCEPT_IDS) {
            // Check fully qualified across packages
            for (pkg in packagesToCheck) {
                val fullResId = "$pkg:id/$bareId"
                val nodes = root.findAccessibilityNodeInfosByViewId(fullResId)
                if (!nodes.isNullOrEmpty()) {
                    val rawNode = nodes[0]
                    val target = resolveClickableTarget(rawNode)
                    val detectedPkg = target.packageName?.toString()?.takeIf { it.isNotEmpty() } ?: pkg
                    for (i in 1 until nodes.size) nodes[i].recycle()
                    Log.i(
                        TAG,
                        "✓ Rapido Accept DETECTED via [Method A: Resource ID] -> Full ID: '$fullResId', Package: '$detectedPkg'"
                    )
                    return DetectedButton(
                        node = target,
                        buttonId = fullResId,
                        packageName = detectedPkg,
                        method = "RESOURCE_ID"
                    )
                }
            }

            // Check bare ID directly
            val bareNodes = root.findAccessibilityNodeInfosByViewId(bareId)
            if (!bareNodes.isNullOrEmpty()) {
                val rawNode = bareNodes[0]
                val target = resolveClickableTarget(rawNode)
                val detectedPkg = target.packageName?.toString()?.takeIf { it.isNotEmpty() } ?: rootPkg
                for (i in 1 until bareNodes.size) bareNodes[i].recycle()
                Log.i(
                    TAG,
                    "✓ Rapido Accept DETECTED via [Method A: Bare Resource ID] -> ID: '$bareId', Package: '$detectedPkg'"
                )
                return DetectedButton(
                    node = target,
                    buttonId = bareId,
                    packageName = detectedPkg,
                    method = "BARE_RESOURCE_ID"
                )
            }
        }

        // -------------------------------------------------------------
        // METHOD 5: System text search fallback
        // -------------------------------------------------------------
        for (query in listOf("Accept", "ACCEPT", "Accept Ride", "Accept Order", "स्वीकार करें", "स्वीकार")) {
            val list = root.findAccessibilityNodeInfosByText(query)
            if (!list.isNullOrEmpty()) {
                val matched = list[0]
                val target = resolveClickableTarget(matched)
                val detectedPkg = target.packageName?.toString()?.takeIf { it.isNotEmpty() } ?: rootPkg
                val id = target.viewIdResourceName ?: "text_query_$query"
                for (i in 1 until list.size) list[i].recycle()
                Log.i(
                    TAG,
                    "✓ Rapido Accept DETECTED via [Method 5: System Text Search] -> Query: '$query', ID: '$id', Package: '$detectedPkg'"
                )
                return DetectedButton(
                    node = target,
                    buttonId = "$id (Query: '$query')",
                    packageName = detectedPkg,
                    method = "SYSTEM_TEXT_SEARCH"
                )
            }
        }

        Log.d(TAG, "Rapido Accept button NOT found after running all detection methods.")
        return null
    }

    /**
     * Finds and returns the first matching accept AccessibilityNodeInfo.
     */
    fun findAcceptNode(root: AccessibilityNodeInfo, currentPackage: String? = null): AccessibilityNodeInfo? {
        val textNode = findAcceptTextNode(root)
        if (textNode != null) return textNode
        return findAcceptButton(root, currentPackage)?.node
    }

    /**
     * Scans ALL clickable nodes on screen to find any node containing "Accept" or "स्वीकार".
     */
    private fun scanAllClickableNodesForAccept(root: AccessibilityNodeInfo, rootPkg: String): DetectedButton? {
        val clickableList = mutableListOf<AccessibilityNodeInfo>()
        collectClickableNodes(root, clickableList)

        for (node in clickableList) {
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()

            // Check node's own text or description
            if (isAcceptText(text) || isAcceptDescription(desc)) {
                val id = node.viewIdResourceName ?: "clickable_node"
                val detectedPkg = node.packageName?.toString()?.takeIf { it.isNotEmpty() } ?: rootPkg
                val label = text ?: desc ?: "Accept"
                return DetectedButton(
                    node = node,
                    buttonId = "$id (Text: '$label')",
                    packageName = detectedPkg,
                    method = "CLICKABLE_NODE_TEXT"
                )
            }

            // Check children's text inside this clickable container
            val childTexts = mutableListOf<String>()
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    val cText = child.text?.toString()?.trim()
                    val cDesc = child.contentDescription?.toString()?.trim()
                    if (isAcceptText(cText) || isAcceptDescription(cDesc)) {
                        child.recycle()
                        val id = node.viewIdResourceName ?: child.viewIdResourceName ?: "clickable_container"
                        val detectedPkg = node.packageName?.toString()?.takeIf { it.isNotEmpty() } ?: rootPkg
                        val matched = cText ?: cDesc ?: "Accept"
                        return DetectedButton(
                            node = node,
                            buttonId = "$id (Child: '$matched')",
                            packageName = detectedPkg,
                            method = "CLICKABLE_CONTAINER_CHILD_TEXT"
                        )
                    }
                    if (!cText.isNullOrEmpty()) childTexts.add(cText)
                    if (!cDesc.isNullOrEmpty()) childTexts.add(cDesc)
                    child.recycle()
                }
            }

            val combined = childTexts.joinToString(" ")
            if (isAcceptText(combined)) {
                val id = node.viewIdResourceName ?: "clickable_combined"
                val detectedPkg = node.packageName?.toString()?.takeIf { it.isNotEmpty() } ?: rootPkg
                return DetectedButton(
                    node = node,
                    buttonId = "$id (Combined: '$combined')",
                    packageName = detectedPkg,
                    method = "CLICKABLE_CONTAINER_COMBINED"
                )
            }
        }
        return null
    }

    /**
     * Tries reject button IDs and text in order.
     */
    fun findRejectNode(root: AccessibilityNodeInfo, currentPackage: String? = null): AccessibilityNodeInfo? {
        val rootPkg = currentPackage?.takeIf { it.isNotEmpty() } ?: root.packageName?.toString().orEmpty()

        // 1. Text & Desc check
        val textNode = findNodeByTextPredicate(root) { text ->
            REJECT_TEXTS.any { text.contains(it, ignoreCase = true) }
        }
        if (textNode != null) {
            val target = resolveClickableTarget(textNode)
            Log.i(TAG, "✓ Rapido Reject DETECTED via text match: '${textNode.text}'")
            return target
        }

        // 2. Resource IDs check
        val packagesToCheck = (listOf(rootPkg) + RAPIDO_PACKAGES).filter { it.isNotEmpty() }.distinct()
        for (bareId in BARE_REJECT_IDS) {
            for (pkg in packagesToCheck) {
                val fullResId = "$pkg:id/$bareId"
                val list = root.findAccessibilityNodeInfosByViewId(fullResId)
                if (!list.isNullOrEmpty()) {
                    val target = resolveClickableTarget(list[0])
                    for (i in 1 until list.size) list[i].recycle()
                    Log.i(TAG, "✓ Rapido Reject DETECTED via ID: '$fullResId'")
                    return target
                }
            }
            val bareList = root.findAccessibilityNodeInfosByViewId(bareId)
            if (!bareList.isNullOrEmpty()) {
                val target = resolveClickableTarget(bareList[0])
                for (i in 1 until bareList.size) bareList[i].recycle()
                Log.i(TAG, "✓ Rapido Reject DETECTED via bare ID: '$bareId'")
                return target
            }
        }
        return null
    }

    /**
     * Collects all clickable nodes in the subtree.
     */
    private fun collectClickableNodes(node: AccessibilityNodeInfo?, outList: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.isClickable) {
            outList.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectClickableNodes(child, outList)
            }
        }
    }

    /**
     * Finds first node satisfying text predicate.
     */
    private fun findNodeByTextPredicate(node: AccessibilityNodeInfo?, predicate: (String) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        val txt = node.text?.toString()?.trim()
        if (!txt.isNullOrEmpty() && predicate(txt)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val matched = findNodeByTextPredicate(child, predicate)
            if (matched != null) return matched
            child?.recycle()
        }
        return null
    }

    /**
     * Finds first node satisfying contentDescription predicate.
     */
    private fun findNodeByContentDescPredicate(node: AccessibilityNodeInfo?, predicate: (String) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        val desc = node.contentDescription?.toString()?.trim()
        if (!desc.isNullOrEmpty() && predicate(desc)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val matched = findNodeByContentDescPredicate(child, predicate)
            if (matched != null) return matched
            child?.recycle()
        }
        return null
    }

    /**
     * Requirement 4: Helper to summarize all clickable button texts and descriptions found on screen.
     */
    fun collectClickableButtonSummaries(root: AccessibilityNodeInfo?): List<String> {
        if (root == null) return emptyList()
        val list = mutableListOf<String>()

        fun walk(node: AccessibilityNodeInfo?) {
            if (node == null) return
            if (node.isClickable) {
                val text = node.text?.toString()?.trim()
                val desc = node.contentDescription?.toString()?.trim()
                val viewId = node.viewIdResourceName?.substringAfterLast(":id/") ?: node.viewIdResourceName
                val childTexts = mutableListOf<String>()
                for (i in 0 until node.childCount) {
                    val c = node.getChild(i)
                    if (c != null) {
                        c.text?.toString()?.trim()?.let { if (it.isNotEmpty()) childTexts.add(it) }
                        c.contentDescription?.toString()?.trim()?.let { if (it.isNotEmpty()) childTexts.add(it) }
                        c.recycle()
                    }
                }
                val label = when {
                    !text.isNullOrEmpty() -> "'$text'"
                    childTexts.isNotEmpty() -> "'${childTexts.joinToString(" ")}'"
                    !desc.isNullOrEmpty() -> "desc:'$desc'"
                    !viewId.isNullOrEmpty() -> "id:$viewId"
                    else -> node.className?.toString()?.substringAfterLast(".") ?: "View"
                }
                val idSuffix = if (!viewId.isNullOrEmpty() && !label.contains("id:")) " [id:$viewId]" else ""
                list.add("$label$idSuffix")
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    walk(child)
                    child.recycle()
                }
            }
        }
        walk(root)
        return list
    }
}

