/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.share

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ignitrondev.auralis.core.designsystem.component.GlassButton
import com.ignitrondev.auralis.core.designsystem.component.GlassIconButton
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen share sheet: a preview of the exact card that will be sent, then **Share to…**
 * (the system share sheet: Instagram Stories, WhatsApp Status, X, chats…) and **Copy link**.
 * Nothing is sent until the user picks an app.
 */
@Composable
fun ShareSheet(card: ShareCard, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BackHandler(onBack = onDismiss)
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val chooserTitle = stringResource(R.string.core_share_to)
        var image by remember(card) { mutableStateOf<Bitmap?>(null) }
        var failed by remember(card) { mutableStateOf(false) }
        var copied by remember(card) { mutableStateOf(false) }
        LaunchedEffect(card) {
            image = try {
                val artwork = ShareActions.loadArtwork(context, card.artworkUri)
                withContext(Dispatchers.Default) { ShareCardRenderer.render(context, card, artwork) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failed = true
                null
            }
        }
        val spacing = AuralisTheme.spacing
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AuralisTheme.colors.background.copy(alpha = SCRIM_ALPHA))
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.screenGutter, vertical = spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(icon = AuralisIcons.Close, contentDescription = stringResource(R.string.core_share_close), onClick = onDismiss)
                    Text(
                        text = stringResource(R.string.core_share_title),
                        style = AuralisTheme.typography.sectionTitle,
                        color = AuralisTheme.colors.contentPrimary,
                        modifier = Modifier.padding(start = spacing.md),
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(targetState = image, animationSpec = AuralisTheme.motion.fade(), label = "card") { ready ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(ShareCardRenderer.WIDTH / ShareCardRenderer.HEIGHT.toFloat())
                                .clip(AuralisTheme.shapes.large)
                                .background(AuralisTheme.colors.selection),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                ready != null -> Image(
                                    bitmap = ready.asImageBitmap(),
                                    contentDescription = stringResource(R.string.core_share_preview),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                failed -> Text(
                                    text = stringResource(R.string.core_share_failed),
                                    style = AuralisTheme.typography.body,
                                    color = AuralisTheme.colors.contentSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(spacing.lg),
                                )
                                else -> CircularProgressIndicator(
                                    color = AuralisTheme.colors.contentSecondary,
                                    trackColor = AuralisTheme.colors.selection,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    GlassButton(
                        text = chooserTitle,
                        icon = AuralisIcons.Share,
                        enabled = image != null,
                        onClick = { image?.let { scope.launch { ShareActions.share(context, card, it, chooserTitle) } } },
                    )
                    card.link?.let { link ->
                        GlassButton(
                            text = stringResource(if (copied) R.string.core_share_link_copied else R.string.core_share_copy_link),
                            icon = if (copied) AuralisIcons.Saved else AuralisIcons.Link,
                            onClick = {
                                ShareActions.copyLink(context, link)
                                copied = true
                            },
                        )
                    }
                }
            }
        }
    }
}

private const val SCRIM_ALPHA = 0.96f
