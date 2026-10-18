package com.luisvtp.portlight.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

/** Límite de tamaño de la respuesta del catálogo. */
class CatalogHttpLimitTest {

    @Test
    fun `lee un cuerpo dentro del limite`() {
        val body = "catalogo ".repeat(10)
        val result = readLimitedText(
            ByteArrayInputStream(body.toByteArray(Charsets.UTF_8)),
            maxBytes = 1024,
        )
        assertEquals(body, result)
    }

    @Test
    fun `lee un cuerpo exactamente en el limite`() {
        val body = "a".repeat(1024)
        val result = readLimitedText(
            ByteArrayInputStream(body.toByteArray(Charsets.UTF_8)),
            maxBytes = 1024,
        )
        assertEquals(body, result)
    }

    @Test
    fun `descarta un cuerpo que supera el limite`() {
        val body = ByteArray(2048) { 65 } // 2 KiB de 'A'
        val result = readLimitedText(ByteArrayInputStream(body), maxBytes = 1024)
        assertNull(result)
    }
}
