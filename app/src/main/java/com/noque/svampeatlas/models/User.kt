package com.noque.svampeatlas.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.util.*

object UserRolesTypeConverters {

    val gson = Gson()

    @TypeConverter
    @JvmStatic
    fun toRoles(data: String?): List<Role> {
        if (data == null) {
            return Collections.emptyList()
        }

        val listType = object : TypeToken<List<Role>>() {

        }.type

        return gson.fromJson(data, listType)
    }

    @TypeConverter
    @JvmStatic
    fun toString(roles: List<Role>?): String? {
        roles?.let {
            return gson.toJson(it)
        }
        return null
    }
}

@Entity(tableName = "user")
class User(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("_id") val id: Int,

    @ColumnInfo(name = "content")
    @SerializedName("name") val name: String,

    @ColumnInfo(name = "initials")
    @SerializedName("Initialer") val initials: String,

    @ColumnInfo(name = "email")
    @SerializedName("email") val email: String,

    @ColumnInfo(name = "facebook_id")
    @SerializedName("facebook") val facebookID: String?,

    @ColumnInfo(name = "roles")
    @SerializedName("Roles") val roles: List<Role>?
) {

    val imageURL: String? get() {
        facebookID?.let {
            return "https://graph.facebook.com/${it}/picture?width=250&height=250"
        }
        return null
    }

    val isValidator: Boolean get() {
        roles?.forEach {
            if (it.name == "validator") return true
        }
        return false
    }
}

data class Role(
    @SerializedName("name") val name: String)