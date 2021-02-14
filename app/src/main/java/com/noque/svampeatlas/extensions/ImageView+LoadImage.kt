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
                            url: String, useThumbnail: Boolean = true) {
    val circularProgressDrawable = CircularProgressDrawable(context)
    circularProgressDrawable.strokeWidth = 1F
    circularProgressDrawable.centerRadius = 15F
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
        GlideApp.with(context)
            .load("${size.value}$url")
            .placeholder(circularProgressDrawable)
            .into(this)
    }
}