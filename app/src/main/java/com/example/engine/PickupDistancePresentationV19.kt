package com.example.engine

import java.util.Locale

/** Presentation only. Never turn missing/unknown pickup distance into a measured zero. */
object PickupDistancePresentationV19 {
    fun label(km: Float?): String =
        if (km != null && km.isFinite() && km >= 0f)
            String.format(Locale.US, "%.1f km", km)
        else
            "Unavailable"
}
