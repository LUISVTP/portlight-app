package com.luisvtp.portlight.install

import com.luisvtp.portlight.data.DownloadUrls
import com.luisvtp.portlight.data.Port
import com.luisvtp.portlight.data.ReleaseAsset
import com.luisvtp.portlight.data.ReleaseDownloader
import com.luisvtp.portlight.domain.AbiSelector
import java.io.File

/** Confirmaciones que la UI pide al usuario antes de continuar (se bloquean hasta la respuesta). */
interface InstallConfirmer {
    /** Aviso legal de copia propia: SIEMPRE, antes de cada descarga. */
    fun confirmOwnCopy(port: Port): Boolean

    /** Aviso extra cuando el catálogo no trae `digest` (no se puede verificar). */
    fun confirmMissingDigest(port: Port): Boolean

    /** Aviso alto cuando la firma del APK cambia respecto a la primera instalación (TOFU). */
    fun confirmSignatureChanged(port: Port, newFingerprint: String): Boolean

    /** Permiso «instalar apps desconocidas»; devuelve true si podemos instalar. */
    fun requestInstallPermission(port: Port): Boolean
}

/** APK ya descargado: package del manifest + huellas SHA-256 de sus firmas. */
data class ApkArchive(val packageName: String, val sha256Fingerprints: List<String>)

/** Lee package y firmas del APK con `PackageManager.getPackageArchiveInfo`. */
interface ApkInspector {
    /** null si el fichero no es un APK legible (se trata como inválido). */
    fun inspect(apkFile: File): ApkArchive?
}

/** Huella TOFU por `id` del port: la del PRIMER APK instalado con éxito. */
interface FingerprintStore {
    fun fingerprintFor(portId: String): String?
    fun recordFingerprint(portId: String, fingerprint: String)
}

/** Quién ejecuta la instalación en el dispositivo (PackageInstaller o ACTION_VIEW). */
interface ApkInstaller {
    fun install(apkFile: File, expectedPackageName: String?): InstallAttempt
}

/** Resultado de intentar instalar; el usuario SIEMPRE confirma en el sistema. */
sealed class InstallAttempt {
    object Success : InstallAttempt()
    object Aborted : InstallAttempt()
    object Blocked : InstallAttempt()
    object FallbackLaunched : InstallAttempt()
    data class Failure(val detail: String) : InstallAttempt()
}

sealed class InstallFailure {
    object NoApkAsset : InstallFailure()
    object AbiUnsupported : InstallFailure()
    object AbiAmbiguous : InstallFailure()
    object UrlRejected : InstallFailure()
    object DigestMismatch : InstallFailure()
    object TooLarge : InstallFailure()
    object RedirectDenied : InstallFailure()
    object NetworkFailure : InstallFailure()
    data class Http(val code: Int) : InstallFailure()
    object ApkInvalid : InstallFailure()
    object PackageMismatch : InstallFailure()
    object InstallBlocked : InstallFailure()
    data class InstallFailed(val detail: String) : InstallFailure()
}

sealed class InstallOutcome {
    object Installed : InstallOutcome()
    object Cancelled : InstallOutcome()
    object SystemInstallerLaunched : InstallOutcome()
    data class Failed(val failure: InstallFailure) : InstallOutcome()
}

/**
 * Orquestador de descarga e instalación (Fase 2B). Se ejecuta en un hilo en
 * segundo plano; cada confirmación se bloquea hasta que la UI responde.
 *
 * Orden y garantías:
 *  1. Aviso legal de copia propia — SIEMPRE el primer paso de cada descarga.
 *  2. Selección de asset por ABI (PLAN §8).
 *  3. Re-validación de la URL de descarga (defensa en profundidad).
 *  4. Descarga con SHA-256 en streaming, límite de tamaño y redirecciones
 *     validadas por [ReleaseDownloader].
 *  5. Sin `digest`: confirmación extra del usuario.
 *  6. Comprobación de `package` y fijado TOFU de la firma (cambio → aviso).
 *  7. Permiso de instalación («apps desconocidas») si hace falta.
 *  8. Instalador del sistema (el usuario confirma allí).
 *
 * El temporal se BORRA tras instalar, cancelar o fallar; la única excepción
 * es el fallback ACTION_VIEW (`FallbackLaunched`), que lo deja para el
 * barrido del próximo inicio porque el sistema aún lo leerá.
 */
class InstallFlow(
    private val confirmer: InstallConfirmer,
    private val downloader: ReleaseDownloader,
    private val inspector: ApkInspector,
    private val fingerprints: FingerprintStore,
    private val installer: ApkInstaller,
    private val supportedAbis: List<String>,
    // Política inyectable para las pruebas: en producción exige la descarga
    // canónica de GitHub del repo de la entrada (los tests usan el servidor
    // local y jamás contactan repositorios reales).
    private val isAssetUrlAllowed: (port: Port, asset: ReleaseAsset, tag: String) -> Boolean =
        { port, asset, tag -> DownloadUrls.matchesReleaseUrl(asset.url, port.repo, tag, asset.name) },
) {

    fun run(port: Port): InstallOutcome {
        val release = port.release ?: return InstallOutcome.Failed(InstallFailure.NoApkAsset)
        if (!confirmer.confirmOwnCopy(port)) return InstallOutcome.Cancelled
        val asset = when (val selection = AbiSelector.select(port, supportedAbis)) {
            is AbiSelector.Selection.Selected -> selection.asset
            AbiSelector.Selection.NoMatchingAsset -> return InstallOutcome.Failed(InstallFailure.NoApkAsset)
            AbiSelector.Selection.Ambiguous -> return InstallOutcome.Failed(InstallFailure.AbiAmbiguous)
            AbiSelector.Selection.UnsupportedAbi -> return InstallOutcome.Failed(InstallFailure.AbiUnsupported)
        }
        // Defensa en profundidad: el esquema ya validó la URL de la release.
        if (!isAssetUrlAllowed(port, asset, release.tag)) {
            return InstallOutcome.Failed(InstallFailure.UrlRejected)
        }
        val outcome = downloader.download(asset, port.repo, release.tag, "${port.id}.apk")
        val downloaded = when (outcome) {
            is ReleaseDownloader.Outcome.Success -> outcome
            ReleaseDownloader.Outcome.Cancelled -> return InstallOutcome.Cancelled
            ReleaseDownloader.Outcome.DigestMismatch ->
                return InstallOutcome.Failed(InstallFailure.DigestMismatch)
            ReleaseDownloader.Outcome.TooLarge -> return InstallOutcome.Failed(InstallFailure.TooLarge)
            ReleaseDownloader.Outcome.UrlRejected -> return InstallOutcome.Failed(InstallFailure.UrlRejected)
            ReleaseDownloader.Outcome.RedirectDenied ->
                return InstallOutcome.Failed(InstallFailure.RedirectDenied)
            ReleaseDownloader.Outcome.NetworkFailure ->
                return InstallOutcome.Failed(InstallFailure.NetworkFailure)
            is ReleaseDownloader.Outcome.HttpFailure ->
                return InstallOutcome.Failed(InstallFailure.Http(outcome.code))
        }
        val file = downloaded.file
        if (!downloaded.digestVerified && !confirmer.confirmMissingDigest(port)) {
            file.delete()
            return InstallOutcome.Cancelled
        }
        val archive = inspector.inspect(file)
            ?: return fail(file, InstallFailure.ApkInvalid)
        if (port.packageName != null && archive.packageName != port.packageName) {
            return fail(file, InstallFailure.PackageMismatch)
        }
        val current = archive.sha256Fingerprints.sorted().joinToString(",")
        val known = fingerprints.fingerprintFor(port.id)
        if (known != null && known != current && !confirmer.confirmSignatureChanged(port, current)) {
            file.delete()
            return InstallOutcome.Cancelled
        }
        if (!confirmer.requestInstallPermission(port)) {
            file.delete()
            return InstallOutcome.Cancelled
        }
        return when (val attempt = installer.install(file, port.packageName)) {
            InstallAttempt.Success -> {
                // TOFU: se guarda solo la huella del primer APK instalado.
                if (known == null) fingerprints.recordFingerprint(port.id, current)
                file.delete()
                InstallOutcome.Installed
            }
            InstallAttempt.Aborted -> {
                file.delete()
                InstallOutcome.Cancelled
            }
            InstallAttempt.Blocked -> fail(file, InstallFailure.InstallBlocked)
            is InstallAttempt.Failure -> fail(file, InstallFailure.InstallFailed(attempt.detail))
            InstallAttempt.FallbackLaunched -> InstallOutcome.SystemInstallerLaunched
        }
    }

    private fun fail(file: File, failure: InstallFailure): InstallOutcome {
        file.delete()
        return InstallOutcome.Failed(failure)
    }
}
