package com.example.engine

import com.example.data.db.HistoryRecordIdentity
import com.example.model.*

/** Pin callbacks to the snapshot that was detected, never a mutable platform active ID. */
class HistoryDetectionRegistry {
    private val ids = object : LinkedHashMap<String, String>(128, .75f, true) {
        override fun removeEldestEntry(e: MutableMap.MutableEntry<String, String>?) = size > 2048
    }
    private val latest = mutableMapOf<Platform, Long>()
    private fun time(c: RideCandidate) = c.detectionTimeMs.takeIf { it > 0L } ?: c.timestamp
    @Synchronized fun remember(candidate: RideCandidate, id: String, announce: Boolean = true) {
        ids[HistoryRecordIdentity.id(candidate)] = id
        if (announce) latest[candidate.platform] = maxOf(latest[candidate.platform] ?: 0L, time(candidate))
    }
    @Synchronized fun find(candidate: RideCandidate): String? = ids[HistoryRecordIdentity.id(candidate)]
    @Synchronized fun mayAnnounce(candidate: RideCandidate): Boolean = time(candidate) >= (latest[candidate.platform] ?: 0L)
}
