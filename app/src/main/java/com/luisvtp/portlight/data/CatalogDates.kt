package com.luisvtp.portlight.data

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/** Fechas `generated_at` del catálogo (ISO-8601, PLAN §4). */
object CatalogDates {

    private val PATTERNS = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX")

    /** Instante en milisegundos, o null si el valor no es ISO-8601 válido. */
    fun parseEpochMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        for (pattern in PATTERNS) {
            val format = SimpleDateFormat(pattern, Locale.US)
            format.isLenient = false
            try {
                return format.parse(value)?.time
            } catch (e: ParseException) {
                // Probar el siguiente patrón.
            } catch (e: IllegalArgumentException) {
                return null
            }
        }
        return null
    }
}
