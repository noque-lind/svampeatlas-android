package com.noque.svampeatlas.Model

sealed class Result<Success, Error>() {

    inline fun onError(action: (error: AppError) -> Unit) {
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
//        contract {
//            callsInPlace(action, InvocationKind.AT_LEAST_ONCE)
//        }

//        Result<String>.onSuccess {  }

    }

    class Success<Success, Error>(val value: Success): Result<Success, Error>()
    class Error<Success, Error>(val error: AppError): Result<Success, Error>()
}