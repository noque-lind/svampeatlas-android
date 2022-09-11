package com.noque.svampeatlas.extensions

import com.android.volley.*
import com.noque.svampeatlas.R
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.utilities.MyApplication

fun VolleyError.toAppError(): DataService.Error {
    when (this) {
        is AuthFailureError -> {
            return DataService.Error.VolleyError(
                MyApplication.applicationContext.getString(R.string.urlSessionError_unAuthorized_title),
                MyApplication.applicationContext.getString(R.string.urlSessionError_unAuthorized_message)
            )
        }

        is NoConnectionError -> {
            return DataService.Error.VolleyError(
                MyApplication.applicationContext.getString(R.string.urlSessionError_noInternet_title),
                MyApplication.applicationContext.getString(R.string.urlSessionError_noInternet_message)
            )
        }

        is TimeoutError -> {
            return DataService.Error.VolleyError(
                MyApplication.applicationContext.getString(R.string.urlSessionError_timeout_title),
                MyApplication.applicationContext.getString(R.string.urlSessionError_timeout_message)
            )
        }

        is ServerError -> {
            return DataService.Error.VolleyError(
                MyApplication.applicationContext.getString(R.string.urlSessionError_serverError_title),
                MyApplication.applicationContext.getString(R.string.urlSessionError_serverError_message)
            )
        }

        is ParseError -> {
            return DataService.Error.VolleyError(
                MyApplication.applicationContext.getString(R.string.urlSessionError_invalidResponse_title),
                MyApplication.applicationContext.getString(R.string.urlSessionError_invalidResponse_message)
            )
        }
    }

    return DataService.Error.VolleyError(MyApplication.applicationContext.getString(R.string.dataServiceError_unknown_title), this.message + "" + this.networkResponse.data.toString() ?: "")
}