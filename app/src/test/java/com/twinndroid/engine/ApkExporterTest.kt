package com.twinndroid.engine

import com.reandroid.apk.ApkModule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Merges a split set (TWINN_TEST_APKS, base first) into one APK; output in app/build/merge-test. */
class ApkExporterTest {

    @Test
    fun mergesSplitsIntoOneApk() = runBlocking {
        val inputs = System.getenv("TWINN_TEST_APKS")?.split(',')?.map { File(it.trim()) }
        assumeTrue("TWINN_TEST_APKS not set", inputs != null && inputs.size > 1)
        val work = File("build/merge-test").apply { deleteRecursively(); mkdirs() }
        val key = SigningKeys.newKey()
        val merged = ApkExporter.mergeTo(inputs!!, work, CloneEngine(work) { key }, key, 21) { println("progress $it") }

        val module = ApkModule.loadApkFile(merged)
        val manifest = module.androidManifest
        println("merged: ${merged.length()} bytes, package=${manifest.packageName}, split=${manifest.split}")
        assertFalse("still marked as split", manifest.isSplit)
        assertTrue("native libs missing", module.zipEntryMap.listInputSources().any { it.alias.startsWith("lib/") })
        module.close()
    }
}
