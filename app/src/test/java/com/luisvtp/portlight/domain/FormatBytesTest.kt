package com.luisvtp.portlight.domain

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Tamaño mostrado en el diálogo de descarga y en el detalle (Fase 4b):
 * base 1000 como GitHub y separador decimal según el idioma.
 */
class FormatBytesTest {

    private var previousLocale: Locale = Locale.getDefault()

    @Before
    fun rememberLocale() {
        previousLocale = Locale.getDefault()
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun `132 558 444 bytes en espanol son 132,6 MB como en GitHub`() {
        Locale.setDefault(Locale("es"))
        assertEquals("132,6 MB", formatBytes(132_558_444))
    }

    @Test
    fun `132 558 444 bytes en ingles son 132_6 MB con punto`() {
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("132.6 MB", formatBytes(132_558_444))
    }

    @Test
    fun `kilobytes y gibibytes con base 1000`() {
        Locale.setDefault(Locale("es"))
        assertEquals("1,5 KB", formatBytes(1_536))
        assertEquals("1,5 GB", formatBytes(1_500_000_000))
    }

    @Test
    fun `por debajo de un kilobyte se quedan en bytes`() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("999 B", formatBytes(999))
        assertEquals("128 B", formatBytes(128))
    }

    @Test
    fun `los limites exactos de cada unidad`() {
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("1.0 KB", formatBytes(1_000))
        assertEquals("1.0 MB", formatBytes(1_000_000))
        assertEquals("1.0 GB", formatBytes(1_000_000_000))
    }
}
