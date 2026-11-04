package com.example.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** One logical insert ticket spans bounded IO retries; action dispatch never waits for it. */
object HistoryInsertRetry {
    suspend fun <T> run(retryable: (Exception) -> Boolean, onRetry: (Int) -> Unit = {}, operation: suspend () -> T): T {
        for (attempt in 0..2) {
            try { return operation() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (attempt == 2 || !retryable(failure)) throw failure
                onRetry(attempt + 1)
                delay(if (attempt == 0) 100L else 300L)
            }
        }
        error("Unreachable")
    }
}
