package com.example.engine

/** Only verified Ola driver packages may enter the Ola window-change reader. */
object OlaWindowEventRoute {
    fun packageFor(eventPackage: String?, windowPackages: List<String>): String? =
        eventPackage?.takeIf { OlaOfferSafety.isDriverPackage(it) }
            ?: windowPackages.firstOrNull { OlaOfferSafety.isDriverPackage(it) }
}
