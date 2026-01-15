package com.example.z_editor.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class FlexibleIntAdapter : JsonDeserializer<Int>, JsonSerializer<Int> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Int? {
        return try {
            val primitive = json.asJsonPrimitive
            when {
                primitive.isNumber -> primitive.asInt
                primitive.isString -> primitive.asString.toIntOrNull()
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun serialize(
        src: Int?,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return if (src != null) {
            JsonPrimitive(src.toString())
        } else {
            JsonNull.INSTANCE
        }
    }
}