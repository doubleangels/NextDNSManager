package com.doubleangels.nextdnsmanagement.data

import com.doubleangels.nextdnsmanagement.api.ApiException
import com.doubleangels.nextdnsmanagement.sentry.SentryManager
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** State of data loaded from the API. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}

val <T> Load<T>.valueOrNull: T? get() = (this as? Load.Ready)?.value

/** Runs [block], turning failures into [Load.Failed]. Unexpected errors are reported. */
suspend fun <T> load(block: suspend () -> T): Load<T> = try {
    Load.Ready(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Load.Failed(e.userMessage())
}

fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "Request failed"
    is IOException -> "Network error. Check your connection and try again."
    else -> {
        SentryManager.captureException(this)
        message ?: javaClass.simpleName
    }
}
