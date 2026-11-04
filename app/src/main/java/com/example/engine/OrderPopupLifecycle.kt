package com.example.engine

import com.example.model.OrderStatus

/** Decision lifetime, not a timer debounce. Window visibility is deliberately independent. */
class OrderPopupLifecycle {
    enum class Result { PUBLISH, DUPLICATE, STALE, PENDING, DOWNGRADE }
    private data class Decision(val status: OrderStatus, val content: Any)
    private val decisions = object : LinkedHashMap<String, Decision>(128, .75f, true) {
        override fun removeEldestEntry(e: MutableMap.MutableEntry<String, Decision>?) = size > 1024
    }
    private val aliases = object : LinkedHashMap<String, String>(128, .75f, true) {
        override fun removeEldestEntry(e: MutableMap.MutableEntry<String, String>?) = size > 2048
    }
    private var latest: String? = null
    private var epoch = 0L
    private fun canonical(key: String): String {
        var value = key
        repeat(8) { value = aliases[value] ?: return value }
        return value
    }
    @Synchronized fun detect(key: String) { if (key.isNotBlank()) latest = canonical(key) }
    @Synchronized fun alias(from: String, to: String) {
        val target = canonical(to)
        if (from == target) return
        val old = canonical(from)
        aliases[from] = target
        if (latest == old || latest == from) latest = target
        decisions.remove(old)?.let { previous ->
            val existing = decisions[target]
            if (existing == null || OrderPopupStatusPolicy.mayReplace(existing.status, previous.status)) decisions[target] = previous
        }
    }
    @Synchronized fun epoch(): Long = epoch
    @Synchronized fun invalidate() { epoch++; latest = null }
    @Synchronized fun publish(key: String, status: OrderStatus, content: Any, requestEpoch: Long): Result {
        if (requestEpoch != epoch) return Result.STALE
        if (status == OrderStatus.PROCESSING) return Result.PENDING
        val row = canonical(key)
        if (row.isBlank() || latest != row) return Result.STALE
        val old = decisions[row]
        if (!OrderPopupStatusPolicy.mayReplace(old?.status, status)) return Result.DOWNGRADE
        if (old == Decision(status, content)) return Result.DUPLICATE
        decisions[row] = Decision(status, content)
        return Result.PUBLISH
    }
}
