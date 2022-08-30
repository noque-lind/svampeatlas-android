package com.noque.svampeatlas.models

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.google.gson.Gson

class PredictionResult(
    @SerializedName("_id") private val id: Int,
    @SerializedName("score") val score: Double,
    @SerializedName("acceptedTaxon") private val acceptedTaxon: AcceptedTaxon,
    @SerializedName("Vernacularname_DK") private val vernacularNameDK: VernacularNameDK?,
    @SerializedName("attributes") private val attributes: Attributes?,
    @SerializedName("Images") private val images: List<Image>?
) {

    companion object {
        fun getNotes(
            selectedPrediction: PredictionResult?,
            predictionsResults: List<PredictionResult>
        ): String {
            var string = ""

            if (selectedPrediction != null) {
                string += "#imagevision_score: ${String.format(
                    "%.2f",
                    selectedPrediction.score * 100
                ).replace(",", ".")}; "
            }

            string += "#imagevision_list: "

            predictionsResults.forEach {
                string += "${it.mushroom.fullName} (${String.format("%.2f", it.score * 100).replace(",", ".")}), "
            }

            string = string.dropLast(2)
            return string
        }
    }

    val mushroom: Mushroom get() {
        return Mushroom(id,
            acceptedTaxon.fullName,
            null,
            null,
            null,
            null,
            null,
            vernacularNameDK,
            attributes,
            null,
            null,
            images)
    }
}

