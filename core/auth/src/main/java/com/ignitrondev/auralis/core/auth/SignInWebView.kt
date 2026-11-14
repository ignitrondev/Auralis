/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.auth

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ignitrondev.auralis.provider.api.ProviderSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private const val SIGN_IN_URL = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"
private const val MUSIC_ORIGIN = "https://music.youtube.com"

/**
 * Google's own sign-in page in a WebView (spec 12.1). Auralis never sees the password: it only
 * reads the YouTube Music session once the page lands on music.youtube.com, then wipes the
 * WebView's cookies so the encrypted vault holds the only copy.
 *
 * @param onSession Called once with the captured session.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SignInWebView(
    onSession: (ProviderSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    val captured = remember { booleanArrayOf(false) }
    DisposableEffect(Unit) {
        // Start clean: never reuse a previous account's cookies.
        clearWebSession()
        onDispose { clearWebSession() }
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                CookieManager.getInstance().setAcceptCookie(true)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        val uri = url?.let(Uri::parse) ?: return
                        if (captured[0] || uri.scheme != "https" || uri.host != "music.youtube.com") return
                        val cookie = CookieManager.getInstance().getCookie(MUSIC_ORIGIN).orEmpty()
                        if ("SAPISID=" !in cookie && "__Secure-3PAPISID=" !in cookie) return
                        view.evaluateJavascript(
                            "(function(){var c=window.yt&&window.yt.config_;return c?JSON.stringify([c.VISITOR_DATA||null,c.DATASYNC_ID||null,String(c.SESSION_INDEX||0)]):null})()",
                        ) { result ->
                            if (captured[0]) return@evaluateJavascript
                            val values = parseConfig(result)
                            captured[0] = true
                            onSession(ProviderSession(cookie, values[0], values[1], values[2] ?: "0"))
                        }
                    }
                }
                loadUrl(SIGN_IN_URL)
            }
        },
    )
}

/** evaluateJavascript returns a JSON string literal holding our JSON array (or "null"). */
private fun parseConfig(result: String?): List<String?> = try {
    val inner = (Json.parseToJsonElement(result ?: "null") as? JsonPrimitive)?.contentOrNull
    val array = inner?.let { Json.parseToJsonElement(it) as? JsonArray }
    List(3) { index -> (array?.getOrNull(index) as? JsonPrimitive)?.contentOrNull?.takeIf { it != "null" } }
} catch (error: Exception) {
    listOf(null, null, null)
}
