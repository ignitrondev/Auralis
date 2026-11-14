/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.api

/**
 * Resolves a playable stream for a track just before playback (spec section 6.1). Streams are
 * short-lived and signed, so they are never stored with the track.
 */
interface StreamProvider {
    /** @throws StreamException when no playable stream can be found. */
    suspend fun resolveStream(trackId: String): PlayableStream
}

/**
 * A resolved audio stream. [url] and [headers] are sensitive (signed, short-lived): never log or
 * persist them. [toString] redacts both.
 *
 * @property chunkSizeBytes When not null, the server only answers byte ranges up to this size,
 * so the player must request the stream in chunks.
 * @property expiresAtMs Wall-clock time after which the URL stops working, if known.
 */
class PlayableStream(
    val trackId: String,
    val url: String,
    val headers: Map<String, String>,
    val mimeType: String?,
    val contentLengthBytes: Long?,
    val chunkSizeBytes: Long?,
    val expiresAtMs: Long?,
    val loudnessDb: Double?,
) {
    override fun toString(): String =
        "PlayableStream(trackId=$trackId, url=<redacted>, headers=${headers.keys}, mimeType=$mimeType, " +
            "contentLengthBytes=$contentLengthBytes, chunkSizeBytes=$chunkSizeBytes, expiresAtMs=$expiresAtMs)"
}

/** Why a stream could not be resolved, so playback can show the right message. */
class StreamException(
    val reason: Reason,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    enum class Reason {
        /** No connection or a server error. Retrying may help. */
        Network,

        /** The track exists but cannot be played here (region, age, removed). */
        Unavailable,

        /** The service refused anonymous playback (bot check). */
        Blocked,

        /** No stream the player can use. */
        NoStream,
    }
}
