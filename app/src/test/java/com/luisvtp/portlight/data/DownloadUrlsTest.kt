package com.luisvtp.portlight.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Política de descarga de APK (Fase 2B): la URL inicial debe ser la descarga
 * canónica de GitHub del repo de la entrada, con política de hosts SEPARADA
 * de la del catálogo.
 */
class DownloadUrlsTest {

    private val url =
        "https://github.com/example/demo/releases/download/v1.0.0/demo-v1.0.0-universal.apk"

    @Test
    fun `acepta la descarga canonica del repo tag y asset exactos`() {
        assertTrue(
            DownloadUrls.matchesReleaseUrl(url, "example/demo", "v1.0.0", "demo-v1.0.0-universal.apk"),
        )
    }

    @Test
    fun `acepta un asset con espacios codificados en la url`() {
        assertTrue(
            DownloadUrls.matchesReleaseUrl(
                "https://github.com/example/demo/releases/download/v1.0/mi%20app.apk",
                "example/demo",
                "v1.0",
                "mi app.apk",
            ),
        )
    }

    @Test
    fun `acepta el puerto 443 explicito`() {
        assertTrue(
            DownloadUrls.matchesReleaseUrl(
                "https://github.com:443/example/demo/releases/download/v1.0/a.apk",
                "example/demo",
                "v1.0",
                "a.apk",
            ),
        )
    }

    @Test
    fun `rechaza esquemas no https`() {
        assertFalse(
            DownloadUrls.matchesReleaseUrl(
                "http://github.com/example/demo/releases/download/v1.0/a.apk",
                "example/demo",
                "v1.0",
                "a.apk",
            ),
        )
    }

    @Test
    fun `rechaza cualquier host que no sea github com`() {
        for (host in listOf("gitlab.com", "evil.github.com", "github.com.evil.example", "objects.githubusercontent.com")) {
            assertFalse(
                "host no permitido como URL inicial: $host",
                DownloadUrls.matchesReleaseUrl(
                    "https://$host/example/demo/releases/download/v1.0/a.apk",
                    "example/demo",
                    "v1.0",
                    "a.apk",
                ),
            )
        }
    }

    @Test
    fun `rechaza query o fragmento`() {
        assertFalse(
            DownloadUrls.matchesReleaseUrl("$url?download=1", "example/demo", "v1.0.0", "demo-v1.0.0-universal.apk"),
        )
        assertFalse(
            DownloadUrls.matchesReleaseUrl("$url#frag", "example/demo", "v1.0.0", "demo-v1.0.0-universal.apk"),
        )
    }

    @Test
    fun `rechaza un puerto distinto de 443`() {
        assertFalse(
            DownloadUrls.matchesReleaseUrl(
                "https://github.com:8080/example/demo/releases/download/v1.0/a.apk",
                "example/demo",
                "v1.0",
                "a.apk",
            ),
        )
    }

    @Test
    fun `rechaza tag o nombre distintos a los de la entrada`() {
        assertFalse(DownloadUrls.matchesReleaseUrl(url, "example/demo", "v2.0.0", "demo-v1.0.0-universal.apk"))
        assertFalse(DownloadUrls.matchesReleaseUrl(url, "example/demo", "v1.0.0", "otro.apk"))
    }

    @Test
    fun `rechaza un repo distinto al de la entrada`() {
        assertFalse(DownloadUrls.matchesReleaseUrl(url, "example/otro", "v1.0.0", "demo-v1.0.0-universal.apk"))
        assertFalse(DownloadUrls.matchesReleaseUrl(url, "otro/demo", "v1.0.0", "demo-v1.0.0-universal.apk"))
    }

    @Test
    fun `rechaza rutas con segmentos de mas`() {
        assertFalse(
            DownloadUrls.matchesReleaseUrl(
                "https://github.com/example/demo/releases/download/v1.0/a.apk/extra",
                "example/demo",
                "v1.0",
                "a.apk",
            ),
        )
    }

    @Test
    fun `rechaza segmentos con puntos`() {
        // "%2e%2e" decodificado es "..": se compara ya decodificado.
        assertFalse(
            DownloadUrls.matchesReleaseUrl(
                "https://github.com/example/demo/releases/download/v1.0/%2e%2e",
                "example/demo",
                "v1.0",
                "..",
            ),
        )
        assertFalse(
            DownloadUrls.matchesReleaseUrl(
                "https://github.com/example/demo/releases/download/v1.0/.",
                "example/demo",
                "v1.0",
                ".",
            ),
        )
    }

    @Test
    fun `la politica de descarga es separada de la del catalogo`() {
        // Ningún host de descarga pertenece a la allowlist del catálogo y viceversa.
        assertTrue(DownloadUrls.ALLOWED_DOWNLOAD_HOSTS.none { it in CatalogUrls.ALLOWED_HOSTS })
        assertTrue(CatalogUrls.ALLOWED_HOSTS.none { it in DownloadUrls.ALLOWED_DOWNLOAD_HOSTS })
        // Hosts verificados/observados de los redirects de releases/download (PLAN §9).
        assertTrue(
            "github.com",
            DownloadUrls.ALLOWED_DOWNLOAD_HOSTS.contains("github.com"),
        )
        assertTrue(
            "release-assets.githubusercontent.com",
            DownloadUrls.ALLOWED_DOWNLOAD_HOSTS.contains("release-assets.githubusercontent.com"),
        )
        assertTrue(
            "objects.githubusercontent.com",
            DownloadUrls.ALLOWED_DOWNLOAD_HOSTS.contains("objects.githubusercontent.com"),
        )
    }
}
