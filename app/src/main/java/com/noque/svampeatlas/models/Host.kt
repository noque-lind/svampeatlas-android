package com.noque.svampeatlas.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.noque.svampeatlas.extensions.capitalized
import com.noque.svampeatlas.extensions.isDanish
import java.util.*

@Entity(tableName = "hosts")
data class Host(
    @PrimaryKey
    @SerializedName("_id") val id: Int,
    @SerializedName("DKname") val dkName: String?,
    @SerializedName("LatinName") val latinName: String,
    @SerializedName("probability") val probability: Int?
) {
    val localizedName: String? get() {
        return if (Locale.getDefault().isDanish() && dkName != null) {
            dkName.capitalized()
        } else {
            null
        }
    }
}