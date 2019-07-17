package com.noque.svampeatlas.Extensions

import android.widget.ImageView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.noque.svampeatlas.R
import com.noque.svampeatlas.Services.DataService
import com.noque.svampeatlas.Utilities.GlideApp



fun ImageView.downloadImage(size: DataService.IMAGESIZE,
                            url: String?) {

    val circularProgressDrawable = CircularProgressDrawable(context)
    circularProgressDrawable.strokeWidth = 5F
    circularProgressDrawable.centerRadius = 30F
    circularProgressDrawable.start()


    val options = RequestOptions()
        .placeholder(circularProgressDrawable)
        .error(R.mipmap.ic_launcher_round)


    GlideApp.with(context)
        .setDefaultRequestOptions(options)
        .load("${size.value}${url}")
        .into(this)
}