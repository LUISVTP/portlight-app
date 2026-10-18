package com.luisvtp.portlight.data

import com.luisvtp.portlight.i18n.ModulePaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regresión (Fase 4): `res/raw/catalog.json` es una copia del catálogo REAL
 * publicado en GitHub Pages, con los campos nuevos (`required_game`, `notes`,
 * `min_sdk_port`, categoría `platformer`). Las entradas ficticias viven ahora
 * en datos de pruebas ([demo_catalog.json]) y en `PreviewData`.
 */
class EmbeddedCatalogTest {

    @Test
    fun `el catalogo embebido es el publicado y valida contra el esquema`() {
        val catalog = CatalogJson.parseOrNull(readEmbedded())

        assertNotNull(catalog)
        val port = catalog!!.ports.single()
        assertEquals("nsmbw-recomp", port.id)
        assertTrue(port.requiresUserGame && port.noGameAssets)
        assertTrue('/' in port.repo && !port.repo.startsWith("http"))
    }

    @Test
    fun `el port real trae required_game notes min_sdk_port y platformer`() {
        val port = CatalogJson.parseOrNull(readEmbedded())!!.ports.single()

        assertEquals(RequiredGame("New Super Mario Bros. Wii (USA)", "SMNE01", 1), port.requiredGame)
        assertEquals(30, port.minSdkPort)
        assertEquals("platformer", port.category)
        assertNotNull(port.notes)
        assertTrue(port.notes!!.es!!.isNotBlank())
        assertTrue(port.notes!!.en!!.isNotBlank())
    }

    @Test
    fun `el port real trae release con digest y abi`() {
        val port = CatalogJson.parseOrNull(readEmbedded())!!.ports.single()

        val asset = port.release!!.assets.single()
        assertEquals("v0.1.1", port.release!!.tag)
        assertTrue(asset.digest!!.startsWith("sha256:"))
        assertEquals("arm64-v8a", asset.abi)
        assertTrue(asset.size > 0)
    }

    @Test
    fun `las entradas ficticias siguen validando como datos de prueba`() {
        val demo = File(ModulePaths.moduleDir, "src/test/resources/demo_catalog.json")
            .readText(Charsets.UTF_8)

        val catalog = CatalogJson.parseOrNull(demo)

        assertNotNull(catalog)
        assertEquals(3, catalog!!.ports.size)
        // Sin los opcionales de la Fase 4: siguen siendo entradas válidas.
        assertTrue(catalog.ports.all { it.requiredGame == null && it.notes == null })
        assertTrue(catalog.ports.all { it.release != null })
    }

    private fun readEmbedded(): String =
        File(ModulePaths.moduleDir, "src/main/res/raw/catalog.json").readText(Charsets.UTF_8)
}
