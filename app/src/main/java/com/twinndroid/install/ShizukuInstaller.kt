package com.twinndroid.install

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.twinndroid.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import java.io.File
import kotlin.coroutines.resume

/** Silent installs through Shizuku (https://shizuku.rikka.app) when it is running and permitted. */
object ShizukuInstaller {
    const val PERMISSION_REQUEST_CODE = 4711

    @Volatile private var service: IShellService? = null

    fun isRunning(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean = isRunning() && runCatching {
        !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun requestPermission() {
        if (isRunning() && !Shizuku.isPreV11()) Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
    }

    suspend fun install(context: Context, apks: List<File>): InstallResult = withContext(Dispatchers.IO) {
        val svc = service?.takeIf { it.asBinder().pingBinder() } ?: bind(context)
        val fds = apks.map { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }
        try {
            val out = svc.installApks(fds, apks.map { it.length() }.toLongArray())
            InstallResult(out.contains("Success"), if (out.contains("Success")) "Installed via Shizuku" else out)
        } finally {
            fds.forEach { runCatching { it.close() } }
        }
    }

    private suspend fun bind(context: Context): IShellService = withTimeout(15_000) {
        suspendCancellableCoroutine { cont ->
            val args = Shizuku.UserServiceArgs(ComponentName(context.packageName, ShellService::class.java.name))
                .daemon(false)
                .processNameSuffix("shell")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE)
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    if (binder == null || !binder.pingBinder()) return
                    val s = IShellService.Stub.asInterface(binder)
                    service = s
                    if (cont.isActive) cont.resume(s)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    service = null
                }
            }
            Shizuku.bindUserService(args, conn)
        }
    }
}
