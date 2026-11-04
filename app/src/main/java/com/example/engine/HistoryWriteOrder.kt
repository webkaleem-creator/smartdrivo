package com.example.engine

import kotlinx.coroutines.CompletableDeferred

/** Captured synchronously; only background writers wait. Independent records never wait on each other. */
class HistoryWriteOrder {
    class Ticket internal constructor(
        internal val previous: CompletableDeferred<Unit>?,
        internal val done: CompletableDeferred<Unit> = CompletableDeferred()
    ) {
        suspend fun awaitPrevious() { previous?.await() }
    }
    private val tails = mutableMapOf<String, CompletableDeferred<Unit>>()
    @Synchronized fun enqueue(id: String): Ticket = Ticket(tails[id]).also { tails[id] = it.done }
    fun complete(id: String, ticket: Ticket) {
        fun finish() = synchronized(this) {
            ticket.done.complete(Unit)
            if (tails[id] === ticket.done) tails.remove(id)
        }
        // A cancelled queued writer must not let its successors jump its predecessor.
        val previous = ticket.previous
        if (previous != null && !previous.isCompleted) previous.invokeOnCompletion { finish() }
        else finish()
    }
}
