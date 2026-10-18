package com.luisvtp.portlight.data

import java.net.URI

/**
 * Política de red de la **descarga de APK** (Fase 2B, PLAN §9). Es
 * deliberadamente DISTINTA de [CatalogUrls] (refresco del catálogo): el
 * catálogo solo sale de GitHub Pages/raw, mientras que un APK solo puede
 * venir de la descarga canónica de releases de GitHub.
 *
 * Verificación de hosts (documentación oficial de GitHub + observación, 2026-10-18):
 *  - `github.com` — VERIFICADO: los docs de la API REST de releases documentan
 *    `browser_download_url = https://github.com/{owner}/{repo}/releases/download/…`
 *    como URL de descarga y confirman que la descarga redirige (200 o 302).
 *  - `release-assets.githubusercontent.com` — OBSERVADO en producción: destino
 *    actual de los redirects (`/github-production-release-asset/…`); GitHub no
 *    publica lista cerrada de hosts.
 *  - `objects.githubusercontent.com` — OBSERVADO históricamente (discusión
 *    oficial de la comunidad #50963); puede seguir apareciendo.
 *
 * Fail-closed: cualquier host no listado aborta la descarga.
 */
object DownloadUrls {

    /** Hosts permitidos en la descarga (iniciales y por salto de redirección). */
    val ALLOWED_DOWNLOAD_HOSTS: Set<String> = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
    )

    /**
     * true si [url] es EXACTAMENTE la descarga canónica de GitHub para ese
     * repo/tag/asset: HTTPS, host `github.com`, sin query ni fragmento y
     * path `/{owner}/{repo}/releases/download/{tag}/{name}` con `repo`
     * idéntico al campo de la entrada. Los segmentos se comparan ya
     * decodificados y no se admiten vacíos, `.` ni `..`.
     */
    fun matchesReleaseUrl(url: String, repo: String, tag: String, assetName: String): Boolean {
        if (repo.isBlank() || tag.isBlank() || assetName.isBlank()) return false
        return try {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase()
            val host = uri.host?.lowercase()
            if (scheme != "https" || host != "github.com") return false
            if (uri.port != -1 && uri.port != 443) return false
            if (uri.rawQuery != null || uri.rawFragment != null) return false
            // uri.path viene decodificado: "%2F" se convertiría en "/" y
            // añadiría segmentos (o permitiría ".."): ambas cosas se rechazan.
            val segments = uri.path.split('/')
            if (segments.size != 7 || segments[0].isNotEmpty()) return false
            if (segments[1] + "/" + segments[2] != repo) return false
            if (segments[3] != "releases" || segments[4] != "download") return false
            if (segments[5] != tag || segments[6] != assetName) return false
            segments.drop(1).none { it.isEmpty() || it == "." || it == ".." }
        } catch (e: Exception) {
            false
        }
    }
}
