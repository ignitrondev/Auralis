/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.component.GlassButton
import com.ignitrondev.auralis.core.designsystem.component.GlassCard
import com.ignitrondev.auralis.core.designsystem.component.GlassIconButton
import com.ignitrondev.auralis.core.designsystem.component.GlassLayers
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.core.designsystem.theme.ThemeMode

/**
 * Debug-only gallery of the design system. Shows tokens and glass components over a busy,
 * artwork-like backdrop, with a working switch between the dark and light themes.
 */
class DesignSystemGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            var dark by rememberSaveable { mutableStateOf(true) }
            AuralisTheme(themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light) {
                Gallery(dark = dark, onToggleTheme = { dark = !dark })
            }
        }
    }
}

@Composable
private fun Gallery(dark: Boolean, onToggleTheme: () -> Unit) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    GlassLayers(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        overlay = {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Glass over backdrop", style = AuralisTheme.typography.itemTitle)
                    Text(
                        "This card blurs the artwork behind it.",
                        style = AuralisTheme.typography.itemSubtitle,
                        color = colors.contentSecondary,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassButton(
                        text = if (dark) "Switch to light" else "Switch to dark",
                        onClick = onToggleTheme,
                        icon = AuralisIcons.Settings,
                    )
                    GlassIconButton(icon = AuralisIcons.Search, contentDescription = "Search", onClick = {})
                    GlassIconButton(
                        icon = AuralisIcons.Home,
                        contentDescription = "Disabled",
                        onClick = {},
                        enabled = false,
                    )
                }
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 260.dp),
        ) {
            item { FakeArtwork(Modifier.fillMaxWidth().height(360.dp)) }
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = spacing.screenGutter, vertical = spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    Text("Auralis", style = AuralisTheme.typography.wordmark, color = colors.contentPrimary)
                    TypeSample("Screen title", AuralisTheme.typography.screenTitle)
                    TypeSample("Now Playing title", AuralisTheme.typography.nowPlayingTitle)
                    TypeSample("Section title", AuralisTheme.typography.sectionTitle)
                    TypeSample("Item title · Midnight City", AuralisTheme.typography.itemTitle)
                    TypeSample("Item subtitle · M83", AuralisTheme.typography.itemSubtitle, secondary = true)
                    TypeSample("Body text for longer descriptions.", AuralisTheme.typography.body)
                    TypeSample("1:07 / 4:03", AuralisTheme.typography.timestamp, secondary = true)
                    TypeSample("日本語 · 한국어 · العربية fallback", AuralisTheme.typography.itemTitle)
                    Spacer(Modifier.height(spacing.lg))
                    Text("Glass without backdrop (fallback)", style = AuralisTheme.typography.sectionTitle, color = colors.contentPrimary)
                    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                        Text("Tappable card", style = AuralisTheme.typography.itemTitle)
                        Text("Press to see the scale spring.", style = AuralisTheme.typography.itemSubtitle, color = colors.contentSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            GlassIconButton(icon = AuralisIcons.Library, contentDescription = "Nested", onClick = {})
                            GlassButton(text = "Nested button", onClick = {})
                        }
                    }
                    Spacer(Modifier.height(spacing.lg))
                    Text("Color tokens", style = AuralisTheme.typography.sectionTitle, color = colors.contentPrimary)
                    Swatch("contentPrimary", colors.contentPrimary)
                    Swatch("contentSecondary", colors.contentSecondary)
                    Swatch("contentTertiary", colors.contentTertiary)
                    Swatch("selection", colors.selection)
                    Swatch("borderStrong", colors.borderStrong)
                }
            }
            item { FakeArtwork(Modifier.fillMaxWidth().height(480.dp), seed = 1) }
        }
    }
}

@Composable
private fun TypeSample(text: String, style: TextStyle, secondary: Boolean = false) {
    Text(
        text = text,
        style = style,
        color = if (secondary) AuralisTheme.colors.contentSecondary else AuralisTheme.colors.contentPrimary,
    )
}

@Composable
private fun Swatch(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.md)) {
        Box(
            Modifier
                .size(28.dp)
                .background(AuralisTheme.colors.background, RoundedCornerShape(6.dp))
                .background(color, RoundedCornerShape(6.dp)),
        )
        Text(name, style = AuralisTheme.typography.itemSubtitle, color = AuralisTheme.colors.contentSecondary)
    }
}

/** Colorful blobs standing in for album artwork, the only source of color in Auralis. */
@Composable
private fun FakeArtwork(modifier: Modifier, seed: Int = 0) {
    val palette = if (seed == 0) {
        listOf(Color(0xFFE94B3C), Color(0xFFF2B134), Color(0xFF3C8DE9), Color(0xFF7B3CE9))
    } else {
        listOf(Color(0xFF1FBF8F), Color(0xFFE93C8D), Color(0xFFF2E634), Color(0xFF3C4BE9))
    }
    Canvas(modifier) {
        drawRect(palette[3])
        val spots = listOf(Offset(0.2f, 0.25f), Offset(0.8f, 0.3f), Offset(0.45f, 0.8f))
        spots.forEachIndexed { index, spot ->
            val center = Offset(size.width * spot.x, size.height * spot.y)
            drawCircle(
                brush = Brush.radialGradient(listOf(palette[index], Color.Transparent), center, size.minDimension * 0.6f),
                radius = size.minDimension * 0.6f,
                center = center,
            )
        }
        repeat(6) { i ->
            drawRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(size.width * 0.08f, size.height * (0.1f + i * 0.14f)),
                size = Size(size.width * (0.3f + 0.1f * (i % 3)), 10f),
            )
        }
    }
}
