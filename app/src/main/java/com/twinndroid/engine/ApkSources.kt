package com.twinndroid.engine

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.content.pm.PackageInfoCompat
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import java.io.File
import java.util.zip.ZipFile

/** A resolved set of APKs (base first, then splits) plus metadata about the app. */
class SourceApks(
    val files: List<File>,
    val packageName: String,
    val label: String,
    val versionCode: Long,
    val versionName: String?,
    val icon: Drawable?,
    val requestedPermissions: List<String>,
    val minSdk: Int = 0,
    val targetSdk: Int = 0,
) {
    /** Anti-tamper / shielding detected in the APK set (computed lazily; see [TamperScan]). */
    val tamper: TamperReport by lazy { TamperScan.scan(files) }
}

object ApkSources {

    fun sourceDir(context: Context, cloneId: String) = File(context.filesDir, "sources/$cloneId")

    /** An installed app: base.apk + all split APKs are world-readable, no root needed. */
    fun installed(context: Context, packageName: String): SourceApks {
        val pm = context.packageManager
        val info = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        val ai = info.applicationInfo ?: error("$packageName has no application info")
        val files = buildList {
            add(File(ai.sourceDir))
            ai.splitSourceDirs?.forEach { add(File(it)) }
        }
        return SourceApks(
            files = files,
            packageName = packageName,
            label = ai.loadLabel(pm).toString(),
            versionCode = PackageInfoCompat.getLongVersionCode(info),
            versionName = info.versionName,
            icon = ai.loadIcon(pm),
            requestedPermissions = info.requestedPermissions?.toList().orEmpty(),
            minSdk = ai.minSdkVersion,
            targetSdk = ai.targetSdkVersion,
        )
    }

    /**
     * Copies an .apk, or extracts every APK inside an .apks/.xapk/.apkm bundle, into [dest].
     * Returns the stored APK files.
     */
    fun import(context: Context, uri: Uri, dest: File): List<File> {
        dest.deleteRecursively()
        dest.mkdirs()
        val raw = File(dest, "import.tmp")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open the selected file" }
            raw.outputStream().use { input.copyTo(it) }
        }
        val out = mutableListOf<File>()
        ZipFile(raw).use { zip ->
            if (zip.getEntry("AndroidManifest.xml") != null) {
                // Plain APK
                val f = File(dest, "base.apk")
                raw.renameTo(f)
                out += f
            } else {
                // Bundle: pull out every *.apk (OBB and metadata are ignored)
                for (entry in zip.entries().asSequence()) {
                    if (entry.isDirectory || !entry.name.endsWith(".apk", ignoreCase = true)) continue
                    val name = entry.name.substringAfterLast('/')
                    val f = File(dest, name)
                    zip.getInputStream(entry).use { i -> f.outputStream().use { i.copyTo(it) } }
                    out += f
                }
            }
        }
        raw.delete()
        require(out.isNotEmpty()) { "No APK found in the selected file (encrypted .apkm files are not supported)" }
        return out
    }

    /** Metadata for an APK set previously stored with [import]. */
    fun stored(context: Context, dir: File): SourceApks {
        val apks = dir.listFiles { f -> f.name.endsWith(".apk", true) }?.toList().orEmpty()
        require(apks.isNotEmpty()) { "Stored APK files are missing" }
        val base = apks.firstOrNull { !isSplit(it) } ?: error("No base APK in the selected bundle")
        val pm = context.packageManager
        val info: PackageInfo = pm.getPackageArchiveInfo(base.path, PackageManager.GET_PERMISSIONS)
            ?: error("Android could not parse ${base.name}")
        val ai = info.applicationInfo?.apply {
            sourceDir = base.path
            publicSourceDir = base.path
        }
        return SourceApks(
            files = listOf(base) + (apks - base),
            packageName = info.packageName,
            label = ai?.loadLabel(pm)?.toString() ?: info.packageName,
            versionCode = PackageInfoCompat.getLongVersionCode(info),
            versionName = info.versionName,
            icon = ai?.loadIcon(pm),
            requestedPermissions = info.requestedPermissions?.toList().orEmpty(),
            minSdk = ai?.minSdkVersion ?: 0,
            targetSdk = ai?.targetSdkVersion ?: 0,
        )
    }

    /** Split name from the APK's manifest ("config.arm64_v8a"), or null for the base APK. */
    fun splitName(apk: File): String? = ZipFile(apk).use { zip ->
        val entry = zip.getEntry("AndroidManifest.xml") ?: return null
        zip.getInputStream(entry).use { AndroidManifestBlock.load(it).split }
    }

    fun isSplit(apk: File): Boolean = ZipFile(apk).use { zip ->
        val entry = zip.getEntry("AndroidManifest.xml") ?: return false
        zip.getInputStream(entry).use { AndroidManifestBlock.load(it).isSplit }
    }
}
