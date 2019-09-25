package com.noque.svampeatlas.models

import com.google.gson.annotations.SerializedName
import com.google.gson.Gson

class PredictionResult(
    @SerializedName("_id") private val id: Int,
    @SerializedName("score") val score: Double,
    @SerializedName("acceptedTaxon") private val acceptedTaxon: AcceptedTaxon,
    @SerializedName("Vernacularname_DK") private val vernacularNameDK: VernacularNameDK?,
    @SerializedName("Images") private val images: List<Image>?
) {

    val mushroom: Mushroom get() {
        return Mushroom(id,
            acceptedTaxon.fullName,
            null,
            null,
            null,
            null,
            vernacularNameDK,
            null,
            null,
            null,
            images)
    }
}

