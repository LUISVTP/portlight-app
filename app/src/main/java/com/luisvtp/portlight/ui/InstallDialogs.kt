package com.luisvtp.portlight.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.luisvtp.portlight.R
import com.luisvtp.portlight.domain.formatBytes
import com.luisvtp.portlight.install.UiInstallConfirmer

/**
 * Diálogos de confirmación del flujo de instalación. `UiInstallConfirmer`
 * BLOQUEA en el hilo de instalación hasta que aquí se responde; cerrar el
 * diálogo sin botón cuenta como «no» (cancelación).
 */
@Composable
fun InstallDialogs(confirmer: UiInstallConfirmer) {
    val request = confirmer.request.value ?: return
    val titleRes = when (request.kind) {
        UiInstallConfirmer.Kind.OWN_COPY -> R.string.dialog_own_copy_title
        UiInstallConfirmer.Kind.MISSING_DIGEST -> R.string.dialog_missing_digest_title
        UiInstallConfirmer.Kind.SIGNATURE_CHANGED -> R.string.dialog_signature_changed_title
        UiInstallConfirmer.Kind.UNKNOWN_SOURCES -> R.string.dialog_unknown_sources_title
    }
    AlertDialog(
        onDismissRequest = { confirmer.respond(false) },
        title = { Text(stringResource(titleRes)) },
        text = { Text(dialogBody(request)) },
        confirmButton = {
            TextButton(onClick = { confirmer.respond(true) }) {
                Text(stringResource(R.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = { confirmer.respond(false) }) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun dialogBody(request: UiInstallConfirmer.Request): String = when (request.kind) {
    UiInstallConfirmer.Kind.OWN_COPY -> {
        // Fase 4: tamaño del APK y recomendación de Wi-Fi junto al aviso legal.
        val sizeLine = request.sizeBytes?.let { stringResource(R.string.download_size, formatBytes(it)) }
        listOfNotNull(
            stringResource(R.string.dialog_own_copy_body, request.portName),
            sizeLine,
            stringResource(R.string.download_wifi_recommendation),
        ).joinToString("\n\n")
    }
    UiInstallConfirmer.Kind.MISSING_DIGEST ->
        stringResource(R.string.dialog_missing_digest_body, request.portName)
    UiInstallConfirmer.Kind.SIGNATURE_CHANGED -> stringResource(
        R.string.dialog_signature_changed_body,
        request.portName,
        request.fingerprint.orEmpty(),
    )
    UiInstallConfirmer.Kind.UNKNOWN_SOURCES ->
        stringResource(R.string.dialog_unknown_sources_body, request.portName)
}
