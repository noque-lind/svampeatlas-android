package com.noque.svampeatlas.services

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.models.*
import org.json.JSONObject
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.toBase64
import com.noque.svampeatlas.utilities.*
import com.noque.svampeatlas.utilities.MultipartFormImage
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

    sealed class Error(title: String, message: String): AppError(title, message) {
        class VolleyError(title: String, message: String): Error(title, message)
        class SearchResponseEmpty(context: Context): Error(context.getString(R.string.dataServiceError_emptyResponse_title), context.getString(R.string.dataServiceError_emptyResponse_message))
        class LoginError(context: Context): Error(context.getString(R.string.dataServiceError_loginError_title), context.getString(R.string.dataServiceError_loginError_message))
        class NotFound(context: Context): Error(context.getString(R.string.dataServiceError_notFound_title), context.getString(R.string.dataServiceError_notFound_message))
        class UnknownError(): Error("Unknown error", "")
        class ExtractionError(): Error("", "")
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
        val request = AppGetRequest<List<Mushroom>>(object: TypeToken<List<Mushroom>>() {}.type,
            api,
            null,
            null,
            Response.Listener {
                if (it.firstOrNull() != null) completion(Result.Success(it.first())) else completion(Result.Error(Error.NotFound(applicationContext)))
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            })

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getMushrooms(tag: String, searchString: String?, queries: List<SpeciesQueries>, offset: Int = 0, limit: Int = 100, completion: (Result<List<Mushroom>, Error>) -> Unit)  {
        val api = API(APIType.Request.Mushrooms(searchString,
            queries,
            offset,
            limit))

        val request = AppGetRequest<List<Mushroom>>(object: TypeToken<List<Mushroom>>() {}.type,
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

    fun getObservationsWithin(tag: String, geometry: Geometry, taxonID: Int? = null, ageInYear: Int? = null, completion: (Result<List<Observation>, AppError>) -> Unit) {
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

    fun getRecentObservations(tag: String, offset: Int, taxonID: Int, completion: (Result<List<Observation>, Error>) -> Unit) {
        val api = API(APIType.Request.Observation(
            null, listOf(ObservationQueries.Images(), ObservationQueries.DeterminationView(taxonID), ObservationQueries.GeomNames(), ObservationQueries.Locality(), ObservationQueries.User(null)),
            null, 10, offset))

        val request = AppGetRequest<List<Observation>>(
            object: TypeToken<List<Observation>>() {}.type,
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

    fun getLocalities(tag: String, coordinate: LatLng, radius: API.Radius = API.Radius.SMALLEST, completion: (Result<List<Locality>, Error>) -> Unit) {
        val api = API(APIType.Request.Locality(Geometry(coordinate, radius.radius, Geometry.Type.RECTANGLE)))

        val request = AppGetRequest<List<Locality>>(
            object: TypeToken<List<Locality>>() {}.type,
            api,
            null,
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

    fun getSubstrateGroups(tag: String, completion: (Result<List<SubstrateGroup>, Error>) -> Unit) {
        val api = API(APIType.Request.Substrate())

        val request = AppGetRequest<List<Substrate>>(
            object: TypeToken<List<Substrate>>() {}.type,
            api,
            null,
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

                completion(Result.Success(substrateGroups))

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
        val request = AppGetRequest<List<VegetationType>>(
            object: TypeToken<List<VegetationType>>() {}.type,
            api,
            null,
            null,
            Response.Listener {
                completion(Result.Success(it.sortedBy {it.id}))
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getHosts(tag: String, searchString: String?, completion: (Result<List<Host>, Error>) -> Unit) {
        val api = API(APIType.Request.Host(null))
        val request = AppGetRequest<List<Host>>(
            object: TypeToken<List<Host>>() {}.type,
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

    fun uploadObservation(tag: String, token: String, jsonObject: JSONObject, images: List<File>?, completion: (Result<Pair<Int, Int>, Error>) -> Unit) {
        val api = API(APIType.Post.Observation())

        val request = AppJSONObjectRequest(api,
            token,
            jsonObject,

            Response.Listener {
                val id = it.getInt("_id")

                if (images != null) {
                    uploadImages(tag, id, images, token) {
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
                completion(Result.Error(parseVolleyError(it)))
            })

        request.tag = tag
        addToRequestQueue(request)
    }

    private fun uploadImages(tag: String, observationID: Int, images: List<File>, token: String, completion: (Result<Int, Error>) -> Unit) {
        var completedUploads = 0

        val dispatchGroup = DispatchGroup()

        images.forEach {
            dispatchGroup.enter()

            uploadImage(tag, observationID, it, token) {
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

    private fun uploadImage(tag: String, observationID: Int, image: File, token: String, completion: (Result<Boolean, Error>) -> Unit) {
//        val api = API(APIType.Post.Image(observationID))
//        val multipartFormImage = MultipartFormImage(image, "file")
//        val request = AppMultipartPost(api,
//            token,
//            multipartFormImage,
//            Response.Listener {
//                completion(Result.Success(true))
//            },
//
//            Response.ErrorListener {
//                completion(Result.Error(parseVolleyError(it)))
//            }
//            )
//
//        request.tag = tag
//        addToRequestQueue(request)
    }

    fun getPredictions(image: Bitmap, completion: (Result<List<PredictionResult>, Error>) -> Unit) {
        val api = API(APIType.Post.ImagePrediction())
        val mutableMap: MutableMap<Any?, Any?> = mutableMapOf(Pair("instances", listOf(mapOf(Pair("image_in", mapOf(Pair("b64", image.toBase64())))))))
        val jsonObject = JSONObject(mutableMap)

        val request = AppGetRequest<List<PredictionResult>>(
            object: TypeToken<List<PredictionResult>>() {}.type,
            api,
            null,
            jsonObject,
            Response.Listener {
                completion(Result.Success(it.filter { it.score >= 0.10 }))
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            })

        addToRequestQueue(request)
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
                if (it is AuthFailureError) completion (Result.Error(Error.LoginError(applicationContext))) else completion (Result.Error(parseVolleyError(it)))
            })

        addToRequestQueue(request)
    }

    fun getUser(tag: String, token: String, completion: (Result<User, Error>) -> Unit) {
        val api = API(APIType.Request.User())
        val request = AppGetRequest<User>(object: TypeToken<User>() {}.type,
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


    fun getUserNotificationCount(tag: String, token: String, completion: (Result<Int, Error>) -> Unit) {
        val api = API(APIType.Request.UserNotificationCount())
        val request = object: JsonObjectRequest(api.volleyMethod(),
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

    fun getNotifications(tag: String, token: String, limit: Int, offset: Int, completion: (Result<List<Notification>, Error>) -> Unit) {
        val api = API(APIType.Request.UserNotifications(limit, offset))
        val request = AppGetRequest<GsonNotifications>(
            object: TypeToken<GsonNotifications>() {}.type,
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

        val request = AppGetRequest<Observation>(
            object: TypeToken<Observation>() {}.type,
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

    fun getObservationCountForUser(tag: String, userId: Int, completion: (Result<Int, Error>) -> Unit) {
        val api = API(APIType.Request.ObservationCountForUser(userId))

        val request = JsonArrayRequest(
            api.volleyMethod(),
            api.url(),
            null,
            Response.Listener {
                completion(Result.Success(it.getJSONObject(0).getInt("count")))
            },

            Response.ErrorListener {
                completion(Result.Error(parseVolleyError(it)))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }


    fun getObservationsForUser(tag: String, userId: Int, offset: Int, limit: Int, completion: (Result<List<Observation>, Error>) -> Unit) {
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

    fun postComment(observationID: Int, comment: String, token: String, completion: (Result<Comment, Error>) -> Unit) {
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
                val name = user.optString("name")
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

    fun clearRequestsWithTag(tag: String) {
        requestQueue.cancelAll(tag)
    }

    private fun parseVolleyError(error: VolleyError): Error {
            when (error) {
                is AuthFailureError -> {
                    return Error.VolleyError(applicationContext.getString(R.string.volleyError_authError_title), applicationContext.getString(R.string.volleyError_authError_message))
                }

               is NoConnectionError -> {
                   return Error.VolleyError(applicationContext.getString(R.string.volleyError_networkError_title), applicationContext.getString(R.string.volleyError_networkError_message))
               }

                is TimeoutError -> {
                    return Error.VolleyError(applicationContext.getString(R.string.volleyError_timeoutError_title), applicationContext.getString(R.string.volleyError_timeoutError_message))
                }

                is ServerError -> {
                    return Error.VolleyError(applicationContext.getString(R.string.volleyError_serverError_title), applicationContext.getString(R.string.volleyError_serverError_message))
                }

                is ParseError -> {
                    return Error.VolleyError(applicationContext.getString(R.string.volleyError_parseError_title), applicationContext.getString(R.string.volleyError_parseError_message))
                }
            }

        return Error.UnknownError()
    }
}