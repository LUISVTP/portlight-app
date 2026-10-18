plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // lint-api se provee en tiempo de análisis por AGP; solo se compila contra ella
    compileOnly(libs.lint.api)

    testImplementation(libs.lint.api)
    testImplementation(libs.lint.tests)
    testImplementation(libs.junit)
}
