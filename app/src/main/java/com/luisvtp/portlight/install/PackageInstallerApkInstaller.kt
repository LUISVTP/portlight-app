package com.luisvtp.portlight.install

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Instalación con **PackageInstaller** como vía principal:
 *
 *  - `commit()` + receiver de estado ⇒ resultado REAL de la instalación:
 *    podemos borrar el temporal con certeza, distinguir cancelación del
 *    usuario (`STATUS_FAILURE_ABORTED`) y bloqueo del sistema
 *    (`STATUS_FAILURE_BLOCKED`, origen desconocido / Play Protect) y guiar
 *    al usuario en cada caso.
 *  - Si la sesión pide confirmación (`STATUS_PENDING_USER_ACTION`, API ≥ 21),
 *    se lanza el `EXTRA_INTENT` del sistema y se sigue esperando el resultado.
 *  - El PendingIntent del estado es **MUTABLE** en API ≥ 31 (el sistema rellena
 *    los extras; sin `FLAG_MUTABLE` recibiríamos un intent vacío).
 *  - Si PackageInstaller lanza una excepción, **fallback `ACTION_VIEW` +
 *    FileProvider** (`FallbackLaunched`): sin resultado síncrono, el temporal
 *    lo retira el barrido del próximo inicio.
 *
 * En ambos caminos quien confirma es SIEMPRE el instalador del sistema.
 */
class PackageInstallerApkInstaller(private val context: Context) : ApkInstaller {

    override fun install(apkFile: File, expectedPackageName: String?): InstallAttempt {
        val packageInstaller = context.packageManager.packageInstaller
        var sessionId = -1
        try {
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            if (expectedPackageName != null) params.setAppPackageName(expectedPackageName)
            sessionId = packageInstaller.createSession(params)
            packageInstaller.openSession(sessionId).use { session ->
                session.openWrite("base.apk", 0, apkFile.length()).use { out ->
                    apkFile.inputStream().use { input -> input.copyTo(out) }
                    session.fsync(out)
                }
                return awaitResult(session, sessionId)
            }
        } catch (e: Exception) {
            if (sessionId != -1) {
                try {
                    packageInstaller.abandonSession(sessionId)
                } catch (ignored: Exception) {
                    // Nada más que hacer: se cae al fallback.
                }
            }
            return launchFallback(apkFile, e)
        }
    }

    private fun awaitResult(session: PackageInstaller.Session, sessionId: Int): InstallAttempt {
        val action = context.packageName + ACTION_SUFFIX + sessionId
        val latch = CountDownLatch(1)
        val status = AtomicInteger(PackageInstaller.STATUS_FAILURE)
        val message = AtomicReference<String?>(null)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val code = intent?.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                    ?: PackageInstaller.STATUS_FAILURE
                if (code == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                    // El sistema pide confirmación: lanzamos su diálogo y
                    // seguimos esperando el resultado final.
                    @Suppress("DEPRECATION")
                    val confirm = intent?.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    if (confirm != null) {
                        try {
                            context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (e: Exception) {
                            status.set(PackageInstaller.STATUS_FAILURE)
                            message.set("no se pudo abrir el instalador del sistema: ${e.message}")
                            latch.countDown()
                        }
                        return
                    }
                    status.set(PackageInstaller.STATUS_FAILURE)
                    message.set("el instalador del sistema no ofreció confirmación")
                    latch.countDown()
                    return
                }
                status.set(code)
                message.set(intent?.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
                latch.countDown()
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(action),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        var result: InstallAttempt = InstallAttempt.Failure("estado inicial")
        try {
            val statusIntent = Intent(action).setPackage(context.packageName)
            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags = flags or PendingIntent.FLAG_MUTABLE
            }
            val sender = PendingIntent.getBroadcast(context, sessionId, statusIntent, flags).intentSender
            session.commit(sender)
            result = if (!latch.await(INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                // Sin respuesta: se cancela la sesión para que no llegue a
                // instalarse «en secreto» más tarde.
                try {
                    context.packageManager.packageInstaller.abandonSession(sessionId)
                } catch (e: Exception) {
                    // Se reporta igualmente como fallo.
                }
                InstallAttempt.Failure("tiempo de espera agotado con el instalador del sistema")
            } else {
                when (status.get()) {
                    PackageInstaller.STATUS_SUCCESS -> InstallAttempt.Success
                    PackageInstaller.STATUS_FAILURE_ABORTED -> InstallAttempt.Aborted
                    PackageInstaller.STATUS_FAILURE_BLOCKED -> InstallAttempt.Blocked
                    else -> InstallAttempt.Failure(
                        message.get() ?: ("codigo " + status.get()),
                    )
                }
            }
        } finally {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ya registrado dos veces o actividad caída: irrelevante.
            }
        }
        return result
    }

    private fun launchFallback(apkFile: File, cause: Exception): InstallAttempt = try {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + FILE_PROVIDER_SUFFIX,
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        InstallAttempt.FallbackLaunched
    } catch (e: Exception) {
        InstallAttempt.Failure(
            "PackageInstaller y ACTION_VIEW no disponibles: " +
                (cause.message ?: cause.javaClass.simpleName) + " / " +
                (e.message ?: e.javaClass.simpleName),
        )
    }

    private companion object {
        const val ACTION_SUFFIX = ".INSTALL_STATUS."
        const val FILE_PROVIDER_SUFFIX = ".fileprovider"
        const val INSTALL_TIMEOUT_MINUTES = 5L
    }
}
