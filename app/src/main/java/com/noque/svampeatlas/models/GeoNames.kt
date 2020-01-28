package com.noque.svampeatlas.models

import com.google.gson.annotations.SerializedName

data class GeoNames(
    @SerializedName("geonames") val geoNames: List<GeoName>
)
