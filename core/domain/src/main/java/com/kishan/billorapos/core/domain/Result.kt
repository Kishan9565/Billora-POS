package com.kishan.billorapos.core.domain

sealed interface Result<out D, out E: com.kishan.billorapos.core.domain.Error> {
    data class Success<out D>(val data: D): Result<D, Nothing>
    data class Error<out E: com.kishan.billorapos.core.domain.Error>(val error: E, val message: String? = null): Result<Nothing, E>
}
