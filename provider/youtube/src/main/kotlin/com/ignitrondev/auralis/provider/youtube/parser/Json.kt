/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube.parser

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// Small, null-safe helpers for walking InnerTube JSON. InnerTube responses change often, so every
// lookup tolerates a missing key instead of throwing.

internal fun JsonElement?.obj(key: String): JsonObject? = (this as? JsonObject)?.get(key) as? JsonObject

internal fun JsonElement?.arr(key: String): JsonArray? = (this as? JsonObject)?.get(key) as? JsonArray

internal fun JsonElement?.str(key: String): String? =
    ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.contentOrNull

/** Follows a path of object keys, for example `path("header", "musicImmersiveHeaderRenderer")`. */
internal fun JsonElement?.path(vararg keys: String): JsonElement? {
    var current: JsonElement? = this
    for (key in keys) current = (current as? JsonObject)?.get(key) ?: return null
    return current
}

internal fun JsonArray?.objects(): List<JsonObject> = this?.mapNotNull { it as? JsonObject }.orEmpty()

/** The text runs of an InnerTube text object (`{ "runs": [ { "text": … } ] }`). */
internal fun JsonElement?.runs(): List<JsonObject> = this.arr("runs").objects()

/** All runs joined, or the simpleText form. Null when empty. */
internal fun JsonElement?.text(): String? =
    (runs().joinToString("") { it.str("text").orEmpty() }.ifBlank { null }) ?: this.str("simpleText")

/** The renderer wrapped in an item, for example the value of `{ "musicShelfRenderer": { … } }`. */
internal fun JsonObject.renderer(name: String): JsonObject? = this[name] as? JsonObject
