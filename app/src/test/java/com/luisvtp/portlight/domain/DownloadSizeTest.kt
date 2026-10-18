package com.luisvtp.portlight.domain

import com.luisvtp.portlight.data.Author
import com.luisvtp.portlight.data.LocalizedText
import com.luisvtp.portlight.data.Port
import com.luisvtp.portlight.data.Release
import com.luisvtp.portlight.data.ReleaseAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tamaño del APK descargable: la misma selección de ABI que el flujo real (Fase 4b). */
class DownloadSizeTest {

    @Test
    fun `elige el asset de la abi preferida del dispositivo`() {
        val size = downloadSizeBytes(portWithTwoAssets, listOf("arm64-v8a", "armeabi-v7a"))

        assertEquals(132_558_444L, size)
    }

    @Test
    fun `ca al universal cuando el dispositivo no es arm64`() {
        val size = downloadSizeBytes(portWithTwoAssets, listOf("x86"))

        assertEquals(20_971_520L, size)
    }

    @Test
    fun `port sin release no tiene tamano`() {
        assertNull(downloadSizeBytes(portWithoutRelease, listOf("arm64-v8a")))
    }

    private val portWithTwoAssets = Port(
        id = "tamano-demo",
        name = "Port de tamaño",
        description = LocalizedText(en = "Size demo"),
        author = Author(name = "Autor"),
        license = "GPL-3.0-only",
        repo = "example/tamano-demo",
        assetPattern = "^.*\\.apk$",
        sourceUrl = "https://github.com/example/tamano-demo",
        release = Release(
            tag = "v1.0.0",
            assets = listOf(
                ReleaseAsset(
                    name = "port-arm64-v8a.apk",
                    size = 132_558_444,
                    url = "https://github.com/example/tamano-demo/releases/download/v1.0.0/port-arm64-v8a.apk",
                    abi = "arm64-v8a",
                ),
                ReleaseAsset(
                    name = "port-universal.apk",
                    size = 20_971_520,
                    url = "https://github.com/example/tamano-demo/releases/download/v1.0.0/port-universal.apk",
                    abi = "universal",
                ),
            ),
        ),
    )

    private val portWithoutRelease = Port(
        id = "sin-release",
        name = "Port sin release",
        description = LocalizedText(en = "No release"),
        author = Author(name = "Autor"),
        license = "MIT",
        repo = "example/sin-release",
        assetPattern = "^.*\\.apk$",
        sourceUrl = "https://github.com/example/sin-release",
    )
}
