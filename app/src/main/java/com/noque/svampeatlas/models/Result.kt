package com.noque.svampeatlas.models

sealed class Result<Success, Error>() {

    inline fun onError(action: (error: Error) -> Unit) {
        when (this) {
            is Result.Error -> action(this.error)
            else -> return
        }
    }

    inline fun onSuccess(action: (value: Success) -> Unit) {
        when (this) {
            is Result.Success -> action(this.value)
            else -> return
        }

    }

    class Success<Success, Error>(val value: Success): Result<Success, Error>()
    class Error<Success, Error>(val error: Error): Result<Success, Error>()
}