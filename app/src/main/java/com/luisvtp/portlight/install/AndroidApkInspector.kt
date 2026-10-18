package com.luisvtp.portlight.install

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.luisvtp.portlight.data.Hashes
import java.io.File

/**
 * Lectura real del APK con `PackageManager.getPackageArchiveInfo`: package del
 * manifest y huellas SHA-256 de los certificados de firma. Las firmas se leen
 * con `GET_SIGNING_CERTIFICATES` en API ≥ 28 y con el `GET_SIGNATURES`
 * clásico por debajo (ambas claves cuando hay que comparar versiones).
 */
class AndroidApkInspector(private val context: Context) : ApkInspector {

    override fun inspect(apkFile: File): ApkArchive? {
        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_SIGNATURES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        @Suppress("DEPRECATION")
        val info = packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return null
        val packageName = info.packageName ?: return null

        @Suppress("DEPRECATION")
        val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            (info.signingInfo?.apkContentsSigners?.takeIf { it.isNotEmpty() } ?: info.signatures)
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }

        val fingerprints = signatures
            ?.map { Hashes.sha256Hex(it.toByteArray()) }
            ?.distinct()
            .orEmpty()
        if (fingerprints.isEmpty()) return null
        return ApkArchive(packageName = packageName, sha256Fingerprints = fingerprints)
    }
}
