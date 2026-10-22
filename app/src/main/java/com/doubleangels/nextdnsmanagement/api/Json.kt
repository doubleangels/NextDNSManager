package com.doubleangels.nextdnsmanagement.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

// Lenient accessors: a missing or differently typed field reads as null instead of throwing.

fun JsonElement?.asObject(): JsonObject? = this as? JsonObject

fun JsonElement?.asArray(): List<JsonElement> = (this as? JsonArray) ?: emptyList()

fun JsonElement?.objects(): List<JsonObject> = asArray().filterIsInstance<JsonObject>()

fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull

fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

/** Number arrays in time series. */
fun JsonObject.longs(key: String): List<Long> =
    (this[key] as? JsonArray)?.map { (it as? JsonPrimitive)?.longOrNull ?: 0L } ?: emptyList()

/** Boolean members of an object, in API order (used to render settings toggles). */
fun JsonObject.booleanEntries(): List<Pair<String, Boolean>> =
    entries.mapNotNull { (key, value) -> (value as? JsonPrimitive)?.booleanOrNull?.let { key to it } }
