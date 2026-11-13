package com.seven.gamehub.runtime

import android.app.PendingIntent
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

/**
 * Result returned by the Windows execution backend.
 *
 * STARTED is reserved for a confirmed process start. LAUNCH_REQUESTED means
 * Winlator accepted the hand-off but has not yet confirmed process creation.
 */
internal enum class RuntimeLaunchResult {
    STARTED,
    LAUNCH_REQUESTED,
    NOT_INSTALLED,
    FAILED
}

internal enum class RuntimeAvailability {
    READY,
    NOT_INSTALLED,
    UNSUPPORTED_DEVICE,
    INITIALIZING,
    ERROR
}

internal data class RuntimeStatus(
    val engineName: String,
    val availability: RuntimeAvailability,
    val message: String
)

internal interface WindowsRuntimeEngine {
    fun status(context: Context): RuntimeStatus

    suspend fun launch(
        context: Context,
        executable: Uri,
        launchProfile: String = "default"
    ): RuntimeLaunchResult
}

/**
 * SevenX-to-Winlator bridge. The runtime receives a temporary read grant for
 * the imported EXE and a one-shot PendingIntent used only to confirm that its
 * Wine/Box64 launcher process was created.
 */
internal object WindowsRuntime : WindowsRuntimeEngine {
    private const val WINLATOR_PACKAGE = "com.winlator"
    private const val WINLATOR_BRIDGE = "com.winlator.SevenXBridgeActivity"

    override fun status(context: Context): RuntimeStatus {
        val installed = runCatching {
            context.packageManager.getActivityInfo(
                ComponentName(WINLATOR_PACKAGE, WINLATOR_BRIDGE),
                0
            )
        }.isSuccess

        return if (installed) {
            RuntimeStatus(
                engineName = "Winlator • SevenX Bridge",
                availability = RuntimeAvailability.READY,
                message = "Winlator установлен. Запуск подтверждается отдельно."
            )
        } else {
            RuntimeStatus(
                engineName = "Winlator Runtime",
                availability = RuntimeAvailability.NOT_INSTALLED,
                message = "Установи сборку Winlator с поддержкой SevenX"
            )
        }
    }

    override suspend fun launch(
        context: Context,
        executable: Uri,
        launchProfile: String = "default"
    ): RuntimeLaunchResult {
        val sharedUri = runCatching {
            if (executable.scheme == "file") {
                val file = executable.path?.let(::File) ?: return RuntimeLaunchResult.FAILED
                FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            } else {
                executable
            }
        }.getOrElse { return RuntimeLaunchResult.FAILED }

        val callbackIntent = Intent(context, RuntimeLaunchReceiver::class.java).apply {
            putExtra(RuntimeLaunchReceiver.EXTRA_SOURCE_URI, executable.toString())
        }
        val callbackFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val callback = PendingIntent.getBroadcast(
            context,
            executable.toString().hashCode(),
            callbackIntent,
            callbackFlags
        )

        val launchIntent = Intent(RuntimeLaunchReceiver.ACTION_RUN_EXE).apply {
            setClassName(WINLATOR_PACKAGE, WINLATOR_BRIDGE)
            setDataAndType(sharedUri, "application/octet-stream")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra("sevenx_callback", callback)
            putExtra("sevenx_profile", if (launchProfile == "compatibility") "compatibility" else "default")
        }

        return runCatching {
            context.startActivity(launchIntent)
            RuntimeLaunchResult.LAUNCH_REQUESTED
        }.getOrElse { error ->
            if (error is PackageManager.NameNotFoundException) {
                RuntimeLaunchResult.NOT_INSTALLED
            } else {
                RuntimeLaunchResult.FAILED
            }
        }
    }
}
