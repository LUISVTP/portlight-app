package com.luisvtp.portlight.i18n

import java.io.File

/**
 * Localiza el módulo :app a partir del directorio de trabajo de los tests JVM,
 * para que los tests puedan leer `src/main/res` sin importar cómo se lance Gradle.
 */
internal object ModulePaths {

    val moduleDir: File = findModuleDir()

    val sourceRoots: List<File> = listOf("src/main/java", "src/main/kotlin")
        .map { File(moduleDir, it) }
        .filter { it.isDirectory }

    private fun findModuleDir(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "src/main/res/values/strings.xml").isFile) {
                return dir
            }
            dir = dir.parentFile
        }
        error(
            "No se encontró el módulo con src/main/res/values/strings.xml desde ${System.getProperty("user.dir")}"
        )
    }
}
