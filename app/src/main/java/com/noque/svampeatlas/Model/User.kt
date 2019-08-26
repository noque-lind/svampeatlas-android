package com.noque.svampeatlas.Model

import com.google.gson.annotations.SerializedName

class User(
    @SerializedName("_id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("Initialer") val initials: String,
    @SerializedName("email") val email: String,
    @SerializedName("facebook") val facebookID: String?
) {

    val imageURL: String? get() {
        facebookID?.let {
            return "https://graph.facebook.com/${it}/picture?width=250&height=250"
        }
        return null
    }
}