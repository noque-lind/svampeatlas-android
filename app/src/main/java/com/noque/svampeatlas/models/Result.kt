package com.noque.svampeatlas.models

sealed class Result<out Success, out Error>() {

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

    class Success<out Success, out Error>(val value: Success): Result<Success, Error>()
    class Error<out Success, out Error>(val error: Error): Result<Success, Error>()
}