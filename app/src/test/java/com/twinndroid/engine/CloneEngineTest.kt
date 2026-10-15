package com.twinndroid.engine

import com.reandroid.apk.ApkModule
import com.twinndroid.data.CloneConfig
import com.twinndroid.data.SourceKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Runs the real clone pipeline on the JVM. Point TWINN_TEST_APKS at an APK (or a
 * comma-separated base+splits set); outputs land in app/build/clone-test for inspection
 * with apksigner/aapt2.
 */
class CloneEngineTest {

    @Test
    fun clonesApkSet() = runBlocking {
        val inputs = System.getenv("TWINN_TEST_APKS")?.split(',')?.map { File(it.trim()) }
        assumeTrue("TWINN_TEST_APKS not set", inputs != null)
        val newPkg = System.getenv("TWINN_TEST_PKG") ?: "com.example.twinnclone"
        val out = File("build/clone-test").apply { deleteRecursively(); mkdirs() }

        val config = CloneConfig(
            id = "test",
            sourceKind = SourceKind.FILE,
            sourcePackage = "",
            sourceLabel = "",
            clonePackage = newPkg,
            label = "Twinn Test Clone",
            removedPermissions = listOf("android.permission.POST_NOTIFICATIONS"),
        )
        // 1x1 PNG; enough to exercise resource-table injection
        val png = java.util.Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=")

        val key = SigningKeys.newKey()
        val signed = CloneEngine(out) { key }.build(config, inputs!!, png) { println(it) }

        assertEquals(inputs.size, signed.size)
        for (apk in signed) {
            val module = ApkModule.loadApkFile(apk)
            val manifest = module.androidManifest
            assertEquals(newPkg, manifest.packageName)
            if (module.hasTableBlock()) {
                module.tableBlock.packages.asSequence().filter { it.id == 0x7f }.forEach {
                    assertEquals("resource table package not renamed", newPkg, it.name)
                }
            }
            println("${apk.name}: package=${manifest.packageName} split=${manifest.split}")
            module.close()
        }
        val base = ApkModule.loadApkFile(signed.first())
        val text = base.androidManifest.toString()
        assertTrue("label not applied", base.androidManifest.applicationLabelString == "Twinn Test Clone")
        assertTrue("permission not removed", "POST_NOTIFICATIONS" !in base.androidManifest.usesPermissions.joinToString())
        base.close()
        println(text.take(200))
    }
}
