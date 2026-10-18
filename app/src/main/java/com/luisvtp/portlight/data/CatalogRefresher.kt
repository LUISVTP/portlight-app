package com.luisvtp.portlight.data

/** Resultado del refresco del catálogo. */
sealed class CatalogRefreshResult {

    /** 200 con JSON válido: la caché privada y el catálogo vigente cambian. */
    data class Updated(val catalog: Catalog) : CatalogRefreshResult()

    /** 304: la copia local sigue vigente; no cambia nada. */
    object NotModified : CatalogRefreshResult()

    /** Error, 404 o JSON inválido: se conserva el catálogo actual (o el embebido). */
    object Kept : CatalogRefreshResult()
}

/**
 * Refresco defensivo del catálogo con revalidación condicional.
 *
 * Reglas: un 304 no cambia nada; un error o un 404 deja el catálogo vigente
 * (probando antes la URL alternativa de raw.githubusercontent.com); un JSON
 * que no valida contra el esquema se descarta y no se sustituye nada.
 */
class CatalogRefresher(
    private val gateway: CatalogHttpGateway,
    private val store: CatalogCacheStore,
    private val primaryUrl: String = CatalogUrls.PRIMARY_CATALOG_URL,
    private val fallbackUrl: String = CatalogUrls.FALLBACK_CATALOG_URL,
) {

    fun refresh(): CatalogRefreshResult {
        val etag = store.readEtag()
        val lastModified = store.readLastModified()
        return refreshFrom(primaryUrl, etag, lastModified)
            ?: refreshFrom(fallbackUrl, etag, lastModified)
            ?: CatalogRefreshResult.Kept
    }

    /** null = fallo de transporte → se intenta la URL alternativa. */
    private fun refreshFrom(
        url: String,
        etag: String?,
        lastModified: String?,
    ): CatalogRefreshResult? = when (val response = gateway.get(url, etag, lastModified)) {
        is CatalogHttpResponse.Success -> {
            val catalog = CatalogJson.parseOrNull(response.body)
            if (catalog == null) {
                // JSON inválido: se descarta y se conserva el catálogo vigente.
                CatalogRefreshResult.Kept
            } else {
                store.write(response.body, response.etag, response.lastModified)
                CatalogRefreshResult.Updated(catalog)
            }
        }
        CatalogHttpResponse.NotModified -> CatalogRefreshResult.NotModified
        is CatalogHttpResponse.HttpError,
        CatalogHttpResponse.IOError,
        CatalogHttpResponse.RejectedByPolicy,
        -> null
    }
}
