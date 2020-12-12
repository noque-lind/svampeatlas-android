package com.noque.svampeatlas.utilities

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.noque.svampeatlas.utilities.api.API
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class AppMultipartPost(private val api: API,
                       private val token: String,
                       private val image: MultipartFormImage,
                       private val listener: Response.Listener<NetworkResponse>,
                       errorListener: Response.ErrorListener): Request<NetworkResponse>(api.volleyMethod(), api.url(), errorListener) {

    private val boundary = "apiclient${System.currentTimeMillis()}"


    override fun getHeaders(): MutableMap<String, String> {
        val header = mutableMapOf<String, String>()
        header.put("Authorization", "Bearer $token")
        return header

    }

    override fun getBodyContentType(): String {
        return "multipart/form-data;boundary=$boundary"
    }

    override fun getBody(): ByteArray {
        val lineBreak = "\r\n"

        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        dos.writeBytes("--${boundary}$lineBreak")
        dos.writeBytes("Content-Disposition: form-data; name=\"${image.key}\"; filename=\"${image.fileName}\"$lineBreak")
        dos.writeBytes("Content-Type: ${image.mimeType}$lineBreak$lineBreak")
        dos.write(image.byteArray)
        dos.writeBytes(lineBreak)
        dos.writeBytes("--$boundary--")
        dos.writeBytes(lineBreak)
        dos.flush()
        bos.flush()
        return bos.toByteArray()
    }


    override fun parseNetworkResponse(response: NetworkResponse?): Response<NetworkResponse> {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
    }

    override fun deliverResponse(response: NetworkResponse?) {
        listener.onResponse(response)
    }
}