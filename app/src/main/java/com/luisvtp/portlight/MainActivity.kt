package com.luisvtp.portlight

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luisvtp.portlight.data.CatalogRefreshResult
import com.luisvtp.portlight.data.CatalogRefresher
import com.luisvtp.portlight.data.CatalogRepository
import com.luisvtp.portlight.data.FileCatalogCacheStore
import com.luisvtp.portlight.data.HttpUrlConnectionGateway
import com.luisvtp.portlight.data.Port
import com.luisvtp.portlight.install.InstallOutcome
import com.luisvtp.portlight.ui.CatalogRefreshState
import com.luisvtp.portlight.ui.InstallDialogs
import com.luisvtp.portlight.ui.PortlightApp
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val refreshExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = CatalogRepository.fromResources(resources)
        // Caché privada: filesDir de la app (no visible para otras apps).
        val cacheStore = FileCatalogCacheStore(filesDir)
        val refresher = CatalogRefresher(HttpUrlConnectionGateway(), cacheStore)
        // Fase 2B: flujo de instalación compuesto en PortlightApplication.
        val app = application as PortlightApplication
        setContent {
            MaterialTheme {
                var catalog by remember { mutableStateOf(repository.currentCatalog(cacheStore)) }
                var refreshState by remember { mutableStateOf(CatalogRefreshState.IDLE) }
                var installBusy by remember { mutableStateOf(false) }
                var installTargetId by remember { mutableStateOf<String?>(null) }
                var installOutcome by remember { mutableStateOf<InstallOutcome?>(null) }

                fun requestRefresh() {
                    if (refreshState == CatalogRefreshState.UPDATING) return
                    refreshState = CatalogRefreshState.UPDATING
                    refreshExecutor.execute {
                        val result = refresher.refresh()
                        mainHandler.post {
                            if (result is CatalogRefreshResult.Updated) {
                                catalog = result.catalog
                            }
                            refreshState = when (result) {
                                is CatalogRefreshResult.Updated -> CatalogRefreshState.UPDATED
                                CatalogRefreshResult.NotModified ->
                                    CatalogRefreshState.NOT_MODIFIED
                                CatalogRefreshResult.Kept -> CatalogRefreshState.FAILED
                            }
                        }
                    }
                }

                // Descarga+instalación en el hilo único de la aplicación; el
                // resultado vuelve por mainHandler y se muestra en el detalle.
                fun requestInstall(port: Port) {
                    if (installBusy) return
                    installBusy = true
                    installTargetId = port.id
                    installOutcome = null
                    app.installExecutor.execute {
                        val result = app.installFlow.run(port)
                        mainHandler.post {
                            installOutcome = result
                            installBusy = false
                        }
                    }
                }

                // Refresco inicial fuera del hilo principal; el resultado vuelve
                // por mainHandler y actualiza el estado de Compose.
                LaunchedEffect(Unit) { requestRefresh() }
                PortlightApp(
                    catalog = catalog,
                    refreshState = refreshState,
                    onRefresh = { requestRefresh() },
                    installBusy = installBusy,
                    installTargetId = installTargetId,
                    installOutcome = installOutcome,
                    onDownloadClick = { port -> requestInstall(port) },
                )
                // Confirmaciones del flujo (bloquean en su hilo hasta la respuesta).
                InstallDialogs(app.installConfirmer)
            }
        }
    }
}
