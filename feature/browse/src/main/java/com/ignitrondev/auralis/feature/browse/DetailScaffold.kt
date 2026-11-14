/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.component.Artwork
import com.ignitrondev.auralis.core.designsystem.component.GlassIconButton
import com.ignitrondev.auralis.core.designsystem.component.LocalBottomChromePadding
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.core.ui.ErrorState
import com.ignitrondev.auralis.core.ui.LoadingState
import com.ignitrondev.auralis.core.ui.R as UiR

/** Shared frame for detail pages: state handling, a back button over the content, and insets. */
@Composable
internal fun <T> DetailScaffold(
    state: CatalogUiState<T>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    content: LazyListScope.(T) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            CatalogUiState.Loading -> LoadingState()
            is CatalogUiState.Error -> ErrorState(state.reason, onRetry)
            is CatalogUiState.Content -> {
                val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = top + AuralisTheme.spacing.xxxl,
                        bottom = LocalBottomChromePadding.current + AuralisTheme.spacing.xl,
                    ),
                ) {
                    content(state.data)
                }
            }
        }
        GlassIconButton(
            icon = AuralisIcons.Back,
            contentDescription = stringResource(UiR.string.core_ui_back),
            onClick = onBack,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(AuralisTheme.spacing.sm),
        )
    }
}

/** Large artwork with title and supporting lines, centered. Artists get round artwork. */
@Composable
internal fun DetailHeader(
    artworkUri: String?,
    title: String,
    lines: List<String>,
    circular: Boolean = false,
) {
    val spacing = AuralisTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenGutter, vertical = spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Artwork(
            uri = artworkUri,
            contentDescription = null,
            shape = if (circular) CircleShape else AuralisTheme.shapes.large,
            modifier = Modifier.size(232.dp),
        )
        Text(
            text = title,
            style = AuralisTheme.typography.nowPlayingTitle,
            color = AuralisTheme.colors.contentPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = spacing.sm)
                .semantics { heading() },
        )
        lines.filter { it.isNotBlank() }.forEach { line ->
            Text(
                text = line,
                style = AuralisTheme.typography.itemSubtitle,
                color = AuralisTheme.colors.contentSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
