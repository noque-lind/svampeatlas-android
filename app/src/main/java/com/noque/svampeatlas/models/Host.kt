package com.noque.svampeatlas.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.noque.svampeatlas.extensions.AppLanguage
import com.noque.svampeatlas.extensions.appLanguage
import com.noque.svampeatlas.extensions.capitalized
import java.util.*

@Entity(tableName = "hosts")
data class Host(
    @PrimaryKey
    @SerializedName("_id") val id: Int,
    @SerializedName("DKname") val dkName: String?,
    @SerializedName("LatinName") val latinName: String,
    @SerializedName("probability") val probability: Int?,
    val isUserSelected: Boolean = false
) {
    val localizedName: String? get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> dkName?.capitalized()
            else -> null
        }
    }
}