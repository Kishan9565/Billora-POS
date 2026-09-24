package com.kishan.billorapos.core.domain

sealed interface DataError : Error {
    sealed interface Local : DataError {
        object NOT_FOUND : Local
        object UNKNOWN : Local
    }
}
