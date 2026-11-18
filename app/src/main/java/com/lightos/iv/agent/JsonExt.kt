package com.lightos.iv.agent

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/** Typed accessors over a tool's arguments object.
 *  JsonObject's own [get] returns JsonElement?, so we cast to JsonPrimitive first. */
fun JsonObject.str(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull
fun JsonObject.int(name: String): Int? = (this[name] as? JsonPrimitive)?.intOrNull
fun JsonObject.long(name: String): Long? = (this[name] as? JsonPrimitive)?.longOrNull
fun JsonObject.bool(name: String): Boolean? = (this[name] as? JsonPrimitive)?.booleanOrNull
