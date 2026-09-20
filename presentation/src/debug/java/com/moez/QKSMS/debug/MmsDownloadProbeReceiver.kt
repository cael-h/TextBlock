package dev.octoshrimpy.quik.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.android.mms.transaction.DownloadManager

/** Debug-only probe for the production carrier MMS download entry point. */
class MmsDownloadProbeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val runId = intent.getStringExtra(EXTRA_RUN_ID) ?: System.currentTimeMillis().toString()
        val location = "https://example.invalid/textblock-mms-probe-$runId"

        runCatching {
            DownloadManager.getInstance().downloadMultimediaMessage(
                context,
                location,
                Telephony.Mms.Inbox.CONTENT_URI,
                true,
                -1
            )
        }.onSuccess {
            Log.i(TAG, "PASS runId=$runId production download accepted")
        }.onFailure { error ->
            Log.e(TAG, "FAIL runId=$runId production download rejected", error)
        }
    }

    companion object {
        const val ACTION_PROBE = "com.caelh.textblock.debug.action.PROBE_MMS_DOWNLOAD"
        const val EXTRA_RUN_ID = "runId"
        const val TAG = "MmsDownloadProbe"
    }
}
