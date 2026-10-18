package com.luisvtp.portlight.domain

import java.util.Locale

private const val THOUSAND = 1_000L
private const val MILLION = 1_000_000L
private const val BILLION = 1_000_000_000L

/**
 * Tamaño legible del APK (diálogo de descarga y detalle, Fase 4b): base 1000,
 * igual que GitHub — B, KB, MB o GB con un decimal — y separador decimal según
 * el idioma del dispositivo (p. ej. «132,6 MB» en español, «132.6 MB» en inglés).
 * Es la ÚNICA forma de formatear tamaños en la app; el texto envolvente vive
 * en resources (`download_size`).
 */
fun formatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String = when {
    bytes >= BILLION -> String.format(locale, "%.1f GB", bytes / BILLION.toDouble())
    bytes >= MILLION -> String.format(locale, "%.1f MB", bytes / MILLION.toDouble())
    bytes >= THOUSAND -> String.format(locale, "%.1f KB", bytes / THOUSAND.toDouble())
    else -> "$bytes B"
}
