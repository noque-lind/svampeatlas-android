package com.noque.svampeatlas.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "substrates")
data class Substrate(
    @PrimaryKey
    @SerializedName("_id") val id: Int,
    @SerializedName("name") val dkName: String,
    @SerializedName("name_uk") val enName: String,
    @SerializedName("group_dk") val groupDkName: String,
    @SerializedName("group_uk") val groupEnName: String)