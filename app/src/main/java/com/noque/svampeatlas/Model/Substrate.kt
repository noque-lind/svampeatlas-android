package com.noque.svampeatlas.Model

import com.google.gson.annotations.SerializedName

data class Substrate(
    @SerializedName("_id") val id: Int,
    @SerializedName("name") val dkName: String,
    @SerializedName("name_uk") val enName: String,
    @SerializedName("group_dk") val groupDkName: String,
    @SerializedName("group_uk") val groupEnName: String)