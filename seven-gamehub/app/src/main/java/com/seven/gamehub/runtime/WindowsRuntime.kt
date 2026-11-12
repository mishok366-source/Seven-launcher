package com.seven.gamehub.runtime

import android.content.Context
import android.net.Uri

/**
 * Result returned by the Windows execution backend.
 *
 * STARTED must only be returned after the backend has positively confirmed that
 * the Windows process was created. Merely accepting a request is not enough.
 */
internal enum class RuntimeLaunchResult {
    STARTED,
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

/**
 * Narrow boundary between the SevenX library/UI and the Windows compatibility
 * implementation. Wine, Box64 and graphics backends will be provided behind
 * this API so the library does not depend on a particular runtime's UI.
 */
internal interface WindowsRuntimeEngine {
    fun status(context: Context): RuntimeStatus

    suspend fun launch(
        context: Context,
        executable: Uri
    ): RuntimeLaunchResult
}

/**
 * Honest no-engine implementation used until a redistributable, Android-ready
 * Wine + translation stack has been integrated and validated on-device.
 */
internal object WindowsRuntime : WindowsRuntimeEngine {
    override fun status(context: Context): RuntimeStatus = RuntimeStatus(
        engineName = "SevenX Windows Engine",
        availability = RuntimeAvailability.NOT_INSTALLED,
        message = "Wine и Box64 пока не подключены"
    )

    override suspend fun launch(
        context: Context,
        executable: Uri
    ): RuntimeLaunchResult = RuntimeLaunchResult.NOT_INSTALLED
}
