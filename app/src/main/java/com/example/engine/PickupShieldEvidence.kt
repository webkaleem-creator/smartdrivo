package com.example.engine

import com.example.model.OrderStatus

/** A label-scoped parent must contain one address value and no destination section. */
object PickupShieldEvidence {
    data class Field(val key: Int, val parent: Int?, val text: String?, val description: String?,
        val resourceId: String?, val trusted: Boolean)

    fun labelledPickup(fields: List<Field>): String? {
        for (label in fields.filter { it.trusted && (PickupOnlyLabel.isLabel(it.text) || PickupOnlyLabel.isLabel(it.description)) }) {
            val parent = label.parent ?: continue
            val section = fields.filter { it.parent == parent && it.key != label.key }
            // Only immediate text siblings. Never infer an address from visual ordering or distance.
            if (section.any { !it.trusted || isDrop(it) }) continue
            val values = section.flatMap { listOfNotNull(it.text, it.description) }
                .map(String::trim).filter(PickupOnlyLabel::plausible).distinct()
            if (values.size == 1) return values.single()
        }
        return null
    }

    private fun isDrop(f: Field): Boolean = listOfNotNull(f.text, f.description, f.resourceId).any {
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
        fun observe(verifiedHome: Boolean, sameOffer: Boolean?, replaced: Boolean): Dismissal {
            if (replaced) return Dismissal.REPLACED
            if (sameOffer == true) { homeSamples = 0; return Dismissal.PENDING }
            if (verifiedHome) homeSamples++ else homeSamples = 0
            return if (homeSamples >= 2) Dismissal.DISMISSED else Dismissal.PENDING
        }
    }
    fun outcome(ack: Boolean, state: Dismissal): OrderStatus = when {
        state == Dismissal.DISMISSED && ack -> OrderStatus.SKIPPED
        !ack -> OrderStatus.FAILED
        else -> OrderStatus.UNCONFIRMED
    }
}
