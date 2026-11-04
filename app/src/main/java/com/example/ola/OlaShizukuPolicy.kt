package com.example.ola

/** Fail-closed policy for a future Ola-only privileged bridge; no active Ola engine. */
object OlaShizukuPolicy {
    enum class Status { SERVICE_MISSING, AUTHORIZATION_REQUIRED, READY }

    fun status(serviceAlive: Boolean, authorized: Boolean): Status = when {
        !serviceAlive -> Status.SERVICE_MISSING
        !authorized -> Status.AUTHORIZATION_REQUIRED
        else -> Status.READY
    }

    fun canUseOlaBridge(
        serviceAlive: Boolean,
        authorized: Boolean,
        foregroundPackage: String?,
        olaEnabled: Boolean,
        masterOn: Boolean
    ): Boolean =
        olaEnabled && masterOn && status(serviceAlive, authorized) == Status.READY &&
            foregroundPackage == "com.olacabs.oladriver"
}
