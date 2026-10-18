package com.luisvtp.portlight.data

import com.luisvtp.portlight.i18n.ModulePaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
/**
 * Regresión: la instantánea embebida debe validar contra el esquema con
 * `repo` en formato `owner/nombre` (PLAN §4). Un `repo` tratado como URL
 * hacía fallar `loadCatalog()` en tiempo de ejecución.
 */
class EmbeddedCatalogTest {

    @Test
    fun `el catalogo embebido valida contra el esquema`() {
        val json = File(ModulePaths.moduleDir, "src/main/res/raw/catalog.json")
            .readText(Charsets.UTF_8)

        val catalog = CatalogJson.parseOrNull(json)

        assertNotNull(catalog)
        assertEquals(3, catalog!!.ports.size)
        assertTrue(catalog.ports.all { it.requiresUserGame && it.noGameAssets })
        assertTrue(catalog.ports.all { '/' in it.repo && !it.repo.startsWith("http") })
    }
}
