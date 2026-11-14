/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.debug

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.ignitrondev.auralis.core.designsystem.component.GlassButton
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.provider.botguard.WebViewAttestationProvider
import com.ignitrondev.auralis.provider.youtube.YouTubeMusicProvider
import kotlinx.coroutines.launch

/**
 * Debug-only playback diagnostics: BotGuard and stream resolution, with the integration's
 * warnings (which clients were refused and why). Shows statuses only; never tokens or stream URLs. Has its own launcher entry so it
 * can be run on any network without adb.
 */
class PlaybackDiagnosticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AuralisTheme { Diagnostics() } }
    }

    @Composable
    private fun Diagnostics() {
        val lines = remember { mutableStateListOf<String>() }
        val running = remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AuralisTheme.colors.background)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(AuralisTheme.spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.sm),
        ) {
            Text("Playback diagnostics", style = AuralisTheme.typography.screenTitle, color = AuralisTheme.colors.contentPrimary)
            GlassButton(
                text = if (running.value) "Running…" else "Run",
                enabled = !running.value,
                onClick = {
                    lines.clear()
                    running.value = true
                    scope.launch {
                        runDiagnostics { lines += it }
                        running.value = false
                    }
                },
            )
            lines.forEach { line ->
                Text(line, style = AuralisTheme.typography.bodySmall, color = AuralisTheme.colors.contentSecondary)
            }
        }
    }

    private suspend fun runDiagnostics(log: (String) -> Unit) {
        log("network: ${networkType()}")
        val attestation = WebViewAttestationProvider(this)
        val start = System.currentTimeMillis()
        val token = attestation.mint("diagnostics")
        log("botguard: ${if (token != null) "ok" else "failed"} (${System.currentTimeMillis() - start} ms)")
        YouTubeMusicProvider(attestation) { warning, tag, message -> if (warning) log("$tag: $message") }.use { provider ->
            val resolveStart = System.currentTimeMillis()
            runCatching { provider.resolveStream(SAMPLE_VIDEO) }
                .onSuccess { log("stream: OK ${it.mimeType} (${System.currentTimeMillis() - resolveStart} ms)") }
                .onFailure { log("stream: FAILED ${it.message}") }
        }
    }

    private fun networkType(): String = runCatching { activeNetworkType() }.getOrDefault("unknown")

    private fun activeNetworkType(): String {
        val manager = getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return "none"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile data"
            else -> "other"
        }
    }

    private companion object {
        /** "Midnight City" by M83. */
        const val SAMPLE_VIDEO = "dX3k_QDnzHE"
    }
}
