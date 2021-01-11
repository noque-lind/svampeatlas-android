package com.noque.svampeatlas.models

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.noque.svampeatlas.extensions.Date
import java.util.*
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.extensions.AppLanguage
import com.noque.svampeatlas.extensions.appLanguage
import com.noque.svampeatlas.extensions.capitalized
import java.util.Collections.emptyList

object RedListDataTypeConverters {

    val gson by lazy { Gson() }

    @TypeConverter
    @JvmStatic
    fun toRedListData(data: String?): List<RedListData> {
        if (data == null) {
            return emptyList()
        } else {
            val listType = object: TypeToken<List<RedListData>>() {}.type
            return gson.fromJson(data, listType)
        }
    }

    @TypeConverter
    @JvmStatic
    fun toString(redListData: List<RedListData>?): String? {
        redListData?.let {
            return gson.toJson(redListData)
        }

        return null
    }
}


object ImagesTypeConverters {

    val gson = Gson()

    @TypeConverter
    @JvmStatic
    fun toImages(data: String?): List<Image> {
        if (data == null) {
            return emptyList()
        }

        val listType = object : TypeToken<List<Image>>() {

        }.type

        return gson.fromJson(data, listType)
    }

    @TypeConverter
    @JvmStatic
    fun toString(images: List<Image>?): String? {
        images?.let {
            return gson.toJson(images)
        }
            return null
    }
}


//data class RoomMushroom(
//    @PrimaryKey
//    @SerializedName("_id") val id: Int,
//    @SerializedName("FullName") val fullName: String?,
//    @SerializedName("Author") val author: String?,
//
//    var images: List<Image>? = null,
//
//    @Embedded
//    @SerializedName("Vernacularname_DK") val _vernacularNameDK: VernacularNameDK?
//) {
//
//
//    val fullNameAuthor: String? get() {return author}
//    val danishName: String?
//        get() {
//            return _vernacularNameDK?._vernacularNameDK
//        }
////    val getImages: List<Image> get() {return _images ?: listOf()}
//}

@Entity(tableName = "mushrooms")
class Mushroom(

    @PrimaryKey
    @SerializedName("_id") val id: Int,
    @SerializedName("FullName") val fullName: String,
    @SerializedName("Author") val fullNameAuthor: String?,
    @SerializedName("updatedAt") val updatedAt: String?,
    @SerializedName("probability") val probability: Int?,
    @SerializedName("RankName") val _rankName: String?,

    @Embedded
    @SerializedName("Vernacularname_DK") val _vernacularNameDK: VernacularNameDK?,

    @Embedded
    @SerializedName("attributes") val attributes: Attributes?,

    @Embedded
    @SerializedName("Statistics") val statistics: Statistics?,

    @SerializedName("redlistdata") val _redListData: List<RedListData>?,
    @SerializedName("Images") val images: List<Image>?
) {

    constructor(id: Int, fullName: String, vernacularNameDK: VernacularNameDK?) : this(id, fullName, null, null, null, null, vernacularNameDK, null, null, null, null)

    val localizedName: String? get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> _vernacularNameDK?._vernacularNameDK?.ifBlank { null }?.capitalized()
            AppLanguage.English -> attributes?.vernacularNameEn?.ifBlank { null }?.capitalized()
            AppLanguage.Czech -> attributes?.vernacularNameCz?.ifBlank { null }?.capitalized()
        }
    }

    val redListStatus: String? get() {
        return _redListData?.firstOrNull()?.status
    }


    val updatedAtDate: Date?
        get() {
            return Date(updatedAt)
        }


    val isGenus: Boolean get() { return (_rankName == "gen.") }
}

data class VernacularNameDK(
    @SerializedName("vernacularname_dk") val _vernacularNameDK: String?,
    @SerializedName("source") val _source: String?
)

data class RedListData(
    @SerializedName("status") val status: String?
)

data class Attributes(
    @SerializedName("diagnose") val diagnosis: String?,
    @SerializedName("bogtekst_gyldendal_en") val diagnosisEn: String?,
    @SerializedName("spiselighedsrapport") val edibility: String?,
    @SerializedName("forvekslingsmuligheder") val similarities: String?,
    @SerializedName("oekologi") val ecology: String?,
    @SerializedName("valideringsrapport") val validationTips: String?,
    @SerializedName("vernacular_name_GB") val vernacularNameEn: String?,
    @SerializedName("vernacular_name_CZ") val vernacularNameCz: String?,
    @SerializedName("PresentInDK") val presentInDenmark: Boolean?
) {

    val localizedDescription: String? get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> diagnosis?.capitalized()
            AppLanguage.English -> diagnosisEn?.capitalized()
            AppLanguage.Czech -> diagnosisEn?.capitalized()
        }
    }

    val localizedEdibility: String? get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> edibility?.capitalized()
            AppLanguage.English -> null
            AppLanguage.Czech -> null
        }
    }

    val localizedSimilarities: String? get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> similarities?.capitalized()
            AppLanguage.English -> null
            AppLanguage.Czech -> null
        }
    }

    val localizedEcology: String? get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> ecology?.capitalized()
            AppLanguage.English -> null
            AppLanguage.Czech -> null
        }
    }

    val isPoisonous: Boolean get() {
            return edibility != null && edibility.contains("giftig", true) && !edibility.contains("ikke giftig", true)
    }
}

data class Statistics(
    @SerializedName("accepted_count") val acceptedCount: Int?,
    @SerializedName("last_accepted_record") val lastAcceptedRecord: String?,
    @SerializedName("first_accepted_record") val firstAcceptedRecord: String?
) {

    val acceptedObservationsCount: Int?
        get() {
            return acceptedCount
        }

    val lastAcceptedObservationDate: Date?
        get() {
            return Date(lastAcceptedRecord)
        }

    val firstAcceptedObservationDate: Date? get() { return Date(firstAcceptedRecord) }
}
