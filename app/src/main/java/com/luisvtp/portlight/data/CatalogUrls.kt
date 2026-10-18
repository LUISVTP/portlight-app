package com.luisvtp.portlight.data

import java.net.URI

/**
 * Política de red del refresco del catálogo: HTTPS obligatorio y lista blanca
 * de hosts. Solo se descarga de GitHub Pages (principal) o de
 * raw.githubusercontent.com (alternativa).
 */
object CatalogUrls {

    /** Catálogo publicado en GitHub Pages del repositorio portlight-catalog. */
    const val PRIMARY_CATALOG_URL =
        "https://luisvtp.github.io/portlight-catalog/catalog.json"

    /** Alternativa: el mismo fichero servido desde raw.githubusercontent.com. */
    const val FALLBACK_CATALOG_URL =
        "https://raw.githubusercontent.com/LUISVTP/portlight-catalog/main/catalog.json"

    /** Hosts permitidos (refresco y redirecciones del catálogo). */
    val ALLOWED_HOSTS: Set<String> = setOf("luisvtp.github.io", "raw.githubusercontent.com")

    /** true si [url] es HTTPS y su host está en la lista blanca. */
    fun isAllowed(url: String): Boolean = try {
        val uri = URI(url)
        uri.scheme == "https" && uri.host != null && uri.host in ALLOWED_HOSTS
    } catch (e: Exception) {
        false
    }
}
