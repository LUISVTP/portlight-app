package com.luisvtp.portlight.ui

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luisvtp.portlight.R
import com.luisvtp.portlight.data.Port
import com.luisvtp.portlight.domain.catalogCredits

/**
 * Pantalla legal: texto aprobado en los dos idiomas (values/ y values-es/) y
 * créditos derivados del catálogo (Fase 4): cada port listado cita a su autor,
 * su licencia y enlaza a su repositorio original.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    ports: List<Port>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.legal_title)) },
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
            Text(
                stringResource(R.string.legal_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            // Canal de retiradas vía Issues del repo (Fase 4b)
            Text(
                stringResource(R.string.legal_takedown),
                style = MaterialTheme.typography.bodyMedium,
            )
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

            // Créditos derivados del catálogo (Fase 4): nada escrito a mano.
            val credits = catalogCredits(ports)
            if (credits.isNotEmpty()) {
                val uriHandler = LocalUriHandler.current
                Text(
                    stringResource(R.string.legal_credits_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.legal_credits_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )
                credits.forEach { credit ->
                    Text(
                        stringResource(
                            R.string.legal_credit_line,
                            credit.portName,
                            credit.authorName,
                            credit.license,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { uriHandler.openUri(credit.url) },
                    )
                }
                Text(
                    stringResource(R.string.legal_not_affiliated),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview(name = "ES", locale = "es")
@Preview(name = "EN", locale = "en")
@Composable
private fun LegalScreenPreview() {
    MaterialTheme {
        LegalScreen(ports = PreviewData.ports, onBack = {})
    }
}
