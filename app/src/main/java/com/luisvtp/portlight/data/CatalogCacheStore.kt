package com.luisvtp.portlight.data

import java.io.File
import java.util.Properties

/** Caché del catálogo en almacenamiento privado de la app. */
interface CatalogCacheStore {
    fun readJson(): String?
    fun readEtag(): String?
    fun readLastModified(): String?
    fun write(json: String, etag: String?, lastModified: String?)
}

/**
 * Caché en ficheros privados (filesDir): el JSON del catálogo y un fichero de
 * metadatos con las cabeceras condicionales (ETag / Last-Modified) para
 * revalidar en la siguiente actualización. Las escrituras son atómicas
 * (fichero temporal + rename, con copia como respaldo).
 */
class FileCatalogCacheStore(private val dir: File) : CatalogCacheStore {

    private val jsonFile = File(dir, JSON_NAME)
    private val metaFile = File(dir, META_NAME)

    override fun readJson(): String? =
        if (jsonFile.isFile) jsonFile.readText(Charsets.UTF_8) else null

    override fun readEtag(): String? = readMeta().getProperty("etag")

    override fun readLastModified(): String? = readMeta().getProperty("lastModified")

    override fun write(json: String, etag: String?, lastModified: String?) {
        dir.mkdirs()
        writeFileAtomically(jsonFile, json)
        val meta = Properties()
        etag?.let { meta.setProperty("etag", it) }
        lastModified?.let { meta.setProperty("lastModified", it) }
        val tmp = File(dir, "$META_NAME.tmp")
        tmp.outputStream().use { meta.store(it, null) }
        if (!tmp.renameTo(metaFile)) {
            tmp.copyTo(metaFile, overwrite = true)
            tmp.delete()
        }
    }

    private fun writeFileAtomically(target: File, content: String) {
        val tmp = File(dir, "${target.name}.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    private fun readMeta(): Properties {
        val props = Properties()
        if (metaFile.isFile) metaFile.inputStream().use { props.load(it) }
        return props
    }

    private companion object {
        const val JSON_NAME = "catalog-cache.json"
        const val META_NAME = "catalog-cache.properties"
    }
}
