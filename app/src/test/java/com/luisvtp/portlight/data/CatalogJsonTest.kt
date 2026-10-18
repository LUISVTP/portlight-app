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

    @Test
    fun `acepta campos desconocidos sin descartar el catalogo`() {
        // ignoreUnknownKeys: campos de futuras versiones no rompen la app.
        val catalog = CatalogJson.parseOrNull(
            VALID_CATALOG.replace("\"category\":\"action\"", "\"category\":\"action\",\"campo_futuro\":1"),
        )
        assertNotNull(catalog)
    }

    @Test
    fun `acepta un port sin los opcionales nuevos`() {
        val catalog = CatalogJson.parseOrNull(VALID_CATALOG)
        assertNotNull(catalog)
        val port = catalog!!.ports.single()
        assertNull(port.requiredGame)
        assertNull(port.notes)
        assertNull(port.minSdkPort)
    }

    @Test
    fun `acepta y parsea required_game notes y min_sdk_port`() {
        val catalog = CatalogJson.parseOrNull(withExtra(OPTIONAL_FIELDS))
        assertNotNull(catalog)
        val port = catalog!!.ports.single()
        assertEquals(30, port.minSdkPort)
        assertEquals(RequiredGame("Juego de ejemplo", "EXAM01", 1), port.requiredGame)
        assertEquals("Aviso en espanol", port.notes?.es)
        assertEquals("Warning in English", port.notes?.en)
    }

    @Test
    fun `descarta required_game con titulo vacio`() {
        assertNull(
            CatalogJson.parseOrNull(
                withExtra(OPTIONAL_FIELDS.replace("\"title\":\"Juego de ejemplo\"", "\"title\":\"\"")),
            ),
        )
    }

    @Test
    fun `descarta required_game con game_id en blanco`() {
        assertNull(
            CatalogJson.parseOrNull(
                withExtra(OPTIONAL_FIELDS.replace("\"game_id\":\"EXAM01\"", "\"game_id\":\"\"")),
            ),
        )
    }

    @Test
    fun `descarta required_game con revision negativa`() {
        assertNull(
            CatalogJson.parseOrNull(
                withExtra(OPTIONAL_FIELDS.replace("\"revision\":1", "\"revision\":-1")),
            ),
        )
    }

    @Test
    fun `descarta notes sin ningun idioma`() {
        assertNull(
            CatalogJson.parseOrNull(
                withExtra(OPTIONAL_FIELDS.replace(
                    "\"notes\":{\"es\":\"Aviso en espanol\",\"en\":\"Warning in English\"}",
                    "\"notes\":{}",
                )),
            ),
        )
    }

    @Test
    fun `descarta min_sdk_port por debajo del soportado`() {
        assertNull(
            CatalogJson.parseOrNull(withExtra(OPTIONAL_FIELDS.replace("\"min_sdk_port\":30", "\"min_sdk_port\":5"))),
        )
    }

    private fun withRelease(releaseJson: String): String =
        VALID_CATALOG.replace("\"category\":\"action\"", "\"category\":\"action\"," + releaseJson)

    private fun withExtra(extraJson: String): String = withRelease(extraJson)

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

        /** Opcionales de la Fase 4: required_game, notes y min_sdk_port. */
        const val OPTIONAL_FIELDS =
            "\"min_sdk_port\":30," +
                "\"required_game\":{\"title\":\"Juego de ejemplo\",\"game_id\":\"EXAM01\",\"revision\":1}," +
                "\"notes\":{\"es\":\"Aviso en espanol\",\"en\":\"Warning in English\"}"
    }
}
