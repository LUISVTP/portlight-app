package com.luisvtp.portlight.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Selección entre caché y embebido por `generated_at` más reciente (Fase 2A, arreglo b). */
class CatalogRepositoryTest {

    @Test
    fun `elige la cache cuando es mas reciente que la embebida`() {
        val repository = CatalogRepository(catalogJson("2026-01-01T00:00:00Z", "embebido"))

        val current = repository.currentCatalog(
            store(catalogJson("2026-06-01T00:00:00Z", "cache")),
        )

        assertEquals("cache", current.ports.single().id)
        assertEquals("2026-06-01T00:00:00Z", current.generatedAt)
    }

    @Test
    fun `elige la embebida cuando es mas reciente que la cache`() {
        val repository = CatalogRepository(catalogJson("2026-06-01T00:00:00Z", "embebido"))

        val current = repository.currentCatalog(
            store(catalogJson("2026-01-01T00:00:00Z", "cache")),
        )

        assertEquals("embebido", current.ports.single().id)
    }

    @Test
    fun `a igual generated_at manda la cache`() {
        val repository = CatalogRepository(catalogJson("2026-03-01T00:00:00Z", "embebido"))

        val current = repository.currentCatalog(
            store(catalogJson("2026-03-01T00:00:00Z", "cache")),
        )

        assertEquals("cache", current.ports.single().id)
    }

    @Test
    fun `la cache sin fecha pierde frente a la embebida con fecha`() {
        val repository = CatalogRepository(catalogJson("2026-03-01T00:00:00Z", "embebido"))

        val current = repository.currentCatalog(store(catalogJson(null, "cache")))

        assertEquals("embebido", current.ports.single().id)
    }

    @Test
    fun `la embebida sin fecha pierde frente a la cache con fecha`() {
        val repository = CatalogRepository(catalogJson(null, "embebido"))

        val current = repository.currentCatalog(
            store(catalogJson("2026-03-01T00:00:00Z", "cache")),
        )

        assertEquals("cache", current.ports.single().id)
    }

    @Test
    fun `sin fechas en ninguna manda la cache`() {
        val repository = CatalogRepository(catalogJson(null, "embebido"))

        val current = repository.currentCatalog(store(catalogJson(null, "cache")))

        assertEquals("cache", current.ports.single().id)
    }

    @Test
    fun `una cache invalida se descarta y se usa la embebida`() {
        val repository = CatalogRepository(catalogJson("2026-06-01T00:00:00Z", "embebido"))

        val current = repository.currentCatalog(store("{esto no es json"))

        assertEquals("embebido", current.ports.single().id)
    }

    @Test
    fun `sin cache se usa la embebida`() {
        val repository = CatalogRepository(catalogJson("2026-06-01T00:00:00Z", "embebido"))

        val current = repository.currentCatalog(store(null))

        assertEquals("embebido", current.ports.single().id)
    }

    private fun catalogJson(generatedAt: String?, portId: String): String {
        val generated = generatedAt?.let { "\"generated_at\":\"$it\"," } ?: ""
        return """
            {
              $generated
              "schema_version": 1,
              "ports": [{
                "id": "$portId",
                "name": "Demo",
                "description": {"en": "Demo port"},
                "author": {"name": "Autor"},
                "license": "GPL-3.0",
                "repo": "example/demo",
                "asset_pattern": "(?i)^.*\\.apk$",
                "requires_user_game": true,
                "no_game_assets": true,
                "source_url": "https://github.com/example/demo"
              }]
            }
        """.trimIndent()
    }

    private fun store(json: String?): CatalogCacheStore = object : CatalogCacheStore {
        override fun readJson(): String? = json
        override fun readEtag(): String? = null
        override fun readLastModified(): String? = null
        override fun write(json: String, etag: String?, lastModified: String?) = Unit
    }
}
