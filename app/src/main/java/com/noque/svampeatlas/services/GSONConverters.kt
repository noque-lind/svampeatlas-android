package com.noque.svampeatlas.services

import androidx.room.Index
import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.models.Image
import com.noque.svampeatlas.models.NewObservation
import com.noque.svampeatlas.models.RedListData
import java.util.*

object IDsConverter {

    val gson by lazy { Gson() }

    @TypeConverter
    @JvmStatic
    fun toData(data: String?): List<Int> {
        if (data == null) {
            return Collections.emptyList()
        } else {
            val listType = object: TypeToken<List<Int>>() {}.type
            return gson.fromJson(data, listType)
        }
    }

    @TypeConverter
    @JvmStatic
    fun toString(ids: List<Int>?): String? {
        ids?.let {
            return gson.toJson(it)
        }
        return null
    }
}

object StringsConverter {

    val gson by lazy { Gson() }

    @TypeConverter
    @JvmStatic
    fun toData(data: String?): List<String> {
        if (data == null) {
            return Collections.emptyList()
        } else {
            val listType = object: TypeToken<List<String>>() {}.type
            return gson.fromJson(data, listType)
        }
    }

    @TypeConverter
    @JvmStatic
    fun toString(paths: List<String>?): String? {
        paths?.let {
            return gson.toJson(it)
        }
        return null
    }
}

object RedListDataConverter {

    val gson by lazy { Gson() }

    @TypeConverter
    @JvmStatic
    fun toData(data: String?): List<RedListData> {
        if (data == null) {
            return Collections.emptyList()
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


object ImagesConverter {
    data class IndexedImage(val index: Int, val image: Image)

    val gson = Gson()

    @TypeConverter
    @JvmStatic
    fun toImages(data: String?): List<Image> {
        if (data == null) {
            return Collections.emptyList()
        }

        val listType = object : TypeToken<List<IndexedImage>>() {
        }.type
        val indexImages = gson.fromJson<List<IndexedImage>>(data, listType)

        return indexImages.sortedBy { it.index }.map { it.image }
    }

    @TypeConverter
    @JvmStatic
    fun toString(images: List<Image>?): String? {
        images?.let {
            val indexedImages = it.mapIndexed { index, image -> IndexedImage(index, image) }
            return gson.toJson(indexedImages)
        }
        return null
    }
}

object DateConverter {
    @TypeConverter
    @JvmStatic
    fun toData(data: Long?): Date? {
        return data?.let { Date(it) }
    }

    @TypeConverter
    @JvmStatic
    fun toLong(date: Date): Long? {
        return date.time
    }
}

object LatLngConverter {
    val gson by lazy { Gson() }

    @TypeConverter
    @JvmStatic
    fun toData(data: String?): LatLng? {
        if (data == null) {
           return null
        } else {
            val listType = object: TypeToken<LatLng>() {}.type
            return gson.fromJson(data, listType)
        }
    }

    @TypeConverter
    @JvmStatic
    fun toString(latLng: LatLng?): String? {
        latLng?.let {
            return gson.toJson(it)
        }
        return null
    }
}