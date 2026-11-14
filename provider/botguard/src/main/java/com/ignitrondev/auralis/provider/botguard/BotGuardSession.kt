/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.botguard

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * One running BotGuard instance in a hidden WebView. Created with [start]; mints tokens until
 * [expiresAtMs], then a new session is needed. Must be created and used on the main thread
 * except where a function switches context itself.
 */
internal class BotGuardSession private constructor(private val webView: WebView) {

    private val bridge = Bridge()
    private val requestIds = AtomicLong()
    var expiresAtMs: Long = 0L
        private set

    val isExpired: Boolean get() = SystemClock.elapsedRealtime() >= expiresAtMs

    /** Mints a base64url token bound to [identifier]. */
    suspend fun mint(identifier: String): String = withContext(Dispatchers.Main) {
        val id = "t${requestIds.incrementAndGet()}"
        val result = CompletableDeferred<String>()
        bridge.pending[id] = result
        webView.evaluateJavascript(
            """(function(){auralisMint(${BotGuardProtocol.uint8Array(identifier)}).then(
                function(t){AuralisBotGuard.onToken("$id", t.join(","))},
                function(e){AuralisBotGuard.onTokenError("$id", String(e))});})()""",
            null,
        )
        try {
            BotGuardProtocol.tokenFromBytes(withTimeout(MINT_TIMEOUT_MS) { result.await() })
        } finally {
            bridge.pending.remove(id)
        }
    }

    fun destroy() {
        webView.stopLoading()
        webView.removeJavascriptInterface(BRIDGE_NAME)
        webView.destroy()
    }

    /** Callbacks from the page. Called on the WebView's JavaBridge thread. */
    private class Bridge {
        val pageLoaded = CompletableDeferred<Unit>()
        val snapshot = CompletableDeferred<String>()
        val minterReady = CompletableDeferred<Unit>()
        val pending = ConcurrentHashMap<String, CompletableDeferred<String>>()

        @JavascriptInterface
        fun onSnapshot(response: String) {
            snapshot.complete(response)
        }

        @JavascriptInterface
        fun onMinterReady() {
            minterReady.complete(Unit)
        }

        @JavascriptInterface
        fun onStartError(message: String) {
            val error = BotGuardException("BotGuard failed: ${message.take(200)}")
            snapshot.completeExceptionally(error)
            minterReady.completeExceptionally(error)
        }

        @JavascriptInterface
        fun onToken(id: String, bytes: String) {
            pending[id]?.complete(bytes)
        }

        @JavascriptInterface
        fun onTokenError(id: String, message: String) {
            pending[id]?.completeExceptionally(BotGuardException("mint failed: ${message.take(200)}"))
        }
    }

    companion object {
        private const val BRIDGE_NAME = "AuralisBotGuard"
        private const val START_TIMEOUT_MS = 45_000L
        private const val MINT_TIMEOUT_MS = 15_000L

        /** Refresh this long before the integrity token expires. */
        private const val EXPIRY_MARGIN_MS = 10 * 60 * 1000L

        /** BotGuard expects a desktop browser. */
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        /** Loads the page, runs BotGuard, exchanges the result for an integrity token and creates the minter. */
        @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
        suspend fun start(context: Context): BotGuardSession {
            val html = withContext(Dispatchers.IO) {
                context.assets.open("botguard.html").bufferedReader().use { it.readText() }
            }
            val session = withContext(Dispatchers.Main) {
                val webView = WebView(context.applicationContext)
                val created = BotGuardSession(webView)
                webView.settings.apply {
                    javaScriptEnabled = true
                    userAgentString = USER_AGENT
                    allowFileAccess = false
                    allowContentAccess = false
                    blockNetworkImage = true
                }
                webView.webViewClient = object : WebViewClient() {
                    // Only our own page is ever shown: block every navigation.
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true

                    override fun onPageFinished(view: WebView, url: String?) {
                        created.bridge.pageLoaded.complete(Unit)
                    }
                }
                webView.addJavascriptInterface(created.bridge, BRIDGE_NAME)
                webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
                created
            }
            try {
                withTimeout(START_TIMEOUT_MS) { session.initialize() }
            } catch (error: Throwable) {
                withContext(Dispatchers.Main) { session.destroy() }
                throw error
            }
            return session
        }
    }

    private suspend fun initialize() {
        bridge.pageLoaded.await()
        val challenge = BotGuardProtocol.parseChallenge(post(BotGuardProtocol.CREATE_URL, BotGuardProtocol.createBody()))
        val interpreterJs = challenge.interpreterJs
            ?: challenge.interpreterUrl?.let { get(it) }
            ?: throw BotGuardException("challenge has no interpreter")
        val startArgument = JsonObject(
            mapOf(
                "interpreterJs" to JsonPrimitive(interpreterJs),
                "program" to JsonPrimitive(challenge.program),
                "globalName" to JsonPrimitive(challenge.globalName),
            ),
        ).toString()
        withContext(Dispatchers.Main) {
            webView.evaluateJavascript(
                """auralisStart($startArgument).then(
                    function(r){AuralisBotGuard.onSnapshot(r)},
                    function(e){AuralisBotGuard.onStartError(String(e))});""",
                null,
            )
        }
        val botguardResponse = bridge.snapshot.await()
        val integrity = BotGuardProtocol.parseIntegrityToken(
            post(BotGuardProtocol.GENERATE_URL, BotGuardProtocol.generateBody(botguardResponse)),
        )
        withContext(Dispatchers.Main) {
            webView.evaluateJavascript(
                """auralisCreateMinter(${integrity.jsBytes}).then(
                    function(){AuralisBotGuard.onMinterReady()},
                    function(e){AuralisBotGuard.onStartError(String(e))});""",
                null,
            )
        }
        bridge.minterReady.await()
        expiresAtMs = SystemClock.elapsedRealtime() + (integrity.lifetimeSeconds * 1000 - EXPIRY_MARGIN_MS).coerceAtLeast(60_000)
    }

    private suspend fun post(url: String, body: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json+protobuf")
            connection.setRequestProperty("x-goog-api-key", BotGuardProtocol.webApiKey)
            connection.setRequestProperty("x-user-agent", "grpc-web-javascript/0.1")
            connection.outputStream.use { it.write(body.toByteArray()) }
            if (connection.responseCode != 200) throw BotGuardException("HTTP ${connection.responseCode}")
            connection.inputStream.bufferedReader().use { it.readText() }.ifEmpty { throw BotGuardException("empty response") }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(BotGuardProtocol.trustedInterpreterUrl(url)).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode != 200) throw BotGuardException("interpreter HTTP ${connection.responseCode}")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
