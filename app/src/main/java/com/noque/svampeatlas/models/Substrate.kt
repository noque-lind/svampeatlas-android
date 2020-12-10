package com.noque.svampeatlas.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.noque.svampeatlas.extensions.AppLanguage
import com.noque.svampeatlas.extensions.appLanguage
import com.noque.svampeatlas.extensions.capitalized
import java.util.*

@Entity(tableName = "substrates")
data class Substrate(
    @PrimaryKey
    @SerializedName("_id") val id: Int,
    @SerializedName("name") val dkName: String,
    @SerializedName("name_uk") val enName: String,
    @SerializedName("name_cz") val czName: String,
    @SerializedName("group_dk") val groupDkName: String,
    @SerializedName("group_uk") val groupEnName: String,
    @SerializedName("group_cz") val groupCzName: String?) {

    val localizedName: String get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> dkName.capitalized()
            AppLanguage.English -> enName.capitalized()
            AppLanguage.Czech -> groupCzName?.capitalized() ?: enName.capitalized()
        }
    }

}