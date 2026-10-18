package com.luisvtp.portlight.data

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Respuesta HTTP cruda de UNA petición de descarga (sin seguir redirecciones). */
sealed class ReleaseHttpResponse {

    /**
     * Respuesta con código HTTP. [body] solo se abre en 2xx. Cerrar libera el
     * stream y desconecta la conexión: hay que cerrarla SIEMPRE, también en
     * redirecciones y respuestas de error.
     */
    class Received(
        val code: Int,
        val location: String?,
        val contentLength: Long?,
        val body: InputStream?,
        private val onClose: () -> Unit = {},
    ) : ReleaseHttpResponse(), Closeable {
        override fun close() {
            try {
                body?.close()
            } finally {
                onClose()
            }
        }
    }

    /** Sin conexión o timeout. */
    object NetworkError : ReleaseHttpResponse()
}

/** Cliente de UNA petición; las pruebas usan el servidor local o una simulación. */
interface ReleaseHttpClient {
    fun get(url: String): ReleaseHttpResponse
}

/**
 * Implementación real con HttpURLConnection: seguimiento manual de
 * redirecciones (`instanceFollowRedirects = false`) para que [ReleaseDownloader]
 * valide cada salto contra la lista blanca de hosts de descarga, tiempos de
 * conexión/lectura y cabecera `Accept` de binario.
 */
class HttpUrlConnectionReleaseClient(
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
) : ReleaseHttpClient {

    override fun get(url: String): ReleaseHttpResponse {
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            return ReleaseHttpResponse.NetworkError
        }
        return try {
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.instanceFollowRedirects = false
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/octet-stream")
            val code = connection.responseCode
            val body = if (code in 200..299) {
                try {
                    connection.inputStream
                } catch (e: IOException) {
                    null
                }
            } else {
                null
            }
            ReleaseHttpResponse.Received(
                code = code,
                location = connection.getHeaderField("Location"),
                contentLength = connection.getHeaderField("Content-Length")?.toLongOrNull(),
                body = body,
                onClose = { connection.disconnect() },
            )
        } catch (e: IOException) {
            connection.disconnect()
            ReleaseHttpResponse.NetworkError
        } catch (e: Exception) {
            connection.disconnect()
            ReleaseHttpResponse.NetworkError
        }
    }
}
