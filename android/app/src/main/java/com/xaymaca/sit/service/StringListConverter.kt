package com.xaymaca.sit.service

import androidx.room.TypeConverter

class StringListConverter {

    @TypeConverter
    fun fromString(value: String): List<String> {
        val trimmed = value.trim()
        if (trimmed == "[]" || trimmed.isBlank()) return emptyList()
        return trimmed
            .removePrefix("[")
            .removeSuffix("]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        if (list.isEmpty()) return "[]"
        return list.joinToString(",", prefix = "[", postfix = "]") {
            "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        }
    }
}
