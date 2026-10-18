package com.luisvtp.portlight.data

import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Servidor HTTP local mínimo para pruebas: sockets sobre 127.0.0.1, sin
 * dependencias de módulos internos del JDK (`com.sun.*` no es visible para el
 * compilador de AGP). Las peticiones nunca salen de esta máquina: los tests
 * jamás contactan repositorios reales.
 */
internal class LocalHttpServer {

    /** Petición recibida: ruta y cabeceras (claves en minúsculas). */
    class Request(private val path: String, private val headers: Map<String, String>) {
        fun header(name: String): String? = headers[name.lowercase()]
        fun path(): String = path
    }

    /** Respuesta a devolver. [body] vacío por defecto (304, 404, 302…). */
    class Response(
        val code: Int,
        val headers: Map<String, String> = emptyMap(),
        val body: ByteArray = ByteArray(0),
    )

    private val routes = ConcurrentHashMap<String, (Request) -> Response>()
    private val requestCounter = AtomicInteger()
    private val pool = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "local-http-test").apply { isDaemon = true }
    }

    /** Peticiones recibidas (para pruebas que exigen no contactar el servidor). */
    fun requestCount(): Int = requestCounter.get()

    @Volatile
    private var running = false

    private var serverSocket: ServerSocket? = null

    val port: Int
        get() = serverSocket?.localPort ?: error("El servidor no está iniciado")

    fun url(path: String): String = "http://127.0.0.1:$port$path"

    fun handle(path: String, handler: (Request) -> Response) {
        routes[path] = handler
    }

    fun start() {
        val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        serverSocket = socket
        running = true
        pool.execute {
            while (running) {
                try {
                    val client = socket.accept()
                    pool.execute { handleSocket(client) }
                } catch (e: IOException) {
                    break
                }
            }
        }
    }

    fun stop() {
        running = false
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            // Ya cerrado.
        }
        pool.shutdownNow()
    }

    private fun handleSocket(socket: Socket) {
        try {
            socket.use { s ->
                s.soTimeout = 5_000
                val request = readRequest(s) ?: return
                requestCounter.incrementAndGet()
                val response = routes[request.path()]?.invoke(request) ?: Response(404)
                writeResponse(s, response)
            }
        } catch (e: IOException) {
            // Cliente que corta la conexión (p. ej. por límite de tamaño) o timeout.
        }
    }

    private fun readRequest(socket: Socket): Request? {
        val reader = socket.getInputStream().bufferedReader(Charsets.ISO_8859_1)
        val requestLine = reader.readLine() ?: return null
        val parts = requestLine.split(' ')
        if (parts.size < 2) return null
        val path = parts[1].substringBefore('?')
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            val colon = line.indexOf(':')
            if (colon > 0) {
                headers[line.substring(0, colon).trim().lowercase()] =
                    line.substring(colon + 1).trim()
            }
        }
        return Request(path, headers)
    }

    private fun writeResponse(socket: Socket, response: Response) {
        val head = StringBuilder()
        head.append("HTTP/1.1 ").append(response.code).append(' ').append(reason(response.code))
        head.append("\r\n")
        // 204/304 no llevan cuerpo ni Content-Length.
        if (response.code != 204 && response.code != 304) {
            head.append("Content-Length: ").append(response.body.size).append("\r\n")
        }
        head.append("Connection: close\r\n")
        for ((name, value) in response.headers) {
            head.append(name).append(": ").append(value).append("\r\n")
        }
        head.append("\r\n")
        val out = socket.getOutputStream()
        out.write(head.toString().toByteArray(Charsets.ISO_8859_1))
        if (response.code != 204 && response.code != 304) {
            out.write(response.body)
        }
        out.flush()
    }

    private fun reason(code: Int): String = when (code) {
        200 -> "OK"
        301, 302, 303, 307, 308 -> "Redirect"
        304 -> "Not Modified"
        404 -> "Not Found"
        else -> "Status"
    }
}
