package com.noque.svampeatlas.Model

import com.google.gson.annotations.SerializedName

data class Host(
    @SerializedName("_id") val id: Int,
    @SerializedName("DKname") val dkName: String,
    @SerializedName("LatinName") val latinName: String,
    @SerializedName("probability") val probability: Int?
)