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
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UNamedExpression

/**
 * Detector propio de Portlight: detecta texto literal de usuario en llamadas Compose.
 *
 * El aviso oficial `HardcodedText` de Android Lint solo cubre XML de recursos
 * («Affects: Resource files»), no Compose. Este detector cierra ese hueco con
 * severidad ERROR para que falle la compilación si aparece un literal en la UI.
 *
 * Escape hatch: anotar la línea con `@Suppress("HardcodedComposableText")`.
 */
class HardcodedComposableTextDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {

            override fun visitCallExpression(node: UCallExpression) {
                node.valueArguments.forEachIndexed { index, argument ->
                    val named = argument as? UNamedExpression
                    val argumentName = named?.name
                    val expression = named?.expression ?: argument
                    if (expression !is ULiteralExpression || expression.value !is String) {
                        return@forEachIndexed
                    }
                    val isPositionalTextCall = index == 0 &&
                        (argumentName == null || argumentName == "text") &&
                        node.methodName in TEXT_COMPOSABLES
                    val isNamedTextArgument = argumentName != null &&
                        argumentName in NAMED_TEXT_ARGUMENTS
                    if (isPositionalTextCall || isNamedTextArgument) {
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
        }

    companion object {

        /** Composables cuyo primer parámetro posicional es texto visible. */
        private val TEXT_COMPOSABLES = setOf("Text")

        /** Parámetros con nombre que muestran texto visible en cualquier llamada. */
        private val NAMED_TEXT_ARGUMENTS =
            setOf("text", "title", "label", "placeholder", "contentDescription")

        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "HardcodedComposableText",
            briefDescription = "Hardcoded text in Compose",
            explanation =
                "Todo el texto visible debe provenir de recursos de cadenas (values/ y values-es/) " +
                    "mediante stringResource(). El aviso oficial HardcodedText de Lint solo cubre XML; " +
                    "este detector aplica la misma política a Jetpack Compose. Las descripciones del " +
                    "catálogo se resuelven con el helper localized() del propio catálogo.",
            category = Category.I18N,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(
                HardcodedComposableTextDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE),
            ),
        )
    }
}
