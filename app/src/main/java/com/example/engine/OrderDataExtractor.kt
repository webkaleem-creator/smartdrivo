package com.example.engine

import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.Platform
import com.example.model.RideCandidate
import com.example.model.VehicleType
import java.util.regex.Pattern

/**
 * Extracts ride parameters from AccessibilityNodeInfo tree and texts
 */
object OrderDataExtractor {

    // Speed filter regex - ignore km/h values
    private val SPEED_REGEX = Pattern.compile("[0-9]+(?:\\.[0-9]+)?\\s*(?:km/h|kmph|km\\s*/\\s*h|km/hr)\\b", Pattern.CASE_INSENSITIVE)

    // Pickup distance regex
    val PICKUP_REGEX = Pattern.compile("(?:(?:pickup|pick\\s*up|away)[\\s:•-]*([0-9]+(?:\\.[0-9]+)?)\\s*(?:km)?)|(?:([0-9]+(?:\\.[0-9]+)?)\\s*(?:km)?\\s*(?:pickup|pick\\s*up|away))", Pattern.CASE_INSENSITIVE)

    // Drop distance regex
    val DROP_REGEX = Pattern.compile(
        "(?:(?:drop|trip|distance|destination)[\\s:•-]*([0-9]+(?:\\.[0-9]+)?)\\s*(?:km)?)" +
        "|(?:([0-9]+(?:\\.[0-9]+)?)\\s*(?:km)?\\s*(?:drop|trip|distance|destination))",
        Pattern.CASE_INSENSITIVE
    )

    // Fare / Currency regex
    private val FARE_RUPEE_REGEX = Pattern.compile("₹\\s*([0-9]+(?:\\.[0-9]+)?)")
    private val FARE_GENERIC_REGEX = Pattern.compile("(?:rs\\.?|inr)\\s*([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE)

    // Bundle order regex
    private val BUNDLE_REGEX = Pattern.compile("bundle\\s*order|bundled\\s*order", Pattern.CASE_INSENSITIVE)

    // Order missed / expired messages
    private val MISSED_REGEX = Pattern.compile("already\\s*accepted\\s*by\\s*another|no\\s*longer\\s*available|order\\s*missed|ride\\s*missed", Pattern.CASE_INSENSITIVE)

    fun isMissedOrder(fullText: String): Boolean {
        return MISSED_REGEX.matcher(fullText).find()
    }

    fun isBundleOrder(fullText: String): Boolean {
        return BUNDLE_REGEX.matcher(fullText).find()
    }

    /**
     * Extracts RideCandidate from the accessibility node tree
     */
    fun extractCandidate(root: AccessibilityNodeInfo, platform: Platform, defaultVehicle: VehicleType = VehicleType.AUTO): RideCandidate {
        if (platform == Platform.UBER) {
            return UberAdapter.extractCandidate(root, defaultVehicle)
        }

        val textList = mutableListOf<String>()
        collectAllText(root, textList)
        val combinedText = textList.joinToString(" \n ")

        val isBundled = isBundleOrder(combinedText)
        val fare = extractFare(textList, combinedText)
        val pickupKm = extractPickupDistance(textList, combinedText)
        val dropKm = extractDropDistance(textList, combinedText)

        val (pickupAddr, dropAddr, dropArea) = extractAddresses(textList, combinedText)

        // Vehicle detection if specified in text
        val detectedVehicle = detectVehicleType(combinedText, defaultVehicle)

        return RideCandidate(
            fare = fare,
            pickupDistKm = pickupKm,
            dropDistKm = dropKm,
            pickupAddress = pickupAddr,
            dropAddress = dropAddr,
            dropArea = dropArea ?: "Main City Area",
            platform = platform,
            vehicleType = detectedVehicle,
            bookingId = extractBookingId(combinedText),
            isBundledOrder = isBundled
        )
    }

    private fun collectAllText(node: AccessibilityNodeInfo?, outList: MutableList<String>) {
        if (node == null) return
        node.text?.toString()?.trim()?.let { if (it.isNotEmpty()) outList.add(it) }
        node.contentDescription?.toString()?.trim()?.let { if (it.isNotEmpty()) outList.add(it) }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectAllText(child, outList)
                child.recycle()
            }
        }
    }

    fun extractFare(lines: List<String>, fullText: String): Float? {
        // Look for all numbers after ₹ and sum them as total fare (e.g. format "₹73 +₹40")
        val rupeeMatcher = FARE_RUPEE_REGEX.matcher(fullText)
        val rupeeAmounts = mutableListOf<Float>()
        while (rupeeMatcher.find()) {
            rupeeMatcher.group(1)?.toFloatOrNull()?.let { rupeeAmounts.add(it) }
        }
        if (rupeeAmounts.isNotEmpty()) {
            return rupeeAmounts.sum()
        }

        // Generic rs/inr fallback
        val genericMatcher = FARE_GENERIC_REGEX.matcher(fullText)
        val genericAmounts = mutableListOf<Float>()
        while (genericMatcher.find()) {
            genericMatcher.group(1)?.toFloatOrNull()?.let { genericAmounts.add(it) }
        }
        if (genericAmounts.isNotEmpty()) {
            return genericAmounts.sum()
        }

        // Check single lines
        for (line in lines) {
            val mLine = FARE_RUPEE_REGEX.matcher(line)
            val lineAmounts = mutableListOf<Float>()
            while (mLine.find()) {
                mLine.group(1)?.toFloatOrNull()?.let { lineAmounts.add(it) }
            }
            if (lineAmounts.isNotEmpty()) return lineAmounts.sum()
        }
        return null
    }

    fun extractPickupDistance(lines: List<String>, fullText: String): Float? {
        // Exclude speed first
        val sanitized = SPEED_REGEX.matcher(fullText).replaceAll(" ")
        val m1 = PICKUP_REGEX.matcher(sanitized)
        if (m1.find()) {
            val g1 = m1.group(1)
            val g2 = m1.group(2)
            val v = (if (!g1.isNullOrEmpty()) g1 else g2)?.toFloatOrNull()
            if (v != null && v > 0f) return v
        }
        for (line in lines) {
            if (SPEED_REGEX.matcher(line).find()) continue
            val lm = PICKUP_REGEX.matcher(line)
            if (lm.find()) {
                val g1 = lm.group(1)
                val g2 = lm.group(2)
                val v = (if (!g1.isNullOrEmpty()) g1 else g2)?.toFloatOrNull()
                if (v != null && v > 0f) return v
            }
        }
        return null
    }

    fun extractDropDistance(lines: List<String>, fullText: String): Float? {
        val sanitized = SPEED_REGEX.matcher(fullText).replaceAll(" ")
        val m1 = DROP_REGEX.matcher(sanitized)
        if (m1.find()) {
            val g1 = m1.group(1)
            val g2 = m1.group(2)
            val v = (if (!g1.isNullOrEmpty()) g1 else g2)?.toFloatOrNull()
            if (v != null && v > 0f) return v
        }
        for (line in lines) {
            if (SPEED_REGEX.matcher(line).find()) continue
            val lm = DROP_REGEX.matcher(line)
            if (lm.find()) {
                val g1 = lm.group(1)
                val g2 = lm.group(2)
                val v = (if (!g1.isNullOrEmpty()) g1 else g2)?.toFloatOrNull()
                if (v != null && v > 0f) return v
            }
        }
        return null
    }

    private fun extractAddresses(lines: List<String>, fullText: String): Triple<String?, String?, String?> {
        var pickup: String? = null
        var drop: String? = null

        for (i in lines.indices) {
            val raw = lines[i].trim()
            val lower = raw.lowercase()

            if (pickup == null && (lower.startsWith("pickup:") || lower.startsWith("from:") || lower.startsWith("pick up:"))) {
                val candidate = raw.substringAfter(":").trim()
                if (candidate.length >= 3) pickup = candidate
            } else if (pickup == null && (lower == "pickup" || lower == "pick up" || lower == "from")) {
                if (i + 1 < lines.size && lines[i + 1].trim().length >= 3) {
                    pickup = lines[i + 1].trim()
                }
            }

            if (drop == null && (lower.startsWith("drop:") || lower.startsWith("destination:") || lower.startsWith("to:"))) {
                val candidate = raw.substringAfter(":").trim()
                if (candidate.length >= 3) drop = candidate
            } else if (drop == null && (lower == "drop" || lower == "destination" || lower == "to")) {
                if (i + 1 < lines.size && lines[i + 1].trim().length >= 3) {
                    drop = lines[i + 1].trim()
                }
            }
        }

        // Fallback: extract candidate address lines if labels weren't explicitly found
        if (pickup == null || drop == null) {
            val candidates = lines.map { it.trim() }.filter { trimmed ->
                trimmed.length >= 3 &&
                    !trimmed.startsWith("₹") &&
                    !trimmed.startsWith("$") &&
                    !trimmed.contains("₹") &&
                    !SPEED_REGEX.matcher(trimmed).find() &&
                    !PICKUP_REGEX.matcher(trimmed).find() &&
                    !DROP_REGEX.matcher(trimmed).find() &&
                    !trimmed.matches(Regex("""^[0-9.]+\s*(?:km|min|mins).*""", RegexOption.IGNORE_CASE)) &&
                    !trimmed.equals("accept", ignoreCase = true) &&
                    !trimmed.equals("confirm", ignoreCase = true) &&
                    !trimmed.equals("match", ignoreCase = true) &&
                    !trimmed.equals("nearby", ignoreCase = true) &&
                    !trimmed.equals("auto", ignoreCase = true) &&
                    !trimmed.equals("services", ignoreCase = true)
            }
            if (pickup == null && candidates.isNotEmpty()) {
                pickup = candidates.firstOrNull()
            }
            if (drop == null && candidates.size >= 2) {
                drop = candidates.firstOrNull { it != pickup } ?: candidates[1]
            }
        }

        // BUG 2: If address cannot be detected, save raw text from accessibility node instead of generic placeholder
        if (pickup == null) {
            pickup = lines.map { it.trim() }.firstOrNull { it.isNotBlank() && !it.contains("₹") && !it.matches(Regex("^[0-9.]+$")) }
                ?: lines.firstOrNull { it.isNotBlank() }
        }
        if (drop == null) {
            drop = lines.map { it.trim() }.filter { it != pickup }.firstOrNull { it.isNotBlank() && !it.contains("₹") && !it.matches(Regex("^[0-9.]+$")) }
                ?: lines.drop(1).firstOrNull { it.isNotBlank() }
                ?: pickup
        }

        val area = drop?.split(",")?.firstOrNull()?.trim() ?: drop?.take(20)?.trim() ?: "City Center"
        return Triple(pickup, drop, area)
    }

    fun detectVehicleType(text: String, defaultVehicle: VehicleType): VehicleType {
        val lower = text.lowercase()
        return when {
            lower.contains("bike") || lower.contains("moto") -> VehicleType.BIKE
            lower.contains("auto") -> VehicleType.AUTO
            lower.contains("car") || lower.contains("cab") || lower.contains("sedan") || lower.contains("premier") -> VehicleType.CAR
            else -> defaultVehicle
        }
    }

    fun extractBookingId(text: String): String? {
        val p = Pattern.compile("(?:CRN|order|id|booking|ride|trip)[\\s:#]*([A-Z0-9-]{3,24})", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(text)
        if (m.find()) return m.group(1)

        val pHash = Pattern.compile("#([A-Z0-9-]{4,16})", Pattern.CASE_INSENSITIVE)
        val mHash = pHash.matcher(text)
        if (mHash.find()) return mHash.group(1)

        return null
    }
}
