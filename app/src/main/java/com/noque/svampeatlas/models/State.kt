package com.noque.svampeatlas.models

sealed class State<T>(val item: T? = null) {
    class Items<T>(val items: T): State<T>(items)
    class Empty<T>(): State<T>()
    class Loading<T>(): State<T>()
    class Error<T>(val error: AppError): State<T>()
}
