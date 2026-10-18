package com.luisvtp.portlight.data

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.security.MessageDigest

/**
 * Descarga de APK con verificación en streaming (Fase 2B, PLAN §9):
 *
 *  - La URL inicial debe ser la descarga canónica `releases/download` del
 *    `repo` de la entrada (política de hosts SEPARADA de la del catálogo,
 *    ver [DownloadUrls]); si no, se rechaza sin conectar.
 *  - Cada redirección se valida por salto contra la allowlist de descarga.
 *  - Tamaño máximo = `size` del asset + margen: comprobado durante el volcado
 *    (y de forma anticipada cuando el servidor declara `Content-Length`).
 *  - SHA-256 en streaming comparado con `digest` del catálogo; si no coincide
 *    se BORRA el archivo y se devuelve [Outcome.DigestMismatch].
 *  - Sin `digest` se descarga igual y se devuelve `digestVerified = false`
 *    (el flujo pide entonces una confirmación extra).
 *  - Los archivos viven en [downloadsDir] (cacheDir privada) y el flujo los
 *    elimina tras instalar o fallar.
 *
 * Las pruebas nunca contactan repositorios reales: usan el servidor local o
 * un cliente simulado.
 */
class ReleaseDownloader(
    private val client: ReleaseHttpClient,
    private val downloadsDir: File,
    private val redirectPolicy: RedirectPolicy = RedirectPolicy(DownloadUrls.ALLOWED_DOWNLOAD_HOSTS),
    private val isStartUrlAllowed: (asset: ReleaseAsset, repo: String, tag: String) -> Boolean =
        { asset, repo, tag -> DownloadUrls.matchesReleaseUrl(asset.url, repo, tag, asset.name) },
    private val sizeMarginBytes: Long = DEFAULT_SIZE_MARGIN_BYTES,
) {

    sealed class Outcome {
        class Success(val file: File, val digestVerified: Boolean) : Outcome()
        object DigestMismatch : Outcome()
        object TooLarge : Outcome()
        object UrlRejected : Outcome()
        object RedirectDenied : Outcome()
        object NetworkFailure : Outcome()
        object Cancelled : Outcome()
        class HttpFailure(val code: Int) : Outcome()
    }

    fun download(
        asset: ReleaseAsset,
        repo: String,
        tag: String,
        fileName: String,
        shouldAbort: () -> Boolean = { false },
    ): Outcome {
        if (!isStartUrlAllowed(asset, repo, tag)) return Outcome.UrlRejected
        downloadsDir.mkdirs()
        // Destino limpio: nada de restos de intentos anteriores.
        val target = File(downloadsDir, fileName.replace(INVALID_NAME_CHARS, "_"))
        target.delete()
        val maxBytes = asset.size + sizeMarginBytes
        var url = asset.url
        var followed = 0
        while (true) {
            if (shouldAbort()) return Outcome.Cancelled
            val response = client.get(url)
            if (response !is ReleaseHttpResponse.Received) return Outcome.NetworkFailure
            if (response.code in REDIRECT_CODES) {
                val next = redirectPolicy.next(url, response.location, followed)
                response.close()
                if (next == null) return Outcome.RedirectDenied
                followed++
                url = next
                continue
            }
            if (response.code != HttpURLConnection.HTTP_OK) {
                val code = response.code
                response.close()
                return Outcome.HttpFailure(code)
            }
            return streamToFile(response, target, maxBytes, asset.digest, shouldAbort)
        }
    }

    private fun streamToFile(
        response: ReleaseHttpResponse.Received,
        target: File,
        maxBytes: Long,
        expectedDigest: String?,
        shouldAbort: () -> Boolean,
    ): Outcome {
        // Comprobación anticipada si el servidor declara un tamaño ya excesivo.
        val declared = response.contentLength
        if (declared != null && declared > maxBytes) {
            response.close()
            target.delete()
            return Outcome.TooLarge
        }
        val input = response.body
        if (input == null) {
            response.close()
            target.delete()
            return Outcome.NetworkFailure
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        var earlyExit: Outcome? = null
        try {
            FileOutputStream(target).use { out ->
                while (true) {
                    if (shouldAbort()) {
                        earlyExit = Outcome.Cancelled
                        break
                    }
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > maxBytes) {
                        earlyExit = Outcome.TooLarge
                        break
                    }
                    digest.update(buffer, 0, read)
                    out.write(buffer, 0, read)
                }
            }
        } catch (e: IOException) {
            earlyExit = Outcome.NetworkFailure
        } finally {
            response.close()
        }
        if (earlyExit != null) {
            target.delete()
            return earlyExit
        }
        val actual = Hashes.toHex(digest.digest())
        val expected = expectedHex(expectedDigest)
        if (expected != null && !actual.equals(expected, ignoreCase = true)) {
            target.delete()
            return Outcome.DigestMismatch
        }
        return Outcome.Success(target, digestVerified = expected != null)
    }

    companion object {
        /** Margen sobre el `size` declarado en el catálogo. */
        const val DEFAULT_SIZE_MARGIN_BYTES = 1L * 1024 * 1024

        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        private val INVALID_NAME_CHARS = Regex("[^A-Za-z0-9._-]")

        /** `sha256:<hex>` (o hex plano, por robustez) → hex minúscculo; null si no hay digest. */
        private fun expectedHex(digest: String?): String? = when {
            digest == null -> null
            digest.startsWith("sha256:", ignoreCase = true) -> digest.substring(7)
            else -> digest
        }
    }
}
