package com.example.engine

import kotlinx.coroutines.CompletableDeferred

/** Orders final writes after their own insert; never blocks the click thread. */
class HistoryInsertGate {
    private val inserts = mutableMapOf<String, CompletableDeferred<Boolean>>()
    @Synchronized fun begin(id: String): CompletableDeferred<Boolean>? {
        if (inserts[id]?.isCompleted == false) return null
        return CompletableDeferred<Boolean>().also { inserts[id] = it }
    }
    fun complete(id: String, ticket: CompletableDeferred<Boolean>, success: Boolean) {
        synchronized(this) {
            ticket.complete(success)
            if (success && inserts[id] === ticket) inserts.remove(id)
        }
    }
    suspend fun await(id: String): Boolean = synchronized(this) { inserts[id] }?.await() ?: true
}
