package com.example.model

enum class Platform(val displayName: String, val packageName: String) {
    RAPIDO("Rapido", "com.rapido.captain"),
    UBER("Uber", "com.ubercab.driver"),
    OLA("Ola", "com.olacabs.oladriver");

    companion object {
        val OLA_PACKAGES = listOf(
            "com.olacabs.oladriver",
            "com.olacabs.driver",
            "com.olacabs.consumer",
            "com.olacabs.customer"
        )
        val RAPIDO_PACKAGES = listOf(
            "com.rapido.captain",
            "com.rapido.rider",
            "com.rapido.passenger"
        )

        fun isRapidoPackage(pkg: String?): Boolean {
            if (pkg == null) return false
            return RAPIDO_PACKAGES.any { pkg.equals(it, ignoreCase = true) } || pkg.contains("rapido", ignoreCase = true)
        }

        fun fromPackage(pkg: String?): Platform? {
            return when {
                pkg == null -> null
                isRapidoPackage(pkg) -> RAPIDO
                pkg.contains("ubercab", ignoreCase = true) || pkg.contains("uber", ignoreCase = true) -> UBER
                pkg.contains("ola", ignoreCase = true) -> OLA
                else -> null
            }
        }
    }
}
