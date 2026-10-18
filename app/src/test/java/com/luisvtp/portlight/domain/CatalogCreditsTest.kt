package com.luisvtp.portlight.domain

import com.luisvtp.portlight.data.Author
import com.luisvtp.portlight.data.LocalizedText
import com.luisvtp.portlight.data.Port
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Los créditos de la pantalla legal se derivan del catálogo (Fase 4). */
class CatalogCreditsTest {

    @Test
    fun `cita autor licencia y enlace por cada port del catalogo`() {
        val credits = catalogCredits(listOf(portWithContact, portWithoutContact))

        assertEquals(2, credits.size)
        assertEquals(
            PortCredit(
                portName = "Port de ejemplo",
                authorName = "Autor de ejemplo",
                license = "GPL-3.0-only",
                url = "https://github.com/example",
            ),
            credits[0],
        )
    }

    @Test
    fun `sin contacto del autor se usa la url original del port`() {
        val credit = catalogCredits(listOf(portWithoutContact)).single()

        assertEquals("https://github.com/example/otro-port", credit.url)
    }

    @Test
    fun `catalogo vacio no produce creditos`() {
        assertTrue(catalogCredits(emptyList()).isEmpty())
    }

    private val portWithContact = Port(
        id = "port-ejemplo",
        name = "Port de ejemplo",
        description = LocalizedText(en = "Example"),
        author = Author(name = "Autor de ejemplo", contact = "https://github.com/example"),
        license = "GPL-3.0-only",
        repo = "example/port-ejemplo",
        assetPattern = "^.*\\.apk$",
        sourceUrl = "https://github.com/example/port-ejemplo",
    )

    private val portWithoutContact = Port(
        id = "otro-port",
        name = "Otro port",
        description = LocalizedText(es = "Otro"),
        author = Author(name = "Otra persona"),
        license = "MIT",
        repo = "example/otro-port",
        assetPattern = "^.*\\.apk$",
        sourceUrl = "https://github.com/example/otro-port",
    )
}
