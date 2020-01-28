package com.noque.svampeatlas.models

data class GeoName(
    val geonameId: Int,
    val name: String,
    val countryName: String,
    val lat: String,
    val lng: String,
    val countryCode: String,
    val fcodeName: String,
    val fclName: String,
    val adminName1: String
)