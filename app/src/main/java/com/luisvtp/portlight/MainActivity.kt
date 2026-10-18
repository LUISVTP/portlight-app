package com.luisvtp.portlight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import com.luisvtp.portlight.data.CatalogRepository
import com.luisvtp.portlight.ui.PortlightApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val catalog = remember { CatalogRepository.fromResources(resources).loadCatalog() }
                PortlightApp(catalog = catalog)
            }
        }
    }
}
