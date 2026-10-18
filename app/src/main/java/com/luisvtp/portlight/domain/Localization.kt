package com.luisvtp.portlight.domain

import androidx.annotation.StringRes
import com.luisvtp.portlight.R
import com.luisvtp.portlight.data.LocalizedText
import java.util.Locale

/**
 * Resuelve un texto bilingüe del catálogo: usa el idioma del sistema y, si esa
 * versión no existe, cae a la otra (regla aprobada en PLAN.md §4.3).
 */
fun LocalizedText.localized(locale: Locale = Locale.getDefault()): String? = when {
    locale.language.equals("es", ignoreCase = true) -> es ?: en
    else -> en ?: es
}

/** Categoría canónica del catálogo (token en inglés) → recurso localizado. */
@StringRes
fun categoryStringRes(category: String?): Int = when (category) {
    "action" -> R.string.category_action
    "racing" -> R.string.category_racing
    "rpg" -> R.string.category_rpg
    "strategy" -> R.string.category_strategy
    "puzzle" -> R.string.category_puzzle
    "adventure" -> R.string.category_adventure
    "simulation" -> R.string.category_simulation
    else -> R.string.category_other
}
