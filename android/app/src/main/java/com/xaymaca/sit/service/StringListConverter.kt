package com.xaymaca.sit.service

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StringListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }
}
