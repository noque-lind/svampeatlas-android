package com.noque.svampeatlas.Services

import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.Model.*
import com.noque.svampeatlas.Utilities.API
import com.noque.svampeatlas.Utilities.APIType
import com.noque.svampeatlas.Utilities.Geometry
import com.noque.svampeatlas.Utilities.ObservationQueries
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.util.function.Predicate
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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

    sealed class DataServiceError(title: String, message: String): AppError(title, message) {
        class NoInternetError(): DataServiceError("Ingen internet", "WTF")
        class SearchResponseEmpty(): DataServiceError("Ugyldig søgning", "Det du søgte efter kunne ikke findes, prøv at søg efter noget andet")
    }


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


    fun getMushrooms(offset: Int, completion: (Result<List<Mushroom>, DataServiceError>) -> Unit)  {
        val api = API(APIType.Request.Mushroom(0,
            100,
            null,
            true))

        val request = AppRequest<List<Mushroom>>(object: TypeToken<List<Mushroom>>() {}.type,
            api,
            null,
            Response.Listener {
                completion(Result.Success(it))
            },
            Response.ErrorListener {
                completion(Result.Error(DataServiceError.NoInternetError()))
            })

        addToRequestQueue(request)
    }

    fun getMushrooms(searchString: String, completion: (Result<List<Mushroom>, DataServiceError>) -> Unit) {
        val api = API(APIType.Request.Mushroom(0,
            100,
            searchString,
            false))

        val request = AppRequest<List<Mushroom>>(object: TypeToken<List<Mushroom>>(){}.type,
            api,
            null,
            Response.Listener {
                if (it.isEmpty()) {
                    completion(Result.Error(DataServiceError.SearchResponseEmpty()))
                } else {
                    completion(Result.Success(it))
                }
            },

            Response.ErrorListener {
                completion(Result.Error(DataServiceError.NoInternetError()))
            })

        addToRequestQueue(request)
    }

    fun getObservationsWithin(geometry: Geometry, taxonID: Int? = null, ageInYear: Int? = null, completion: (Result<List<Observation>, AppError>) -> Unit) {
        val api = API(APIType.Request.Observation(
            geometry,
            listOf(
                ObservationQueries.Comments(),
                ObservationQueries.Images(),
                ObservationQueries.Locality(),
                ObservationQueries.User(null),
                ObservationQueries.DeterminationView(taxonID)
            ),
            ageInYear,
            null,
            null
            ))

       val request = AppRequest<List<Observation>>(
           object: TypeToken<List<Observation>>() {}.type,
        api,
         null,
           Response.Listener {
               Log.d("DetailsViewModel", it.toString())
               completion(Result.Success(it))
           },
           Response.ErrorListener {
               Log.d("Dataservice", it.toString())
               completion(Result.Error(DataServiceError.NoInternetError()))
           })

        request.setRetryPolicy(DefaultRetryPolicy(50000, 2, 10F))
        addToRequestQueue(request)
    }

    fun getLocalities(geometry: Geometry, completion: (Result<List<Locality>, DataServiceError>) -> Unit) {
        val api = API(APIType.Request.Locality(geometry))

        val request = AppRequest<List<Locality>>(
            object: TypeToken<List<Locality>>() {}.type,
            api,
            null,
            Response.Listener {
                completion(Result.Success(it))
            },
            Response.ErrorListener {
                Log.d("DataService", it.toString())
                completion(Result.Error(DataServiceError.NoInternetError()))
            }
        )

        addToRequestQueue(request)
    }

    fun getSubstrateGroups(completion: (Result<List<SubstrateGroup>, DataServiceError>) -> Unit) {
        val api = API(APIType.Request.Substrate())

        val request = AppRequest<List<Substrate>>(
            object: TypeToken<List<Substrate>>() {}.type,
            api,
            null,
            Response.Listener {
                var substrateGroups = mutableListOf<SubstrateGroup>()

                it.forEach { substrate ->
                    val substrateGroup = substrateGroups.firstOrNull { it.dkName == substrate.groupDkName }

                    if (substrateGroup != null) {
                        substrateGroup.appendSubstrate(substrate)
                    } else {
                        substrateGroups.add(SubstrateGroup(substrate.groupDkName, substrate.groupEnName, mutableListOf(substrate)))
                    }
                }

                Log.d("Dataserver", substrateGroups.toString())
                completion(Result.Success(substrateGroups))

            },
            Response.ErrorListener {
                completion(Result.Error(DataServiceError.NoInternetError()))
            }
        )

        addToRequestQueue(request)
    }

    fun getVegetationTypes(completion: (Result<List<VegetationType>, DataServiceError>) -> Unit) {
        val api = API(APIType.Request.VegetationType())
        val request = AppRequest<List<VegetationType>>(
            object: TypeToken<List<VegetationType>>() {}.type,
            api,
            null,
            Response.Listener {
                completion(Result.Success(it))
            },

            Response.ErrorListener {
                completion(Result.Error(DataServiceError.NoInternetError()))
            }
        )

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