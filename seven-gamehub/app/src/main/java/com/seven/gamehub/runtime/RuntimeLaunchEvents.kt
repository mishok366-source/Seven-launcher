package com.seven.gamehub.runtime

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/**
 * Correlates asynchronous Winlator process-exit broadcasts with the coroutine
 * that started a compatibility installer. Tokens prevent concurrent launches
 * from consuming one another's callbacks.
 */
internal object RuntimeLaunchEvents {
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Int>>()

    fun register(token: String): CompletableDeferred<Int> {
        val deferred = CompletableDeferred<Int>()
        check(pending.putIfAbsent(token, deferred) == null) {
            "A Winlator callback is already registered for this operation"
        }
        return deferred
    }

    fun complete(token: String, exitCode: Int) {
        pending[token]?.complete(exitCode)
    }

    fun unregister(token: String, deferred: CompletableDeferred<Int>) {
        pending.remove(token, deferred)
        deferred.cancel()
    }
}
