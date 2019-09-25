package com.noque.svampeatlas.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "hosts")
data class Host(
    @PrimaryKey
    @SerializedName("_id") val id: Int,
    @SerializedName("DKname") val dkName: String,
    @SerializedName("LatinName") val latinName: String,
    @SerializedName("probability") val probability: Int?
)