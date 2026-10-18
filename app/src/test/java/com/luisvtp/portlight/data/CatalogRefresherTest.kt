package com.luisvtp.portlight.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Refresco del catálogo con respuestas HTTP simuladas: 200, 304, 404,
 * JSON inválido y falta de red.
 */
class CatalogRefresherTest {

    @Test
    fun `200 con json valido actualiza la cache y devuelve el catalogo`() {
        val store = MemoryStore()
        val gateway = FakeGateway(
            mapOf(
                CatalogUrls.PRIMARY_CATALOG_URL to CatalogHttpResponse.Success(
                    body = VALID_CATALOG,
                    etag = "\"v1\"",
                    lastModified = "Wed, 01 Jan 2025 00:00:00 GMT",
                ),
            ),
        )

        val result = CatalogRefresher(gateway, store).refresh()

        assertTrue(result is CatalogRefreshResult.Updated)
        assertEquals(VALID_CATALOG, store.json)
        assertEquals("\"v1\"", store.etag)
        assertEquals("Wed, 01 Jan 2025 00:00:00 GMT", store.lastModified)
        // La URL alternativa no se prueba si la principal funciona.
        assertEquals(1, gateway.requests.size)
    }

    @Test
    fun `304 no cambia nada y revalida con las cabeceras guardadas`() {
        val store = MemoryStore().apply {
            json = "cache-previa"
            etag = "\"v1\""
            lastModified = "Wed, 01 Jan 2025 00:00:00 GMT"
        }
        val gateway = FakeGateway(
            mapOf(
                CatalogUrls.PRIMARY_CATALOG_URL to CatalogHttpResponse.NotModified,
            ),
        )

        val result = CatalogRefresher(gateway, store).refresh()

        assertEquals(CatalogRefreshResult.NotModified, result)
        assertEquals("cache-previa", store.json)
        assertEquals(0, store.writes)
        val request = gateway.requests.single()
        assertEquals("\"v1\"", request.etag)
        assertEquals("Wed, 01 Jan 2025 00:00:00 GMT", request.lastModified)
    }

    @Test
    fun `404 en la principal prueba la alternativa de raw`() {
        val store = MemoryStore().apply { json = "cache-previa" }
        val gateway = FakeGateway(
            mapOf(
                CatalogUrls.PRIMARY_CATALOG_URL to CatalogHttpResponse.HttpError(404),
                CatalogUrls.FALLBACK_CATALOG_URL to
                    CatalogHttpResponse.Success(VALID_CATALOG, null, null),
            ),
        )

        val result = CatalogRefresher(gateway, store).refresh()

        assertTrue(result is CatalogRefreshResult.Updated)
        assertEquals(2, gateway.requests.size)
        assertEquals(CatalogUrls.FALLBACK_CATALOG_URL, gateway.requests[1].url)
    }

    @Test
    fun `404 en ambas urls conserva el catalogo vigente`() {
        val store = MemoryStore().apply { json = "cache-previa" }
        val gateway = FakeGateway(
            mapOf(
                CatalogUrls.PRIMARY_CATALOG_URL to CatalogHttpResponse.HttpError(404),
                CatalogUrls.FALLBACK_CATALOG_URL to CatalogHttpResponse.HttpError(404),
            ),
        )

        val result = CatalogRefresher(gateway, store).refresh()

        assertEquals(CatalogRefreshResult.Kept, result)
        assertEquals("cache-previa", store.json)
        assertEquals(0, store.writes)
    }

    @Test
    fun `json invalido se descarta y conserva la cache`() {
        val store = MemoryStore().apply { json = "cache-previa" }
        val gateway = FakeGateway(
            mapOf(
                CatalogUrls.PRIMARY_CATALOG_URL to
                    CatalogHttpResponse.Success("{no es json", null, null),
            ),
        )

        val result = CatalogRefresher(gateway, store).refresh()

        assertEquals(CatalogRefreshResult.Kept, result)
        assertEquals("cache-previa", store.json)
        assertEquals(0, store.writes)
    }

    @Test
    fun `falta de red conserva el catalogo tras probar la alternativa`() {
        val store = MemoryStore().apply { json = "cache-previa" }
        val gateway = FakeGateway(emptyMap())

        val result = CatalogRefresher(gateway, store).refresh()

        assertEquals(CatalogRefreshResult.Kept, result)
        assertEquals("cache-previa", store.json)
        assertEquals(0, store.writes)
        assertEquals(2, gateway.requests.size)
    }

    @Test
    fun `falta de red sin cache previa devuelve Kept sin datos`() {
        val store = MemoryStore()

        val result = CatalogRefresher(FakeGateway(emptyMap()), store).refresh()

        assertEquals(CatalogRefreshResult.Kept, result)
        assertNull(store.json)
    }

    @Test
    fun `url rechazada por la politica se trata como fallo de transporte`() {
        val store = MemoryStore()
        val gateway = FakeGateway(
            mapOf(
                CatalogUrls.PRIMARY_CATALOG_URL to CatalogHttpResponse.RejectedByPolicy,
                CatalogUrls.FALLBACK_CATALOG_URL to CatalogHttpResponse.RejectedByPolicy,
            ),
        )

        val result = CatalogRefresher(gateway, store).refresh()

        assertEquals(CatalogRefreshResult.Kept, result)
        assertEquals(0, store.writes)
    }

    /** Gateway simulado: respuestas por URL; sin entrada → IOError (sin red). */
    private class FakeGateway(
        private val responses: Map<String, CatalogHttpResponse>,
    ) : CatalogHttpGateway {
        val requests = mutableListOf<Request>()

        data class Request(val url: String, val etag: String?, val lastModified: String?)

        override fun get(
            url: String,
            etag: String?,
            lastModified: String?,
        ): CatalogHttpResponse {
            requests += Request(url, etag, lastModified)
            return responses[url] ?: CatalogHttpResponse.IOError
        }
    }

    /** Caché en memoria: registra escrituras para comprobar que 304 e 404 no tocan nada. */
    private class MemoryStore : CatalogCacheStore {
        var json: String? = null
        var etag: String? = null
        var lastModified: String? = null
        var writes = 0

        override fun readJson(): String? = json
        override fun readEtag(): String? = etag
        override fun readLastModified(): String? = lastModified

        override fun write(json: String, etag: String?, lastModified: String?) {
            writes++
            this.json = json
            this.etag = etag
            this.lastModified = lastModified
        }
    }

    private companion object {
        /** Mismo catálogo mínimo que CatalogJsonTest (ficticio). */
        const val VALID_CATALOG =
            "{\"schema_version\":1,\"ports\":[{" +
                "\"id\":\"demo\",\"name\":\"Demo\"," +
                "\"description\":{\"en\":\"Demo port\"}," +
                "\"author\":{\"name\":\"Autor\"}," +
                "\"license\":\"GPL-3.0\"," +
                "\"repo\":\"example/demo\"," +
                "\"asset_pattern\":\"*.apk\"," +
                "\"requires_user_game\":true," +
                "\"no_game_assets\":true," +
                "\"source_url\":\"https://github.com/example/demo/releases\"}]}"
    }
}
