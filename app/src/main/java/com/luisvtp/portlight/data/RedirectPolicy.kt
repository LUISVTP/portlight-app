package com.luisvtp.portlight.data

import java.net.URI

/**
 * Seguimiento manual de redirecciones: cada salto se valida contra una lista
 * blanca de hosts. Se usa con `HttpURLConnection.instanceFollowRedirects =
 * false`, porque el seguimiento automático no revalida el host de destino.
 *
 * Reglas por salto: `Location` presente, URL bien formada (absoluta o
 * relativa —se resuelve contra la URL actual—), esquema permitido (HTTPS en
 * producción), host en la lista blanca y no más de [maxHops] saltos.
 */
class RedirectPolicy(
    private val allowedHosts: Set<String>,
    private val allowedSchemes: Set<String> = setOf("https"),
    private val maxHops: Int = DEFAULT_MAX_HOPS,
) {

    /** URL absoluta del siguiente salto, o null si el salto se rechaza. */
    fun next(fromUrl: String, location: String?, alreadyFollowed: Int): String? {
        if (alreadyFollowed >= maxHops) return null
        if (location.isNullOrBlank()) return null
        return try {
            val target = URI(fromUrl).resolve(location.trim())
            val scheme = target.scheme?.lowercase()
            val host = target.host?.lowercase()
            if (scheme in allowedSchemes && host != null && host in allowedHosts) {
                target.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val DEFAULT_MAX_HOPS = 5
    }
}
