package com.luisvtp.portlight.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luisvtp.portlight.R
import com.luisvtp.portlight.data.Port
import com.luisvtp.portlight.domain.categoryStringRes
import com.luisvtp.portlight.domain.localized

/** Detalle del port: descripción localizada, autor, licencia, repo y aviso de copia propia. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortDetailScreen(
    port: Port,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
            DetailRow(label = stringResource(R.string.port_category), value = port.category?.let { stringResource(categoryStringRes(it)) })
            DetailRow(label = stringResource(R.string.port_author), value = port.author.name)
            DetailRow(label = stringResource(R.string.port_license), value = port.license)
            DetailRow(label = stringResource(R.string.port_repo), value = port.repo)

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
