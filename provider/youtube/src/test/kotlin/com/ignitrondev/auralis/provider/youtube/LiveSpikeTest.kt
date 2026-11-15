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

            val home = itx.browse(YouTubeClient.WEB_REMIX, browseId = "FEmusic_home").json("home")
            val explore = itx.browse(YouTubeClient.WEB_REMIX, browseId = "FEmusic_explore").json("explore")
            println("SPIKE explore sections: " + com.ignitrondev.auralis.provider.youtube.parser.FeedParser.parse(explore).map { "${it.title} (${it.items.size})" })
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

    @Test
    fun providerEndToEnd(): Unit = runBlocking {
        assumeTrue("live spike disabled; run with -Plive", live)
        YouTubeMusicProvider().use { provider ->
            val home = provider.home()
            println("SPIKE provider home: ${home.sections.map { "${it.title} (${it.items.size})" }}")
            val results = provider.search("Daft Punk")
            println("SPIKE provider search: top=${results.topResult?.id} tracks=${results.tracks.size} albums=${results.albums.size} artists=${results.artists.size} playlists=${results.playlists.size}")
            val album = provider.album(results.albums.first().id)
            println("SPIKE provider album: ${album.album.title} by ${album.album.artistNames}, ${album.tracks.size} tracks")
            val artist = provider.artist(results.artists.first().id)
            println("SPIKE provider artist: ${artist.artist.name}, top=${artist.topTracks.size}, sections=${artist.sections.map { it.title }}")
            val playlist = provider.playlist(results.playlists.first().id)
            println("SPIKE provider playlist: ${playlist.playlist.title}, ${playlist.tracks.size} tracks, more=${playlist.hasMore}")
            check(home.sections.isNotEmpty() && !results.isEmpty && album.tracks.isNotEmpty() && playlist.tracks.isNotEmpty())
        }
    }

    @Test
    fun searchRejectsFullDeviceTagButAcceptsMappedLocale(): Unit = runBlocking {
        assumeTrue("live spike disabled; run with -Plive", live)
        InnerTubeSession().use { session ->
            session.innerTube.locale = com.metrolist.innertubex.models.YouTubeLocale(gl = "ID", hl = "en-ID")
            val raw = runCatching { session.innerTube.search(YouTubeClient.WEB_REMIX, query = "daft punk").status.value.toString() }
                .getOrElse { it.message ?: it.toString() }
            session.innerTube.locale = youTubeLocaleFor(java.util.Locale.forLanguageTag("en-ID"))
            val mapped = session.innerTube.search(YouTubeClient.WEB_REMIX, query = "daft punk").status.value
            println("SPIKE locale: hl=en-ID -> HTTP $raw, mapped hl=${session.innerTube.locale.hl} -> HTTP $mapped")
            check(mapped == 200)
        }
    }

    @Test
    fun dumpSearchRows(): Unit = runBlocking {
        assumeTrue("live spike disabled; run with -Plive", live)
        InnerTubeSession().use { session ->
            val root = session.innerTube.search(YouTubeClient.WEB_REMIX, query = "daft punk").json("search_daftpunk")
            fun walk(e: JsonElement) {
                when (e) {
                    is JsonObject -> {
                        (e["musicResponsiveListItemRenderer"] as? JsonObject)?.let { item ->
                            val cols = (item["flexColumns"] as JsonArray).map { col ->
                                ((col.jsonObject["musicResponsiveListItemFlexColumnRenderer"]!!.jsonObject["text"] as? JsonObject)?.get("runs") as? JsonArray)
                                    ?.joinToString("|") { r -> r.jsonObject["text"]!!.jsonPrimitive.content + (if (r.jsonObject.containsKey("navigationEndpoint")) "*" else "") } ?: "-"
                            }
                            println("SPIKE row: $cols")
                        }
                        (e["musicCardShelfRenderer"] as? JsonObject)?.let { card ->
                            println("SPIKE card subtitle: " + (card["subtitle"]?.jsonObject?.get("runs") as? JsonArray)?.joinToString("|") { it.jsonObject["text"]!!.jsonPrimitive.content })
                        }
                        e.values.forEach(::walk)
                    }
                    is JsonArray -> e.forEach(::walk)
                    else -> Unit
                }
            }
            walk(root)
        }
    }

    @Test
    fun anonymousClientsByOverride(): Unit = runBlocking {
        assumeTrue("live spike disabled; run with -Plive", live)
        val logger = InnerTubeLogger { event ->
            if (event.level >= InnerTubeLogLevel.WARN) println("ITX ${event.tag}: ${event.message} ${event.details.mapValues { if ("://" in it.value) "<url>" else it.value }}")
        }
        for (client in listOf("VISIONOS_0_1", "TVHTML5_SAMSUNG", "ANDROID_VR_1_65_10", "WEB_REMIX")) {
            InnerTubeSession(logger = logger).use { session ->
                val started = System.currentTimeMillis()
                val result = runCatching {
                    val stream = session.extractor.extract(
                        videoId = SAMPLE_VIDEO_ID,
                        hints = ContentHints(playbackClientOverrideId = client)
                            .withStreamCapabilities(allowHls = false, allowSabr = false, allowBoundedRange = true),
                        excludedClients = emptySet(),
                        audioQuality = AudioQuality.HIGH,
                        clientPlaybackNonce = generateClientPlaybackNonce(),
                    ) ?: error("null stream")
                    val ranges = (0 until 3).map { index ->
                        val from = index * 512L * 1024
                        session.httpClient.get(stream.audioUrl) {
                            stream.headers.forEach { (key, value) -> header(key, value) }
                            header("Range", "bytes=$from-${from + 512 * 1024 - 1}")
                        }.status.value
                    }
                    "client=${stream.clientName} mime=${stream.mimeType} chunk=${stream.rangeChunkSizeBytes} ranges=$ranges"
                }.getOrElse { "FAILED ${it.message}" }
                println("SPIKE override $client: $result (${System.currentTimeMillis() - started} ms)")
            }
        }
    }

    private class StopSpike : RuntimeException()

    /**
     * Lyrics spike (issue #34): where YouTube Music keeps lyrics, and whether they are timed.
     * Prints shapes and counts only, never lyric text (it is copyrighted).
     */
    @Test
    fun lyrics(): Unit = runBlocking {
        assumeTrue("live spike disabled; run with -Plive", live)
        InnerTubeSession().use { session ->
            val itx = session.innerTube
            for (query in listOf("Midnight City M83", "Teh Hijau Tulus", "Blinding Lights The Weeknd", "Shape of You Ed Sheeran", "Komang Raim Laode")) {
                val videoId = itx.search(YouTubeClient.WEB_REMIX, query = query).json("lyrics-search").findString("videoId") ?: continue
                val next = itx.next(YouTubeClient.WEB_REMIX, videoId = videoId).json("lyrics-next")
                val lyricsId = Regex("\"(MPLYt[^\"]+)\"").find(next.toString())?.groupValues?.get(1)
                println("SPIKE lyrics $query: lyricsBrowseId=${lyricsId != null}")
                if (lyricsId == null) continue
                for (client in listOf(YouTubeClient.IOS_MUSIC)) {
                    val page = runCatching { itx.browse(client, browseId = lyricsId).json("lyrics-page") }
                        .onFailure { println("SPIKE   ${client.clientName} browse failed: ${it::class.simpleName} ${it.message?.take(120)}") }
                        .getOrNull() ?: continue
                    val text = page.toString()
                    val timed = Regex("\"startTimeMilliseconds\":\"?(\\d+)").findAll(text).map { it.groupValues[1] }.toList()
                    // Shape of the timed model: key paths and value kinds only, never the lyric text.
                    fun shape(element: JsonElement, path: String, depth: Int): List<String> = when {
                        depth > 9 -> emptyList()
                        element is JsonObject -> element.flatMap { (key, value) -> shape(value, "$path.$key", depth + 1) }
                        element is kotlinx.serialization.json.JsonArray ->
                            listOf("$path[${element.size}]") + (element.firstOrNull()?.let { shape(it, "$path[0]", depth + 1) } ?: emptyList())
                        else -> listOf(path + if ((element as kotlinx.serialization.json.JsonPrimitive).isString) ":str" else ":${element}")
                    }
                    Regex("\"timedLyricsModel\"").find(text)?.let {
                        fun find(element: JsonElement): JsonElement? = when (element) {
                            is JsonObject -> element["timedLyricsModel"] ?: element.values.firstNotNullOfOrNull(::find)
                            is kotlinx.serialization.json.JsonArray -> element.firstNotNullOfOrNull(::find)
                            else -> null
                        }
                        find(page)?.let { model ->
                            val lines = model.jsonObject["lyricsData"]?.jsonObject?.get("timedLyricsData") as? kotlinx.serialization.json.JsonArray
                            val first = lines?.firstOrNull()
                            val keys = (first as? JsonObject)?.let { shape(it, "line", 0) }.orEmpty().filterNot { it.startsWith("line.lyricLine") }
                            println("SPIKE     lines=${lines?.size} static=${model.jsonObject["lyricsData"]?.jsonObject?.get("staticLayout")} firstLineFields=$keys")
                        }
                    }
                    println(
                        "SPIKE   ${client.clientName}: keys=${page.keys.sorted()} plain=${"musicDescriptionShelfRenderer" in text} " +
                            "timedModel=${"timedLyricsModel" in text} timedLines=${timed.size} firstTimes=${timed.take(3)} " +
                            "source=${Regex("Source: [^\"]+").find(text)?.value}",
                    )
                }
            }
        }
    }

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
