package com.luisvtp.portlight.install

import android.content.Context

/**
 * Persistencia de la huella TOFU por `id` del port en preferencias privadas
 * (solo interna, `MODE_PRIVATE`). Nunca se sobreescribe: la referencia es la
 * firma del PRIMER APK instalado con éxito para ese id (PLAN §9).
 */
class SharedPrefsFingerprintStore(context: Context) : FingerprintStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun fingerprintFor(portId: String): String? = prefs.getString(key(portId), null)

    override fun recordFingerprint(portId: String, fingerprint: String) {
        prefs.edit().putString(key(portId), fingerprint).apply()
    }

    private fun key(portId: String) = "fingerprint_$portId"

    private companion object {
        const val PREFS_NAME = "portlight_tofu"
    }
}
