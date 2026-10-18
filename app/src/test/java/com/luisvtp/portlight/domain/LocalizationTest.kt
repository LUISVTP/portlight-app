package com.luisvtp.portlight.domain

import com.luisvtp.portlight.R
import com.luisvtp.portlight.data.LocalizedText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** Categorías (Fase 4: `platformer`) y alternado de idiomas del catálogo. */
class LocalizationTest {

    @Test
    fun `platformer tiene recurso propio y el resto sigue cubierto`() {
        assertEquals(R.string.category_platformer, categoryStringRes("platformer"))
        assertEquals(R.string.category_action, categoryStringRes("action"))
        assertEquals(R.string.category_other, categoryStringRes("desconocida"))
        assertEquals(R.string.category_other, categoryStringRes(null))
    }

    @Test
    fun `usa el idioma del sistema y cae al otro si falta`() {
        val both = LocalizedText(es = "Aviso", en = "Warning")
        assertEquals("Aviso", both.localized(Locale("es")))
        assertEquals("Warning", both.localized(Locale("en")))

        val onlyEs = LocalizedText(es = "Solo español")
        assertEquals("Solo español", onlyEs.localized(Locale("en")))

        val onlyEn = LocalizedText(en = "English only")
        assertEquals("English only", onlyEn.localized(Locale("es")))
    }
}
