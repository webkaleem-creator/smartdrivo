package com.example.engine

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.Platform
import com.example.model.RideCandidate
import com.example.model.VehicleType
import java.util.regex.Pattern

data class UberOrderData(
    val fare: Float? = null,
    val pickupKm: Float? = null,
    val dropKm: Float? = null,
    val pickupAddress: String? = null,
    val dropAddress: String? = null,
    val dropArea: String? = null,
    val bookingId: String? = null,
    val vehicleType: VehicleType? = null
)

data class UberNodeEntry(
    val text: String,
    val desc: String?,
    val viewId: String?,
    val bounds: Rect
)

object UberAdapter {

    private const val TAG = "UberAdapter"

    val UBER_PACKAGES = listOf(
        "com.ubercab.driver",
        "com.ubercab"
    )

    // Method A: Resource IDs for Uber accept
    val ACCEPT_IDS = listOf(
        "com.ubercab.driver:id/accept_button",
        "com.ubercab.driver:id/btn_accept",
        "com.ubercab.driver:id/btn_match",
        "com.ubercab.driver:id/confirm_button",
        "com.ubercab.driver:id/match_button",
        "com.ubercab.driver:id/trip_action_button",
        "com.ubercab.driver:id/trip_radar_action",
        "com.ubercab.driver:id/cta_button",
        // Bare IDs
        "accept_button",
        "btn_accept",
        "btn_match",
        "confirm_button",
        "match_button",
        "trip_action_button",
        "trip_radar_action",
        "cta_button"
    )

    val ACCEPT_TEXTS = listOf(
        "Accept",
        "ACCEPT",
        "Confirm",
        "CONFIRM",
        "Match",
        "MATCH",
        "Accept Trip",
        "Accept Delivery",
        "Tap to Accept",
        "Swipe to Accept",
        "स्वीकार करें",
        "स्वीकार"
    )

    val ACCEPT_CONTENT_DESCRIPTIONS = listOf(
        "Accept",
        "Confirm",
        "Match",
        "Accept trip",
        "Accept delivery",
        "स्वीकार करें"
    )

    val UBER_CARD_CLASSES = listOf(
        "com.ubercab.event_card_v2.CardJobOfferViewV2",
        "com.uber.carbon.driveroffersjobboard.browse.DriverOffersJobBoardBrowseView"
    )

    fun isAcceptText(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.trim().lowercase()
        if (lower.contains("don't accept") || lower.contains("do not accept")) return false
        if (ACCEPT_TEXTS.any { lower.contains(it.lowercase()) }) return true
        if (text.contains("स्वीकार करें") || text.contains("स्वीकार")) return true
        if (lower.contains("accept") || lower.contains("confirm") || lower == "match") return true
        return false
    }

    fun isAcceptDesc(desc: String?): Boolean {
        if (desc.isNullOrBlank()) return false
        val lower = desc.trim().lowercase()
        if (ACCEPT_CONTENT_DESCRIPTIONS.any { lower.contains(it.lowercase()) }) return true
        if (desc.contains("स्वीकार करें") || desc.contains("स्वीकार")) return true
        return false
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
        // 1. Scan clickable nodes for accept text
        val clickableAccept = scanClickableNodesForAccept(root)
        if (clickableAccept != null) {
            Log.i(TAG, "✓ Uber Accept detected via Clickable Node Scan")
            return clickableAccept
        }

        // 2. Text Content Match
        val textMatched = searchNode(root) { isAcceptText(it) }
        if (textMatched != null) {
            val target = resolveClickableTarget(textMatched)
            Log.i(TAG, "✓ Uber Accept detected via Text Match: '${textMatched.text}'")
            return target
        }

        // 3. Content Description Match
        val descMatched = searchNodeByDesc(root) { isAcceptDesc(it) }
        if (descMatched != null) {
            val target = resolveClickableTarget(descMatched)
            Log.i(TAG, "✓ Uber Accept detected via Content Desc: '${descMatched.contentDescription}'")
            return target
        }

        // 4. Resource IDs
        for (resId in ACCEPT_IDS) {
            val list = root.findAccessibilityNodeInfosByViewId(resId)
            if (!list.isNullOrEmpty()) {
                val target = resolveClickableTarget(list[0])
                for (i in 1 until list.size) list[i].recycle()
                Log.i(TAG, "✓ Uber Accept detected via Resource ID: '$resId'")
                return target
            }
        }

        // 5. System text search queries
        for (q in listOf("Accept", "Confirm", "Match", "स्वीकार करें")) {
            val list = root.findAccessibilityNodeInfosByText(q)
            if (!list.isNullOrEmpty()) {
                val target = resolveClickableTarget(list[0])
                for (i in 1 until list.size) list[i].recycle()
                Log.i(TAG, "✓ Uber Accept detected via text query '$q'")
                return target
            }
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
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { c ->
                    c.text?.toString()?.trim()?.let { if (it.isNotEmpty()) childTexts.add(it) }
                    c.contentDescription?.toString()?.trim()?.let { if (it.isNotEmpty()) childTexts.add(it) }
                    c.recycle()
                }
            }
            val combined = childTexts.joinToString(" ")
            if (isAcceptText(combined)) {
                return node
            }
        }
        return null
    }

    fun isUberOfferCardPresent(root: AccessibilityNodeInfo): Boolean {
        for (className in UBER_CARD_CLASSES) {
            val list = root.findAccessibilityNodeInfosByViewId(className)
            if (!list.isNullOrEmpty()) {
                for (n in list) n.recycle()
                return true
            }
        }
        return false
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

    // View IDs for Uber Pickup, Drop, and Fare
    val PICKUP_VIEW_IDS = listOf(
        "pickup_address", "ub__pickup_address", "pickup_location", "pickup_text",
        "origin_address", "origin_text", "source_address", "ub__pickup", "ub__origin",
        "address_pickup", "job_card_pickup", "trip_pickup", "txt_pickup"
    )

    val DROP_VIEW_IDS = listOf(
        "drop_address", "ub__dropoff_address", "ub__destination", "drop_location",
        "drop_text", "destination_address", "destination_text", "ub__dropoff",
        "address_dropoff", "job_card_dropoff", "trip_destination", "txt_drop"
    )

    val FARE_VIEW_IDS = listOf(
        "ub__fare", "fare_amount", "fare_text", "trip_fare", "rate_amount",
        "job_card_fare", "text_fare", "fare"
    )

    private val FARE_RUPEE_REGEX = Pattern.compile("₹\\s*([0-9]+(?:\\.[0-9]+)?)")
    private val FARE_GENERIC_REGEX = Pattern.compile("(?:rs\\.?|inr)\\s*([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE)
    private val SPEED_REGEX = Pattern.compile("[0-9]+(?:\\.[0-9]+)?\\s*(?:km/h|kmph|km\\s*/\\s*h|km/hr)\\b", Pattern.CASE_INSENSITIVE)
    private val PICKUP_KM_REGEX = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*km", Pattern.CASE_INSENSITIVE)
    private val DROP_KM_REGEX = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*km", Pattern.CASE_INSENSITIVE)
    private val BOOKING_ID_REGEX = Pattern.compile("CRN-?[0-9A-Z]+|BK-?[0-9A-Z]+|[0-9a-f]{8}-[0-9a-f]{4}", Pattern.CASE_INSENSITIVE)

    private val UBER_NON_ADDRESS_PATTERNS = listOf(
        Regex("^[₹$€£]|(?:rs\\.?|inr)\\b", RegexOption.IGNORE_CASE),
        Regex("^[0-9]+(?:\\.[0-9]+)?\\s*(?:km|mi|miles)\\b", RegexOption.IGNORE_CASE),
        Regex("^[0-9]+(?:\\.[0-9]+)?\\s*(?:min|mins|minute|minutes|sec|secs|hr|hrs)(?:\\s+(?:away|trip|total))?$", RegexOption.IGNORE_CASE),
        Regex("^[0-9.]+\\s*mins?\\s*(?:\\([0-9.]+\\s*km\\)|•\\s*[0-9.]+\\s*km)?(?:\\s*(?:away|trip))?$", RegexOption.IGNORE_CASE),
        Regex("[0-9]+(?:\\.[0-9]+)?\\s*(?:km/h|kmph|km\\s*/\\s*h)", RegexOption.IGNORE_CASE),
        Regex("^★?\\s*[0-9]\\.[0-9]{1,2}\\s*★?$"),
        Regex("^[0-9.]+$")
    )

    private val UBER_FORBIDDEN_KEYWORDS = setOf(
        "accept", "match", "confirm", "decline", "reject", "pass", "skip", "dismiss", "close",
        "tap to accept", "swipe to accept", "accept trip", "accept delivery",
        "स्वीकार करें", "स्वीकार",
        "uber", "uber go", "uber auto", "uber moto", "uber premier", "uberxl", "uber black",
        "uber connect", "uber comfort", "uber green", "uber pet", "uber taxi", "uber intercity",
        "uber package", "package", "delivery", "moto", "auto", "car", "cab", "bike", "sedan",
        "trip radar", "exclusive", "trip request", "new request", "upfront fare", "reserve",
        "scheduled", "priority", "surge", "boost",
        "you're online", "online", "offline", "finding matches", "finding riders",
        "back to radar", "matching", "earning", "earnings", "today", "weekly",
        "pickup", "dropoff", "drop off", "destination", "from", "to"
    )

    fun isInvalidAddress(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 2) return true
        val lower = trimmed.lowercase()
        if (UBER_FORBIDDEN_KEYWORDS.contains(lower)) return true
        if (UBER_NON_ADDRESS_PATTERNS.any { it.containsMatchIn(trimmed) }) return true
        if (lower.contains("don't accept") || lower.contains("tap to accept") || lower.contains("swipe to accept")) return true
        if (lower.contains("includes") && lower.contains("surge")) return true
        return false
    }

    fun cleanAddressLine(line: String): String {
        var res = line.trim()
        res = res.replace(Regex("""^[•\-\s:]+"""), "").trim()
        res = res.replace(Regex("""^(?:at|from|to|pickup\s*at|drop\s*at|destination\s*at)\s+""", RegexOption.IGNORE_CASE), "").trim()
        return res
    }

    /**
     * Extracts Uber ride information from raw text sequences
     */
    fun extractOrderDataFromTexts(texts: List<String>): UberOrderData {
        if (texts.isEmpty()) return UberOrderData()

        val fullText = texts.joinToString(" \n ")

        // 1. Extract Fare
        var detectedFare: Float? = null
        for (t in texts) {
            val trimmed = t.trim()
            if (trimmed.contains("today", ignoreCase = true) ||
                trimmed.contains("balance", ignoreCase = true) ||
                trimmed.contains("weekly", ignoreCase = true) ||
                trimmed.contains("wallet", ignoreCase = true)
            ) {
                continue
            }
            // Check for surge addition e.g. "₹80 +₹25"
            val surgeAddMatcher = Pattern.compile("₹\\s*([0-9]+(?:\\.[0-9]+)?)\\s*\\+\\s*₹?\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(trimmed)
            if (surgeAddMatcher.find()) {
                val f1 = surgeAddMatcher.group(1)?.toFloatOrNull() ?: 0f
                val f2 = surgeAddMatcher.group(2)?.toFloatOrNull() ?: 0f
                detectedFare = f1 + f2
                break
            }
            // Check if includes surge e.g. "₹150 includes ₹25 surge" -> 150
            val rupeeMatcher = FARE_RUPEE_REGEX.matcher(trimmed)
            if (rupeeMatcher.find()) {
                detectedFare = rupeeMatcher.group(1)?.toFloatOrNull()
                break
            }
        }
        if (detectedFare == null) {
            val genMatcher = FARE_GENERIC_REGEX.matcher(fullText)
            if (genMatcher.find()) {
                detectedFare = genMatcher.group(1)?.toFloatOrNull()
            }
        }

        // 2. Identify Pickup & Drop Markers / Distance
        var pickupMarkerIdx = -1
        var dropMarkerIdx = -1
        var pickupKm: Float? = null
        var dropKm: Float? = null
        val pickupMarkerRegex = Regex("""(?i)(?:away\b|pick\s*up|pickup|[0-9.]+\s*mins?\s*(?:\([0-9.]+\s*km\)|•\s*[0-9.]+\s*km)?\s*away|[0-9.]+\s*mins?\s*\(\s*[0-9.]+\s*km\s*\))""")
        val dropMarkerRegex = Regex("""(?i)(?:trip\b|drop\s*off|dropoff|destination|[0-9.]+\s*mins?\s*(?:\([0-9.]+\s*km\)|•\s*[0-9.]+\s*km)?\s*trip|[0-9.]+\s*mins?\s*\(\s*[0-9.]+\s*km\s*\))""")
        for (i in texts.indices) {
            val line = texts[i].trim()
            if (SPEED_REGEX.matcher(line).find()) continue

            if (pickupMarkerIdx == -1 && pickupMarkerRegex.containsMatchIn(line)) {
                pickupMarkerIdx = i
                val kmMatcher = PICKUP_KM_REGEX.matcher(line)
                if (kmMatcher.find()) {
                    pickupKm = kmMatcher.group(1)?.toFloatOrNull()
                }
            } else if (dropMarkerIdx == -1 && dropMarkerRegex.containsMatchIn(line)) {
                dropMarkerIdx = i
                val kmMatcher = DROP_KM_REGEX.matcher(line)
                if (kmMatcher.find()) {
                    dropKm = kmMatcher.group(1)?.toFloatOrNull()
                }
            }
        }

        // If distances not yet found, check all lines
        if (pickupKm == null) {
            for (i in texts.indices) {
                val line = texts[i].trim()
                if (SPEED_REGEX.matcher(line).find()) continue
                if (line.contains("away", ignoreCase = true) || line.contains("pickup", ignoreCase = true)) {
                    val m = PICKUP_KM_REGEX.matcher(line)
                    if (m.find()) {
                        pickupKm = m.group(1)?.toFloatOrNull()
                        break
                    }
                }
            }
        }
        if (dropKm == null) {
            for (i in texts.indices) {
                val line = texts[i].trim()
                if (SPEED_REGEX.matcher(line).find()) continue
                if (line.contains("trip", ignoreCase = true) || line.contains("drop", ignoreCase = true)) {
                    val m = DROP_KM_REGEX.matcher(line)
                    if (m.find()) {
                        dropKm = m.group(1)?.toFloatOrNull()
                        break
                    }
                }
            }
        }

        // =========================================================
        // UBER_CURRENT_CARD_FALLBACK
        //
        // Current Uber Trip Radar format:
        //   6 min (0.8 km)
        //   Pickup address
        //   9 mins (2.4 km)
        //   Drop address
        //
        // Uber does not always include words like "pickup",
        // "away", "trip" or "drop" in these distance rows.
        // First timed-km row = pickup.
        // Second timed-km row = trip/drop.
        // =========================================================

        if (
            pickupKm == null ||
            dropKm == null ||
            pickupMarkerIdx == -1 ||
            dropMarkerIdx == -1
        ) {

            val timedKmRegex =
                Regex(
                    """(?i)\b[0-9]+(?:\.[0-9]+)?\s*mins?\s*\(\s*([0-9]+(?:\.[0-9]+)?)\s*km\s*\)"""
                )

            val timedKmRows =
                mutableListOf<Pair<Int, Float>>()

            var lastNormalizedTimedRow: String? = null

            for (i in texts.indices) {

                val line =
                    texts[i].trim()

                if (
                    line.isBlank() ||
                    SPEED_REGEX.matcher(line).find()
                ) {
                    continue
                }

                val match =
                    timedKmRegex.find(line)

                if (match != null) {

                    val km =
                        match.groupValues
                            .getOrNull(1)
                            ?.toFloatOrNull()

                    if (
                        km != null &&
                        km > 0f
                    ) {

                        val normalized =
                            line.lowercase()
                                .replace(
                                    Regex("""\s+"""),
                                    " "
                                )
                                .trim()

                        // Some Accessibility trees expose the same
                        // visible row twice through parent/child nodes.
                        if (
                            normalized !=
                            lastNormalizedTimedRow
                        ) {
                            timedKmRows.add(
                                i to km
                            )

                            lastNormalizedTimedRow =
                                normalized
                        }
                    }
                }
            }


            if (timedKmRows.size >= 2) {

                val firstRow =
                    timedKmRows[0]

                val secondRow =
                    timedKmRows[1]


                if (
                    pickupKm == null ||
                    pickupKm <= 0f
                ) {
                    pickupKm =
                        firstRow.second
                }


                if (
                    dropKm == null ||
                    dropKm <= 0f
                ) {
                    dropKm =
                        secondRow.second
                }


                if (
                    pickupMarkerIdx == -1
                ) {
                    pickupMarkerIdx =
                        firstRow.first
                }


                if (
                    dropMarkerIdx == -1
                ) {
                    dropMarkerIdx =
                        secondRow.first
                }


                Log.i(
                    TAG,
                    "UBER CURRENT CARD parsed: " +
                        "pickup=${pickupKm}km " +
                        "trip=${dropKm}km"
                )
            }
        }

        // =========================================================
        // UBER_ANY_TWO_KM_FALLBACK
        //
        // Some Uber versions split:
        //   "6 min"   "(0.9 km)"
        // into different accessibility nodes.
        //
        // On a verified offer, the first two genuine KM values are:
        //   first  = pickup distance
        //   second = trip/drop distance
        // =========================================================

        if (pickupKm == null || dropKm == null) {

            val allKmValues =
                mutableListOf<Pair<Int, Float>>()

            for (i in texts.indices) {

                val line = texts[i].trim()

                if (
                    line.isBlank() ||
                    SPEED_REGEX.matcher(line).find()
                ) {
                    continue
                }

                val kmMatcher =
                    Regex(
                        """(?i)([0-9]+(?:\.[0-9]+)?)\s*km\b"""
                    )

                for (match in kmMatcher.findAll(line)) {

                    val value =
                        match.groupValues[1]
                            .toFloatOrNull()

                    if (
                        value != null &&
                        value > 0f &&
                        value < 200f
                    ) {

                        if (
                            allKmValues.none {
                                it.first == i &&
                                it.second == value
                            }
                        ) {
                            allKmValues.add(
                                i to value
                            )
                        }
                    }
                }
            }

            if (allKmValues.size >= 2) {

                if (
                    pickupKm == null ||
                    pickupKm <= 0f
                ) {
                    pickupKm =
                        allKmValues[0].second
                }

                if (
                    dropKm == null ||
                    dropKm <= 0f
                ) {
                    dropKm =
                        allKmValues[1].second
                }

                if (pickupMarkerIdx == -1) {
                    pickupMarkerIdx =
                        allKmValues[0].first
                }

                if (dropMarkerIdx == -1) {
                    dropMarkerIdx =
                        allKmValues[1].first
                }

                Log.i(
                    TAG,
                    "UBER any-two-km fallback: " +
                        "pickup=${pickupKm} km, trip=${dropKm} km"
                )
            }
        }

        // 3. Address Extraction
        var pickupAddress: String? = null
        var dropAddress: String? = null

        if (pickupMarkerIdx != -1 && dropMarkerIdx != -1 && pickupMarkerIdx < dropMarkerIdx) {
            // Lines between pickupMarker and dropMarker are pickup address lines
            val pLines = mutableListOf<String>()
            for (i in (pickupMarkerIdx + 1) until dropMarkerIdx) {
                val line = texts[i].trim()
                if (!isInvalidAddress(line)) {
                    pLines.add(cleanAddressLine(line))
                }
            }
            if (pLines.isNotEmpty()) {
                pickupAddress = pLines.joinToString(", ")
            }

            // Lines after dropMarker are drop address lines (until accept button / action)
            val dLines = mutableListOf<String>()
            for (i in (dropMarkerIdx + 1) until texts.size) {
                val line = texts[i].trim()
                if (isAcceptText(line) || isAcceptDesc(line)) break
                if (!isInvalidAddress(line)) {
                    dLines.add(cleanAddressLine(line))
                }
            }
            if (dLines.isNotEmpty()) {
                dropAddress = dLines.joinToString(", ")
            }
        }

        // Fallback: If either pickup or drop address is still missing
        if (pickupAddress.isNullOrBlank() || dropAddress.isNullOrBlank()) {
            val candidateLines = mutableListOf<String>()
            for (line in texts) {
                val trimmed = line.trim()
                if (!isInvalidAddress(trimmed) && !isAcceptText(trimmed) && !isAcceptDesc(trimmed)) {
                    candidateLines.add(cleanAddressLine(trimmed))
                }
            }

            if (candidateLines.size >= 2) {
                if (pickupAddress.isNullOrBlank() && dropAddress.isNullOrBlank()) {
                    if (candidateLines.size == 2) {
                        pickupAddress = candidateLines[0]
                        dropAddress = candidateLines[1]
                    } else if (candidateLines.size == 4) {
                        pickupAddress = "${candidateLines[0]}, ${candidateLines[1]}"
                        dropAddress = "${candidateLines[2]}, ${candidateLines[3]}"
                    } else {
                        // Split evenly
                        val mid = candidateLines.size / 2
                        pickupAddress = candidateLines.subList(0, mid).joinToString(", ")
                        dropAddress = candidateLines.subList(mid, candidateLines.size).joinToString(", ")
                    }
                } else if (pickupAddress.isNullOrBlank()) {
                    pickupAddress = candidateLines.firstOrNull { it != dropAddress }
                } else if (dropAddress.isNullOrBlank()) {
                    dropAddress = candidateLines.lastOrNull { it != pickupAddress }
                }
            } else if (candidateLines.size == 1) {
                if (pickupAddress.isNullOrBlank()) {
                    pickupAddress = candidateLines[0]
                } else if (dropAddress.isNullOrBlank()) {
                    dropAddress = candidateLines[0]
                }
            }
        }

        // Vehicle detection
        val lowerFull = fullText.lowercase()
        val detectedVehicle = when {
            lowerFull.contains("uber auto") || lowerFull.contains("auto") -> VehicleType.AUTO
            lowerFull.contains("uber moto") || lowerFull.contains("moto") || lowerFull.contains("bike") -> VehicleType.BIKE
            lowerFull.contains("uber go") || lowerFull.contains("uber premier") || lowerFull.contains("uberxl") ||
            lowerFull.contains("car") || lowerFull.contains("cab") || lowerFull.contains("sedan") -> VehicleType.CAR
            else -> null
        }

        val dropArea = dropAddress?.split(",")?.firstOrNull()?.trim() ?: dropAddress

        // Booking ID
        val bookingMatcher = BOOKING_ID_REGEX.matcher(fullText)
        val bookingId = if (bookingMatcher.find()) bookingMatcher.group(0) else null

        return UberOrderData(
            fare = detectedFare,
            pickupKm = pickupKm,
            dropKm = dropKm,
            pickupAddress = pickupAddress?.takeIf { it.isNotBlank() },
            dropAddress = dropAddress?.takeIf { it.isNotBlank() },
            dropArea = dropArea?.takeIf { it.isNotBlank() },
            bookingId = bookingId,
            vehicleType = detectedVehicle
        )
    }

    /**
     * Extracts Uber trip details using View IDs, Accessibility Content Descriptions, and Text Hierarchies
     */
    fun extractOrderData(root: AccessibilityNodeInfo): UberOrderData {
        val nodeEntries = mutableListOf<UberNodeEntry>()
        collectUberNodeEntries(root, nodeEntries)

        // 1. Check View IDs
        var viewIdPickup: String? = null
        var viewIdDrop: String? = null
        var viewIdFare: Float? = null

        for (entry in nodeEntries) {
            val vId = entry.viewId?.lowercase().orEmpty()
            if (vId.isNotEmpty()) {
                if (viewIdPickup == null && PICKUP_VIEW_IDS.any { vId.contains(it) }) {
                    val candidate = entry.text.ifEmpty { entry.desc.orEmpty() }
                    if (!isInvalidAddress(candidate)) {
                        viewIdPickup = cleanAddressLine(candidate)
                    }
                }
                if (viewIdDrop == null && DROP_VIEW_IDS.any { vId.contains(it) }) {
                    val candidate = entry.text.ifEmpty { entry.desc.orEmpty() }
                    if (!isInvalidAddress(candidate)) {
                        viewIdDrop = cleanAddressLine(candidate)
                    }
                }
                if (viewIdFare == null && FARE_VIEW_IDS.any { vId.contains(it) }) {
                    val candidate = entry.text.ifEmpty { entry.desc.orEmpty() }
                    val rm = FARE_RUPEE_REGEX.matcher(candidate)
                    if (rm.find()) {
                        viewIdFare = rm.group(1)?.toFloatOrNull()
                    }
                }
            }
        }

        // 2. Check Content Descriptions
        var descPickup: String? = null
        var descDrop: String? = null
        var descFare: Float? = null

        val descPickupPattern = Regex("""(?i)(?:pick\s*up\s*(?:at|from|location)?|pickup\s*(?:at|from|location)?|from)[:\s]+(.+)""")
        val descDropPattern = Regex("""(?i)(?:drop\s*off\s*(?:at|to|location)?|dropoff\s*(?:at|to|location)?|destination\s*(?:at|to|location)?|to)[:\s]+(.+)""")
        val descFarePattern = Regex("""(?i)(?:fare|estimated\s*earnings?|amount)[:\s]*₹\s*([0-9]+(?:\.[0-9]+)?)""")

        for (entry in nodeEntries) {
            val d = entry.desc.orEmpty()
            if (d.isNotEmpty()) {
                if (descPickup == null) {
                    val match = descPickupPattern.find(d)
                    if (match != null) {
                        val c = match.groupValues[1].trim()
                        if (!isInvalidAddress(c)) descPickup = cleanAddressLine(c)
                    }
                }
                if (descDrop == null) {
                    val match = descDropPattern.find(d)
                    if (match != null) {
                        val c = match.groupValues[1].trim()
                        if (!isInvalidAddress(c)) descDrop = cleanAddressLine(c)
                    }
                }
                if (descFare == null) {
                    val match = descFarePattern.find(d)
                    if (match != null) {
                        descFare = match.groupValues[1].toFloatOrNull()
                    }
                }
            }
        }

        // 3. Extract from sequential texts
        val texts = mutableListOf<String>()
        for (entry in nodeEntries) {
            if (entry.text.isNotEmpty()) texts.add(entry.text)
            else if (!entry.desc.isNullOrEmpty()) texts.add(entry.desc)
        }
        val fromTexts = extractOrderDataFromTexts(texts)

        val finalPickup = viewIdPickup ?: descPickup ?: fromTexts.pickupAddress
        val finalDrop = viewIdDrop ?: descDrop ?: fromTexts.dropAddress
        val finalFare = viewIdFare ?: descFare ?: fromTexts.fare
        val finalDropArea = fromTexts.dropArea ?: (finalDrop?.split(",")?.firstOrNull()?.trim() ?: finalDrop)

        return UberOrderData(
            fare = finalFare,
            pickupKm = fromTexts.pickupKm,
            dropKm = fromTexts.dropKm,
            pickupAddress = finalPickup,
            dropAddress = finalDrop,
            dropArea = finalDropArea,
            bookingId = fromTexts.bookingId,
            vehicleType = fromTexts.vehicleType
        )
    }

    private fun collectUberNodeEntries(node: AccessibilityNodeInfo?, outList: MutableList<UberNodeEntry>) {
        if (node == null) return
        val txt = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        if (!txt.isNullOrEmpty() || !desc.isNullOrEmpty()) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            outList.add(
                UberNodeEntry(
                    text = txt.orEmpty(),
                    desc = desc,
                    viewId = node.viewIdResourceName,
                    bounds = bounds
                )
            )
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectUberNodeEntries(child, outList)
                child.recycle()
            }
        }
    }

    fun collectAllNodeTexts(node: AccessibilityNodeInfo?): List<String> {
        val list = mutableListOf<String>()
        collectTextsRecursive(node, list)
        return list
    }

    private fun collectTextsRecursive(node: AccessibilityNodeInfo?, outList: MutableList<String>) {
        if (node == null) return
        node.text?.toString()?.trim()?.let { if (it.isNotEmpty()) outList.add(it) }
        node.contentDescription?.toString()?.trim()?.let { if (it.isNotEmpty()) outList.add(it) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectTextsRecursive(child, outList)
                child.recycle()
            }
        }
    }

    /**
     * Builds a RideCandidate directly from Uber Accessibility tree
     */
    fun extractCandidate(root: AccessibilityNodeInfo, defaultVehicle: VehicleType = VehicleType.AUTO): RideCandidate {
        val data = extractOrderData(root)
        val textList = collectAllNodeTexts(root)
        val fullText = textList.joinToString(" \n ")

        val finalFare = data.fare ?: OrderDataExtractor.extractFare(textList, fullText)
        val finalPickupKm = data.pickupKm ?: OrderDataExtractor.extractPickupDistance(textList, fullText)
        val finalDropKm = data.dropKm ?: OrderDataExtractor.extractDropDistance(textList, fullText)
        val finalVehicle = data.vehicleType ?: OrderDataExtractor.detectVehicleType(fullText, defaultVehicle)
        val finalBookingId = data.bookingId ?: OrderDataExtractor.extractBookingId(fullText)

        return RideCandidate(
            fare = finalFare,
            pickupDistKm = finalPickupKm,
            dropDistKm = finalDropKm,
            pickupAddress = data.pickupAddress?.takeIf { it.isNotBlank() },
            dropAddress = data.dropAddress?.takeIf { it.isNotBlank() },
            dropArea = data.dropArea ?: (data.dropAddress?.split(",")?.firstOrNull()?.trim() ?: "City Area"),
            platform = Platform.UBER,
            vehicleType = finalVehicle,
            bookingId = finalBookingId,
            detectionTimeMs = System.currentTimeMillis()
        )
    }
}

