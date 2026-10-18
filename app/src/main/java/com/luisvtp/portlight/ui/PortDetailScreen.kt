package com.luisvtp.portlight.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luisvtp.portlight.R
import com.luisvtp.portlight.data.Port
import com.luisvtp.portlight.domain.categoryStringRes
import com.luisvtp.portlight.domain.downloadSizeBytes
import com.luisvtp.portlight.domain.formatBytes
import com.luisvtp.portlight.domain.localized
import com.luisvtp.portlight.domain.unmetMinSdk
import com.luisvtp.portlight.install.InstallOutcome

/** Detalle del port: descripción localizada, autor, licencia, repo, aviso de copia propia y descarga (Fase 2B). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortDetailScreen(
    port: Port,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    installBusy: Boolean = false,
    installOutcome: InstallOutcome? = null,
    onDownloadClick: () -> Unit = {},
) {
    val locale = LocalConfiguration.current.locales.get(0)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(port.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            port.description.localized(locale)?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            // Línea de créditos (Fase 4): enlaza al repositorio original del port.
            CreditsRow(port = port)
            DetailRow(label = stringResource(R.string.port_category), value = port.category?.let { stringResource(categoryStringRes(it)) } ?: stringResource(R.string.category_other))
            DetailRow(label = stringResource(R.string.port_license), value = port.license)
            DetailRow(label = stringResource(R.string.port_repo), value = port.repo)
            port.release?.let { release ->
                DetailRow(
                    label = stringResource(R.string.port_version_label),
                    value = release.tag,
                )
            }
            // Tamaño del APK que se descargará (Fase 4b), con el mismo selector de ABI
            downloadSizeBytes(port, Build.SUPPORTED_ABIS.toList())?.let { size ->
                Text(
                    stringResource(R.string.download_size, formatBytes(size)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Juego que la persona usuaria debe aportar (Fase 4)
            port.requiredGame?.let { game ->
                OutlinedCard {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            stringResource(R.string.required_game_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(R.string.required_game_info, game.gameTitle, game.gameId, game.revision),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // Avisos de compatibilidad publicados por el autor del port (Fase 4)
            port.notes?.localized(locale)?.let { notes ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.warning_cd))
                        Spacer(Modifier.size(8.dp))
                        Text(notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // El port puede exigir una versión de Android superior a la del dispositivo
            SdkRequirementWarning(minSdkPort = port.minSdkPort)

            // Aviso obligatorio de copia propia (política del catálogo)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        stringResource(R.string.legal_notice_required_copy),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Capturas: el marcador genérico se muestra si el autor no publica ninguna
            if (port.screenshots.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painterResource(android.R.drawable.ic_menu_gallery),
                        contentDescription = stringResource(R.string.screenshot_marker_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.port_screenshots_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Descarga e instalación (Fase 2B): solo si la release publica assets
            if (port.release != null) {
                Button(
                    onClick = onDownloadClick,
                    enabled = !installBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.port_download_install))
                }
                val status: String? = when {
                    installBusy -> stringResource(R.string.download_in_progress)
                    installOutcome != null -> installOutcomeMessage(installOutcome)
                    else -> null
                }
                if (status != null) {
                    Text(
                        status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** «Port de {autor}» / «Port by {author}», enlazado al repositorio original (Fase 4). */
@Composable
private fun CreditsRow(port: Port, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(modifier.fillMaxWidth()) {
        Text(stringResource(R.string.port_author), style = MaterialTheme.typography.labelMedium)
        Text(
            stringResource(R.string.port_credits, port.author.name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { uriHandler.openUri(port.sourceUrl) },
        )
    }
}

/** Aviso si el dispositivo queda por debajo del `min_sdk_port` del port (Fase 4). */
@Composable
private fun SdkRequirementWarning(
    minSdkPort: Int?,
    sdkInt: Int = Build.VERSION.SDK_INT,
    modifier: Modifier = Modifier,
) {
    val unmet = unmetMinSdk(sdkInt, minSdkPort) ?: return
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.warning_cd))
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(R.string.port_sdk_warning_below, sdkInt, unmet),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(name = "ES", locale = "es")
@Preview(name = "EN", locale = "en")
@Composable
private fun PortDetailScreenPreview() {
    MaterialTheme {
        PortDetailScreen(port = PreviewData.ports.first(), onBack = {})
    }
}

@Preview(name = "ES sin capturas", locale = "es")
@Composable
private fun PortDetailWithoutScreenshotsPreview() {
    MaterialTheme {
        PortDetailScreen(port = PreviewData.ports.last(), onBack = {})
    }
}

@Preview(name = "ES aviso de SDK", locale = "es")
@Preview(name = "EN SDK warning", locale = "en")
@Composable
private fun SdkRequirementWarningPreview() {
    MaterialTheme {
        SdkRequirementWarning(minSdkPort = 34, sdkInt = 29)
    }
}
