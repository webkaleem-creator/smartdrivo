package com.example.engine

/** Single coalescing reader; all retries reread windows and grant no click permission. */
class RapidoEventRecovery {
    class Ticket internal constructor()
    private var active: Ticket? = null
    private var pending = false
    private var retries = 0
    @Synchronized fun signal(): Ticket? {
        if (active != null) { pending = true; return null }
        pending = false
        retries = 0
        return Ticket().also { active = it }
    }
    @Synchronized fun current(ticket: Ticket): Boolean = active === ticket
    @Synchronized fun nextDelay(ticket: Ticket, enabled: Boolean, recover: Boolean): Long? {
        if (active !== ticket) return null
        if (enabled && pending) { pending = false; return 0L }
        if (enabled && recover && retries < delays.size) return delays[retries++]
        active = null
        return null
    }
    @Synchronized fun finish(ticket: Ticket) { if (active === ticket) active = null }
    @Synchronized fun cancel() { active = null; pending = false }
    companion object { val delays = listOf(20L, 45L, 30L, 100L, 200L, 400L, 800L) }
}
