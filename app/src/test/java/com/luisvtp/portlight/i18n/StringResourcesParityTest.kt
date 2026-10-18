package com.luisvtp.portlight.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Gate de i18n (Fase 0): paridad entre `values/` (inglés, idioma por defecto del sistema
 * de recursos de Android) y `values-es/` (español). Falla la compilación del módulo
 * si una cadena queda sin traducir o si los marcadores de formato no coinciden.
 */
class StringResourcesParityTest {

    @Test
    fun `english and spanish declare the same translatable keys`() {
        assertEquals(
            "Claves distintas entre values/ (en) y values-es/ (es). " +
                "Faltantes en es: ${base.keys - spanish.keys}; sobrantes en es: ${spanish.keys - base.keys}",
            base.keys,
            spanish.keys,
        )
    }

    @Test
    fun `every key uses the same placeholders in both languages`() {
        val mismatches = (base.keys intersect spanish.keys).mapNotNull { key ->
            val en = placeholders(base.getValue(key))
            val es = placeholders(spanish.getValue(key))
            if (en != es) "$key -> en=$en es=$es" else null
        }
        assertTrue("Placeholders inconsistentes:\n${mismatches.joinToString("\n")}", mismatches.isEmpty())
    }

    @Test
    fun `no language declares blank values`() {
        val blanks = base.filterValues { it.isBlank() }.keys.map { "en:$it" } +
            spanish.filterValues { it.isBlank() }.keys.map { "es:$it" }
        assertTrue("Valores en blanco: $blanks", blanks.isEmpty())
    }

    @Test
    fun `app name is Portlight and identical in both languages`() {
        assertEquals("Portlight", base["app_name"])
        assertEquals("Portlight", spanish["app_name"])
    }

    companion object {

        private val base: Map<String, String> =
            parse(File(ModulePaths.moduleDir, "src/main/res/values/strings.xml"))

        private val spanish: Map<String, String> =
            parse(File(ModulePaths.moduleDir, "src/main/res/values-es/strings.xml"))

        private fun parse(file: File): Map<String, String> {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val result = LinkedHashMap<String, String>()
            val nodes = doc.getElementsByTagName("string")
            for (i in 0 until nodes.length) {
                val element = nodes.item(i) as Element
                if (element.getAttribute("translatable") == "false") continue
                result[element.getAttribute("name")] = element.textContent.trim()
            }
            return result
        }

        private val PLACEHOLDER = Regex("%(?:(\\d+)\\$)?[A-Za-z]+")

        private fun placeholders(value: String): List<String> =
            PLACEHOLDER.findAll(value.replace("%%", "")).map { it.value }.toList().sorted()
    }
}
