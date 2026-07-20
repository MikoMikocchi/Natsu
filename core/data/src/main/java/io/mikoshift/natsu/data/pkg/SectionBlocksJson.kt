package io.mikoshift.natsu.data.pkg

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

internal object SectionBlocksJson {
    fun normalize(raw: String, json: kotlinx.serialization.json.Json): String {
        val array = json.parseToJsonElement(raw).jsonArray
        if (array.all { it.jsonObject.containsKey("type") }) {
            return raw
        }
        return JsonArray(
            array.map { element ->
                val obj = element.jsonObject
                if (obj.containsKey("type")) {
                    element
                } else {
                    JsonObject(obj + ("type" to JsonPrimitive(inferBlockType(obj))))
                }
            },
        ).toString()
    }

    private fun inferBlockType(obj: JsonObject): String = when {
        "asset_id" in obj -> "image"
        "ordered" in obj -> "list_item"
        "level" in obj -> "heading"
        obj.keys == setOf("id") -> "divider"
        else -> "paragraph"
    }
}
