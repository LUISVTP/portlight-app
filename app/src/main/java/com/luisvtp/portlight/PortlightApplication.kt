package com.luisvtp.portlight

import android.app.Application
import android.os.Build
import com.luisvtp.portlight.data.HttpUrlConnectionReleaseClient
import com.luisvtp.portlight.data.ReleaseDownloader
import com.luisvtp.portlight.install.AndroidApkInspector
import com.luisvtp.portlight.install.InstallFlow
import com.luisvtp.portlight.install.PackageInstallerApkInstaller
import com.luisvtp.portlight.install.SharedPrefsFingerprintStore
import com.luisvtp.portlight.install.UiInstallConfirmer
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Composición de la instalación (Fase 2B): un único hilo de instalación por
 * proceso y barrido de `cacheDir/downloads` al arrancar.
 *
 * El barrido vive en `Application.onCreate` (proceso, no actividad): borra
 * temporales de procesos anteriores —incluido el del fallback ACTION_VIEW—
 * sin peligro de rotación de pantalla. Regla PLAN §9: el APK descargado se
 * borra tras instalar o fallar; solo puede sobrevivir si el instalador del
 * sistema aún lo necesita o si el proceso murió a mitad de flujo.
 */
class PortlightApplication : Application() {

    lateinit var installConfirmer: UiInstallConfirmer
        private set

    lateinit var installFlow: InstallFlow
        private set

    /** Cola única: una instalación a la vez por proceso. */
    val installExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()

        val downloadsDir = File(cacheDir, DOWNLOADS_DIR)
        downloadsDir.mkdirs()
        downloadsDir.listFiles()?.forEach { entry ->
            if (entry.isFile) entry.delete()
        }

        val confirmer = UiInstallConfirmer(this)
        installConfirmer = confirmer
        installFlow = InstallFlow(
            confirmer = confirmer,
            downloader = ReleaseDownloader(HttpUrlConnectionReleaseClient(), downloadsDir),
            inspector = AndroidApkInspector(this),
            fingerprints = SharedPrefsFingerprintStore(this),
            installer = PackageInstallerApkInstaller(this),
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
        )
    }

    private companion object {
        const val DOWNLOADS_DIR = "downloads"
    }
}
