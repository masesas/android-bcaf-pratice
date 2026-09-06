package com.masesas.exercise.bcaf_test_1.domain.common

/** Hasil operasi: sukses membawa data, gagal membawa [AppFailure] bertipe. */
sealed interface AppResult<out T> {

    data class Success<out T>(val data: T) : AppResult<T>

    data class Failure(val failure: AppFailure) : AppResult<Nothing>

    companion object {
        fun <T> success(data: T): AppResult<T> = Success(data)

        fun failure(failure: AppFailure): AppResult<Nothing> = Failure(failure)
    }
}

/** Menjalankan [onSuccess] bila sukses, mengembalikan receiver apa adanya. */
inline fun <T> AppResult<T>.onSuccess(onSuccess: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) onSuccess(data)
    return this
}

/** Menjalankan [onFailure] bila gagal, mengembalikan receiver apa adanya. */
inline fun <T> AppResult<T>.onFailure(onFailure: (AppFailure) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) onFailure(failure)
    return this
}

/** Memetakan payload sukses tanpa menyentuh cabang gagal. */
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}
