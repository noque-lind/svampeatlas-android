package com.noque.svampeatlas.extensions

import android.widget.ImageView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.noque.svampeatlas.R
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.utilities.GlideApp



fun ImageView.downloadImage(size: DataService.ImageSize,
                            url: String, useThumbnail: Boolean = true, roundedCorners: Int? = null, circular: Boolean = false) {
    val circularProgressDrawable = CircularProgressDrawable(context)
    circularProgressDrawable.strokeWidth = 5F
    circularProgressDrawable.centerRadius = 30F
    circularProgressDrawable.start()


    if (size == DataService.ImageSize.FULL) {
        if (useThumbnail) {
            val thumbnailRequest = GlideApp.with(context).load("${DataService.ImageSize.MINI.value}$url").onlyRetrieveFromCache(true)
            GlideApp
                .with(context)
                .load(url)
                .thumbnail(thumbnailRequest)
                .into(this)

        } else {
            GlideApp
                .with(context)
                .load(url)
                .into(this)
        }
    } else {

        val options: RequestOptions

        if (circular) {
            options = RequestOptions().circleCrop()
            .placeholder(circularProgressDrawable)
        } else if (roundedCorners != null) {
            options = RequestOptions().centerCrop().dontTransform()
            .placeholder(circularProgressDrawable)
                .transform(CenterCrop(), RoundedCorners(roundedCorners))
        } else {
            options = RequestOptions().centerCrop().dontTransform()
                .placeholder(circularProgressDrawable)
        }


        GlideApp.with(context)
            .applyDefaultRequestOptions(options)
            .load("${size.value}$url")
            .into(this)
    }
}