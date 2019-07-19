package com.noque.svampeatlas.Model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Image(
    @SerializedName("thumburi") private val _thumburi: String?,
    @SerializedName("uri") private val _uri: String?,
    @SerializedName("photographer") private val _photographer: String?
) {

    val url: String get() {return _uri?:""}
}