package com.luisvtp.portlight.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class HardcodedComposableTextDetectorTest {

    @Test
    fun `un literal de texto en Text produce el error HardcodedComposableText`() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    import androidx.compose.material3.Text

                    @androidx.compose.runtime.Composable
                    fun Greeting() {
                        Text("Hola literal")
                    }
                    """.trimIndent(),
                ).indented(),
            )
            .issues(HardcodedComposableTextDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectContains("HardcodedComposableText")
    }

    @Test
    fun `un literal en contentDescription produce el error HardcodedComposableText`() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    import androidx.compose.material3.Icon

                    @androidx.compose.runtime.Composable
                    fun Greeting() {
                        Icon(Icons.Default.Info, contentDescription = "Info literal")
                    }
                    """.trimIndent(),
                ).indented(),
            )
            .issues(HardcodedComposableTextDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectContains("HardcodedComposableText")
    }

    @Test
    fun `stringResource no produce ningun aviso`() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    import androidx.compose.material3.Text
                    import androidx.compose.ui.res.stringResource

                    @androidx.compose.runtime.Composable
                    fun Greeting() {
                        Text(stringResource(R.string.greeting))
                    }
                    """.trimIndent(),
                ).indented(),
            )
            .issues(HardcodedComposableTextDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }
}
