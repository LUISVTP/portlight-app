package com.luisvtp.portlight.domain

import com.luisvtp.portlight.data.Port
import com.luisvtp.portlight.data.ReleaseAsset

/**
 * Selección del asset a descargar (PLAN §8):
 *  1. Filtrar `assets[]` con `asset_pattern` sobre el nombre (case-insensitive)
 *     y quedarse solo con los que terminan en `.apk`.
 *  2. Un candidato → se usa sin más.
 *  3. Varias ABIs → elegir por `supportedAbis` (orden de preferencia del
 *     dispositivo, p. ej. arm64-v8a → armeabi-v7a → x86_64/x86) y al final
 *     `universal`.
 *  4. Ambigüedad (mismo ABI repetido, ABI sin declarar o candidatos sin
 *     discernir) → NUNCA elegir «a ojo»: se devuelve [Selection.Ambiguous]
 *     para que la entrada se corrija ajustando `asset_pattern`.
 */
object AbiSelector {

    const val UNIVERSAL_ABI = "universal"

    sealed class Selection {
        data class Selected(val asset: ReleaseAsset) : Selection()
        object NoMatchingAsset : Selection()
        object Ambiguous : Selection()
        object UnsupportedAbi : Selection()
    }

    fun select(port: Port, supportedAbis: List<String>): Selection {
        val release = port.release ?: return Selection.NoMatchingAsset
        val pattern = try {
            Regex(port.assetPattern, RegexOption.IGNORE_CASE)
        } catch (e: Exception) {
            // asset_pattern inválido en el catálogo: no se elige nada.
            return Selection.NoMatchingAsset
        }
        val candidates = release.assets.filter { asset ->
            asset.name.endsWith(".apk", ignoreCase = true) &&
                pattern.containsMatchIn(asset.name)
        }
        if (candidates.isEmpty()) return Selection.NoMatchingAsset
        if (candidates.size == 1) return Selection.Selected(candidates.single())
        val priority = (supportedAbis + UNIVERSAL_ABI).distinct()
        for (abi in priority) {
            val group = candidates.filter { it.abi == abi }
            if (group.size == 1) return Selection.Selected(group.single())
            if (group.size > 1) return Selection.Ambiguous
        }
        // Sin coincidencia: si algún candidato no declara `abi`, es ambiguo;
        // si todos declaran ABIs que este dispositivo no soporta, no sirve.
        return if (candidates.any { it.abi == null }) {
            Selection.Ambiguous
        } else {
            Selection.UnsupportedAbi
        }
    }
}
