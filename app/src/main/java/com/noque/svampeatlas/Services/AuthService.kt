package com.noque.svampeatlas.Services

import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.Model.Animal
import com.noque.svampeatlas.Model.ApiKey
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Utilities.API
import com.noque.svampeatlas.Utilities.APIType
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.lang.reflect.Type
import java.nio.charset.Charset
import kotlin.Result

const val BASE_URL = "https://us-central1-apis-4674e.cloudfunctions.net"
const val URL_REGISTER = "{$BASE_URL}account/register"


class AppRequest<T>(private val type: Type, private val endpoint: API,
                    private val token: String?,
                    private val listener: Response.Listener<T>,
                    errorListener: Response.ErrorListener): Request<T>(endpoint.volleyMethod(), endpoint.url(), errorListener) {

    override fun getHeaders(): MutableMap<String, String> {

        val mutableMap = mutableMapOf<String, String>()

        token?.let {
            mutableMap.put("Authorization", "Bearer ${token}")
        }

        return mutableMap
    }

    override fun deliverResponse(response: T) {
        listener.onResponse(response)
    }

    override fun deliverError(error: VolleyError?) {
        super.deliverError(error)
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

class DataService private constructor(context: Context) {

    enum class IMAGESIZE(val value: String) {
        FULL(""), MINI("https://svampe.databasen.org/unsafe/175x175/")
    }


    companion object {
        @Volatile
        private var INSTANCE: DataService? = null

        fun getInstance(context: Context): DataService {

            context.applicationContext
            return INSTANCE ?: synchronized(this) {
                return DataService(context).also { INSTANCE = it }
            }
        }
    }


    val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext)
    }


    private fun <RequestType> addToRequestQueue(request: Request<RequestType>) {
        requestQueue.add(request)
    }

     class DataServiceError(message: String): Throwable(message) {

    }

    fun getMushrooms(offset: Int, completion: (Result<List<Mushroom>>) -> Unit)  {
        val api = API(APIType.Request.Mushroom(0,
            100,
            null,
            true))

        val request = AppRequest<List<Mushroom>>(object: TypeToken<List<Mushroom>>() {}.type,
            api,
            null,
            Response.Listener {
                it.forEach {
                    Log.d("Dataserver", it.fullName)
                }
                completion(Result.success(it))
            },
            Response.ErrorListener {
                completion(Result.failure(it))
            })

        addToRequestQueue(request)
    }

//    fun getAPIKey(completion: (Result<ApiKey>) -> Unit) {
//
//        val typeToken = object : TypeToken<ApiKey>(){}.type
//
//
//        val request = GsonRequest<ApiKey>("https://us-central1-apis-4674e.cloudfunctions.net/getKey",
//            typeToken,
//            null,
//            Response.Listener {
//                completion(Result.success(it))
//            },
//
//            Response.ErrorListener {
//                completion(Result.failure(it))
//            })
//
//        addToRequestQueue(request)
//    }

//    fun getAnimals(key: String, completion: (Result<List<Animal>>) -> Unit) {
//        val typeToken = object : TypeToken<List<Animal>>(){}.type
//
//
//        val requestOne = object: StringRequest(Request.Method.POST, "https://us-central1-apis-4674e.cloudfunctions.net/getAnimals",
//            Response.Listener {
//
//                val animals: List<Animal> = Gson().fromJson(it, typeToken)
//                completion(Result.success(animals))
//                Log.d("LOL", animals.toString())
//            },
//
//            Response.ErrorListener {
//                Log.d("LOL", it.message)
//            }) {
//
//            override fun getBodyContentType(): String {
//                return "application/x-www-form-urlencoded; charset=utf-8"
//            }
//
//
//            override fun getParams(): MutableMap<String, String> {
//               return mutableMapOf("key" to key)
//            }
//        }
//
//        addToRequestQueue(requestOne)
//
//
//
//
////        val typeToken = object : TypeToken<List<Animal>>() {}.type
//
//        val request = GsonRequest<List<Animal>>("http://us-central1-apis-4674e.cloudfunctions.net/getAnimal",
//            typeToken,
//            mutableMapOf("key" to key),
//            Response.Listener {
//                completion(Result.success(it))
//            },
//
//            Response.ErrorListener {
//                completion(Result.failure(it))
//            }
//            )
//
//    }
}


class AuthService {

    fun registerUser(context: Context, email: String, password: String, complete: (Boolean) -> Unit) {

        val url = URL_REGISTER
        val jsonBody = JSONObject()

        jsonBody.put("name", email)
        jsonBody.put("password", password)

        var requestBody = jsonBody.toString()

        val registerRequest = object : StringRequest(Method.POST, url, Response.Listener { _ ->

        }, Response.ErrorListener { error ->

        }) {
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getBody(): ByteArray {
                return requestBody.toByteArray()
            }
        }


        Volley.newRequestQueue(context).add(registerRequest)
    }

    fun getAPIKey() {
//        val request = object: JsonRequest<>(Request.Method.GET)


    }
}