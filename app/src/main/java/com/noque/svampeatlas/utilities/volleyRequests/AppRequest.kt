package com.noque.svampeatlas.utilities.volleyRequests

import android.util.Log
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.noque.svampeatlas.utilities.api.API
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.lang.reflect.Type
import java.nio.charset.Charset

class AppRequest<T>(private val type: Type, private val endpoint: API,
                    private val token: String?,
                    private val jsonObject: JSONObject? = null,
                    private val listener: Response.Listener<T>,
                    errorListener: Response.ErrorListener): Request<T>(endpoint.volleyMethod(), endpoint.url(), errorListener) {

    override fun getBody(): ByteArray {
        return jsonObject.toString().toByteArray()
    }

    override fun getHeaders(): MutableMap<String, String> {
        val mutableMap = mutableMapOf(Pair("Content-Type", "application/json"))
        token?.let {
            mutableMap.put("Authorization", "Bearer ${token}")
        }
        return mutableMap
    }

    override fun deliverResponse(response: T) {
        listener.onResponse(response)
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<T> {
        return try {
            val json = String(
                response?.data ?: ByteArray(0),
                Charset.forName(HttpHeaderParser.parseCharset(response?.headers)))
            Response.success(
                Gson().fromJson<T>(json, type),
                HttpHeaderParser.parseCacheHeaders(response)
            )
        } catch (e: UnsupportedEncodingException) {
            Response.error(ParseError(e))
        } catch (e: JsonSyntaxException) {
            Response.error(ParseError(e))
        }
    }
}