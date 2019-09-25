package com.noque.svampeatlas.models

sealed class State<T> {
    class Items<T>(val items: T): State<T>()
    class Empty<T>(): State<T>()
    class Loading<T>(): State<T>()
    class Error<T>(val error: AppError): State<T>()
}
