package com.luisvtp.portlight.data

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files

/**
 * Descargador contra el servidor local simulado (nunca contra repos reales):
 * hash correcto/incorrecto/ausente, tamaño excedido (cabecera y streaming),
 * redirecciones permitidas y no permitidas, errores HTTP, red caída y
 * cancelación a mitad de descarga.
 */
class ReleaseDownloaderTest {

    private lateinit var server: LocalHttpServer
    private lateinit var downloadsDir: File

    @Before
    fun setUp() {
        server = LocalHttpServer()
        server.start()
        downloadsDir = Files.createTempDirectory("portlight-dl").toFile()
    }

    @After
    fun tearDown() {
        server.stop()
        downloadsDir.deleteRecursively()
    }

    /**
     * Descargador de prueba: la URL inicial debe ser del servidor local y las
     * redirecciones solo pueden quedarse en 127.0.0.1 por http (configuración
     * de prueba; en producción exige https y los hosts del PLAN §9).
     */
    private fun downloader(
        margin: Long = ReleaseDownloader.DEFAULT_SIZE_MARGIN_BYTES,
        client: ReleaseHttpClient = HttpUrlConnectionReleaseClient(connectTimeoutMs = 5_000, readTimeoutMs = 5_000),
        startAllowed: ((ReleaseAsset, String, String) -> Boolean)? = null,
        policy: RedirectPolicy = RedirectPolicy(setOf("127.0.0.1"), allowedSchemes = setOf("http")),
    ) = ReleaseDownloader(
        client = client,
        downloadsDir = downloadsDir,
        redirectPolicy = policy,
        isStartUrlAllowed = startAllowed ?: { asset, _, _ -> asset.url.startsWith(server.url("/")) },
        sizeMarginBytes = margin,
    )

    private fun asset(
        url: String,
        size: Long,
        digest: String? = null,
    ) = ReleaseAsset(name = "port.apk", size = size, url = url, digest = digest)

    private fun fakeClient(body: ByteArray, contentLength: Long? = null): ReleaseHttpClient =
        object : ReleaseHttpClient {
            override fun get(url: String): ReleaseHttpResponse = ReleaseHttpResponse.Received(
                code = 200,
                location = null,
                contentLength = contentLength,
                body = ByteArrayInputStream(body),
            )
        }

    @Test
    fun `hash correcto descarga y verifica en streaming`() {
        val body = "apk-ficticio-de-prueba".toByteArray()
        server.handle("/port.apk") { LocalHttpServer.Response(200, body = body) }
        val digest = "sha256:" + Hashes.sha256Hex(body)

        val outcome = downloader().download(
            asset(server.url("/port.apk"), size = body.size.toLong(), digest = digest),
            repo = "example/demo",
            tag = "v1.0",
            fileName = "demo.apk",
        )

        assertTrue(outcome is ReleaseDownloader.Outcome.Success)
        val success = outcome as ReleaseDownloader.Outcome.Success
        assertTrue(success.digestVerified)
        assertArrayEquals(body, success.file.readBytes())
        assertEquals(File(downloadsDir, "demo.apk"), success.file)
    }

    @Test
    fun `hash incorrecto devuelve digest mismatch y borra el archivo`() {
        val body = "apk-ficticio-de-prueba".toByteArray()
        server.handle("/port.apk") { LocalHttpServer.Response(200, body = body) }
        val digest = "sha256:" + Hashes.sha256Hex("otra-cosa".toByteArray())

        val outcome = downloader().download(
            asset(server.url("/port.apk"), size = body.size.toLong(), digest = digest),
            repo = "example/demo",
            tag = "v1.0",
            fileName = "demo.apk",
        )

        assertTrue(outcome is ReleaseDownloader.Outcome.DigestMismatch)
        assertFalse(File(downloadsDir, "demo.apk").exists())
    }

    @Test
    fun `sin digest la descarga se acepta pero queda sin verificar`() {
        val body = "apk-sin-digest".toByteArray()
        server.handle("/port.apk") { LocalHttpServer.Response(200, body = body) }

        val outcome = downloader().download(
            asset(server.url("/port.apk"), size = body.size.toLong(), digest = null),
            repo = "example/demo",
            tag = "v1.0",
            fileName = "demo.apk",
        )

        assertTrue(outcome is ReleaseDownloader.Outcome.Success)
        val success = outcome as ReleaseDownloader.Outcome.Success
        assertFalse(success.digestVerified)
        assertTrue(success.file.exists())
    }

    @Test
    fun `tamano excedido segun content-length aborta antes de escribir`() {
        val body = ByteArray(2_048)
        server.handle("/port.apk") { LocalHttpServer.Response(200, body = body) }

        val outcome = downloader(margin = 100L).download(
            asset(server.url("/port.apk"), size = 10L, digest = null),
            repo = "example/demo",
            tag = "v1.0",
            fileName = "demo.apk",
        )

        assertTrue(outcome is ReleaseDownloader.Outcome.TooLarge)
        assertFalse(File(downloadsDir, "demo.apk").exists())
    }

    @Test
    fun `tamano excedido en streaming corta aunque no haya content-length`() {
        val client = fakeClient(ByteArray(5_000), contentLength = null)

        val outcome = downloader(margin = 100L, client = client, startAllowed = { _, _, _ -> true })
            .download(
                asset("https://github.com/example/demo/releases/download/v1.0/port.apk", size = 10L),
                repo = "example/demo",
                tag = "v1.0",
                fileName = "demo.apk",
            )

        assertTrue(outcome is ReleaseDownloader.Outcome.TooLarge)
        assertFalse(File(downloadsDir, "demo.apk").exists())
    }

    @Test
    fun `sigue una redireccion permitida y descarga el asset`() {
        val body = "redirect-ok".toByteArray()
        server.handle("/dl/port.apk") {
            LocalHttpServer.Response(302, mapOf("Location" to "/final/port.apk"))
        }
        server.handle("/final/port.apk") { LocalHttpServer.Response(200, body = body) }

        val outcome = downloader().download(
            asset(server.url("/dl/port.apk"), size = body.size.toLong(), digest = "sha256:" + Hashes.sha256Hex(body)),
            repo = "example/demo",
            tag = "v1.0",
            fileName = "demo.apk",
        )

        assertTrue(outcome is ReleaseDownloader.Outcome.Success)
        assertArrayEquals(body, (outcome as ReleaseDownloader.Outcome.Success).file.readBytes())
    }

    @Test
    fun `redireccion a host no permitido se rechaza sin conectar`() {
        server.handle("/dl/port.apk") {
            LocalHttpServer.Response(302, mapOf("Location" to "https://evil.example.invalid/payload.apk"))
        }

        val outcome = downloader().download(
            asset(server.url("/dl/port.apk"), size = 100L),
            repo = "example/demo",
            tag = "v1.0",
            fileName = "demo.apk",
        )

        assertTrue(outcome is ReleaseDownloader.Outcome.RedirectDenied)
        assertEquals(1, server.requestCount())
        assertFalse(File(downloadsDir, "demo.apk").exists())
    }

    @Test
    fun `un bucle de redirecciones se corta por el limite de saltos`() {
        server.handle("/loop/port.apk") {
            LocalHttpServer.Response(302, mapOf("Location" to "/loop/port.apk"))
        }

        val outcome = downloader().download(
            asset(server.url("/loop/port.apk"), size = 100L),
            repo = "example/demo",
            tag = "v1.0",
            fileName = "demo.apk",
        )

        assertTrue(outcome is ReleaseDownloader.Outcome.RedirectDenied)
    }

    @Test
    fun `url inicial fuera de la politica se rechaza sin conectar`() {
        val outcome = downloader(startAllowed = { _, _, _ -> false }).download(
            asset(server.url("/port.apk"), size = 100L),
            repo = "example/demo",
            tag = "v1.0",
            fileName = "demo.apk",
        )

        assertTrue(outcome is ReleaseDownloader.Outcome.UrlRejected)
        assertEquals(0, server.requestCount())
    }

    @Test
    fun `404 se clasifica como error http`() {
        // Sin ruta registrada el servidor responde 404.
        val outcome = downloader().download(
            asset(server.url("/inexistente.apk"), size = 100L),
            repo = "example/demo",
            tag = "v1.0",
            fileName = "demo.apk",
        )

        assertTrue(outcome is ReleaseDownloader.Outcome.HttpFailure)
        assertEquals(404, (outcome as ReleaseDownloader.Outcome.HttpFailure).code)
    }

    @Test
    fun `sin servidor vivo la descarga falla de red`() {
        val closed = LocalHttpServer()
        closed.start()
        val url = closed.url("/port.apk")
        closed.stop()

        val outcome = downloader(startAllowed = { _, _, _ -> true }).download(
            asset(url, size = 100L),
            repo = "example/demo",
            tag = "v1.0",
            fileName = "demo.apk",
        )

        assertTrue(outcome is ReleaseDownloader.Outcome.NetworkFailure)
        assertFalse(File(downloadsDir, "demo.apk").exists())
    }

    @Test
    fun `cancelar a mitad de descarga borra el archivo parcial`() {
        val client = fakeClient(ByteArray(10_000), contentLength = null)
        var checks = 0

        val outcome = downloader(client = client, startAllowed = { _, _, _ -> true }).download(
            asset("https://github.com/example/demo/releases/download/v1.0/port.apk", size = 100_000L),
            repo = "example/demo",
            tag = "v1.0",
            fileName = "demo.apk",
            shouldAbort = {
                checks++
                checks > 2
            },
        )

        assertTrue(outcome is ReleaseDownloader.Outcome.Cancelled)
        assertTrue("debería haber comprobado la cancelación", checks > 2)
        assertFalse(File(downloadsDir, "demo.apk").exists())
    }

    @Test
    fun `el nombre de fichero se sanea contra rutas raras`() {
        val body = "ok".toByteArray()
        server.handle("/port.apk") { LocalHttpServer.Response(200, body = body) }

        val outcome = downloader().download(
            asset(server.url("/port.apk"), size = body.size.toLong(), digest = "sha256:" + Hashes.sha256Hex(body)),
            repo = "example/demo",
            tag = "v1.0",
            fileName = "../raro.apk",
        )

        assertTrue(outcome is ReleaseDownloader.Outcome.Success)
        val file = (outcome as ReleaseDownloader.Outcome.Success).file
        assertEquals(downloadsDir.canonicalFile, file.parentFile!!.canonicalFile)
        assertTrue(file.exists())
    }
}
