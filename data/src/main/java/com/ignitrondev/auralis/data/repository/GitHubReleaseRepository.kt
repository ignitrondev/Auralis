/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.data.repository

import com.ignitrondev.auralis.domain.update.AppRelease
import com.ignitrondev.auralis.domain.update.AppReleaseRepository
import com.ignitrondev.auralis.domain.update.AppVersion
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auralis releases from the public GitHub Releases API. Called only when the user asks to check
 * for updates; the request carries no account data, only a generic User-Agent.
 */
@Singleton
class GitHubReleaseRepository @Inject constructor() : AppReleaseRepository {

    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = TIMEOUT_MS
                requestTimeoutMillis = TIMEOUT_MS
            }
        }
    }

    override suspend fun releases(): List<AppRelease> = withContext(Dispatchers.IO) {
        val response = client.get(RELEASES_URL) {
            header("Accept", "application/vnd.github+json")
            header("User-Agent", "Auralis")
        }
        if (response.status.value !in 200..299) throw IOException("releases: HTTP ${response.status.value}")
        parseReleases(response.bodyAsText())
    }

    internal companion object {
        const val RELEASES_URL = "https://api.github.com/repos/ignitrondev/Auralis/releases?per_page=30"
        private const val TIMEOUT_MS = 15_000L
        private const val APK_PREFIX = "Auralis-"
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Published releases with a version tag. Drafts and tags that are not versions are left
         * out. The APK is the asset named `Auralis-….apk`, as the release workflow names it; the
         * older IgniteTunes releases in the same repository have other APK names, so they never
         * count as an Auralis update.
         */
        fun parseReleases(body: String): List<AppRelease> {
            val releases = json.parseToJsonElement(body) as? JsonArray ?: throw IOException("releases: not a list")
            return releases.mapNotNull { element ->
                val release = element as? JsonObject ?: return@mapNotNull null
                if (release.bool("draft") == true) return@mapNotNull null
                val version = release.string("tag_name")?.let(AppVersion::parse) ?: return@mapNotNull null
                val pageUrl = release.string("html_url") ?: return@mapNotNull null
                val apkUrl = (release["assets"] as? JsonArray).orEmpty()
                    .mapNotNull { it as? JsonObject }
                    .firstOrNull { asset ->
                        val name = asset.string("name").orEmpty()
                        name.startsWith(APK_PREFIX, ignoreCase = true) && name.endsWith(".apk", ignoreCase = true)
                    }
                    ?.string("browser_download_url")
                AppRelease(version, pageUrl, apkUrl)
            }
        }

        private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

        private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
    }
}
