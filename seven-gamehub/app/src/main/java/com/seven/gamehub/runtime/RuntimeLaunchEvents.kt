package com.seven.gamehub.runtime

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/**
 * Correlates process-start and process-exit callbacks with the coroutine that
 * launched an installer. A hand-off to an Android Activity is not treated as
 * proof that Wine actually created the guest process.
 */
internal object RuntimeLaunchEvents {
    data class PendingLaunch(
        val started: CompletableDeferred<Boolean> = Completabletable(),
        val completed: CompletableDeferred<Int> = Completabletable()
    )

    private fun <T> Completabletable(): CompletableDeferred<T> = CompletableDeferred()

    private val pending = ConcurrentHashMap<String, PendingLaunch>()

    fun register(token: String): PendingLaunch {
        val operation = PendingLaunch()
        check(pending.putIfAbsent(token, operation) == null) {
            "A Winlator callback is already registered for this operation"
        }
        return operation
    }

    fun started(token: String, didStart: Boolean) {
        pending[token]?.started?.complete(didStart)
    }

    fun complete(token: String, exitCode: Int) {
        pending[token]?.completed?.complete(exitCode)
    }

    fun unregister(token: String, operation: PendingLaunch) {
        pending.remove(token, operation)
        operation.started.cancel()
        operation.completed.cancel()
    }
}

/** Distinguishes a runtime that never starts Wine from an installer that hangs. */
internal class RuntimeStartTimeoutException : kotlinx.coroutines.CancellationException(
    "Winlator не подтвердил запуск Windows-процесса за 90 секунд. Очередь остановлена."
)
