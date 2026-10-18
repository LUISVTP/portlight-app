package com.luisvtp.portlight.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Validación del JSON del catálogo contra el esquema soportado. */
class CatalogJsonTest {

    @Test
    fun `acepta un catalogo valido`() {
        val catalog = CatalogJson.parseOrNull(VALID_CATALOG)
        assertNotNull(catalog)
        assertEquals("demo", catalog!!.ports.single().id)
    }

    @Test
    fun `descarta json malformado`() {
        assertNull(CatalogJson.parseOrNull("esto no es un json {"))
    }

    @Test
    fun `descarta version de esquema no soportada`() {
        assertNull(
            CatalogJson.parseOrNull(
                VALID_CATALOG.replace("\"schema_version\":1", "\"schema_version\":99"),
            ),
        )
    }

    @Test
    fun `descarta un port sin nombre`() {
        assertNull(
            CatalogJson.parseOrNull(VALID_CATALOG.replace("\"name\":\"Demo\"", "\"name\":\"\"")),
        )
    }

    @Test
    fun `descarta un port sin idiomas`() {
        assertNull(
            CatalogJson.parseOrNull(
                VALID_CATALOG.replace(
                    "\"description\":{\"en\":\"Demo port\",\"es\":\"Port de demostracion\"}",
                    "\"description\":{}",
                ),
            ),
        )
    }

    @Test
    fun `descarta un port sin copia propia del juego`() {
        assertNull(
            CatalogJson.parseOrNull(
                VALID_CATALOG.replace(
                    "\"requires_user_game\":true",
                    "\"requires_user_game\":false",
                ),
            ),
        )
    }

    @Test
    fun `descarta un port con assets alojados`() {
        assertNull(
            CatalogJson.parseOrNull(
                VALID_CATALOG.replace("\"no_game_assets\":true", "\"no_game_assets\":false"),
            ),
        )
    }

    @Test
    fun `descarta repo con forma de url en lugar de owner nombre`() {
        assertNull(
            CatalogJson.parseOrNull(
                VALID_CATALOG.replace(
                    "\"repo\":\"example/demo\"",
                    "\"repo\":\"https://github.com/example/demo\"",
                ),
            ),
        )
    }

    @Test
    fun `descarta repo con segmentos de mas`() {
        assertNull(
            CatalogJson.parseOrNull(
                VALID_CATALOG.replace("\"repo\":\"example/demo\"", "\"repo\":\"example/demo/extra\""),
            ),
        )
    }

    @Test
    fun `descarta repo con puntos peligrosos`() {
        assertNull(
            CatalogJson.parseOrNull(
                VALID_CATALOG.replace("\"repo\":\"example/demo\"", "\"repo\":\"../demo\""),
            ),
        )
    }

    @Test
    fun `descarta campos obligatorios ausentes`() {
        assertNull(
            CatalogJson.parseOrNull(VALID_CATALOG.replace("\"license\":\"GPL-3.0\",", "")),
        )
    }

    @Test
    fun `acepta un port con release valida`() {
        val catalog = CatalogJson.parseOrNull(withRelease(VALID_RELEASE))
        assertNotNull(catalog)
        assertEquals("v1.0.0", catalog!!.ports.single().release!!.tag)
    }

    @Test
    fun `descarta un id con formato invalido`() {
        assertNull(
            CatalogJson.parseOrNull(VALID_CATALOG.replace("\"id\":\"demo\"", "\"id\":\"ab\"")),
        )
        assertNull(
            CatalogJson.parseOrNull(VALID_CATALOG.replace("\"id\":\"demo\"", "\"id\":\"Demo\"")),
        )
    }

    @Test
    fun `descarta release con digest malformado`() {
        assertNull(
            CatalogJson.parseOrNull(
                withRelease(VALID_RELEASE.replace(DIGEST, "\"digest\":\"sha256:zz\"")),
            ),
        )
        assertNull(
            CatalogJson.parseOrNull(
                withRelease(VALID_RELEASE.replace(DIGEST, "\"digest\":\"sha1:00\"")),
            ),
        )
    }

    @Test
    fun `descarta release con size negativo`() {
        assertNull(
            CatalogJson.parseOrNull(withRelease(VALID_RELEASE.replace("\"size\":1024", "\"size\":-1"))),
        )
    }

    @Test
    fun `descarta release con tag vacio`() {
        assertNull(
            CatalogJson.parseOrNull(withRelease(VALID_RELEASE.replace("\"tag\":\"v1.0.0\"", "\"tag\":\"\""))),
        )
    }

    @Test
    fun `descarta release con url de otro repo`() {
        assertNull(
            CatalogJson.parseOrNull(
                withRelease(
                    VALID_RELEASE.replace(
                        "/example/demo/releases/download/",
                        "/example/otro-repo/releases/download/",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `descarta release con url fuera de la politica`() {
        assertNull(
            CatalogJson.parseOrNull(
                withRelease(
                    VALID_RELEASE.replace(
                        "https://github.com/example/demo/releases/download/",
                        "https://evil.example.invalid/demo/releases/download/",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `descarta release con abi en blanco`() {
        assertNull(
            CatalogJson.parseOrNull(withRelease(VALID_RELEASE.replace("\"abi\":\"universal\"", "\"abi\":\"\""))),
        )
    }

    private fun withRelease(releaseJson: String): String =
        VALID_CATALOG.replace("\"category\":\"action\"", "\"category\":\"action\"," + releaseJson)

    private companion object {
        /** Digest con la forma exacta de GitHub (`sha256:<hex64>`). */
        const val DIGEST =
            "\"digest\":\"sha256:0000000000000000000000000000000000000000000000000000000000000000\""

        const val VALID_RELEASE =
            "\"release\":{" +
                "\"tag\":\"v1.0.0\"," +
                "\"published_at\":\"2026-10-01T10:00:00Z\"," +
                "\"source_url_release\":\"https://github.com/example/demo/releases/tag/v1.0.0\"," +
                "\"assets\":[{" +
                "\"name\":\"demo-v1.0.0-universal.apk\"," +
                "\"size\":1024," +
                "\"url\":\"https://github.com/example/demo/releases/download/v1.0.0/demo-v1.0.0-universal.apk\"," +
                DIGEST + "," +
                "\"abi\":\"universal\"" +
                "}]" +
                "}"

        /** Catálogo mínimo que cumple el esquema (ficticio, sin proyectos reales). */
        const val VALID_CATALOG =
            "{" +
                "\"schema_version\":1," +
                "\"generated_at\":\"2025-01-01T00:00:00Z\"," +
                "\"ports\":[{" +
                "\"id\":\"demo\"," +
                "\"name\":\"Demo\"," +
                "\"description\":{\"en\":\"Demo port\",\"es\":\"Port de demostracion\"}," +
                "\"author\":{\"name\":\"Autor\"}," +
                "\"license\":\"GPL-3.0\"," +
                "\"repo\":\"example/demo\"," +
                "\"asset_pattern\":\"*.apk\"," +
                "\"requires_user_game\":true," +
                "\"no_game_assets\":true," +
                "\"source_url\":\"https://github.com/example/demo/releases\"," +
                "\"category\":\"action\"" +
                "}]" +
                "}"
    }
}
