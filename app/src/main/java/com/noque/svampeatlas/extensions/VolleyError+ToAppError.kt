package com.noque.svampeatlas.extensions

import com.android.volley.*
import com.noque.svampeatlas.R
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.utilities.MyApplication

fun VolleyError.toAppError(): DataService.Error {
    when (this) {
        is AuthFailureError -> {
            return DataService.Error.VolleyError(
                MyApplication.applicationContext.getString(R.string.error_network_unAuthorized_title),
                MyApplication.applicationContext.getString(R.string.error_network_unAuthorized_message)
            )
        }

        is NoConnectionError -> {
            return DataService.Error.VolleyError(
                MyApplication.applicationContext.getString(R.string.error_network_noInternet_title),
                MyApplication.applicationContext.getString(R.string.error_network_noInternet_message)
            )
        }

        is TimeoutError -> {
            return DataService.Error.VolleyError(
                MyApplication.applicationContext.getString(R.string.error_network_timeout_title),
                MyApplication.applicationContext.getString(R.string.error_network_timeout_message)
            )
        }

        is ServerError -> {
            return DataService.Error.VolleyError(
                MyApplication.applicationContext.getString(R.string.error_network_serverError_title),
                MyApplication.applicationContext.getString(R.string.error_network_serverError_message)
            )
        }

        is ParseError -> {
            return DataService.Error.VolleyError(
                MyApplication.applicationContext.getString(R.string.error_network_invalidResponse_title),
                MyApplication.applicationContext.getString(R.string.error_network_invalidResponse_message)
            )
        }
    }

    return DataService.Error.VolleyError(MyApplication.applicationContext.getString(R.string.error_dataService_unknown_title), this.message + "" + this.networkResponse.data.toString() ?: "")
}