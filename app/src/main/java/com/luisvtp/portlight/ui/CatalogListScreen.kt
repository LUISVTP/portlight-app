package com.luisvtp.portlight.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luisvtp.portlight.R
import com.luisvtp.portlight.data.Port
import com.luisvtp.portlight.domain.categoryStringRes
import com.luisvtp.portlight.domain.localized
import java.util.Locale

/**
 * Lista del catálogo con buscador por nombre/descripción y filtro por categoría.
 * Estado interno (consulta y categoría) — sin red: Fase 1 con catálogo embebido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogListScreen(
    ports: List<Port>,
    onPortClick: (String) -> Unit,
    onLegalClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    // "" = todas las categorías (sentinela compatible con rememberSaveable)
    var selectedCategory by rememberSaveable { mutableStateOf("") }

    val locale = LocalConfiguration.current.locales.get(0)
    val categories = remember(ports) { ports.mapNotNull { it.category }.distinct().sorted() }
    val filtered = ports.filter { port ->
        val matchesCategory = selectedCategory.isEmpty() || port.category == selectedCategory
        val matchesQuery = query.isBlank() ||
            port.name.contains(query, ignoreCase = true) ||
            port.description.localized(locale)?.contains(query, ignoreCase = true) == true
        matchesCategory && matchesQuery
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalog_title)) },
                actions = {
                    IconButton(onClick = onLegalClick) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.nav_legal),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.catalog_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory.isEmpty(),
                        onClick = { selectedCategory = "" },
                        label = { Text(stringResource(R.string.category_all)) },
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = if (selectedCategory == category) "" else category
                        },
                        label = { Text(stringResource(categoryStringRes(category))) },
                    )
                }
            }
            if (filtered.isEmpty()) {
                Text(
                    stringResource(R.string.catalog_empty),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.id }) { port ->
                        PortCard(
                            port = port,
                            locale = locale,
                            onClick = { onPortClick(port.id) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortCard(
    port: Port,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(port.name, style = MaterialTheme.typography.titleMedium)
            port.category?.let {
                Text(
                    stringResource(categoryStringRes(it)),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            port.description.localized(locale)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(name = "ES", locale = "es")
@Preview(name = "EN", locale = "en")
@Composable
private fun CatalogListScreenPreview() {
    MaterialTheme {
        CatalogListScreen(
            ports = PreviewData.ports,
            onPortClick = {},
            onLegalClick = {},
        )
    }
}
