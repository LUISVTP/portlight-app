package com.luisvtp.portlight.domain

import java.util.Locale

private const val KIB = 1024L
private const val MIB = 1024L * 1024
private const val GIB = 1024L * 1024 * 1024

/**
 * Tamaño legible para el diálogo de descarga (Fase 4): B, KB, MB o GB con un
 * decimal. Sufijos en `Locale.ROOT` para que el texto sea estable en pruebas;
 * la frase completa sí va localizada en recursos.
 */
fun formatBytes(bytes: Long): String = when {
    bytes >= GIB -> String.format(Locale.ROOT, "%.1f GB", bytes.toDouble() / GIB)
    bytes >= MIB -> String.format(Locale.ROOT, "%.1f MB", bytes.toDouble() / MIB)
    bytes >= KIB -> String.format(Locale.ROOT, "%.1f KB", bytes.toDouble() / KIB)
    else -> "$bytes B"
}
