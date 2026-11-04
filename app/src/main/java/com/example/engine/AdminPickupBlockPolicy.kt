package com.example.engine

import com.example.model.AdminPickupBlockSettings
import com.example.model.Platform
import java.text.Normalizer
import java.util.Locale

/** Explicit pickup text only. NEVER evaluate a destination or an entire mixed offer screen. */
object AdminPickupBlockPolicy {
    const val MAX_WORDS = 60
    const val MAX_LENGTH = 80

    sealed class Decision {
        object Allow : Decision()
        object Unreadable : Decision()
        object OwnerUnavailable : Decision()
        data class Blocked(val word: String) : Decision()
    }

    fun parse(raw: String): List<String> {
        val tokens = raw.split(',', '\n', '\r')
            .map { normalize(it) }
            .filter { it.length in 2..MAX_LENGTH }
            .distinct()
        return tokens.take(MAX_WORDS)
    }

    fun normalize(value: String): String = Normalizer.normalize(
        value, Normalizer.Form.NFKC
    ).trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

    fun valid(settings: AdminPickupBlockSettings): Boolean =
        settings.blockedWords.size <= MAX_WORDS &&
            settings.blockedWords.all { it.length in 2..MAX_LENGTH && it == normalize(it) } &&
            settings.blockedWords.distinct().size == settings.blockedWords.size &&
            (!settings.enabled || settings.blockedWords.isNotEmpty())

    fun evaluate(settings: AdminPickupBlockSettings, pickupText: String?): Decision {
        if (!settings.enabled) return Decision.Allow
        if (settings.blockedWords.isEmpty()) return Decision.Unreadable
        val pickup = pickupText?.let(::normalize).orEmpty()
        if (pickup.isBlank() || pickup == "address unavailable" ||
            pickup == "pickup" || pickup == "pickup location") return Decision.Unreadable
        for (word in settings.blockedWords) {
            // Full word/phrase matching, Unicode-safe boundaries, not substring false positives.
            val pattern = Regex(
                "(?<![\\p{L}\\p{N}])" + Regex.escape(word) + "(?![\\p{L}\\p{N}])",
                RegexOption.IGNORE_CASE
            )
            if (pattern.containsMatchIn(pickup)) return Decision.Blocked(word)
        }
        return Decision.Allow
    }
}

/** Pure helper for prefixed fields, used by the bounded live Accessibility extractor. */
object PickupOnlyLabel {
    private val explicit = Regex(
        """^(?:pick\s*up|pickup|from|source)(?:\s+(?:address|location|area|landmark))?\s*[:：\-]\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val pickupId = Regex(
        """(?:^|_)(?:pickup|pick_up|source|from|start)(?:_|$)""", RegexOption.IGNORE_CASE
    )
    private val addressId = Regex(
        """(?:addr|address|location|locality|area|landmark|place|street|building|name)""", RegexOption.IGNORE_CASE
    )
    private val distanceId = Regex(
        """(?:distance|dist|kilomet|_km|km_|fare|amount|eta)""", RegexOption.IGNORE_CASE
    )
    private val dropId = Regex("""(?:^|_)(?:drop|dropoff|drop_off|destination|dest|to|end)(?:_|$)""", RegexOption.IGNORE_CASE)

    private fun normalizedId(raw: String?): String = raw?.substringAfterLast('/')
        ?.replace(Regex("([a-z])([A-Z])"), "$1_$2")
        ?.replace('-', '_')?.lowercase(Locale.ROOT).orEmpty()

    fun isDropResourceId(raw: String?): Boolean = dropId.containsMatchIn(normalizedId(raw))

    fun trustedNode(rootPackage: String, nodePackage: String, platform: Platform,
        rootWindow: Int, nodeWindow: Int, visible: Boolean): Boolean {
        if (!visible || rootWindow != nodeWindow || rootPackage != nodePackage) return false
        return when (platform) {
            Platform.RAPIDO -> rootPackage in RapidoAdapter.RAPIDO_PACKAGES
            Platform.OLA -> OlaOfferSafety.isDriverPackage(rootPackage)
            Platform.UBER -> rootPackage == "com.ubercab" || rootPackage.startsWith("com.ubercab.") ||
                rootPackage == "com.uber.driver" || rootPackage.startsWith("com.uber.driver.")
        }
    }

    fun fromField(text: String?, resourceId: String?): String? {
        val raw = text?.trim().orEmpty()
        if (!plausible(raw)) return null
        val id = normalizedId(resourceId)
        if (isDropResourceId(resourceId)) return null
        if (id.isNotBlank() && pickupId.containsMatchIn(id) &&
            addressId.containsMatchIn(id) && !distanceId.containsMatchIn(id)
        ) return raw
        val value = explicit.matchEntire(raw)?.groupValues?.getOrNull(1)?.trim()
        return value?.takeIf(::plausible)
    }

    fun isLabel(text: String?): Boolean {
        val key = AdminPickupBlockPolicy.normalize(text.orEmpty())
        return key in setOf("pickup", "pick up", "pickup address",
            "pickup location", "pickup area", "from", "from location", "source")
    }

    fun plausible(value: String): Boolean {
        val clean = value.trim()
        if (clean.length < 3 || clean.length > 240) return false
        if (clean.contains('₹')) return false
        if (Regex("""\d+(?:[.,]\d+)?\s*(?:km|m|min|mins|minute|minutes)""", RegexOption.IGNORE_CASE).matches(clean)) return false
        if (clean.matches(Regex("""[\d.,+\s-]+"""))) return false
        val lower = AdminPickupBlockPolicy.normalize(clean)
        if (isLabel(clean) || lower.startsWith("drop:") ||
            lower.startsWith("destination:")) return false
        if (lower in setOf("pickup", "pick up", "drop", "drop location", "destination",
                "accept", "decline", "reject", "skip", "pass", "navigate", "directions",
                "view", "view details", "go to", "nearby", "services", "address unavailable")) return false
        // Mixed pickup/drop text must never be mistaken for a pickup-only field.
        if (Regex("""(?:^|\s)(?:drop|destination)(?:\s+(?:address|location|area|landmark))?\s*[:：\-]""", RegexOption.IGNORE_CASE).containsMatchIn(clean)) return false
        return true
    }
}
