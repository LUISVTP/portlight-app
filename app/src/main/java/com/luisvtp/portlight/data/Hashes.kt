package com.luisvtp.portlight.data

import java.security.MessageDigest

/**
 * SHA-256 en hexadecimal minúsculo: lo usan la verificación de integridad de
 * la descarga (Fase 2B) y la huella de firma TOFU (mismo formato para
 * comparar digest del catálogo y firmas de certificado).
 */
object Hashes {

    fun sha256Hex(bytes: ByteArray): String = toHex(
        MessageDigest.getInstance("SHA-256").digest(bytes),
    )

    fun toHex(bytes: ByteArray): String = bytes.joinToString("") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }
}
