package com.example.engine

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

/** Single coalescing reader; all retries reread windows and grant no click permission. */
class RapidoEventRecovery {
    class Ticket internal constructor(internal var eventTime: Long) {
        internal val wake = Channel<Unit>(Channel.CONFLATED)
    }
    private var active: Ticket? = null
    private var pending = false
    private var retries = 0
    @Synchronized fun signal(eventTime: Long = 0L): Ticket? {
        active?.let {
            pending = true
            it.eventTime = eventTime
            it.wake.trySend(Unit)
            return null
        }
        pending = false
        retries = 0
        return Ticket(eventTime).also { active = it }
    }
    @Synchronized fun current(ticket: Ticket): Boolean = active === ticket
    @Synchronized fun nextDelay(ticket: Ticket, enabled: Boolean, recover: Boolean): Long? {
        if (active !== ticket) return null
        if (enabled && pending) { pending = false; ticket.wake.tryReceive(); return 0L }
        if (enabled && recover && retries < delays.size) return delays[retries++]
        active = null
        return null
    }
    @Synchronized fun finish(ticket: Ticket) { if (active === ticket) active = null }
    @Synchronized fun cancel() { active?.wake?.trySend(Unit); active = null; pending = false }
    @Synchronized fun eventTime(ticket: Ticket): Long = ticket.eventTime
    suspend fun waitForNext(ticket: Ticket, delayMs: Long): Boolean {
        val event = withTimeoutOrNull(delayMs) { ticket.wake.receive(); true } == true
        if (event) synchronized(this) {
            if (active === ticket) { pending = false; ticket.wake.tryReceive() }
        }
        return event
    }
    companion object { val delays = listOf(20L, 45L, 30L, 100L, 200L, 400L, 800L) }
}
