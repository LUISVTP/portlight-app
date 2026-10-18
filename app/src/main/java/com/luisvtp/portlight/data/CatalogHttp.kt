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
}

/** Cliente mínimo del catálogo; los tests unitarios usan una simulación. */
interface CatalogHttpGateway {
    fun get(url: String, etag: String?, lastModified: String?): CatalogHttpResponse
}

/**
 * Implementación real con HttpURLConnection: HTTPS obligatorio y lista blanca
 * de hosts ([CatalogUrls]), tiempos máximos de conexión/lectura y límite de
 * tamaño del cuerpo. El catálogo se sirve en UTF-8 (GitHub Pages y raw).
 */
class HttpUrlConnectionGateway(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 10_000,
    private val maxBodyBytes: Int = MAX_BODY_BYTES,
) : CatalogHttpGateway {

    override fun get(url: String, etag: String?, lastModified: String?): CatalogHttpResponse {
        if (!CatalogUrls.isAllowed(url)) {
            return CatalogHttpResponse.RejectedByPolicy
        }
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            etag?.let { connection.setRequestProperty("If-None-Match", it) }
            lastModified?.let { connection.setRequestProperty("If-Modified-Since", it) }
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.use { readLimitedText(it, maxBodyBytes) }
                    if (body == null) {
                        CatalogHttpResponse.IOError
                    } else {
                        CatalogHttpResponse.Success(
                            body = body,
                            etag = connection.getHeaderField("ETag"),
                            lastModified = connection.getHeaderField("Last-Modified"),
                        )
                    }
                }
                HttpURLConnection.HTTP_NOT_MODIFIED -> CatalogHttpResponse.NotModified
                else -> CatalogHttpResponse.HttpError(code)
            }
        } catch (e: IOException) {
            CatalogHttpResponse.IOError
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** Límite del cuerpo: un catálogo real ronda decenas de kB. */
        const val MAX_BODY_BYTES = 2 * 1024 * 1024
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
