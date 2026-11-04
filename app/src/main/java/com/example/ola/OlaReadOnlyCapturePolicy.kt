package com.example.ola

/** Only real Ola-driver events, from logged-in admins, can reach the preview reader. */
object OlaReadOnlyCapturePolicy {
    const val OLA_DRIVER_PACKAGE = "com.olacabs.oladriver"

    fun allowed(
        eventPackage: String?,
        isAdmin: Boolean,
        loggedIn: Boolean,
        isActive: Boolean,
        isBlocked: Boolean,
        bankingSuspended: Boolean
    ): Boolean =
        eventPackage == OLA_DRIVER_PACKAGE && isAdmin && loggedIn &&
            isActive && !isBlocked && !bankingSuspended
}
