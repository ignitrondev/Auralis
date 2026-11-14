/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube

import com.ignitrondev.auralis.domain.model.AlbumDetail
import com.ignitrondev.auralis.domain.model.ArtistDetail
import com.ignitrondev.auralis.domain.model.HomeFeed
import com.ignitrondev.auralis.domain.model.PlaylistDetail
import com.ignitrondev.auralis.domain.model.SearchResults
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.provider.api.AccountSessionProvider
import com.ignitrondev.auralis.provider.api.CatalogProvider
import com.ignitrondev.auralis.provider.api.ProviderAccount
import com.ignitrondev.auralis.provider.api.ProviderSession
import com.ignitrondev.auralis.provider.api.PlayableStream
import com.ignitrondev.auralis.provider.api.StreamException
import com.ignitrondev.auralis.provider.api.StreamProvider
import com.ignitrondev.auralis.provider.youtube.parser.AlbumParser
import com.ignitrondev.auralis.provider.youtube.parser.ArtistParser
import com.ignitrondev.auralis.provider.youtube.parser.FeedParser
import com.ignitrondev.auralis.provider.youtube.parser.PlaylistParser
import com.ignitrondev.auralis.provider.youtube.parser.SOURCE_ID
import com.ignitrondev.auralis.provider.youtube.parser.SearchParser
import com.ignitrondev.auralis.provider.youtube.parser.UnexpectedResponseException
import com.ignitrondev.auralis.provider.youtube.parser.arr
import com.ignitrondev.auralis.provider.youtube.parser.largestThumbnail
import com.ignitrondev.auralis.provider.youtube.parser.obj
import com.ignitrondev.auralis.provider.youtube.parser.path
import com.ignitrondev.auralis.provider.youtube.parser.text
import com.metrolist.innertubex.InnerTubeHttpException
import com.metrolist.innertubex.InnerTubeLogger
import com.metrolist.innertubex.bodyAsTextLimited
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.StreamResolveException
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import com.metrolist.innertubex.models.YouTubeClient
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.IOException

/**
 * YouTube Music catalog through InnerTube (spec section 6.1). Signed out for now; account
 * features come with the sign-in flow. One instance per process: it owns an HTTP client.
 */
class YouTubeMusicProvider internal constructor(
    private val session: InnerTubeSession,
) : CatalogProvider, StreamProvider, AccountSessionProvider, AutoCloseable {

    /**
     * @param attestation Mints BotGuard tokens for signed-out playback. Without it the catalog
     * still works, but YouTube refuses most anonymous streams.
     * @param logger Receives redacted diagnostics from the integration.
     */
    constructor(
        attestation: AttestationProvider? = null,
        logger: ProviderLogger? = null,
    ) : this(
        InnerTubeSession(
            attestation = attestation,
            logger = logger?.toInnerTubeLogger() ?: InnerTubeLogger.NONE,
        ),
    )

    override val sourceId: String = SOURCE_ID

    /**
     * Home = the home feed plus the explore page (new releases, trending). The signed-out home
     * feed alone has only a couple of shelves, and its follow-up pages need a request shape
     * InnerTubeX does not send. Both pages load in parallel; if explore fails, home still shows.
     */
    override suspend fun home(): HomeFeed = request("home") {
        coroutineScope {
            val explore = async { runCatching { FeedParser.parse(browse(EXPLORE_BROWSE_ID)) } }
            val home = FeedParser.parse(browse(HOME_BROWSE_ID))
            val extra = explore.await().getOrElse { error ->
                if (error is CancellationException) throw error
                emptyList()
            }
            HomeFeed((home + extra).distinctBy { it.title })
        }
    }

    override suspend fun search(query: String): SearchResults = request("search") {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            SearchResults(trimmed, null, emptyList(), emptyList(), emptyList(), emptyList())
        } else {
            SearchParser.parse(trimmed, session.innerTube.search(MUSIC_CLIENT, query = trimmed).toJson("search"))
        }
    }

    override suspend fun album(id: String): AlbumDetail = request("album") {
        AlbumParser.parse(id, browse(id))
    }

    override suspend fun artist(id: String): ArtistDetail = request("artist") {
        ArtistParser.parse(id, browse(id))
    }

    override suspend fun playlist(id: String): PlaylistDetail = request("playlist") {
        // Browse ids for playlists carry a "VL" prefix; plain playlist ids are accepted too.
        val browseId = if (id.startsWith("VL")) id else "VL$id"
        PlaylistParser.parse(browseId, browse(browseId))
    }

    /**
     * Resolves a direct audio URL for [trackId]. SABR and HLS are disabled: the player streams a
     * plain progressive URL, in byte-range chunks when the server requires it.
     */
    override suspend fun resolveStream(trackId: String): PlayableStream =
        try {
            val hints = ContentHints().withStreamCapabilities(allowHls = false, allowSabr = false, allowBoundedRange = true)
            val stream = session.extractor.extract(
                videoId = trackId,
                hints = hints,
                excludedClients = emptySet(),
                audioQuality = AudioQuality.HIGH,
                clientPlaybackNonce = generateClientPlaybackNonce(),
            ) ?: throw StreamException(StreamException.Reason.NoStream, "no playable stream")
            PlayableStream(
                trackId = trackId,
                url = stream.audioUrl,
                headers = stream.headers,
                mimeType = stream.mimeType,
                contentLengthBytes = stream.contentLengthBytes,
                chunkSizeBytes = stream.rangeChunkSizeBytes.takeIf { stream.requireBoundedRange || stream.useRangeChunks },
                expiresAtMs = stream.expiresAt?.toEpochMilliseconds(),
                loudnessDb = stream.loudnessDb,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: StreamException) {
            throw error
        } catch (error: StreamResolveException) {
            val reason = when (error.reason) {
                StreamResolveException.Reason.NETWORK -> StreamException.Reason.Network
                StreamResolveException.Reason.UNAVAILABLE,
                StreamResolveException.Reason.AGE_RESTRICTED,
                StreamResolveException.Reason.EXPLICIT_UNSUPPORTED,
                StreamResolveException.Reason.NO_MUSIC_VIDEO,
                -> StreamException.Reason.Unavailable
                // Signed out, "no playable stream" means YouTube refused anonymous playback
                // (LOGIN_REQUIRED for every client): signing in is the fix (spec 12.1).
                else -> if (session.innerTube.hasSapCookieAuth()) StreamException.Reason.NoStream else StreamException.Reason.Blocked
            }
            throw StreamException(reason, "stream: ${error.reason}", error)
        } catch (error: IOException) {
            throw StreamException(StreamException.Reason.Network, "stream: network", error)
        }

    override suspend fun applySession(session: ProviderSession?) {
        val innerTube = this.session.innerTube
        if (session == null) {
            innerTube.replaceSession(cookie = null, visitorData = null, dataSyncId = null, authUser = "0", useLoginForBrowse = false)
        } else {
            innerTube.replaceSession(
                cookie = session.cookie,
                visitorData = session.visitorData,
                // InnerTube expects only the account part of the data sync id ("<account>||").
                dataSyncId = session.dataSyncId?.substringBefore("||")?.ifBlank { null },
                authUser = session.authUser,
                useLoginForBrowse = true,
            )
        }
    }

    /** Reads the active account from the account menu; null when signed out or the session expired. */
    override suspend fun currentAccount(): ProviderAccount? {
        if (!session.innerTube.hasSapCookieAuth()) return null
        return try {
            withContext(Dispatchers.Default) {
                val root = session.innerTube.accountMenu(MUSIC_CLIENT).toJson("account")
                val header = (root.arr("actions")?.firstOrNull() as? JsonObject)
                    .path("openPopupAction", "popup", "multiPageMenuRenderer", "header", "activeAccountHeaderRenderer")
                val name = header.obj("accountName").text() ?: return@withContext null
                ProviderAccount(
                    name = name,
                    handle = header.obj("channelHandle").text(),
                    avatarUrl = header.obj("accountPhoto").largestThumbnail(),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            null
        }
    }

    override fun close() = session.close()

    private suspend fun browse(browseId: String): JsonObject =
        session.innerTube.browse(MUSIC_CLIENT, browseId = browseId).toJson("browse")

    private suspend fun HttpResponse.toJson(operation: String): JsonObject {
        when (status.value) {
            in 200..299 -> Unit
            400, 404 -> throw CatalogException(CatalogException.Reason.NotFound, "$operation: HTTP ${status.value}")
            else -> throw CatalogException(CatalogException.Reason.Network, "$operation: HTTP ${status.value}")
        }
        return InnerTubeSession.json.parseToJsonElement(bodyAsTextLimited(InnerTubeSession.MAX_RESPONSE_BYTES)).jsonObject
    }

    /** Runs a catalog call and reports every failure as a [CatalogException] with a reason. */
    private suspend fun <T> request(operation: String, block: suspend () -> T): T =
        try {
            // Responses are large JSON documents: parse them off the caller's (often main) thread.
            withContext(Dispatchers.Default) { block() }
        } catch (error: CancellationException) {
            throw error
        } catch (error: CatalogException) {
            throw error
        } catch (error: InnerTubeHttpException) {
            val reason = if (error.status.value in listOf(400, 404)) CatalogException.Reason.NotFound else CatalogException.Reason.Network
            throw CatalogException(reason, "$operation failed (HTTP ${error.status.value})", error)
        } catch (error: IOException) {
            throw CatalogException(CatalogException.Reason.Network, "$operation failed: network", error)
        } catch (error: UnexpectedResponseException) {
            throw CatalogException(CatalogException.Reason.UnexpectedResponse, "$operation: ${error.message}", error)
        } catch (error: SerializationException) {
            throw CatalogException(CatalogException.Reason.UnexpectedResponse, "$operation: malformed response", error)
        } catch (error: IllegalArgumentException) {
            throw CatalogException(CatalogException.Reason.UnexpectedResponse, "$operation: malformed response", error)
        }

    private companion object {
        val MUSIC_CLIENT = YouTubeClient.WEB_REMIX
        const val HOME_BROWSE_ID = "FEmusic_home"
        const val EXPLORE_BROWSE_ID = "FEmusic_explore"
    }
}
