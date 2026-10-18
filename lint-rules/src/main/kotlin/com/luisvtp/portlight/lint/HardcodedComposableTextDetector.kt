package com.luisvtp.portlight.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UNamedExpression
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.expressions.UInjectionHost

/**
 * Detector propio de Portlight: detecta texto literal de usuario en llamadas Compose.
 *
 * El aviso oficial `HardcodedText` de Android Lint solo cubre XML de recursos
 * («Affects: Resource files»), no Compose. Este detector cierra ese hueco con
 * severidad ERROR para que falle la verificación si aparece un literal en la UI.
 *
 * Política:
 *  - Marca: `Text("…")` (y `Text(text = "…")`) y los parámetros con nombre
 *    `text`, `title`, `label`, `placeholder` y `contentDescription`.
 *  - No marca: cadenas vacías, de solo espacios o de solo símbolos (sin ninguna
 *    letra ni dígito), interpolaciones totalmente dinámicas (p. ej. `"$nombre"`)
 *    y otros parámetros (p. ej. `testTag`).
 *
 * Detección estructural (sin mirar el texto fuente): en Kotlin un literal de cadena
 * no es un [ULiteralExpression] sino una plantilla
 * (`KotlinStringTemplateUPolyadicExpression`: [UPolyadicExpression] +
 * [UInjectionHost]); se inspeccionan los operandos de la plantilla (partes
 * literales frente a interpolaciones) y [UInjectionHost.evaluateToString].
 *
 * Escape hatch: anotar la línea con `@Suppress("HardcodedComposableText")`.
 */
class HardcodedComposableTextDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {

            override fun visitCallExpression(node: UCallExpression) {
                // En esta versión de UAST los argumentos con nombre NO llegan envueltos
                // en UNamedExpression: el nombre del parámetro se obtiene del método
                // resuelto mediante computeArgumentMapping (robusto ante reordenación).
                val method = node.resolve()
                val argumentMapping =
                    if (method != null) {
                        context.evaluator.computeArgumentMapping(node, method)
                    } else {
                        emptyMap()
                    }
                node.valueArguments.forEachIndexed { index, argument ->
                    val named = argument as? UNamedExpression
                    val expression = named?.expression ?: argument
                    val parameterName = named?.name ?: argumentMapping[expression]?.name
                    val isPositionalTextCall = index == 0 &&
                        (parameterName == null || parameterName == "text") &&
                        node.methodName in TEXT_COMPOSABLES
                    val isNamedTextArgument = parameterName != null &&
                        parameterName in NAMED_TEXT_ARGUMENTS
                    if (!isPositionalTextCall && !isNamedTextArgument) {
                        return@forEachIndexed
                    }
                    val text = stringContent(expression) ?: return@forEachIndexed
                    if (!text.any { it.isLetterOrDigit() }) {
                        // Vacío, solo espacios o solo símbolos: no es texto de usuario.
                        return@forEachIndexed
                    }
                    context.report(
                        ISSUE,
                        expression,
                        context.getLocation(expression),
                        "Texto hardcodeado en Compose: usa stringResource(R.string.…) " +
                            "(el texto vive en values/ y values-es/)",
                        null,
                    )
                }
            }
        }

    companion object {

        /** Composables cuyo primer parámetro posicional es texto visible. */
        private val TEXT_COMPOSABLES = setOf("Text")

        /** Parámetros con nombre que muestran texto visible en cualquier llamada. */
        private val NAMED_TEXT_ARGUMENTS =
            setOf("text", "title", "label", "placeholder", "contentDescription")

        /**
         * Texto que [expression] representa si es una cadena literal; null en caso
         * contrario. Cubre literales Java ([ULiteralExpression]) y plantillas de
         * cadena Kotlin ([UInjectionHost]), con o sin interpolación.
         */
        private fun stringContent(expression: UExpression): String? {
            if (expression is ULiteralExpression) {
                // Literal de cadena (p. ej. en Java).
                return expression.value as? String
            }
            if (expression is UInjectionHost) {
                // Plantilla de cadena de Kotlin: es un UPolyadicExpression cuyos
                // operandos son las partes literales (ULiteralExpression) y las
                // interpolaciones; solo las partes fijas aportan texto hardcodeado.
                val operands = (expression as? UPolyadicExpression)?.operands
                if (!operands.isNullOrEmpty()) {
                    val literals = operands
                        .filterIsInstance<ULiteralExpression>()
                        .mapNotNull { it.value as? String }
                    return literals
                        .takeIf { partes -> partes.any { it.isNotEmpty() } }
                        ?.joinToString("")
                }
                // Sin operandos: se evalúa la plantilla si es posible (texto plano).
                return expression.evaluateToString()?.takeIf { it.isNotEmpty() }
            }
            return null
        }

        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "HardcodedComposableText",
            briefDescription = "Hardcoded text in Compose",
            explanation =
                "Todo el texto visible debe provenir de recursos de cadenas (values/ y values-es/) " +
                    "mediante stringResource(). El aviso oficial HardcodedText de Lint solo cubre XML; " +
                    "este detector aplica la misma política a Jetpack Compose. Las descripciones del " +
                    "catálogo se resuelven con el helper localized() del propio catálogo. Las cadenas " +
                    "vacías, de solo espacios o de solo símbolos no se marcan.",
            category = Category.I18N,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(
                HardcodedComposableTextDetector::class.java,
                EnumSet.of(Scope.ALL_JAVA_FILES),
            ),
        )
    }
}
