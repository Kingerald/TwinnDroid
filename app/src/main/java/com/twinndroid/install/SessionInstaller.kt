package com.twinndroid.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class InstallResult(val success: Boolean, val message: String)

/** Standard, unprivileged install via PackageInstaller (shows Android's confirmation dialog). */
object SessionInstaller {

    internal val pending = ConcurrentHashMap<Int, CompletableDeferred<InstallResult>>()

    suspend fun install(context: Context, packageName: String, apks: List<File>): InstallResult {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            setInstallReason(android.content.pm.PackageManager.INSTALL_REASON_USER)
            if (Build.VERSION.SDK_INT >= 31) {
                // Lets updates of clones we installed go through without a prompt where Android allows it.
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val deferred = CompletableDeferred<InstallResult>()
        val sessionId = withContext(Dispatchers.IO) {
            val id = installer.createSession(params)
            pending[id] = deferred
            try {
                installer.openSession(id).use { session ->
                    apks.forEachIndexed { i, apk ->
                        session.openWrite("$i-${apk.name}", 0, apk.length()).use { out ->
                            apk.inputStream().use { it.copyTo(out) }
                            session.fsync(out)
                        }
                    }
                    val intent = Intent(context, InstallReceiver::class.java).putExtra(InstallReceiver.EXTRA_SESSION, id)
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                        (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                    val pi = PendingIntent.getBroadcast(context, id, intent, flags)
                    session.commit(pi.intentSender)
                }
            } catch (t: Throwable) {
                pending.remove(id)
                runCatching { installer.abandonSession(id) }
                throw t
            }
            id
        }
        return try { deferred.await() } finally { pending.remove(sessionId) }
    }
}
