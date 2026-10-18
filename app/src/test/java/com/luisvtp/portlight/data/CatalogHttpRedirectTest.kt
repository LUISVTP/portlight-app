package com.luisvtp.portlight.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Gateway del catálogo contra un servidor local simulado: comprueba el
 * seguimiento manual de redirecciones validando cada salto (Fase 2A, arreglo a).
 */
class CatalogHttpRedirectTest {

    private lateinit var server: LocalHttpServer

    @Before
    fun setUp() {
        server = LocalHttpServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    /**
     * Gateway de prueba: la URL inicial debe ser del servidor local y solo se
     * permite redirigir a 127.0.0.1 por http (configuración de prueba; en
     * producción la política exige HTTPS y los hosts del PLAN §9).
     */
    private fun gateway(
        isUrlAllowed: (String) -> Boolean = { it.startsWith(server.url("/")) },
    ) = HttpUrlConnectionGateway(
        connectTimeoutMs = 5_000,
        readTimeoutMs = 5_000,
        isUrlAllowed = isUrlAllowed,
        redirectPolicy = RedirectPolicy(setOf("127.0.0.1"), allowedSchemes = setOf("http")),
    )

    @Test
    fun `200 devuelve el cuerpo y reenvia las cabeceras condicionales`() {
        server.handle("/catalog.json") { request ->
            val echo = "etag=" + (request.header("if-none-match") ?: "-") +
                "|lm=" + (request.header("if-modified-since") ?: "-")
            LocalHttpServer.Response(
                code = 200,
                headers = mapOf("ETag" to "\"v1\""),
                body = echo.toByteArray(),
            )
        }

        val response = gateway().get(
            server.url("/catalog.json"),
            "v0",
            "Wed, 01 Jan 2025 00:00:00 GMT",
        )

        assertTrue(response is CatalogHttpResponse.Success)
        response as CatalogHttpResponse.Success
        assertEquals("etag=v0|lm=Wed, 01 Jan 2025 00:00:00 GMT", response.body)
        assertEquals("\"v1\"", response.etag)
    }

    @Test
    fun `304 se clasifica como NotModified`() {
        server.handle("/catalog.json") { LocalHttpServer.Response(304) }

        val response = gateway().get(server.url("/catalog.json"), "v0", null)

        assertEquals(CatalogHttpResponse.NotModified, response)
    }

    @Test
    fun `404 se clasifica como error HTTP`() {
        server.handle("/catalog.json") { LocalHttpServer.Response(404) }

        val response = gateway().get(server.url("/catalog.json"), null, null)

        assertEquals(CatalogHttpResponse.HttpError(404), response)
    }

    @Test
    fun `sigue una redireccion relativa a un host permitido`() {
        server.handle("/start") {
            LocalHttpServer.Response(302, mapOf("Location" to "/final"))
        }
        server.handle("/final") {
            LocalHttpServer.Response(200, body = "redirigido".toByteArray())
        }

        val response = gateway().get(server.url("/start"), null, null)

        assertTrue(response is CatalogHttpResponse.Success)
        assertEquals("redirigido", (response as CatalogHttpResponse.Success).body)
    }

    @Test
    fun `sigue una redireccion absoluta a un host permitido`() {
        server.handle("/start") {
            LocalHttpServer.Response(301, mapOf("Location" to server.url("/final")))
        }
        server.handle("/final") {
            LocalHttpServer.Response(200, body = "final".toByteArray())
        }

        val response = gateway().get(server.url("/start"), null, null)

        assertTrue(response is CatalogHttpResponse.Success)
        assertEquals("final", (response as CatalogHttpResponse.Success).body)
    }

    @Test
    fun `rechaza una redireccion a un host no permitido sin conectar`() {
        server.handle("/start") {
            LocalHttpServer.Response(302, mapOf("Location" to "https://evil.example.invalid/payload"))
        }

        val response = gateway().get(server.url("/start"), null, null)

        assertEquals(CatalogHttpResponse.RedirectDenied, response)
    }

    @Test
    fun `corta un bucle de redirecciones por el limite de saltos`() {
        server.handle("/loop") {
            LocalHttpServer.Response(302, mapOf("Location" to "/loop"))
        }

        val response = gateway().get(server.url("/loop"), null, null)

        assertEquals(CatalogHttpResponse.RedirectDenied, response)
    }

    @Test
    fun `rechaza la url inicial fuera de la lista blanca sin conectar`() {
        val response = gateway(isUrlAllowed = { false })
            .get("https://example.com/catalog.json", null, null)

        assertEquals(CatalogHttpResponse.RejectedByPolicy, response)
    }

    @Test
    fun `sin servidor responde IOError`() {
        val closed = LocalHttpServer()
        closed.start()
        val url = closed.url("/catalog.json")
        closed.stop()

        // isUrlAllowed abierto a propósito: la allowlist por defecto apunta al
        // servidor principal (otro puerto) y rechazaría la URL antes de conectar.
        val response = gateway(isUrlAllowed = { true }).get(url, null, null)

        assertEquals(CatalogHttpResponse.IOError, response)
    }
}
