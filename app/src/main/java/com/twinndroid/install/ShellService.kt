package com.twinndroid.install

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.annotation.Keep
import kotlin.system.exitProcess

/**
 * Shizuku user service. Runs as the shell user, so it can drive `pm install-*` directly — this is
 * what makes installs silent. APK contents are streamed in over file descriptors.
 */
@Keep
class ShellService() : IShellService.Stub() {

    @Keep
    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context) : this()

    override fun destroy() = exitProcess(0)

    override fun installApks(apks: List<ParcelFileDescriptor>, sizes: LongArray): String {
        val create = run("pm", "install-create", "--user", "current", "-r", "-S", sizes.sum().toString())
        val session = Regex("\\[(\\d+)]").find(create)?.groupValues?.get(1)
            ?: return "Failure: $create"
        try {
            apks.forEachIndexed { i, fd ->
                val proc = ProcessBuilder("pm", "install-write", "-S", sizes[i].toString(), session, "$i.apk", "-")
                    .redirectErrorStream(true)
                    .start()
                ParcelFileDescriptor.AutoCloseInputStream(fd).use { input ->
                    proc.outputStream.use { input.copyTo(it) }
                }
                val out = proc.inputStream.bufferedReader().readText()
                proc.waitFor()
                if (!out.contains("Success")) {
                    run("pm", "install-abandon", session)
                    return "Failure: $out"
                }
            }
            return run("pm", "install-commit", session)
        } catch (t: Throwable) {
            run("pm", "install-abandon", session)
            return "Failure: ${t.message}"
        }
    }

    private fun run(vararg cmd: String): String {
        val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val out = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        return out.trim()
    }
}
