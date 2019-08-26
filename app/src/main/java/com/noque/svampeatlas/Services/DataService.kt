package com.noque.svampeatlas.Services

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.Model.*
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.lang.reflect.Type
import java.nio.charset.Charset
import com.android.volley.VolleyError
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.AuthFailureError
import com.google.android.gms.common.util.IOUtils.toByteArray
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.Utilities.*
import com.noque.svampeatlas.ViewModel.MultipartFormImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.util.*


const val BASE_URL = "https://us-central1-apis-4674e.cloudfunctions.net"
const val URL_REGISTER = "{$BASE_URL}account/register"

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

class AppPostRequest(private val endpoint: API,
                     private val token: String?,
                     private val jsonObject: JSONObject,
                     private val listener: Response.Listener<JSONObject>,
                     errorListener: Response.ErrorListener): Request<JSONObject>(endpoint.volleyMethod(), endpoint.url(), errorListener) {

    override fun getBody(): ByteArray {
        val byteArray = jsonObject.toString().toByteArray()
        Log.d("POST reqeust", jsonObject.toString())
        return byteArray
    }


    override fun getHeaders(): MutableMap<String, String> {
        val mutableMap = mutableMapOf(Pair("Content-Type", "application/json"))

        token?.let {
            mutableMap.put("Authorization", "Bearer $it")
        }

        return mutableMap
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
        return try {
            val json = String(response?.data ?: ByteArray(0),
            Charset.forName(HttpHeaderParser.parseCharset(response?.headers)))

        Response.success(JSONObject(json), HttpHeaderParser.parseCacheHeaders(response))

    } catch (error: UnsupportedEncodingException) {
            Response.error(ParseError(error))
        } catch (error: JsonSyntaxException) {
            Response.error(ParseError(error))
        }
    }

    override fun deliverResponse(response: JSONObject?) {
        listener.onResponse(response)
    }

}


class AppGetRequest<T>(private val type: Type, private val endpoint: API,
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
        class ExtractionError(): DataServiceError("", "")
    }


    enum class IMAGESIZE(val value: String) {
        FULL(""), MINI("https://svampe.databasen.org/unsafe/175x175/")
    }


    companion object {
        @Volatile
        private var INSTANCE: DataService? = null
        val TAG = "DataService"

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

        val request = AppGetRequest<List<Mushroom>>(object: TypeToken<List<Mushroom>>() {}.type,
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

        val request = AppGetRequest<List<Mushroom>>(object: TypeToken<List<Mushroom>>(){}.type,
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
                ObservationQueries.Locality(),
                ObservationQueries.DeterminationView(taxonID)
            ),
            ageInYear,
            null,
            null
            ))

       val request = AppGetRequest<List<Observation>>(
           object: TypeToken<List<Observation>>() {}.type,
        api,
         null,
           Response.Listener {
               completion(Result.Success(it))
           },
           Response.ErrorListener {
               Log.d("Dataservice", it.toString())
               completion(Result.Error(DataServiceError.NoInternetError()))
           })

        request.setRetryPolicy(DefaultRetryPolicy(50000, 2, 10F))
        addToRequestQueue(request)
    }

    fun getLocalities(coordinate: LatLng, radius: API.Radius = API.Radius.SMALLEST, completion: (Result<List<Locality>, DataServiceError>) -> Unit) {
        val api = API(APIType.Request.Locality(Geometry(coordinate, radius.radius, Geometry.Type.RECTANGLE)))

        val request = AppGetRequest<List<Locality>>(
            object: TypeToken<List<Locality>>() {}.type,
            api,
            null,
            Response.Listener {
                if (it.count() <= 3) {
                    var newRadius: API.Radius

                    when (radius) {
                        API.Radius.SMALLEST -> newRadius = API.Radius.SMALLER
                        API.Radius.SMALLER -> newRadius = API.Radius.SMALL
                        API.Radius.SMALL -> newRadius = API.Radius.MEDIUM
                        API.Radius.MEDIUM -> newRadius = API.Radius.LARGE
                        API.Radius.LARGE -> newRadius = API.Radius.LARGER
                        API.Radius.LARGER -> newRadius = API.Radius.LARGEST
                        API.Radius.LARGEST -> newRadius = API.Radius.HUGE
                        API.Radius.HUGE -> newRadius = API.Radius.HUGER
                        API.Radius.HUGER -> newRadius = API.Radius.HUGEST
                        API.Radius.HUGEST -> {
                            if (it.count() != 0) {
                                (completion(Result.Success(it)))
                                return@Listener
                            } else {
                                newRadius = API.Radius.COUNTRY
                            }
                        }
                        API.Radius.COUNTRY -> return@Listener
                    }

                    getLocalities(coordinate, newRadius, completion)
                } else {
                    completion(Result.Success(it))
                }
            },
            Response.ErrorListener {
                completion(Result.Error(DataServiceError.NoInternetError()))
            }
        )

        addToRequestQueue(request)
    }

    fun getSubstrateGroups(completion: (Result<List<SubstrateGroup>, DataServiceError>) -> Unit) {
        val api = API(APIType.Request.Substrate())

        val request = AppGetRequest<List<Substrate>>(
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
        val request = AppGetRequest<List<VegetationType>>(
            object: TypeToken<List<VegetationType>>() {}.type,
            api,
            null,
            Response.Listener {
                completion(Result.Success(it.sortedBy {it.id}))
            },

            Response.ErrorListener {
                completion(Result.Error(DataServiceError.NoInternetError()))
            }
        )

        addToRequestQueue(request)
    }

    fun getHosts(searchString: String?, completion: (Result<List<Host>, DataServiceError>) -> Unit) {
        val api = API(APIType.Request.Host(null))
        val request = AppGetRequest<List<Host>>(
            object: TypeToken<List<Host>>() {}.type,
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

    fun uploadObservation(jsonObject: JSONObject, images: List<Bitmap>?, completion: (Result<Pair<Int, Int>, DataServiceError>) -> Unit) {
        val api = API(APIType.Post.Observation())
        val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJfaWQiOjI3MDYsImlhdCI6MTU1ODYzODk0M30.Uk0TvwQ0R1skKhQ0ZvF7kcXqY4JX9W08isNi3d2-2UA"

        val request = AppPostRequest(api,
            token,
            jsonObject,

            Response.Listener {
                val id = it.getInt("_id")

                if (images != null) {
                    uploadImages(id, images, token) {
                        when (it) {
                            is Result.Success -> { completion(Result.Success(Pair(id, it.value))) }
                            is Result.Error -> { completion(Result.Success(Pair(id, 0))) }
                        }
                    }
                } else {
                    completion(Result.Success(Pair(id, 0)))
                }
            },

            Response.ErrorListener {
                completion(Result.Error(DataServiceError.NoInternetError()))
            })

        addToRequestQueue(request)
    }

    private fun uploadImages(observationID: Int, images: List<Bitmap>, token: String, completion: (Result<Int, DataServiceError>) -> Unit) {
        var completedUploads = 0

        val dispatchGroup = DispatchGroup()

        images.forEach {
            dispatchGroup.enter()

            uploadImage(observationID, it, token) {
                when (it) {
                   is Result.Success -> { completedUploads += 1 }
                }
                dispatchGroup.leave()
            }
        }

        dispatchGroup.notify {
            completion(Result.Success(completedUploads))
        }
    }

    private fun uploadImage(observationID: Int, image: Bitmap, token: String, completion: (Result<Boolean, DataServiceError>) -> Unit) {
        val api = API(APIType.Post.Image(observationID))
        val multipartFormImage = MultipartFormImage(image, "file")
        val request = AppMultipartPost(api,
            token,
            multipartFormImage,
            Response.Listener {
                completion(Result.Success(true))
            },

            Response.ErrorListener {
                completion(Result.Error(DataServiceError.NoInternetError()))
            }
            )


        requestQueue.sequenceNumber
        addToRequestQueue(request)
    }

    fun login(initials: String, password: String, completion: (Result<String, DataServiceError>) -> Unit) {
        val api = API(APIType.Post.Login())
        val jsonObject = JSONObject()
        jsonObject.put("Initialer", initials)
        jsonObject.put("password", password)

        val request = AppPostRequest(api,
            null,
            jsonObject,

            Response.Listener {
                val token = it.getString("token")
                completion(Result.Success(token))
            },

            Response.ErrorListener {
                completion(Result.Error(DataServiceError.NoInternetError()))
            })

        addToRequestQueue(request)
    }

    fun getUser(token: String, completion: (Result<User, DataServiceError>) -> Unit) {
        val api = API(APIType.Request.User())
        val request = AppGetRequest<User>(object: TypeToken<User>() {}.type,
            api,
            token,
            Response.Listener {
                completion(Result.Success(it))
            },

            Response.ErrorListener {
                completion(Result.Error(DataServiceError.NoInternetError()))
            })

        addToRequestQueue(request)
    }

    fun getUserNotificationCount(token: String, completion: (Result<Int, DataServiceError>) -> Unit) {
        val api = API(APIType.Request.UserNotificationCount())
        val request = object: JsonObjectRequest(api.volleyMethod(),
            api.url(),
            null,
            Response.Listener {
                val count = it.getInt("count")
                completion(Result.Success(count))
            },
            Response.ErrorListener {
                Log.d(TAG, it.toString())
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(Pair("Authorization", "Bearer ${token}"))
            }

        }

        addToRequestQueue(request)
    }

    fun getNotifications(token: String, limit: Int, offset: Int, completion: (Result<List<Notification>, DataServiceError>) -> Unit) {
        val api = API(APIType.Request.UserNotifications(limit, offset))
        val request = AppGetRequest<GsonNotifications>(
            object: TypeToken<GsonNotifications>() {}.type,
            api,
            token,
            Response.Listener {
                completion(Result.Success(it.results))
            },
            Response.ErrorListener {

            }
        )

        addToRequestQueue(request)
    }

    fun getObservation(id: Int, completion: (Result<Observation, DataServiceError>) -> Unit) {
        val api = API(APIType.Request.SingleObservation(id))

        val request = AppGetRequest<Observation>(
            object: TypeToken<Observation>() {}.type,
            api,
            null,
            Response.Listener {
                completion(Result.Success(it))
            },

            Response.ErrorListener {

            }
        )
        addToRequestQueue(request)
    }

    fun getObservationCountForUser(userId: Int, completion: (Result<Int, DataServiceError>) -> Unit) {
        val api = API(APIType.Request.ObservationCountForUser(userId))

        val request = JsonArrayRequest(
            api.volleyMethod(),
            api.url(),
            null,
            Response.Listener {
                Log.d(TAG, it.toString())
                completion(Result.Success(it.getJSONObject(0).getInt("count")))
            },

            Response.ErrorListener {
                Log.d(TAG, it.toString())
                completion(Result.Error(parseVolleyError()))
            }
        )

        addToRequestQueue(request)
    }


    fun getObservationsForUser(userId: Int, offset: Int, limit: Int, completion: (Result<List<Observation>, DataServiceError>) -> Unit) {
        val api = API(APIType.Request.Observation(
            null,
            listOf(ObservationQueries.User(userId),
                ObservationQueries.Images(),
                ObservationQueries.GeomNames(),
                ObservationQueries.Locality(),
                ObservationQueries.DeterminationView(null),
                ObservationQueries.Comments())
        ,null,
            limit, offset))

        val request = AppGetRequest<List<Observation>>(
            object: TypeToken<List<Observation>>() {}.type,
            api,
            null,
            Response.Listener {
                completion(Result.Success(it))
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError()))
            }
        )

        addToRequestQueue(request)
    }

    fun postComment(observationID: Int, comment: String, token: String, completion: (Result<Comment, DataServiceError>) -> Unit) {
        val api = API(APIType.Post.Comment(observationID))

        val jsonObject = JSONObject().put("content", comment)

        val request = AppPostRequest(
            api,
            token,
            jsonObject,

            Response.Listener {
                val id = it.optInt("_id")
                val date = it.optString("createdAt")
                val content = it.optString("content")
                val user = it.optJSONObject("User")
                val name = user.optString("name")
                val facebook = it.optString("facebook")
                val initials = it.optString("Initialer")

                if (!name.isNullOrEmpty() && !date.isNullOrEmpty() && !content.isNullOrEmpty() && !name.isNullOrEmpty()) {
                    completion(Result.Success(Comment(id, date, content, name, initials, facebook)))
                } else {
                    completion(Result.Error(DataServiceError.ExtractionError()))
                }
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError()))
            }
            )

        addToRequestQueue(request)
    }


    private fun parseVolleyError(): DataServiceError {
        return DataServiceError.NoInternetError()
    }
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