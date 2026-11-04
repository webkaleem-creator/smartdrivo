package com.example.engine

import com.example.model.OrderStatus

/** A label-scoped parent must contain one address value and no destination section. */
object PickupShieldEvidence {
    data class Field(val key: Int, val parent: Int?, val text: String?, val description: String?,
        val resourceId: String?, val trusted: Boolean, val interactive: Boolean = false,
        val className: String? = null)

    fun labelledPickup(fields: List<Field>): String? {
        for (label in fields.filter { it.trusted && (PickupOnlyLabel.isLabel(it.text) || PickupOnlyLabel.isLabel(it.description)) }) {
            val parent = label.parent ?: continue
            val section = mutableListOf<Field>()
            var parents = setOf(parent)
            repeat(3) {
                val level = fields.filter { it.parent in parents && it.key != label.key }
                section += level
                parents = level.map { it.key }.toSet()
            }
            if (section.size > 24) continue
            // Only descendants of this labelled section; never visual ordering or distance inference.
            if (section.any { !it.trusted || isDrop(it) }) continue
            val values = section.filter { !it.interactive }.flatMap { listOfNotNull(it.text, it.description) }
                .map(String::trim).filter(PickupOnlyLabel::plausible).distinct()
            if (values.size == 1) return values.single()
        }
        return null
    }

    fun structure(fields: List<Field>): String = fields.take(24).joinToString(";") { f ->
        val id = f.resourceId?.substringAfterLast('/')?.takeIf { it.matches(Regex("[A-Za-z_]{1,64}")) } ?: "?"
        val role = when { PickupOnlyLabel.isLabel(f.text) || PickupOnlyLabel.isLabel(f.description) -> "pickup_label"
            isDrop(f) -> "drop_marker"; else -> "field" }
        val clazz = f.className?.substringAfterLast('.')?.takeIf { it.matches(Regex("[A-Za-z_]{1,40}")) } ?: "?"
        "${f.key}/${f.parent ?: -1}:$role:$id:$clazz:t${if (f.trusted) 1 else 0}:i${if (f.interactive) 1 else 0}:" +
            "text${if (f.text.isNullOrBlank()) 0 else 1}:desc${if (f.description.isNullOrBlank()) 0 else 1}"
    }

    private fun isDrop(f: Field): Boolean = PickupOnlyLabel.isDropResourceId(f.resourceId) || listOfNotNull(f.text, f.description,
        f.resourceId?.replace(Regex("([a-z])([A-Z])"), "$1_$2")).any {
        Regex("""(?:^|[/_\s])(?:drop|dropoff|destination|dest)(?:$|[_\s:：])""", RegexOption.IGNORE_CASE).containsMatchIn(it)
    }

    fun rejectLabel(text: String?, description: String?, resourceId: String?): Boolean {
        val labels = listOfNotNull(text, description).map { it.trim().lowercase() }
        if (labels.any { it.contains("accept") }) return false
        val explicit = setOf("reject", "decline", "skip", "pass", "अस्वीकार करें", "अस्वीकार")
        val id = resourceId?.substringAfterLast('/').orEmpty()
        return labels.any { it in explicit } || id in RapidoAdapter.BARE_REJECT_IDS
    }

    enum class Dismissal { PENDING, DISMISSED, REPLACED, UNKNOWN }
    class Watch {
        private var homeSamples = 0
        fun observe(verifiedHome: Boolean, sameOffer: Boolean?, replaced: Boolean,
            otherOfferVisible: Boolean = false, windowsComplete: Boolean = true): Dismissal {
            if (replaced) return Dismissal.REPLACED
            if (sameOffer == true) { homeSamples = 0; return Dismissal.PENDING }
            if (verifiedHome && !otherOfferVisible && windowsComplete) homeSamples++ else homeSamples = 0
            return if (homeSamples >= 2) Dismissal.DISMISSED else Dismissal.PENDING
        }
    }
    fun outcome(ack: Boolean, state: Dismissal): OrderStatus = when {
        state == Dismissal.DISMISSED && ack -> OrderStatus.SKIPPED
        !ack -> OrderStatus.FAILED
        else -> OrderStatus.UNCONFIRMED
    }
}
