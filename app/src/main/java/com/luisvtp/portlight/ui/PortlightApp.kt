package com.luisvtp.portlight.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.luisvtp.portlight.data.Catalog

private object Routes {
    const val CATALOG = "catalog"
    const val LEGAL = "legal"
    const val PORT_DETAIL = "port/{portId}"

    fun portDetail(portId: String) = "port/$portId"
}

/** Raíz de la app: navegación entre catálogo, detalle y aviso legal. */
@Composable
fun PortlightApp(catalog: Catalog) {
    MaterialTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = Routes.CATALOG) {
            composable(Routes.CATALOG) {
                CatalogListScreen(
                    ports = catalog.ports,
                    onPortClick = { portId -> navController.navigate(Routes.portDetail(portId)) },
                    onLegalClick = { navController.navigate(Routes.LEGAL) },
                )
            }
            composable(
                route = Routes.PORT_DETAIL,
                arguments = listOf(navArgument("portId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val portId = backStackEntry.arguments?.getString("portId")
                val port = catalog.ports.firstOrNull { it.id == portId }
                if (port != null) {
                    PortDetailScreen(port = port, onBack = { navController.popBackStack() })
                } else {
                    LaunchedEffect(portId) { navController.popBackStack() }
                }
            }
            composable(Routes.LEGAL) {
                LegalScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
