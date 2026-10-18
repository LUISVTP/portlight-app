package com.luisvtp.portlight.data

import android.content.res.Resources
import com.luisvtp.portlight.R
import kotlinx.serialization.json.Json

/**
 * Acceso al catálogo. En la Fase 1 solo existe la instantánea embebida en res/raw;
 * la Fase 2 añadirá el refresco remoto (GitHub Pages + raw con caché ETag).
 */
class CatalogRepository(private val json: String) {

    private val format = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    /** Carga defensiva: política del catálogo (copia propia, sin assets del juego). */
    fun loadCatalog(): Catalog {
        val catalog = format.decodeFromString<Catalog>(json)
        return catalog.copy(
            ports = catalog.ports.filter { it.requiresUserGame && it.noGameAssets },
        )
    }

    companion object {
        fun fromResources(resources: Resources): CatalogRepository =
            CatalogRepository(
                resources.openRawResource(R.raw.catalog)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() },
            )
    }
}
