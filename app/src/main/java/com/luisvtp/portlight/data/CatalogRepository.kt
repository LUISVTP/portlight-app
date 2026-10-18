package com.luisvtp.portlight.data

import android.content.res.Resources
import com.luisvtp.portlight.R

/**
 * Acceso al catálogo. La instantánea embebida en res/raw es el respaldo
 * permanente; la Fase 2 añade la caché privada y el refresco remoto
 * (GitHub Pages con revalidación ETag y raw como alternativa).
 */
class CatalogRepository(private val embeddedJson: String) {

    /** Carga defensiva de la instantánea embebida (política del catálogo). */
    fun loadCatalog(): Catalog = filterForPolicy(
        CatalogJson.parseOrNull(embeddedJson)
            ?: error("El catálogo embebido no valida contra el esquema"),
    )

    /**
     * Catálogo vigente: entre la caché privada (si valida) y la instantánea
     * embebida se elige el de `generated_at` más reciente; a igual fecha (o
     * sin fechas) manda la caché. Si la caché no valida, la embebida.
     */
    fun currentCatalog(cacheStore: CatalogCacheStore): Catalog {
        val cached = cacheStore.readJson()
            ?.let { CatalogJson.parseOrNull(it) }
            ?.let { filterForPolicy(it) }
        val embedded = loadCatalog()
        if (cached == null) return embedded
        val cachedAt = CatalogDates.parseEpochMillis(cached.generatedAt) ?: Long.MIN_VALUE
        val embeddedAt = CatalogDates.parseEpochMillis(embedded.generatedAt) ?: Long.MIN_VALUE
        return if (cachedAt >= embeddedAt) cached else embedded
    }

    /** Política del catálogo: copia propia del juego y sin assets alojados. */
    private fun filterForPolicy(catalog: Catalog): Catalog = catalog.copy(
        ports = catalog.ports.filter { it.requiresUserGame && it.noGameAssets },
    )

    companion object {
        fun fromResources(resources: Resources): CatalogRepository =
            CatalogRepository(
                resources.openRawResource(R.raw.catalog)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() },
            )
    }
}
