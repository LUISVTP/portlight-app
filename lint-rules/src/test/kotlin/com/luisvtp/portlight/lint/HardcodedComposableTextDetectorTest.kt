package com.luisvtp.portlight.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestLintResult
import org.junit.Test

class HardcodedComposableTextDetectorTest {

    /**
     * Analiza un snippet Kotlin con el detector propio, resolviendo las importaciones
     * de Compose contra stubs mínimos (sin allowCompilationErrors: el análisis UAST
     * se ejecuta sobre fuentes correctamente resueltas).
     */
    private fun analiza(vararg lineas: String): TestLintResult {
        val source = (listOf("package test", "") + lineas).joinToString("\n")
        return lint()
            .files(
                kotlin(
                    """
                    package androidx.compose.runtime

                    @Target(AnnotationTarget.FUNCTION)
                    @Retention(AnnotationRetention.BINARY)
                    annotation class Composable
                    """.trimIndent(),
                ).indented(),
                kotlin(
                    """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable

                    @Composable
                    fun Text(text: String) {}

                    @Composable
                    fun Icon(icon: Any, contentDescription: String? = null) {}

                    object Icons {
                        object Default {
                            val Info: Any = Any()
                        }
                    }
                    """.trimIndent(),
                ).indented(),
                kotlin(
                    """
                    package androidx.compose.ui.res

                    import androidx.compose.runtime.Composable

                    @Composable
                    fun stringResource(id: Int): String = ""
                    """.trimIndent(),
                ).indented(),
                kotlin(source).indented(),
            )
            .issues(HardcodedComposableTextDetector.ISSUE)
            .allowMissingSdk()
            .run()
    }

    @Test
    fun `un literal de texto en Text produce el error HardcodedComposableText`() {
        analiza(
            """
            import androidx.compose.material3.Text

            @androidx.compose.runtime.Composable
            fun Greeting() {
                Text("Hola literal")
            }
            """.trimIndent(),
        ).expectContains("HardcodedComposableText")
    }

    @Test
    fun `un literal en contentDescription produce el error HardcodedComposableText`() {
        analiza(
            """
            import androidx.compose.material3.Icon

            @androidx.compose.runtime.Composable
            fun Greeting() {
                Icon(Icons.Default.Info, contentDescription = "Info literal")
            }
            """.trimIndent(),
        ).expectContains("HardcodedComposableText")
    }

    @Test
    fun `title label y placeholder con literal producen el error`() {
        analiza(
            """
            import androidx.compose.runtime.Composable

            @Composable
            fun MiComponente(title: String, label: String, placeholder: String) {}

            @Composable
            fun Greeting() {
                MiComponente(title = "Titulo", label = "Etiqueta", placeholder = "Buscar")
            }
            """.trimIndent(),
        ).expectContains("HardcodedComposableText")
    }

    @Test
    fun `un literal con signo dolar produce el error`() {
        analiza(
            """
            import androidx.compose.material3.Text

            @androidx.compose.runtime.Composable
            fun Greeting() {
                Text("Coste: 5${'$'}")
            }
            """.trimIndent(),
        ).expectContains("HardcodedComposableText")
    }

    @Test
    fun `una interpolacion con texto hardcodeado produce el error`() {
        analiza(
            """
            import androidx.compose.material3.Text

            val nombre = "Ana"

            @androidx.compose.runtime.Composable
            fun Greeting() {
                Text("Hola ${'$'}nombre")
            }
            """.trimIndent(),
        ).expectContains("HardcodedComposableText")
    }

    @Test
    fun `un literal con saltos de linea produce el error`() {
        analiza(
            """
            import androidx.compose.material3.Text

            @androidx.compose.runtime.Composable
            fun Greeting() {
                Text("Fila uno\nFila dos")
            }
            """.trimIndent(),
        ).expectContains("HardcodedComposableText")
    }

    @Test
    fun `stringResource no produce ningun aviso`() {
        analiza(
            """
            import androidx.compose.material3.Text
            import androidx.compose.ui.res.stringResource

            object R {
                object string {
                    const val greeting = 1
                }
            }

            @androidx.compose.runtime.Composable
            fun Greeting() {
                Text(stringResource(R.string.greeting))
            }
            """.trimIndent(),
        ).expectClean()
    }

    @Test
    fun `cadenas vacias de solo espacios y de solo simbolos no producen aviso`() {
        analiza(
            """
            import androidx.compose.material3.Text

            @androidx.compose.runtime.Composable
            fun Greeting() {
                Text("")
                Text("   ")
                Text("...")
                Text("•••")
            }
            """.trimIndent(),
        ).expectClean()
    }

    @Test
    fun `testTag y funciones ajenas al texto no producen aviso`() {
        analiza(
            """
            fun testTag(tag: String) {}

            @androidx.compose.runtime.Composable
            fun Greeting() {
                testTag("mi_etiqueta")
            }
            """.trimIndent(),
        ).expectClean()
    }

    @Test
    fun `una interpolacion totalmente dinamica no produce aviso`() {
        analiza(
            """
            import androidx.compose.material3.Text

            val nombre = "Ana"

            @androidx.compose.runtime.Composable
            fun Greeting() {
                Text("${'$'}nombre")
            }
            """.trimIndent(),
        ).expectClean()
    }
}
