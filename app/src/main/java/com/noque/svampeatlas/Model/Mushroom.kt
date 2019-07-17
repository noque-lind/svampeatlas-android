package com.noque.svampeatlas.Model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


data class Mushroom(@SerializedName("_id") private val _id: Int = 0,
                    @SerializedName("FullName") private val _fullName: String?,
                    @SerializedName("Author") private val _author: String?,
                    @SerializedName("updatedAt") private val _updatedAt: String?,
                    @SerializedName("probability") private val _probability: Int = 0,
                    @SerializedName("Vernacularname_DK") private val _vernacularNameDK: PrivateVernacularNameDK?,
                    @SerializedName("redlistdata") private val _redlistData: List<RedListData>?,
                    @SerializedName("images") private val _images: List<Image>,
                    @SerializedName("attributes") private val _attributes: Attributes?,
                    @SerializedName("Statistics") private val _statistics: PrivateStatistics?) {

    val id: Int get() {return _id}
    val fullName: String get() {return _fullName ?: throw IllegalArgumentException("Mushroom fullname is required")}
    val fullNameAuthor: String? get() {return _author}
    val updatedAt: String? get() {return _updatedAt}
    val danishName: String? get() {return _vernacularNameDK?._vernacularNameDK}
    val totalObservations: Int? get() {return _statistics?._totalCount}
    val lastAcceptedObservation: String? get() {return _statistics?._lastAcceptedRecord}
    val redlistData: RedListData? get() {return _redlistData?.firstOrNull()}
    val attributes: Attributes? get() {return _attributes}
    val images: List<Image> get() {return _images}
}

data class PrivateVernacularNameDK(
    @SerializedName("vernacularname_dk") val _vernacularNameDK: String?,
    @SerializedName("source") val _source: String?)

data class RedListData(
    @SerializedName("status") val status: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("Udbredelse") val distribution: String?)

data class Image(
    @SerializedName("thumburi") private val _thumburi: String?,
    @SerializedName("uri") private val _uri: String?,
    @SerializedName("photographer") private val _photographer: String?): Parcelable {

    val url: String? get() {return _uri}

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(_thumburi)
        parcel.writeString(_uri)
        parcel.writeString(_photographer)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Image> {
        override fun createFromParcel(parcel: Parcel): Image {
            return Image(parcel)
        }

        override fun newArray(size: Int): Array<Image?> {
            return arrayOfNulls(size)
        }
    }
}

data class Attributes(@SerializedName("diagnose") val diagnosis: String?,
                      @SerializedName("forvekslingsmuligheder") val similarities: String?,
                      @SerializedName("oekologi") val ecology: String?,
                      @SerializedName("spiselighedsrapport") val eatability: String?)

data class PrivateStatistics(@SerializedName("total_count") val _totalCount: Int?,
                             @SerializedName("last_accepted_record") val _lastAcceptedRecord: String?)
