package com.noque.svampeatlas.Model

import com.google.gson.annotations.SerializedName

data class VegetationType(
    @SerializedName("_id") val id: Int,
    @SerializedName("name") val dkName: String,
    @SerializedName("name_uk") val enName: String)