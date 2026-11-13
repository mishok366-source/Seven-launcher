package com.seven.gamehub.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONArray

/**
 * Receives start and process-exit callbacks from the SevenX-enabled Winlator
 * runtime. Batch-install callbacks are correlated by token; imported source
 * files are never deleted by this receiver.
 */
class RuntimeLaunchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val callbackToken = intent.getStringExtra(EXTRA_CALLBACK_TOKEN).orEmpty()
        if (intent.getBooleanExtra(EXTRA_PROCESS_FINISHED, false)) {
            if (callbackToken.isNotBlank()) {
                RuntimeLaunchEvents.complete(callbackToken, intent.getIntExtra(EXTRA_EXIT_CODE, -1))
                return
            }
            // A normal game-exit notification has no start-result extra; ignore
            // it so it cannot overwrite the launch result shown in SevenX.
            if (!intent.hasExtra(EXTRA_STARTED)) return
        }
        val sourceUri = intent.getStringExtra(EXTRA_SOURCE_URI) ?: return
        val started = intent.getBooleanExtra(EXTRA_STARTED, false)
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val source = JSONArray(preferences.getString(PREFS_GAMES, "[]") ?: "[]")
        val updated = JSONArray()

        for (index in 0 until source.length()) {
            val item = source.optJSONObject(index) ?: continue
            val sameEntry = item.optString("uri") == sourceUri
            val installer = item.optBoolean("isInstaller", false)
            if (started && sameEntry) {
                item.put("launchCount", item.optInt("launchCount", 0) + 1)
                item.put("lastLaunchedAt", System.currentTimeMillis())
            }
            if (!(started && sameEntry && installer)) updated.put(item)
        }

        preferences.edit()
            .putString(PREFS_GAMES, updated.toString())
            .putString(PREFS_LAST_RESULT_URI, sourceUri)
            .putBoolean(PREFS_LAST_RESULT_STARTED, started)
            .apply()
    }

    companion object {
        const val EXTRA_SOURCE_URI = "sevenx_source_uri"
        const val EXTRA_STARTED = "sevenx_started"
        const val EXTRA_CALLBACK_TOKEN = "sevenx_callback_token"
        const val EXTRA_PROCESS_FINISHED = "sevenx_process_finished"
        const val EXTRA_EXIT_CODE = "sevenx_exit_code"
        const val ACTION_RUN_EXE = "com.seven.gamehub.action.RUN_EXE"
        const val PREFS_NAME = "seven_gamehub_library"
        const val PREFS_GAMES = "games"
        const val PREFS_LAST_RESULT_URI = "runtime_last_result_uri"
        const val PREFS_LAST_RESULT_STARTED = "runtime_last_result_started"
    }
}
