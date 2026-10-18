package com.luisvtp.portlight.i18n

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Barrera 3 (interina): detecta texto literal de usuario en las fuentes Kotlin de UI
 * mientras no existe el detector Lint propio para Compose (previsto en la Fase 1, `lint-rules/`).
 * El aviso oficial `HardcodedText` de Lint solo cubre XML de recursos, no Compose.
 * Excepción puntual por línea: comentario `// i18n-exempt`.
 */
class HardcodedTextInSourcesTest {

    @Test
    fun `no hardcoded user-facing literals in kotlin sources`() {
        val violations = mutableListOf<String>()
        ModulePaths.sourceRoots.forEach { root ->
            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    file.readLines().forEachIndexed { index, line ->
                        if (line.contains("i18n-exempt")) return@forEachIndexed
                        if (PATTERNS.any { it.containsMatchIn(line) }) {
                            violations += "${file.relativeTo(ModulePaths.moduleDir)}:${index + 1}: ${line.trim()}"
                        }
                    }
                }
        }
        assertTrue(
            "Texto hardcodeado (usa recursos + stringResource):\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    companion object {
        private val PATTERNS = listOf(
            Regex("Text\\(\\s*\""),
            Regex("contentDescription\\s*=\\s*\""),
            Regex("(title|label|placeholder)\\s*=\\s*\""),
        )
    }
}
