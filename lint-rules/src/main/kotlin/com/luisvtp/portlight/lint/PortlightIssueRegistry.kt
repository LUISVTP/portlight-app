package com.luisvtp.portlight.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API

/** Registro de los checks Lint propios de Portlight (instalado vía lintPublish). */
class PortlightIssueRegistry : IssueRegistry() {

    override val issues = listOf(HardcodedComposableTextDetector.ISSUE)

    override val api: Int = CURRENT_API
}
