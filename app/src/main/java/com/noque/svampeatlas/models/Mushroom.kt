package com.noque.svampeatlas.models

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.noque.svampeatlas.extensions.Date
import java.util.*
import com.google.gson.reflect.TypeToken
import java.util.Collections.emptyList
import java.util.Collections.list

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


    val updatedAtDate: Date?
        get() {
            return Date(updatedAt)
        }

    val danishName: String?
        get() {
            return _vernacularNameDK?._vernacularNameDK
        }

    val redListStatus: String? get() {
        return _redListData?.firstOrNull()?.status
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
    @SerializedName("PresentInDK") val presentInDenmark: Boolean?,
    @SerializedName("diagnose") val diagnosis: String?,
    @SerializedName("forvekslingsmuligheder") val similarities: String?,
    @SerializedName("oekologi") val ecology: String?,
    @SerializedName("spiselighedsrapport") val edibility: String?,
    @SerializedName("beskrivelse") val description: String?,
    @SerializedName("BeskrivelseUK") val englishDescription: String?
)

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
