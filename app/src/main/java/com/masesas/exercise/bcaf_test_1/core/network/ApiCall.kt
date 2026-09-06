package com.masesas.exercise.bcaf_test_1.core.network

import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.common.CommonFailure
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403

/** Menjalankan panggilan API dan menerjemahkan exception jaringan menjadi [CommonFailure]. */
suspend fun <T> runApiCatching(
    json: Json,
    block: suspend () -> AppResult<T>,
): AppResult<T> = try {
    block()
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (http: HttpException) {
    AppResult.failure(http.toFailure(json))
} catch (io: IOException) {
    AppResult.failure(CommonFailure.Network(io))
} catch (throwable: Throwable) {
    AppResult.failure(CommonFailure.Unexpected(throwable))
}

/** Mengambil payload envelope, atau melaporkan blok `error` bila server mengirimkannya. */
fun <T> ApiEnvelope<T>.requirePayload(): AppResult<T> {
    error?.let { return AppResult.failure(CommonFailure.ApiError(it.code, it.details)) }

    val payload = data
        ?: return AppResult.failure(CommonFailure.ApiError(details = listOfNotNull(message)))

    return AppResult.success(payload)
}

private fun HttpException.toFailure(json: Json): CommonFailure {
    if (code() == HTTP_UNAUTHORIZED || code() == HTTP_FORBIDDEN) return CommonFailure.Unauthorized

    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
        ?: return CommonFailure.Unexpected(this)

    val parsed = runCatching {
        json.decodeFromString(ApiEnvelope.serializer(ApiErrorDto.serializer()), body)
    }.getOrNull()

    val error = parsed?.error
        ?: return CommonFailure.ApiError(details = listOfNotNull(parsed?.message))

    return CommonFailure.ApiError(error.code, error.details)
}
