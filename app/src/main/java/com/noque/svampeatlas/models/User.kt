package com.noque.svampeatlas.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "user")
class User(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("_id") val id: Int,

    @ColumnInfo(name = "content")
    @SerializedName("name") val name: String,

    @ColumnInfo(name = "initials")
    @SerializedName("Initialer") val initials: String,

    @ColumnInfo(name = "email")
    @SerializedName("email") val email: String,

    @ColumnInfo(name = "facebook_id")
    @SerializedName("facebook") val facebookID: String?
) {

    val imageURL: String? get() {
        facebookID?.let {
            return "https://graph.facebook.com/${it}/picture?width=250&height=250"
        }
        return null
    }
}