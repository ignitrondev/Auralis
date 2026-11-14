/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube

import com.metrolist.innertubex.InnerTubeLogLevel
import com.metrolist.innertubex.InnerTubeLogger
import com.metrolist.innertubex.bodyAsTextLimited
import com.metrolist.innertubex.sabr.ExperimentalSabrApi
import com.metrolist.innertubex.sabr.SabrAudioStream
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import com.metrolist.innertubex.models.YouTubeClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * InnerTubeX spike (spec section 6.2). Talks to the real YouTube Music service, so it only runs
 * with `./gradlew :provider:youtube:test -Plive`. Add `-PrecordFixtures` to save scrubbed
 * responses as parser fixtures. It never prints or saves stream URLs, cookies or tokens.
 */
class LiveSpikeTest {
    private val live = System.getProperty("auralis.live") == "true"
    private val record = System.getProperty("auralis.recordFixtures") == "true"
    private val fixtures = File(System.getProperty("auralis.fixtures") ?: "build/fixtures")

    @Test
    fun signedOutCatalog(): Unit = runBlocking {
        assumeTrue("live spike disabled; run with -Plive", live)
        InnerTubeSession().use { session ->
            val itx = session.innerTube

            val search = itx.search(YouTubeClient.WEB_REMIX, query = "Midnight City M83").json("search")
            val videoId = search.findString("videoId")
            val albumId = search.findBrowseId("MUSIC_PAGE_TYPE_ALBUM")
            val artistId = search.findBrowseId("MUSIC_PAGE_TYPE_ARTIST")
            println("SPIKE search: videoId=$videoId album=$albumId artist=$artistId")

            itx.browse(YouTubeClient.WEB_REMIX, browseId = "FEmusic_home").json("home")
            albumId?.let { itx.browse(YouTubeClient.WEB_REMIX, browseId = it).json("album") }
            val artist = artistId?.let { itx.browse(YouTubeClient.WEB_REMIX, browseId = it).json("artist") }
            val playlistId = (artist ?: search).findBrowseId("MUSIC_PAGE_TYPE_PLAYLIST")
            println("SPIKE playlist: $playlistId")
            playlistId?.let { itx.browse(YouTubeClient.WEB_REMIX, browseId = it).json("playlist") }

            requireNotNull(videoId) { "search returned no song" }
        }
    }

    @OptIn(ExperimentalSabrApi::class)
    @Test
    fun streamModes(): Unit = runBlocking {
        assumeTrue("live spike disabled; run with -Plive", live)
        val logger = InnerTubeLogger { event ->
            if (event.level >= InnerTubeLogLevel.INFO) {
                val details = event.details.mapValues { (_, value) -> if ("http" in value) "<url>" else value }
                println("ITX ${event.level} ${event.tag}: ${event.message} $details")
            }
        }
        val modes = listOf(
            "direct" to ContentHints().withStreamCapabilities(allowHls = false, allowSabr = false, allowBoundedRange = true),
            "hls" to ContentHints().withStreamCapabilities(allowHls = true, allowSabr = false, allowBoundedRange = true),
            "sabr" to ContentHints().withStreamCapabilities(allowHls = false, allowSabr = true, allowBoundedRange = true),
        )
        for ((mode, hints) in modes) {
            InnerTubeSession(logger = logger).use { session ->
                println("SPIKE ===== mode=$mode")
                val started = System.currentTimeMillis()
                val stream = runCatching {
                    session.extractor.extract(
                        videoId = SAMPLE_VIDEO_ID,
                        hints = hints,
                        excludedClients = emptySet(),
                        audioQuality = AudioQuality.HIGH,
                        clientPlaybackNonce = generateClientPlaybackNonce(),
                    )
                }.onFailure { println("SPIKE mode=$mode extract failed: $it") }.getOrNull() ?: return@use
                println(
                    "SPIKE mode=$mode client=${stream.clientName} profile=${stream.profileId} itag=${stream.itag} " +
                        "mime=${stream.mimeType} bitrate=${stream.bitrate} length=${stream.contentLengthBytes} " +
                        "boundedRange=${stream.requireBoundedRange} chunk=${stream.rangeChunkSizeBytes} " +
                        "sabr=${stream.sabrBootstrap != null} headerKeys=${stream.headers.keys} " +
                        "extractMs=${System.currentTimeMillis() - started}",
                )
                val bootstrap = stream.sabrBootstrap
                if (bootstrap != null) {
                    var bytes = 0L
                    withTimeoutOrNull(20_000) {
                        SabrAudioStream(session.httpClient, bootstrap, logger = logger).bytes().collect { chunk ->
                            bytes += chunk.size
                            if (bytes > 512 * 1024) throw StopSpike()
                        }
                    }
                    println("SPIKE mode=$mode sabrBytes=$bytes")
                } else {
                    val response = session.httpClient.get(stream.audioUrl) {
                        stream.headers.forEach { (key, value) -> header(key, value) }
                        header("Range", "bytes=0-65535")
                    }
                    val buffer = ByteArray(65_536)
                    val read = response.bodyAsChannel().readAvailable(buffer, 0, buffer.size)
                    println("SPIKE mode=$mode media status=${response.status.value} firstReadBytes=$read type=${response.headers["Content-Type"]}")
                }
            }
        }
    }

    @Test
    fun playabilityByClient(): Unit = runBlocking {
        assumeTrue("live spike disabled; run with -Plive", live)
        InnerTubeSession().use { session ->
            val clients = listOf(
                "WEB_REMIX" to YouTubeClient.WEB_REMIX,
                "WEB" to YouTubeClient.WEB,
                "IOS" to YouTubeClient.IOS,
                "TVHTML5" to YouTubeClient.TVHTML5,
                "WEB_EMBEDDED_PLAYER" to YouTubeClient.WEB_EMBEDDED_PLAYER,
            )
            for ((name, client) in clients) {
                val result = runCatching {
                    val response = session.innerTube.player(client, videoId = SAMPLE_VIDEO_ID)
                    val root = InnerTubeSession.json.parseToJsonElement(
                        response.bodyAsTextLimited(InnerTubeSession.MAX_RESPONSE_BYTES),
                    ).jsonObject
                    val status = root["playabilityStatus"]?.jsonObject
                    val formats = root["streamingData"]?.jsonObject?.get("adaptiveFormats") as? JsonArray
                    val withUrl = formats?.count { (it as? JsonObject)?.containsKey("url") == true } ?: 0
                    "http=${response.status.value} status=${status?.get("status")} reason=${status?.get("reason")} " +
                        "formats=${formats?.size ?: 0} plainUrlFormats=$withUrl sabr=${root["streamingData"]?.jsonObject?.containsKey("serverAbrStreamingUrl")}"
                }.getOrElse { "error=$it" }
                println("SPIKE player $name: $result")
            }
        }
    }

    private class StopSpike : RuntimeException()

    private suspend fun HttpResponse.json(name: String): JsonObject {
        check(status.value in 200..299) { "$name failed: ${status.value}" }
        val text = bodyAsTextLimited(InnerTubeSession.MAX_RESPONSE_BYTES)
        val root = InnerTubeSession.json.parseToJsonElement(text).jsonObject
        println("SPIKE $name: ${text.length} chars, keys=${root.keys}")
        if (record) {
            fixtures.mkdirs()
            File(fixtures, "$name.json").writeText(InnerTubeSession.json.encodeToString(JsonElement.serializer(), scrub(root)))
        }
        return root
    }

    /** Drops session identifiers and tracking noise before a response is stored as a fixture. */
    private fun scrub(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.filterKeys { it !in DroppedKeys }.mapValues { (_, value) -> scrub(value) },
        )
        is JsonArray -> JsonArray(element.map(::scrub))
        else -> element
    }

    private fun JsonElement.findString(key: String): String? = when (this) {
        is JsonObject -> (this[key] as? JsonPrimitive)?.contentOrNull ?: values.firstNotNullOfOrNull { it.findString(key) }
        is JsonArray -> firstNotNullOfOrNull { it.findString(key) }
        else -> null
    }

    private fun JsonElement.findBrowseId(pageType: String): String? = when (this) {
        is JsonObject -> {
            val endpoint = this["browseEndpoint"] as? JsonObject
            val type = endpoint?.findString("pageType")
            if (endpoint != null && type == pageType) {
                endpoint["browseId"]?.jsonPrimitive?.contentOrNull
            } else {
                values.firstNotNullOfOrNull { it.findBrowseId(pageType) }
            }
        }
        is JsonArray -> firstNotNullOfOrNull { it.findBrowseId(pageType) }
        else -> null
    }

    private companion object {
        /** "Midnight City" by M83, found by the catalog spike. */
        const val SAMPLE_VIDEO_ID = "dX3k_QDnzHE"
        val DroppedKeys = setOf("responseContext", "trackingParams", "clickTrackingParams", "visitorData", "loggingContext")
    }
}
