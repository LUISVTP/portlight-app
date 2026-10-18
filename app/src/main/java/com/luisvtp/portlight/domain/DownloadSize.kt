package com.luisvtp.portlight.domain

import com.luisvtp.portlight.data.Port

/**
 * Tamaño del APK que SE va a descargar (Fase 4b): la misma selección
 * determinista de ABI que usan el diálogo de instalación y el flujo real
 * ([AbiSelector]); null si no hay asset descargable para este dispositivo.
 * El resultado se muestra siempre con [formatBytes].
 */
fun downloadSizeBytes(port: Port, supportedAbis: List<String>): Long? =
    (AbiSelector.select(port, supportedAbis) as? AbiSelector.Selection.Selected)?.asset?.size
