package com.luisvtp.portlight.data

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Respuesta del catálogo ya interpretada a nivel de transporte. */
sealed class CatalogHttpResponse {

    /** 200 con cuerpo. */
    data class Success(
        val body: String,
        val etag: String?,
        val lastModified: String?,
    ) : CatalogHttpResponse()

    /** 304: el servidor confirma que la copia local sigue vigente. */
    object NotModified : CatalogHttpResponse()

    /** Cualquier otro código HTTP (404 incluido). */
    data class HttpError(val code: Int) : CatalogHttpResponse()

    /** Sin conexión, timeout o cuerpo que supera el límite de tamaño. */
    object IOError : CatalogHttpResponse()

    /** URL rechazada por la política (HTTPS + lista blanca de hosts). */
    object RejectedByPolicy : CatalogHttpResponse()

    /** Redirección sin `Location`, a host fuera de la lista o con exceso de saltos. */
    object RedirectDenied : CatalogHttpResponse()
}

/** Cliente mínimo del catálogo; los tests unitarios usan una simulación. */
interface CatalogHttpGateway {
    fun get(url: String, etag: String?, lastModified: String?): CatalogHttpResponse
}

/**
 * Implementación real con HttpURLConnection: HTTPS obligatorio y lista blanca
 * de hosts ([CatalogUrls]), tiempos máximos de conexión/lectura, límite de
 * tamaño del cuerpo y **seguimiento manual de redirecciones**: cada salto se
 * valida contra [redirectPolicy] porque el seguimiento automático no revalida
 * el host de destino. El catálogo se sirve en UTF-8 (GitHub Pages y raw).
 */
class HttpUrlConnectionGateway(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 10_000,
    private val maxBodyBytes: Int = MAX_BODY_BYTES,
    private val isUrlAllowed: (String) -> Boolean = CatalogUrls::isAllowed,
    private val redirectPolicy: RedirectPolicy = RedirectPolicy(CatalogUrls.ALLOWED_HOSTS),
) : CatalogHttpGateway {

    override fun get(url: String, etag: String?, lastModified: String?): CatalogHttpResponse {
        if (!isUrlAllowed(url)) {
            return CatalogHttpResponse.RejectedByPolicy
        }
        var currentUrl = url
        var followed = 0
        while (true) {
            when (val step = requestOnce(currentUrl, etag, lastModified)) {
                is Step.Redirected -> {
                    val next = redirectPolicy.next(currentUrl, step.location, followed)
                        ?: return CatalogHttpResponse.RedirectDenied
                    followed++
                    currentUrl = next
                }
                is Step.Done -> return step.response
            }
        }
    }

    private sealed class Step {
        data class Redirected(val location: String?) : Step()
        data class Done(val response: CatalogHttpResponse) : Step()
    }

    private fun requestOnce(url: String, etag: String?, lastModified: String?): Step {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            // Cada salto se valida a mano: el automático no revalida el host.
            connection.instanceFollowRedirects = false
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            etag?.let { connection.setRequestProperty("If-None-Match", it) }
            lastModified?.let { connection.setRequestProperty("If-Modified-Since", it) }
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.use { readLimitedText(it, maxBodyBytes) }
                    Step.Done(
                        if (body == null) {
                            CatalogHttpResponse.IOError
                        } else {
                            CatalogHttpResponse.Success(
                                body = body,
                                etag = connection.getHeaderField("ETag"),
                                lastModified = connection.getHeaderField("Last-Modified"),
                            )
                        },
                    )
                }
                HttpURLConnection.HTTP_NOT_MODIFIED -> Step.Done(CatalogHttpResponse.NotModified)
                in REDIRECT_CODES -> Step.Redirected(connection.getHeaderField("Location"))
                else -> Step.Done(CatalogHttpResponse.HttpError(code))
            }
        } catch (e: IOException) {
            Step.Done(CatalogHttpResponse.IOError)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** Límite del cuerpo: un catálogo real ronda decenas de kB. */
        const val MAX_BODY_BYTES = 2 * 1024 * 1024

        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}

/**
 * Lee hasta [maxBytes] y devuelve el texto UTF-8; devuelve null si el cuerpo
 * lo supera (se descarta en lugar de consumir memoria sin límite).
 */
internal fun readLimitedText(input: InputStream, maxBytes: Int): String? {
    val buffer = ByteArray(8 * 1024)
    val out = ByteArrayOutputStream()
    while (true) {
        val read = input.read(buffer)
        if (read == -1) break
        out.write(buffer, 0, read)
        if (out.size() > maxBytes) return null
    }
    return out.toString(Charsets.UTF_8.name())
}
