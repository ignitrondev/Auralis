/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.botguard

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.net.URI
import java.util.Base64

/** The BotGuard challenge returned by the Create endpoint. */
internal class BotGuardChallenge(
    val interpreterJs: String?,
    val interpreterUrl: String?,
    val program: String,
    val globalName: String,
) {
    override fun toString() = "BotGuardChallenge(interpreterJs=${interpreterJs?.length} chars, interpreterUrl=$interpreterUrl, globalName=$globalName)"
}

/** Integrity token bytes (as a JavaScript Uint8Array literal) and how long they stay valid. */
internal class IntegrityToken(val jsBytes: String, val lifetimeSeconds: Long) {
    override fun toString() = "IntegrityToken(<redacted>, lifetimeSeconds=$lifetimeSeconds)"
}

internal class BotGuardException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Parsing and encoding for the BotGuard web endpoints. Pure Kotlin, so it is unit tested on the JVM. */
internal object BotGuardProtocol {
    const val CREATE_URL = "https://www.youtube.com/api/jnn/v1/Create"
    const val GENERATE_URL = "https://www.youtube.com/api/jnn/v1/GenerateIT"

    /** Identifies the YouTube web BotGuard integration. Public: it ships in youtube.com's own script. */
    const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"

    /** The public YouTube web client key used by youtube.com for these requests. Not an Auralis credential. */
    const val WEB_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"

    private const val MAX_RESPONSE_CHARS = 4 * 1024 * 1024
    private val trustedInterpreterHosts = setOf("www.google.com", "www.gstatic.com")

    fun createBody(): String = JsonArray(listOf(JsonPrimitive(REQUEST_KEY))).toString()

    fun generateBody(botguardResponse: String): String =
        JsonArray(listOf(JsonPrimitive(REQUEST_KEY), JsonPrimitive(botguardResponse))).toString()

    /**
     * The Create response is a JSON array; the challenge itself is either the first element or,
     * when scrambled, a base64 string in the second element whose bytes are shifted by 97.
     */
    fun parseChallenge(raw: String): BotGuardChallenge {
        require(raw.length <= MAX_RESPONSE_CHARS) { "challenge too large" }
        try {
            val root = Json.parseToJsonElement(raw).jsonArray
            val second = root.getOrNull(1)
            val data = if (second is JsonPrimitive && second.isString) {
                Json.parseToJsonElement(descramble(second.content)).jsonArray
            } else {
                root[0].jsonArray
            }
            fun firstString(index: Int): String? =
                (data.getOrNull(index)?.takeIf { it !is JsonNull } as? JsonArray)
                    ?.firstOrNull { it is JsonPrimitive && it.isString }?.jsonPrimitive?.content
            return BotGuardChallenge(
                interpreterJs = firstString(1),
                interpreterUrl = firstString(2)?.let(::trustedInterpreterUrl),
                program = data[4].jsonPrimitive.content,
                globalName = data[5].jsonPrimitive.content,
            )
        } catch (error: BotGuardException) {
            throw error
        } catch (error: Exception) {
            throw BotGuardException("invalid BotGuard challenge", error)
        }
    }

    fun parseIntegrityToken(raw: String): IntegrityToken {
        try {
            val data = Json.parseToJsonElement(raw).jsonArray
            return IntegrityToken(
                jsBytes = uint8Array(decodeBase64(data[0].jsonPrimitive.content)),
                lifetimeSeconds = data[1].jsonPrimitive.long,
            )
        } catch (error: Exception) {
            throw BotGuardException("invalid integrity token response", error)
        }
    }

    /** Only Google's own BotGuard interpreter hosts are accepted. */
    fun trustedInterpreterUrl(value: String): String {
        val uri = URI(if (value.startsWith("//")) "https:$value" else value)
        val trusted = uri.scheme == "https" && uri.host in trustedInterpreterHosts && uri.userInfo == null &&
            (uri.port == -1 || uri.port == 443) && uri.path.endsWith(".js")
        if (!trusted) throw BotGuardException("untrusted interpreter URL")
        return uri.toString()
    }

    /** A JavaScript `new Uint8Array([...])` literal for [bytes]. */
    fun uint8Array(bytes: ByteArray): String =
        "new Uint8Array([" + bytes.joinToString(",") { (it.toInt() and 0xff).toString() } + "])"

    fun uint8Array(identifier: String): String = uint8Array(identifier.toByteArray(Charsets.UTF_8))

    /** Converts the comma-separated token bytes from JavaScript to the base64url token form. */
    fun tokenFromBytes(commaSeparated: String): String {
        val bytes = commaSeparated.split(',').map { part ->
            val value = part.trim().toInt()
            require(value in 0..255) { "byte out of range" }
            value.toByte()
        }.toByteArray()
        require(bytes.isNotEmpty()) { "empty token" }
        return Base64.getUrlEncoder().encodeToString(bytes)
    }

    private fun descramble(scrambled: String): String =
        decodeBase64(scrambled).map { (it + 97).toByte() }.toByteArray().toString(Charsets.UTF_8)

    /** Decodes standard or URL-safe base64, with or without padding (YouTube mixes both). */
    private fun decodeBase64(value: String): ByteArray {
        val normalized = value.replace('-', '+').replace('_', '/').replace('.', '=').trimEnd('=')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return Base64.getDecoder().decode(padded)
    }
}
