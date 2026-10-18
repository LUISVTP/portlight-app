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

    private companion object {
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
