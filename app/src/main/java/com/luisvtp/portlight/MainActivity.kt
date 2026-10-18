package com.luisvtp.portlight

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/**
 * Actividad mínima de la Fase 0 (andamiaje). Las pantallas reales en Compose llegan en la Fase 1.
 * Todo el texto visible proviene de recursos (values/ y values-es/).
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this)
        title.text = getString(R.string.catalog_title)
        setContentView(title)
    }
}
