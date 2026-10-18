package com.luisvtp.portlight.install

import com.luisvtp.portlight.data.Author
import com.luisvtp.portlight.data.Hashes
import com.luisvtp.portlight.data.HttpUrlConnectionReleaseClient
import com.luisvtp.portlight.data.LocalHttpServer
import com.luisvtp.portlight.data.LocalizedText
import com.luisvtp.portlight.data.Port
import com.luisvtp.portlight.data.RedirectPolicy
import com.luisvtp.portlight.data.Release
import com.luisvtp.portlight.data.ReleaseAsset
import com.luisvtp.portlight.data.ReleaseDownloader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Flujo completo de descarga+instalación contra el servidor local simulado
 * (Fase 2B). Falsos inspector/almacén/instalador; el downloader es el real
 * apuntando a 127.0.0.1. Los tests jamás contactan repositorios reales.
 */
class InstallFlowTest {

    private lateinit var server: LocalHttpServer
    private lateinit var downloadsDir: File

    private val apkBytes = "apk-ficticio-de-portlight".toByteArray()
    private val goodDigest = "sha256:" + Hashes.sha256Hex(apkBytes)

    @Before
    fun setUp() {
        server = LocalHttpServer()
        server.start()
        downloadsDir = Files.createTempDirectory("portlight-flow").toFile()
    }

    @After
    fun tearDown() {
        server.stop()
        downloadsDir.deleteRecursively()
    }

    // --- Fakes ---------------------------------------------------------------

    private class FakeConfirmer(
        var ownCopy: Boolean = true,
        var missingDigest: Boolean = true,
        var signatureChanged: Boolean = true,
        var permission: Boolean = true,
    ) : InstallConfirmer {
        val calls = mutableListOf<String>()
        override fun confirmOwnCopy(port: Port): Boolean { calls += "own-copy"; return ownCopy }
        override fun confirmMissingDigest(port: Port): Boolean { calls += "missing-digest"; return missingDigest }
        override fun confirmSignatureChanged(port: Port, newFingerprint: String): Boolean {
            calls += "signature-changed"
            return signatureChanged
        }
        override fun requestInstallPermission(port: Port): Boolean { calls += "permission"; return permission }
    }

    private class FakeInspector(var archive: ApkArchive?) : ApkInspector {
        var inspections = 0
        override fun inspect(apkFile: File): ApkArchive? {
            inspections++
            return archive
        }
    }

    private class FakeStore(initial: Map<String, String> = emptyMap()) : FingerprintStore {
        val map = initial.toMutableMap()
        override fun fingerprintFor(portId: String): String? = map[portId]
        override fun recordFingerprint(portId: String, fingerprint: String) { map[portId] = fingerprint }
    }

    private class FakeInstaller(var attempt: InstallAttempt) : ApkInstaller {
        var installs = 0
        var lastExpectedPackage: String? = null
        override fun install(apkFile: File, expectedPackageName: String?): InstallAttempt {
            installs++
            lastExpectedPackage = expectedPackageName
            return attempt
        }
    }

    // --- Constructores -------------------------------------------------------

    private fun asset(digest: String?, size: Long = apkBytes.size.toLong(), name: String = "port.apk") =
        ReleaseAsset(
            name = name,
            size = size,
            url = server.url("/example/demo/releases/download/v1.0/$name"),
            digest = digest,
            abi = null,
        )

    private fun port(
        digest: String? = goodDigest,
        size: Long = apkBytes.size.toLong(),
        assets: List<ReleaseAsset>? = null,
        packageName: String? = "com.example.demo",
    ) = Port(
        id = "demo-port",
        name = "Demo Port",
        description = LocalizedText(en = "Demo", es = "Demo"),
        author = Author(name = "Autor"),
        license = "GPL-3.0-only",
        repo = "example/demo",
        assetPattern = "(?i)^.*\\.apk$",
        sourceUrl = "https://github.com/example/demo",
        packageName = packageName,
        release = Release(tag = "v1.0", assets = assets ?: listOf(asset(digest, size))),
    )

    private fun flow(
        confirmer: InstallConfirmer = FakeConfirmer(),
        inspector: ApkInspector = FakeInspector(ApkArchive("com.example.demo", listOf("fp-actual"))),
        store: FingerprintStore = FakeStore(),
        installer: ApkInstaller = FakeInstaller(InstallAttempt.Success),
        abis: List<String> = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"),
        margin: Long = ReleaseDownloader.DEFAULT_SIZE_MARGIN_BYTES,
    ) = InstallFlow(
        confirmer = confirmer,
        downloader = ReleaseDownloader(
            client = HttpUrlConnectionReleaseClient(connectTimeoutMs = 5_000, readTimeoutMs = 5_000),
            downloadsDir = downloadsDir,
            redirectPolicy = RedirectPolicy(setOf("127.0.0.1"), allowedSchemes = setOf("http")),
            isStartUrlAllowed = { a, _, _ -> a.url.startsWith(server.url("/")) },
            sizeMarginBytes = margin,
        ),
        inspector = inspector,
        fingerprints = store,
        installer = installer,
        supportedAbis = abis,
        isAssetUrlAllowed = { _, a, _ -> a.url.startsWith(server.url("/")) },
    )

    private fun serveApk() {
        server.handle("/example/demo/releases/download/v1.0/port.apk") {
            LocalHttpServer.Response(200, body = apkBytes)
        }
    }

    private fun temporal(): File = File(downloadsDir, "demo-port.apk")

    // --- Escenarios ----------------------------------------------------------

    @Test
    fun `hash correcto instala registra la firma tofu y borra el temporal`() {
        serveApk()
        val confirmer = FakeConfirmer()
        val store = FakeStore()
        val installer = FakeInstaller(InstallAttempt.Success)

        val outcome = flow(confirmer, store = store, installer = installer).run(port())

        assertEquals(InstallOutcome.Installed, outcome)
        assertEquals("fp-actual", store.fingerprintFor("demo-port"))
        assertEquals(1, installer.installs)
        assertEquals("com.example.demo", installer.lastExpectedPackage)
        // El aviso legal de copia propia es lo único previo a tocar la red.
        assertEquals(listOf("own-copy", "permission"), confirmer.calls)
        assertEquals(1, server.requestCount())
        assertFalse(temporal().exists())
    }

    @Test
    fun `hash incorrecto devuelve error_digest_mismatch y borra el archivo`() {
        serveApk()
        val installer = FakeInstaller(InstallAttempt.Success)

        val outcome = flow(installer = installer).run(
            port(digest = "sha256:" + Hashes.sha256Hex("otra-cosa".toByteArray())),
        )

        assertTrue(outcome is InstallOutcome.Failed)
        assertEquals(InstallFailure.DigestMismatch, (outcome as InstallOutcome.Failed).failure)
        assertEquals(0, installer.installs)
        assertFalse(temporal().exists())
    }

    @Test
    fun `sin digest y confirmado el aviso extra se instala`() {
        serveApk()
        val confirmer = FakeConfirmer()

        val outcome = flow(confirmer).run(port(digest = null))

        assertEquals(InstallOutcome.Installed, outcome)
        assertEquals(listOf("own-copy", "missing-digest", "permission"), confirmer.calls)
        assertFalse(temporal().exists())
    }

    @Test
    fun `sin digest y rechazado el aviso extra se cancela y borra`() {
        serveApk()
        val confirmer = FakeConfirmer(missingDigest = false)
        val installer = FakeInstaller(InstallAttempt.Success)

        val outcome = flow(confirmer, installer = installer).run(port(digest = null))

        assertEquals(InstallOutcome.Cancelled, outcome)
        assertEquals(1, server.requestCount())
        assertEquals(0, installer.installs)
        assertFalse(temporal().exists())
    }

    @Test
    fun `redireccion a host no permitido aborta la descarga`() {
        server.handle("/example/demo/releases/download/v1.0/port.apk") {
            LocalHttpServer.Response(302, mapOf("Location" to "https://evil.example.invalid/payload.apk"))
        }

        val outcome = flow().run(port())

        assertTrue(outcome is InstallOutcome.Failed)
        assertEquals(InstallFailure.RedirectDenied, (outcome as InstallOutcome.Failed).failure)
        assertEquals(1, server.requestCount())
        assertFalse(temporal().exists())
    }

    @Test
    fun `tamano excedido devuelve error_size_exceeded y borra`() {
        serveApk()
        // size=1 con margen 0: cualquier byte de más corta la descarga.
        val outcome = flow(margin = 0L).run(port(size = 1L))

        assertTrue(outcome is InstallOutcome.Failed)
        assertEquals(InstallFailure.TooLarge, (outcome as InstallOutcome.Failed).failure)
        assertFalse(temporal().exists())
    }

    @Test
    fun `paquete distinto al declarado devuelve error_package_mismatch`() {
        serveApk()
        val inspector = FakeInspector(ApkArchive("com.otro.paquete", listOf("fp-actual")))
        val store = FakeStore()
        val installer = FakeInstaller(InstallAttempt.Success)

        val outcome = flow(inspector = inspector, store = store, installer = installer).run(port())

        assertEquals(InstallOutcome.Failed(InstallFailure.PackageMismatch), outcome)
        assertEquals(0, installer.installs)
        assertNull(store.fingerprintFor("demo-port"))
        assertFalse(temporal().exists())
    }

    @Test
    fun `apk ilegible devuelve error_apk_invalido`() {
        serveApk()
        val outcome = flow(inspector = FakeInspector(null)).run(port())

        assertEquals(InstallOutcome.Failed(InstallFailure.ApkInvalid), outcome)
        assertFalse(temporal().exists())
    }

    @Test
    fun `firma cambiada rechazada por el usuario se cancela y borra`() {
        serveApk()
        val confirmer = FakeConfirmer(signatureChanged = false)
        val store = FakeStore(mapOf("demo-port" to "fp-vieja"))
        val installer = FakeInstaller(InstallAttempt.Success)

        val outcome = flow(confirmer, store = store, installer = installer).run(port())

        assertEquals(InstallOutcome.Cancelled, outcome)
        assertTrue(confirmer.calls.contains("signature-changed"))
        assertEquals(0, installer.installs)
        assertFalse(temporal().exists())
    }

    @Test
    fun `firma cambiada aceptada se instala pero conserva la huella original`() {
        serveApk()
        val confirmer = FakeConfirmer(signatureChanged = true)
        val store = FakeStore(mapOf("demo-port" to "fp-vieja"))

        val outcome = flow(confirmer, store = store).run(port())

        assertEquals(InstallOutcome.Installed, outcome)
        // La TOFU mantiene la huella del primer APK instalado, no la nueva.
        assertEquals("fp-vieja", store.fingerprintFor("demo-port"))
    }

    @Test
    fun `cancelar el aviso legal no contacta el servidor`() {
        val confirmer = FakeConfirmer(ownCopy = false)
        val installer = FakeInstaller(InstallAttempt.Success)

        val outcome = flow(confirmer, installer = installer).run(port())

        assertEquals(InstallOutcome.Cancelled, outcome)
        assertEquals(0, server.requestCount())
        assertEquals(listOf("own-copy"), confirmer.calls)
        assertEquals(0, installer.installs)
        assertFalse(temporal().exists())
    }

    @Test
    fun `permiso de instalacion denegado se cancela y borra`() {
        serveApk()
        val confirmer = FakeConfirmer(permission = false)
        val installer = FakeInstaller(InstallAttempt.Success)

        val outcome = flow(confirmer, installer = installer).run(port())

        assertEquals(InstallOutcome.Cancelled, outcome)
        assertEquals(0, installer.installs)
        assertFalse(temporal().exists())
    }

    @Test
    fun `instalador bloqueado devuelve error_install_blocked y borra`() {
        serveApk()
        val store = FakeStore()

        val outcome = flow(store = store, installer = FakeInstaller(InstallAttempt.Blocked)).run(port())

        assertEquals(InstallOutcome.Failed(InstallFailure.InstallBlocked), outcome)
        assertNull(store.fingerprintFor("demo-port"))
        assertFalse(temporal().exists())
    }

    @Test
    fun `usuario que cancela en el instalador del sistema se cancela y borra`() {
        serveApk()

        val outcome = flow(installer = FakeInstaller(InstallAttempt.Aborted)).run(port())

        assertEquals(InstallOutcome.Cancelled, outcome)
        assertFalse(temporal().exists())
    }

    @Test
    fun `error del instalador del sistema se informa y borra`() {
        serveApk()

        val outcome = flow(
            installer = FakeInstaller(InstallAttempt.Failure("detalle del sistema")),
        ).run(port())

        assertEquals(
            InstallOutcome.Failed(InstallFailure.InstallFailed("detalle del sistema")),
            outcome,
        )
        assertFalse(temporal().exists())
    }

    @Test
    fun `abi no soportada falla sin contactar el servidor`() {
        val arm64 = asset(goodDigest, name = "port-arm64-v8a.apk").copy(abi = "arm64-v8a")
        val x86 = asset(goodDigest, name = "port-x86_64.apk").copy(abi = "x86_64")
        val installer = FakeInstaller(InstallAttempt.Success)

        val outcome = flow(installer = installer, abis = listOf("armeabi-v7a"))
            .run(port(assets = listOf(arm64, x86)))

        assertEquals(InstallOutcome.Failed(InstallFailure.AbiUnsupported), outcome)
        assertEquals(0, server.requestCount())
        assertEquals(0, installer.installs)
    }

    @Test
    fun `sin release no hay nada que descargar`() {
        val outcome = flow().run(port().copy(release = null))

        assertEquals(InstallOutcome.Failed(InstallFailure.NoApkAsset), outcome)
        assertEquals(0, server.requestCount())
    }
}
