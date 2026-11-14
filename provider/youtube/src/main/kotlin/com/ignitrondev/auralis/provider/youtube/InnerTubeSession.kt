/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube

import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.InnerTubeLogger
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Owns the InnerTubeX objects for one app process. Internal to this module: callers only see
 * Auralis domain types.
 *
 * Privacy (spec section 12): no remote player-config store is passed to the cipher service or
 * config parser, so InnerTubeX never downloads config from GitHub. All traffic goes to YouTube
 * hosts only.
 */
internal class InnerTubeSession(
    val httpClient: HttpClient = defaultHttpClient(),
    logger: InnerTubeLogger = InnerTubeLogger.NONE,
) : AutoCloseable {
    val innerTube = InnerTube(httpClient = httpClient, logger = logger).apply {
        // InnerTubeX would send the full device tag (for example "en-ID"), which search rejects.
        locale = youTubeLocaleFor(java.util.Locale.getDefault())
    }

    private val cipherService = YouTubeCipherService(httpClient = httpClient, remotePlayerConfigStore = null, logger = logger)

    val extractor = InnerTubeExtractor(
        configParser = YtConfigParserImpl(
            httpClient = httpClient,
            innerTube = innerTube,
            remotePlayerConfigStore = null,
            logger = logger,
            cipherService = cipherService,
        ),
        cipherService = cipherService,
        innerTube = innerTube,
        logger = logger,
    )

    override fun close() {
        innerTube.close()
        httpClient.close()
    }

    companion object {
        /** InnerTube JSON changes often; unknown keys are expected and ignored. */
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        /** Upper bound for one InnerTube JSON response, so a broken reply cannot exhaust memory. */
        const val MAX_RESPONSE_BYTES = 8 * 1024 * 1024

        /**
         * HTTP client set up the way InnerTubeX expects: JSON request bodies through
         * ContentNegotiation, and redirects left to the library, which follows them itself.
         */
        fun defaultHttpClient(): HttpClient = HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            followRedirects = false
        }
    }
}
