package com.luisvtp.portlight.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.luisvtp.portlight.R
import com.luisvtp.portlight.install.InstallFailure
import com.luisvtp.portlight.install.InstallOutcome

/**
 * Texto localizado del resultado del flujo de instalación (Fase 2B): mapea
 * cada [InstallOutcome]/[InstallFailure] a su recurso y formatea los que
 * llevan argumentos (código HTTP, detalle del sistema).
 */
@Composable
fun installOutcomeMessage(outcome: InstallOutcome): String {
    if (outcome is InstallOutcome.Failed) {
        val failure = outcome.failure
        return when (failure) {
            is InstallFailure.Http -> stringResource(R.string.error_http, failure.code)
            is InstallFailure.InstallFailed ->
                stringResource(R.string.error_install_failed, failure.detail)
            else -> stringResource(failureStringRes(failure))
        }
    }
    return stringResource(
        when (outcome) {
            InstallOutcome.Installed -> R.string.download_installed
            InstallOutcome.Cancelled -> R.string.download_cancelled
            InstallOutcome.SystemInstallerLaunched -> R.string.download_installer_opened
            is InstallOutcome.Failed -> R.string.download_cancelled // inalcanzable
        },
    )
}

@StringRes
private fun failureStringRes(failure: InstallFailure): Int = when (failure) {
    InstallFailure.NoApkAsset -> R.string.error_no_apk_asset
    InstallFailure.AbiUnsupported -> R.string.error_abi_unsupported
    InstallFailure.AbiAmbiguous -> R.string.error_ambiguous_asset
    InstallFailure.UrlRejected -> R.string.error_url_not_allowed
    InstallFailure.DigestMismatch -> R.string.error_digest_mismatch
    InstallFailure.TooLarge -> R.string.error_size_exceeded
    InstallFailure.RedirectDenied -> R.string.error_redirect_blocked
    InstallFailure.NetworkFailure -> R.string.error_network
    is InstallFailure.Http -> R.string.error_http
    InstallFailure.ApkInvalid -> R.string.error_apk_invalid
    InstallFailure.PackageMismatch -> R.string.error_package_mismatch
    InstallFailure.InstallBlocked -> R.string.error_install_blocked
    is InstallFailure.InstallFailed -> R.string.error_install_failed
}
