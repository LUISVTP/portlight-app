package com.luisvtp.portlight.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelos del catálogo. Reflejan schema/catalog.schema.json de portlight-catalog (Fase 3).
 * Política del catálogo: requires_user_game y no_game_assets deben ser siempre true;
 * el repositorio filtra cualquier entrada que no cumpla la política.
 */
@Serializable
data class Catalog(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    @SerialName("generated_at") val generatedAt: String? = null,
    val ports: List<Port> = emptyList(),
)

@Serializable
data class Port(
    val id: String,
    val name: String,
    val description: LocalizedText,
    val author: Author,
    val license: String,
    val repo: String,
    @SerialName("asset_pattern") val assetPattern: String,
    @SerialName("requires_user_game") val requiresUserGame: Boolean = true,
    @SerialName("no_game_assets") val noGameAssets: Boolean = true,
    @SerialName("source_url") val sourceUrl: String,
    val screenshots: List<String> = emptyList(),
    @SerialName("package") val packageName: String? = null,
    val category: String? = null,
    @SerialName("min_sdk_port") val minSdkPort: Int? = null,
    val status: String? = null,
    /** Release generada por la Action (PLAN §4); null = sin release descargable. */
    val release: Release? = null,
    @SerialName("last_checked") val lastChecked: String? = null,
)

/** Release del repo original: tag y assets descargables (Fase 2B). */
@Serializable
data class Release(
    val tag: String,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("source_url_release") val sourceUrlRelease: String? = null,
    val assets: List<ReleaseAsset> = emptyList(),
)

/**
 * Asset de la Release. `url` debe ser la descarga canónica de GitHub
 * (`https://github.com/{owner}/{repo}/releases/download/...`) — la valida
 * [DownloadUrls] contra el `repo` y el `tag` de la entrada.
 * `digest` es `sha256:<hex64>` (assets de GitHub desde jun-2025) o null.
 */
@Serializable
data class ReleaseAsset(
    val name: String,
    val size: Long,
    val url: String,
    val digest: String? = null,
    val abi: String? = null,
)

/** Texto bilingüe del catálogo: al menos un idioma es obligatorio (esquema). */
@Serializable
data class LocalizedText(
    val es: String? = null,
    val en: String? = null,
)

@Serializable
data class Author(
    val name: String,
    val contact: String? = null,
)
