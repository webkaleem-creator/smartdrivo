package com.example.data

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.BufferOverflow

/** Process-local anonymous tokens; no booking/fingerprint/address is emitted. Logging never waits for IO. */
object HistoryLatencyTrace {
    private data class Entry(val token: String, val start: Long, val stages: MutableSet<String> = mutableSetOf())
    private val entries = object : LinkedHashMap<String, Entry>(256, .75f, true) {
        override fun removeEldestEntry(e: MutableMap.MutableEntry<String, Entry>?) = size > 1024
    }
    private var sequence = 0L
    private val lines = Channel<String>(256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    init { CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { for (line in lines) Log.i("HistoryLatency", line) } }
    @Synchronized fun alias(from: String, to: String) { entries[from]?.let { entries[to] = it } }
    @Synchronized fun mark(id: String, platform: String, stage: String, eventUptimeMs: Long = 0L) {
        val now = SystemClock.uptimeMillis()
        val entry = entries.getOrPut(id) { Entry("h${++sequence}", eventUptimeMs.takeIf { it > 0L } ?: now) }
        if (!entry.stages.add(stage)) return
        // All stage/platform values are application constants, never UI text.
        lines.trySend("token=${entry.token} platform=$platform stage=$stage eventAgeMs=${(now-entry.start).coerceAtLeast(0L)}")
    }
}
