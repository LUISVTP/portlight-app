package com.luisvtp.portlight.domain

import com.luisvtp.portlight.data.Author
import com.luisvtp.portlight.data.LocalizedText
import com.luisvtp.portlight.data.Port
import com.luisvtp.portlight.data.Release
import com.luisvtp.portlight.data.ReleaseAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Selección de asset/ABI del PLAN §8 contra datos ficticios. */
class AbiSelectorTest {

    @Test
    fun `un unico candidato se usa sin mirar la abi`() {
        val port = port(assetPattern = "(?i)^.*\\.apk$", assets = listOf(apk("solo.apk")))
        val selection = AbiSelector.select(port, listOf("armeabi-v7a"))
        assertEquals(AbiSelector.Selection.Selected(apk("solo.apk")), selection)
    }

    @Test
    fun `ignora los archivos que no son apk`() {
        val port = port(
            assetPattern = "(?i)^.*-universal\\.(apk|zip)$",
            assets = listOf(apk("port-universal.zip"), apk("port-universal.apk")),
        )
        val selection = AbiSelector.select(port, listOf("arm64-v8a"))
        assertEquals(AbiSelector.Selection.Selected(apk("port-universal.apk")), selection)
    }

    @Test
    fun `elige la abi preferida del dispositivo sobre universal`() {
        val port = port(
            assetPattern = "(?i)^.*\\.(apk)$",
            assets = listOf(apk("port-arm64-v8a.apk", abi = "arm64-v8a"), apk("port-universal.apk", abi = "universal")),
        )
        val selection = AbiSelector.select(port, listOf("arm64-v8a", "armeabi-v7a"))
        assertEquals(AbiSelector.Selection.Selected(apk("port-arm64-v8a.apk", abi = "arm64-v8a")), selection)
    }

    @Test
    fun `cae a universal cuando el dispositivo no tiene coincidencia directa`() {
        val port = port(
            assetPattern = "(?i)^.*\\.(apk)$",
            assets = listOf(apk("port-arm64-v8a.apk", abi = "arm64-v8a"), apk("port-universal.apk", abi = "universal")),
        )
        val selection = AbiSelector.select(port, listOf("x86", "armeabi"))
        assertEquals(AbiSelector.Selection.Selected(apk("port-universal.apk", abi = "universal")), selection)
    }

    @Test
    fun `un unico candidato se usa aunque su abi no coincida con el dispositivo`() {
        // PLAN §8.2: con un solo candidato no hay nada que elegir.
        val port = port(
            assetPattern = "(?i)^.*\\.apk$",
            assets = listOf(apk("port-arm64-v8a.apk", abi = "arm64-v8a")),
        )
        val selection = AbiSelector.select(port, listOf("x86_64"))
        assertEquals(AbiSelector.Selection.Selected(apk("port-arm64-v8a.apk", abi = "arm64-v8a")), selection)
    }

    @Test
    fun `sin universal ni coincidencia devuelve abi no soportada`() {
        val port = port(
            assetPattern = "(?i)^.*\\.(apk)$",
            assets = listOf(
                apk("port-arm64-v8a.apk", abi = "arm64-v8a"),
                apk("port-x86_64.apk", abi = "x86_64"),
            ),
        )
        assertEquals(
            AbiSelector.Selection.UnsupportedAbi,
            AbiSelector.select(port, listOf("armeabi-v7a")),
        )
    }

    @Test
    fun `dos candidatos de la misma abi son ambiguos`() {
        val port = port(
            assetPattern = "(?i)^.*\\.(apk)$",
            assets = listOf(apk("a-arm64-v8a.apk", abi = "arm64-v8a"), apk("b-arm64-v8a.apk", abi = "arm64-v8a")),
        )
        assertEquals(
            AbiSelector.Selection.Ambiguous,
            AbiSelector.select(port, listOf("arm64-v8a")),
        )
    }

    @Test
    fun `varios candidatos sin abi declarada son ambiguos`() {
        val port = port(
            assetPattern = "(?i)^.*\\.(apk)$",
            assets = listOf(apk("uno.apk"), apk("dos.apk")),
        )
        assertEquals(
            AbiSelector.Selection.Ambiguous,
            AbiSelector.select(port, listOf("arm64-v8a")),
        )
    }

    @Test
    fun `sin release o sin candidatos no hay asset`() {
        val sinRelease = port(assetPattern = "(?i)^.*\\.apk$").copy(release = null)
        assertEquals(AbiSelector.Selection.NoMatchingAsset, AbiSelector.select(sinRelease, listOf("arm64-v8a")))

        val sinCandidatos = port(
            assetPattern = "(?i)^.*-arm64-v8a\\.apk$",
            assets = listOf(apk("port-universal.apk", abi = "universal")),
        )
        assertEquals(AbiSelector.Selection.NoMatchingAsset, AbiSelector.select(sinCandidatos, listOf("x86")))
    }

    @Test
    fun `un asset_pattern invalido no selecciona nada`() {
        val port = port(assetPattern = "([", assets = listOf(apk("port.apk")))
        assertEquals(AbiSelector.Selection.NoMatchingAsset, AbiSelector.select(port, listOf("arm64-v8a")))
    }

    @Test
    fun `un candidato no apk nunca se selecciona`() {
        val port = port(assetPattern = "(?i)^.*", assets = listOf(apk("fuentes.zip")))
        assertEquals(AbiSelector.Selection.NoMatchingAsset, AbiSelector.select(port, listOf("arm64-v8a")))
        assertTrue(AbiSelector.select(port, listOf("arm64-v8a")) is AbiSelector.Selection.NoMatchingAsset)
    }

    private fun apk(name: String, abi: String? = null) = ReleaseAsset(
        name = name,
        size = 1_000,
        url = "https://github.com/example/demo/releases/download/v1.0/$name",
        digest = null,
        abi = abi,
    )

    private fun port(
        assetPattern: String,
        assets: List<ReleaseAsset> = emptyList(),
    ) = Port(
        id = "demo-port",
        name = "Demo",
        description = LocalizedText(en = "Demo"),
        author = Author(name = "Autor"),
        license = "GPL-3.0-only",
        repo = "example/demo",
        assetPattern = assetPattern,
        sourceUrl = "https://github.com/example/demo",
        release = Release(tag = "v1.0", assets = assets),
    )
}
