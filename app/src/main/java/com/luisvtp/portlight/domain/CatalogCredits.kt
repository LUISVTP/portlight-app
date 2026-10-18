package com.luisvtp.portlight.domain

import com.luisvtp.portlight.data.Port

/**
 * Créditos de la pantalla legal (Fase 4): se DERIVAN del catálogo, nunca se
 * escriben a mano. `url` apunta al contacto del autor o, si no tiene, al
 * repositorio original del port.
 */
data class PortCredit(
    val portName: String,
    val authorName: String,
    val license: String,
    val url: String,
)

fun catalogCredits(ports: List<Port>): List<PortCredit> = ports.map { port ->
    PortCredit(
        portName = port.name,
        authorName = port.author.name,
        license = port.license,
        url = port.author.contact ?: port.sourceUrl,
    )
}
