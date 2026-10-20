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

    override fun allowAppOp(packageName: String, op: String): String {
        // Only the special-access ops TwinnDroid offers, and only a well-formed package name, so this
        // can't be used to run anything else as the shell user.
        val byUid = ALLOWED_OPS[op] ?: return "Failure: op not allowed"
        if (!PACKAGE.matches(packageName)) return "Failure: bad package name"
        val args = buildList {
            add("appops"); add("set")
            if (byUid) add("--uid")
            add(packageName); add(op); add("allow")
        }
        val out = run(*args.toTypedArray())
        return if (out.isEmpty()) "Success" else "Failure: $out"
    }

    private companion object {
        // op -> whether Android tracks it per uid (true) or per package (false)
        val ALLOWED_OPS = mapOf(
            "MANAGE_EXTERNAL_STORAGE" to true,
            "REQUEST_INSTALL_PACKAGES" to false,
            "SYSTEM_ALERT_WINDOW" to false,
        )
        val PACKAGE = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
    }

    private fun run(vararg cmd: String): String {
        val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val out = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        return out.trim()
    }
}
