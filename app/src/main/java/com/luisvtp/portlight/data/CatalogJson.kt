package com.luisvtp.portlight.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Parseo y validación del JSON del catálogo contra el esquema soportado.
 * Devuelve null (el JSON se descarta) si no cumple: versión de esquema,
 * campos obligatorios, al menos un idioma por port y la política de copia
 * propia (requires_user_game y no_game_assets siempre true).
 */
object CatalogJson {

    private val SUPPORTED_SCHEMA_VERSIONS = setOf(1)

    /** PLAN §4: `id` del port, `[a-z0-9][a-z0-9-]{2,63}` (también protege el nombre de fichero de descarga). */
    private val ID_PATTERN = Regex("[a-z0-9][a-z0-9-]{2,63}")

    /** PLAN §4: `repo` es `owner/nombre`, la única fuente del APK. */
    private val REPO_PATTERN = Regex("[A-Za-z0-9._-]+/[A-Za-z0-9._-]+")

    /** PLAN §4/§9: digest SHA-256 con prefijo (`sha256:<hex64>`), como el de GitHub. */
    private val DIGEST_PATTERN = Regex("sha256:[0-9a-fA-F]{64}")

    private val format = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun parseOrNull(json: String): Catalog? {
        val catalog = try {
            format.decodeFromString<Catalog>(json)
        } catch (e: SerializationException) {
            return null
        } catch (e: IllegalArgumentException) {
            return null
        }
        return catalog.takeIf { it.satisfiesSchema() }
    }

    private fun Catalog.satisfiesSchema(): Boolean {
        if (schemaVersion !in SUPPORTED_SCHEMA_VERSIONS) return false
        return ports.all { it.satisfiesSchema() }
    }

    private fun Port.satisfiesSchema(): Boolean {
        val hasLanguage = description.en?.isNotBlank() == true ||
            description.es?.isNotBlank() == true
        return id.matches(ID_PATTERN) &&
            name.isNotBlank() &&
            hasLanguage &&
            author.name.isNotBlank() &&
            license.isNotBlank() &&
            isValidRepo(repo) &&
            sourceUrl.startsWith("https://") &&
            requiresUserGame &&
            noGameAssets &&
            (release?.satisfiesSchema(repo) ?: true)
    }

    /** La release es opcional; si está, tag y assets deben ser descargables. */
    private fun Release.satisfiesSchema(repo: String): Boolean =
        tag.isNotBlank() &&
            (sourceUrlRelease == null || sourceUrlRelease.startsWith("https://")) &&
            assets.all { it.satisfiesSchema(repo, tag) }

    private fun ReleaseAsset.satisfiesSchema(repo: String, tag: String): Boolean =
        name.isNotBlank() &&
            size >= 0 &&
            (digest == null || DIGEST_PATTERN.matches(digest)) &&
            (abi == null || abi.isNotBlank()) &&
            DownloadUrls.matchesReleaseUrl(url, repo, tag, name)

    /** `owner/nombre` con segmentos simples; sin rutas, puntos ni trucos. */
    private fun isValidRepo(repo: String): Boolean {
        if (!REPO_PATTERN.matches(repo)) return false
        return repo.split('/').none { seg ->
            seg.isEmpty() || seg.startsWith(".") || seg.endsWith(".") || seg.contains("..")
        }
    }
}
