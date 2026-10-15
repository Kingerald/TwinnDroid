package com.twinndroid.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat

class InstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(EXTRA_SESSION, -1)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                if (confirm != null) {
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    complete(sessionId, InstallResult(false, "Installer did not provide a confirmation screen"))
                }
            }
            PackageInstaller.STATUS_SUCCESS -> complete(sessionId, InstallResult(true, "Installed"))
            else -> complete(sessionId, InstallResult(false, explain(status, message)))
        }
    }

    private fun complete(sessionId: Int, result: InstallResult) {
        SessionInstaller.pending.remove(sessionId)?.complete(result)
    }

    companion object {
        const val EXTRA_SESSION = "com.twinndroid.SESSION_ID"

        fun explain(status: Int, message: String): String = when {
            message.contains("UPDATE_INCOMPATIBLE") || status == PackageInstaller.STATUS_FAILURE_CONFLICT ->
                "A different-signed app with this package already exists. Uninstall the old clone first. ($message)"
            status == PackageInstaller.STATUS_FAILURE_ABORTED -> "Installation cancelled"
            status == PackageInstaller.STATUS_FAILURE_STORAGE -> "Not enough storage ($message)"
            status == PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "Incompatible with this device ($message)"
            status == PackageInstaller.STATUS_FAILURE_BLOCKED -> "Installation blocked ($message)"
            else -> message.ifBlank { "Installation failed (status $status)" }
        }
    }
}
