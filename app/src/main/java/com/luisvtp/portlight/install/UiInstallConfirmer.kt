package com.luisvtp.portlight.install

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.luisvtp.portlight.data.Port
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Confirmaciones del usuario en la UI (diálogos Compose) para el flujo que
 * corre en segundo plano: cada petición coloca un [Request] en [request] y
 * BLOQUEA hasta que la UI llama a [respond] (o agota el tiempo).
 *
 * Además gestiona el permiso «instalar apps desconocidas»: pregunta, abre la
 * pantalla de Ajustes correspondiente y espera el retorno de la actividad
 * (vía callbacks de ciclo de vida) para comprobar si se concedió.
 */
class UiInstallConfirmer(private val app: Application) : InstallConfirmer {

    enum class Kind { OWN_COPY, MISSING_DIGEST, SIGNATURE_CHANGED, UNKNOWN_SOURCES }

    data class Request(
        val kind: Kind,
        val portName: String,
        val fingerprint: String? = null,
    )

    /** Estado observable: `InstallDialogs` muestra el diálogo de la petición activa. */
    private val _request = mutableStateOf<Request?>(null)
    val request: State<Request?> = _request

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pending = AtomicReference<Pair<Request, CountDownLatch>?>(null)

    @Volatile
    private var answer = false

    /** Retorno desde Ajustes: se completa al reanudarse cualquier actividad. */
    private val settingsLatch = AtomicReference<CountDownLatch?>(null)

    @Volatile
    private var awaitingSettings = false

    init {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) {
                if (awaitingSettings) {
                    awaitingSettings = false
                    settingsLatch.getAndSet(null)?.countDown()
                }
            }
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    override fun confirmOwnCopy(port: Port): Boolean =
        ask(Request(Kind.OWN_COPY, port.name))

    override fun confirmMissingDigest(port: Port): Boolean =
        ask(Request(Kind.MISSING_DIGEST, port.name))

    override fun confirmSignatureChanged(port: Port, newFingerprint: String): Boolean =
        ask(Request(Kind.SIGNATURE_CHANGED, port.name, newFingerprint))

    override fun requestInstallPermission(port: Port): Boolean {
        if (canInstallPackages()) return true
        // Sin REQUEST_INSTALL_PACKAGES por app en API < 26: el propio sistema
        // pedirá la confirmación cuando toque.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        if (!ask(Request(Kind.UNKNOWN_SOURCES, port.name))) return false
        launchUnknownSourcesSettings()
        awaitSettingsReturn()
        return canInstallPackages()
    }

    /** Lo llama la UI al pulsar un botón del diálogo (hilo principal). */
    fun respond(accepted: Boolean) {
        answer = accepted
        val current = pending.getAndSet(null)
        mainHandler.post { _request.value = null }
        current?.second?.countDown()
    }

    private fun ask(request: Request): Boolean {
        val latch = CountDownLatch(1)
        answer = false
        pending.set(request to latch)
        mainHandler.post { _request.value = request }
        val completed = latch.await(DIALOG_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        val held = pending.get()
        if (held != null && held.first === request) pending.compareAndSet(held, null)
        if (!completed || !answer) {
            // Tiempo agotado: retira el diálogo si sigue visible y trata como cancelación.
            mainHandler.post { if (_request.value == request) _request.value = null }
            return false
        }
        return true
    }

    private fun canInstallPackages(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return app.packageManager.canRequestPackageInstalls()
    }

    private fun launchUnknownSourcesSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:" + app.packageName),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(intent)
    }

    private fun awaitSettingsReturn() {
        val latch = CountDownLatch(1)
        settingsLatch.set(latch)
        awaitingSettings = true
        try {
            latch.await(SETTINGS_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        awaitingSettings = false
    }

    private companion object {
        const val DIALOG_TIMEOUT_MINUTES = 5L
        const val SETTINGS_TIMEOUT_MINUTES = 2L
    }
}
