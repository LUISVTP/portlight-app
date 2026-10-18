package com.luisvtp.portlight.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tamaño mostrado en el diálogo de descarga (Fase 4). */
class FormatBytesTest {

    @Test
    fun `formata bytes kilobytes mebibytes y gibibytes`() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("512 B", formatBytes(512))
        assertEquals("1.0 KB", formatBytes(1024))
        assertEquals("10.0 MB", formatBytes(10_485_760))
        assertEquals("1.0 GB", formatBytes(1_073_741_824))
    }

    @Test
    fun `el tamano del apk real del catalogo se lee en megabytes`() {
        // Tamaño de NSMBW-0.1.1-android-arm64.apk en el catálogo publicado.
        assertEquals("126.4 MB", formatBytes(132_558_444))
    }
}
