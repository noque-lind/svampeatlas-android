package com.noque.svampeatlas.services

import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.noque.svampeatlas.extensions.getBitmap
import com.noque.svampeatlas.extensions.rotate
import com.noque.svampeatlas.extensions.toAppError
import com.noque.svampeatlas.extensions.toJPEG
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.utilities.MultipartFormImage
import com.noque.svampeatlas.utilities.api.API
import com.noque.svampeatlas.utilities.api.APIType
import com.noque.svampeatlas.utilities.volleyRequests.AppMultipartPost
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ImageService private constructor() {
    companion object {
        suspend fun uploadImage(
            requestQueue: RequestQueue,
            observationID: Int,
            image: File,
            token: String
        ): Result<Void?, AppError> {
            return when (val bitmapResult = image.getBitmap()) {
                is Result.Error -> {
                    Result.Error(bitmapResult.error)
                }
                is Result.Success -> {
                    val byteArray = bitmapResult.value.rotate(image).toJPEG(0.6)
                    return suspendCoroutine { cont ->
                        val multipartFormImage = MultipartFormImage(byteArray, "file")
                        val request = AppMultipartPost(
                            API(APIType.Post.Image(observationID)),
                            token,
                            multipartFormImage,
                            {
                                cont.resume(Result.Success(null))
                            },
                            {
                                cont.resume((Result.Error(it.toAppError())))
                            }
                        )

                        request.retryPolicy = DefaultRetryPolicy(
                            20000,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                        )
                        requestQueue.add(request)
                    }
                }
            }
        }
    }
}