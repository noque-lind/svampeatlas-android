package com.noque.svampeatlas.services

import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.models.*
import org.json.JSONObject
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.getBitmap
import com.noque.svampeatlas.extensions.rotate
import com.noque.svampeatlas.extensions.toBase64
import com.noque.svampeatlas.extensions.toJPEG
import com.noque.svampeatlas.utilities.*
import com.noque.svampeatlas.utilities.api.*
import com.noque.svampeatlas.utilities.volleyRequests.AppEmptyRequest
import com.noque.svampeatlas.utilities.volleyRequests.AppJSONObjectRequest
import com.noque.svampeatlas.utilities.volleyRequests.AppMultipartPost
import com.noque.svampeatlas.utilities.volleyRequests.AppRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DataService private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: DataService? = null
        val TAG = "DataService"

        fun getInstance(context: Context): DataService {
            return INSTANCE ?: synchronized(this) {
                return DataService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    sealed class Error(title: String, message: String) : AppError(title, message, null) {
        class VolleyError(title: String, message: String) : Error(title, message)
        class SearchResponseEmpty(context: Context) : Error(
            context.getString(R.string.error_dataService_searchResponseEmpty_title),
            context.getString(R.string.error_dataService_searchResponseEmpty_message)
        )

        class LoginError(context: Context) : Error(
            context.getString(R.string.error_dataService_loginError_title),
            context.getString(R.string.error_dataService_loginError_message)
        )

        class NotFound(context: Context) : Error(
            context.getString(R.string.error_dataService_empty_title),
            context.getString(R.string.error_dataService_empty_message)
        )

        class UnknownError(context: Context) : Error(context.getString(R.string.error_dataService_unknown_title), context.getString(R.string.error_dataService_unknown_message))
        class ExtractionError() : Error("", "")
    }

    enum class ImageSize(val value: String) {
        FULL(""), MINI("https://svampe.databasen.org/unsafe/175x175/")
    }

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext)
    }

    private fun <RequestType> addToRequestQueue(request: Request<RequestType>) {
        requestQueue.add(request)
    }

    private val applicationContext = context.applicationContext

    fun getMushroom(tag: String, id: Int, completion: (Result<Mushroom, Error>) -> Unit) {
        val api = API(APIType.Request.Mushroom(id))
        val request = AppRequest<List<Mushroom>>(object : TypeToken<List<Mushroom>>() {}.type,
            api,
            null,
            null,
            Response.Listener {
                if (it.firstOrNull() != null) completion(Result.Success(it.first())) else completion(
                    Result.Error(Error.NotFound(applicationContext))
                )
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            })

        request.tag = tag
        addToRequestQueue(request)
    }

    fun testDownload(): Long {
        val api = API(APIType.Request.Mushrooms(null, listOf(SpeciesQueries.Attributes(null), SpeciesQueries.Images(false), SpeciesQueries.Statistics, SpeciesQueries.DanishNames), 0, null))
       var request = DownloadManager.Request(Uri.parse(api.url()))
        request.setAllowedOverRoaming(false)
        val file = FileManager.createDocumentFile(api.url(), applicationContext).toUri()
        Log.d(TAG, file.toString())
        request.setDestinationUri(file)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.setTitle("Test mushroom")
        request.setDescription("Henter alle svampene")
        val downloadService = applicationContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
       return downloadService.enqueue(request)

    }

    fun getMushrooms(
        tag: String,
        searchString: String?,
        queries: List<SpeciesQueries>,
        offset: Int = 0,
        limit: Int = 100,
        completion: (Result<List<Mushroom>, Error>) -> Unit
    ) {
        val request = AppRequest<List<Mushroom>>(object : TypeToken<List<Mushroom>>() {}.type,
            API(
                APIType.Request.Mushrooms(
                    searchString,
                    queries,
                    offset,
                    limit
                )
            ),
            null,
            null,
            Response.Listener {
                if (it.isEmpty()) {
                    completion(Result.Error(Error.SearchResponseEmpty(applicationContext)))
                } else {
                    completion(Result.Success(it))
                }
            },
            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            })
        request.tag = tag
        addToRequestQueue(request)
    }

    fun getObservationsWithin(
        tag: String,
        geometry: Geometry,
        taxonID: Int? = null,
        ageInYear: Int? = null,
        completion: (Result<List<Observation>, AppError>) -> Unit
    ) {
        val api = API(
            APIType.Request.Observation(
                geometry,
                listOf(
                    ObservationQueries.Locality,
                    ObservationQueries.DeterminationView(taxonID),
                    ObservationQueries.Images,
                    ObservationQueries.GeomNames,
                    ObservationQueries.User(null)
                ),
                ageInYear,
                null,
                null
            )
        )

        val request = AppRequest<List<Observation>>(
            object : TypeToken<List<Observation>>() {}.type,
            api,
            null,
            null,
            Response.Listener {
                completion(Result.Success(it))
            },
            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            })

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getRecentObservations(
        tag: String,
        offset: Int,
        taxonID: Int,
        completion: (Result<List<Observation>, Error>) -> Unit
    ) {
        val api = API(
            APIType.Request.Observation(
                null,
                listOf(
                    ObservationQueries.Images,
                    ObservationQueries.DeterminationView(taxonID),
                    ObservationQueries.GeomNames,
                    ObservationQueries.Locality,
                    ObservationQueries.User(null)
                ),
                null,
                10,
                offset
            )
        )

        val request = AppRequest<List<Observation>>(
            object : TypeToken<List<Observation>>() {}.type,
            api,
            null,
            null,
            Response.Listener {
                completion(Result.Success(it))
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getLocalities(
        tag: String,
        coordinate: LatLng,
        radius: API.Radius = API.Radius.SMALLEST,
        completion: (Result<List<Locality>, Error>) -> Unit
    ) {

        if (radius == API.Radius.COUNTRY) {
            val api = API(APIType.Request.GeomNames(coordinate))

            val request = AppRequest<GeoNames>(
                object : TypeToken<GeoNames>() {}.type,
                api,
                null,
                null,
                Response.Listener {
                    val localities = it.geoNames.map {
                        Locality(
                            it.geonameId,
                            "${it.name}, ${it.countryName}",
                            null,
                            it.lat.toDouble(),
                            it.lng.toDouble(),
                            it
                        )
                    }
                    completion(Result.Success(localities))
                },
                Response.ErrorListener {
                    completion(Result.Error(parseVolleyError(it)))
                }
            )

            request.tag = tag
            addToRequestQueue(request)

        } else {
            val api = API(
                APIType.Request.Locality(
                    Geometry(
                        coordinate,
                        radius.radius,
                        Geometry.Type.RECTANGLE
                    )
                )
            )


            val request = AppRequest<List<Locality>>(
                object : TypeToken<List<Locality>>() {}.type,
                api,
                null,
                null,
                Response.Listener {
                    if (it.count() <= 3) {
                        val newRadius: API.Radius

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

                        getLocalities(tag, coordinate, newRadius, completion)
                    } else {
                        completion(Result.Success(it))
                    }
                },
                Response.ErrorListener {
                    completion(Result.Error(parseVolleyError(it)))
                }
            )

            request.tag = tag
            addToRequestQueue(request)
        }
    }

    fun getSubstrateGroups(tag: String, completion: (Result<List<SubstrateGroup>, Error>) -> Unit) {
        val api = API(APIType.Request.Substrate())

        val request = AppRequest<List<Substrate>>(
            object : TypeToken<List<Substrate>>() {}.type,
            api,
            null,
            null,
            Response.Listener {
                GlobalScope.launch {
                    RoomService.substrates.save(it)
                }

                completion(Result.Success(SubstrateGroup.createFromSubstrates(it)))
            },
            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getVegetationTypes(tag: String, completion: (Result<List<VegetationType>, Error>) -> Unit) {
        val api = API(APIType.Request.VegetationType())
        val request = AppRequest<List<VegetationType>>(
            object : TypeToken<List<VegetationType>>() {}.type,
            api,
            null,
            null,
            Response.Listener {
                completion(Result.Success(it.sortedBy { it.id }))
                GlobalScope.launch {
                    RoomService.vegetationTypes.save(it)
                }
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getHosts(
        tag: String,
        searchString: String?,
        completion: (Result<List<Host>, Error>) -> Unit
    ) {
        val api = API(APIType.Request.Host(searchString))
        val request = AppRequest<List<Host>>(
            object : TypeToken<List<Host>>() {}.type,
            api,
            null,
            null,
            Response.Listener {
                if (searchString == null) {
                    GlobalScope.launch {
                        RoomService.hosts.saveHosts(it)
                    }
                    completion(Result.Success(it))
                } else {
                    completion(Result.Success(it.map { Host(it.id, it.dkName, it.latinName, it.probability, true) }))
                }
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun uploadObservation(
        tag: String,
        token: String,
        jsonObject: JSONObject,
        completion: (Result<Int, Error>) -> Unit
    ) {
        val request = AppJSONObjectRequest(
            API(APIType.Post.Observation()),
            token,
            jsonObject,
            Response.Listener {
                val id = it.getInt("_id")
                completion(Result.Success(id))
            },
            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            })

        request.tag = tag
        addToRequestQueue(request)

    }

    fun editObservation(
        tag: String,
        id: Int,
        token: String,
        jsonObject: JSONObject,
        completion: (Result<Int, Error>) -> Unit
    ) {
        val request = AppJSONObjectRequest(
            API(APIType.Put.Observation(id)),
            token,
            jsonObject,
            Response.Listener {
                completion(Result.Success(id))
            },
            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            })

        request.tag = tag
        addToRequestQueue(request)

    }

    suspend fun uploadImages(
        tag: String,
        observationID: Int,
        images: List<File>,
        token: String,
        completion: (Result<Int, Error>) -> Unit
    ) {
        var completedUploads = 0

        val dispatchGroup = DispatchGroup("UploadImages")

        images.forEach {
            dispatchGroup.enter()

            uploadImage(tag, observationID, it, token) {
                when (it) {
                    is Result.Success -> {
                        completedUploads += 1
                    }
                }
                dispatchGroup.leave()
            }
        }

        dispatchGroup.notify (Runnable {
            completion(Result.Success(completedUploads))
        })
    }

    suspend fun deleteObservation(
        tag: String,
        id: Int,
        token: String,
        completion: (Result<Void?, Error>) -> Unit) = withContext(Dispatchers.IO) {
        val api = API(APIType.Delete.Observation(id))
        val request = AppEmptyRequest(
            api,
            token,
            Response.Listener<Void> {
                completion(Result.Success(null))
            },
            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            }
        )
        request.tag = tag
        addToRequestQueue(request)
    }

    suspend fun deleteImage(
        tag: String,
        id: Int,
        token: String,
        completion: (Result<Void?, Error>) -> Unit
    ) = withContext(Dispatchers.IO) {
        val api = API(APIType.Delete.Image(id))
        val request = AppEmptyRequest(
            api,
            token,
            Response.Listener<Void> {
                completion(Result.Success(null))
            },
            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            }
        )
        request.tag = tag
        addToRequestQueue(request)
    }

    private suspend fun uploadImage(
        tag: String,
        observationID: Int,
        image: File,
        token: String,
        completion: (Result<Boolean, AppError>) -> Unit
    ) = withContext(Dispatchers.IO) {
        val result = image.getBitmap()
        result.onError { completion(Result.Error(it)) }
        result.onSuccess {

            val api = API(APIType.Post.Image(observationID))
            val multipartFormImage = MultipartFormImage(it.rotate(image).toJPEG(0.6), "file")
            val request = AppMultipartPost(api,
                token,
                multipartFormImage,
                Response.Listener {
                    image.delete()
                    completion(Result.Success(true))
                },
                Response.ErrorListener {
                    image.delete()
                    completion(Result.Error(parseVolleyError(it)))
                }
            )

            request.tag = tag
            request.retryPolicy = DefaultRetryPolicy(
                20000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            addToRequestQueue(request)
        }
    }

    suspend fun getPredictions(
        imageFile: File,
        completion: (Result<List<PredictionResult>, AppError>) -> Unit
    ) = withContext(Dispatchers.IO) {
        val bitmapResult = imageFile.getBitmap()
        bitmapResult.onError { completion(Result.Error(it)) }
        bitmapResult.onSuccess {
            val mutableMap: MutableMap<Any?, Any?> = mutableMapOf(
                Pair(
                    "instances",
                    listOf(
                        mapOf(
                            Pair(
                                "image_in",
                                mapOf(Pair("b64", it.rotate(imageFile).toBase64()))
                            )
                        )
                    )
                )
            )
            val jsonObject = JSONObject(mutableMap)
            val request = AppRequest<List<PredictionResult>>(
                object : TypeToken<List<PredictionResult>>() {}.type,
                API(APIType.Post.ImagePrediction()),
                null,
                jsonObject,
                Response.Listener {
                    completion(Result.Success(it))
                },

                Response.ErrorListener {
                    completion(Result.Error(parseVolleyError(it)))
                })


            request.retryPolicy =
                DefaultRetryPolicy(
                    20000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
            addToRequestQueue(request)
        }
    }

    fun login(initials: String, password: String, completion: (Result<String, Error>) -> Unit) {
        val api = API(APIType.Post.Login())
        val jsonObject = JSONObject()
        jsonObject.put("Initialer", initials)
        jsonObject.put("password", password)

        val request = AppJSONObjectRequest(api,
            null,
            jsonObject,

            Response.Listener {
                val token = it.getString("token")
                completion(Result.Success(token))
            },

            Response.ErrorListener {
                if (it is AuthFailureError) completion(
                    Result.Error(
                        Error.LoginError(
                            applicationContext
                        )
                    )
                ) else completion(Result.Error(parseVolleyError(it)))
            })

        addToRequestQueue(request)
    }

    fun getUser(tag: String, token: String, completion: (Result<User, Error>) -> Unit) {
        val api = API(APIType.Request.User())
        val request = AppRequest<User>(object : TypeToken<User>() {}.type,
            api,
            token,
            null,
            Response.Listener {
                //                saveUser(it)
                completion(Result.Success(it))
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            })

        request.tag = tag
        addToRequestQueue(request)
    }


    fun getUserNotificationCount(
        tag: String,
        token: String,
        completion: (Result<Int, Error>) -> Unit
    ) {
        val api = API(APIType.Request.UserNotificationCount())
        val request = object : JsonObjectRequest(api.volleyMethod(),
            api.url(),
            null,
            Response.Listener {
                val count = it.getInt("count")
                completion(Result.Success(count))
            },
            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(Pair("Authorization", "Bearer ${token}"))
            }
        }

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getNotifications(
        tag: String,
        token: String,
        limit: Int,
        offset: Int,
        completion: (Result<List<Notification>, Error>) -> Unit
    ) {
        val api = API(APIType.Request.UserNotifications(limit, offset))
        val request = AppRequest<GsonNotifications>(
            object : TypeToken<GsonNotifications>() {}.type,
            api,
            token,
            null,
            Response.Listener {
                Log.d(TAG, it.toString())
                completion(Result.Success(it.results))
            },
            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getObservation(tag: String, id: Int, completion: (Result<Observation, Error>) -> Unit) {
        val api = API(APIType.Request.SingleObservation(id))

        val request = AppRequest<Observation>(
            object : TypeToken<Observation>() {}.type,
            api,
            null,
            null,
            Response.Listener {
                completion(Result.Success(it))
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getObservationCountForUser(
        tag: String,
        userId: Int,
        completion: (Result<Int, Error>) -> Unit
    ) {
        val api = API(APIType.Request.ObservationCountForUser(userId))

        val request = JsonArrayRequest(
            api.volleyMethod(),
            api.url(),
            null,
            {
                completion(Result.Success(it.getJSONObject(0).getInt("count")))
            },

            {
                completion(Result.Error(parseVolleyError(it)))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }


    fun getObservationsForUser(
        tag: String,
        userId: Int,
        offset: Int,
        limit: Int,
        completion: (Result<List<Observation>, Error>) -> Unit
    ) {
        val api = API(
            APIType.Request.Observation(
                null,
                listOf(
                    ObservationQueries.User(userId),
                    ObservationQueries.Images,
                    ObservationQueries.GeomNames,
                    ObservationQueries.Locality,
                    ObservationQueries.DeterminationView(null),
                    ObservationQueries.Comments
                )
                , null,
                limit, offset
            )
        )

        val request = AppRequest<List<Observation>>(
            object : TypeToken<List<Observation>>() {}.type,
            api,
            null,
            null,
            Response.Listener {
                completion(Result.Success(it))
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun postComment(
        observationID: Int,
        comment: String,
        token: String,
        completion: (Result<Comment, Error>) -> Unit
    ) {
        val api = API(APIType.Post.Comment(observationID))

        val jsonObject = JSONObject().put("content", comment)

        val request = AppJSONObjectRequest(
            api,
            token,
            jsonObject,

            Response.Listener {
                val id = it.optInt("_id")
                val date = it.optString("createdAt")
                val content = it.optString("content")
                val user = it.optJSONObject("User")
                val name = user?.optString("name")
                val facebook = it.optString("facebook")
                val initials = it.optString("Initialer")

                if (!name.isNullOrEmpty() && !date.isNullOrEmpty() && !content.isNullOrEmpty() && !name.isNullOrEmpty()) {
                    completion(Result.Success(Comment(id, date, content, name, initials, facebook)))
                } else {
                    completion(Result.Error(Error.ExtractionError()))
                }
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            }
        )

        addToRequestQueue(request)
    }

    fun markNotificationAsRead(tag: String, notificationID: Int, token: String) {
        val api = API(APIType.Put.NotificationLastRead(notificationID))
        val request = AppJSONObjectRequest(
            api,
            token,
            JSONObject(),
            Response.Listener {},
            Response.ErrorListener {}
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun clearRequestsWithTag(tag: String) {
        requestQueue.cancelAll(tag)
    }

    fun postOffensiveComment(tag: String, observationID: Int, json: JSONObject, token: String) {
        val api = API(APIType.Post.OffensiveContentComment(observationID))
        val request = AppJSONObjectRequest(
            api,
            token,
            json,
            Response.Listener {  },
            Response.ErrorListener {  }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    private fun parseVolleyError(error: VolleyError): Error {
        when (error) {
            is AuthFailureError -> {
                return Error.VolleyError(
                    applicationContext.getString(R.string.error_network_unAuthorized_title),
                    applicationContext.getString(R.string.error_network_unAuthorized_message)
                )
            }

            is NoConnectionError -> {
                return Error.VolleyError(
                    applicationContext.getString(R.string.error_network_noInternet_title),
                    applicationContext.getString(R.string.error_network_noInternet_message)
                )
            }

            is TimeoutError -> {
                return Error.VolleyError(
                    applicationContext.getString(R.string.error_network_timeout_title),
                    applicationContext.getString(R.string.error_network_timeout_message)
                )
            }

            is ServerError -> {
                return Error.VolleyError(
                    applicationContext.getString(R.string.error_network_serverError_title),
                    applicationContext.getString(R.string.error_network_serverError_message)
                )
            }

            is ParseError -> {
                return Error.VolleyError(
                    applicationContext.getString(R.string.error_network_invalidResponse_title),
                    applicationContext.getString(R.string.error_network_invalidResponse_message)
                )
            }
        }

        return Error.UnknownError(applicationContext)
    }
}