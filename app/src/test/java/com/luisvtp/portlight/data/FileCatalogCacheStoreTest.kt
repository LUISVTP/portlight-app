package com.luisvtp.portlight.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Caché del catálogo en almacenamiento privado (ficheros). */
class FileCatalogCacheStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `sin escritura no hay nada que leer`() {
        val store = FileCatalogCacheStore(tempFolder.root)
        assertNull(store.readJson())
        assertNull(store.readEtag())
        assertNull(store.readLastModified())
    }

    @Test
    fun `guarda y recupera json y cabeceras condicionales`() {
        val store = FileCatalogCacheStore(tempFolder.root)
        store.write(
            json = """{"schema_version":1}""",
            etag = "\"v1\"",
            lastModified = "Wed, 01 Jan 2025 00:00:00 GMT",
        )

        assertEquals("""{"schema_version":1}""", store.readJson())
        assertEquals("\"v1\"", store.readEtag())
        assertEquals("Wed, 01 Jan 2025 00:00:00 GMT", store.readLastModified())
    }

    @Test
    fun `reescritura sustituye el contenido anterior`() {
        val store = FileCatalogCacheStore(tempFolder.root)
        store.write(json = "uno", etag = null, lastModified = null)
        store.write(json = "dos", etag = "\"v2\"", lastModified = null)

        assertEquals("dos", store.readJson())
        assertEquals("\"v2\"", store.readEtag())
        assertNull(store.readLastModified())
    }

    @Test
    fun `crea el directorio privado si no existe`() {
        val dir = File(tempFolder.root, "privado")
        FileCatalogCacheStore(dir).write(json = "json", etag = null, lastModified = null)

        assertEquals("json", FileCatalogCacheStore(dir).readJson())
    }
}
