package com.seven.gamehub.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONArray

/**
 * Receives a one-shot PendingIntent callback from the SevenX-enabled Winlator
 * runtime. Installer library entries are removed only after Wine's launcher
 * process has actually been created. The imported file is never deleted.
 */
class RuntimeLaunchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sourceUri = intent.getStringExtra(EXTRA_SOURCE_URI) ?: return
        val started = intent.getBooleanExtra(EXTRA_STARTED, false)
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val source = JSONArray(preferences.getString(PREFS_GAMES, "[]") ?: "[]")
        val updated = JSONArray()

        for (index in 0 until source.length()) {
            val item = source.optJSONObject(index) ?: continue
            val sameEntry = item.optString("uri") == sourceUri
            val installer = item.optBoolean("isInstaller", false)
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
        const val ACTION_RUN_EXE = "com.seven.gamehub.action.RUN_EXE"
        const val PREFS_NAME = "seven_gamehub_library"
        const val PREFS_GAMES = "games"
        const val PREFS_LAST_RESULT_URI = "runtime_last_result_uri"
        const val PREFS_LAST_RESULT_STARTED = "runtime_last_result_started"
    }
}
