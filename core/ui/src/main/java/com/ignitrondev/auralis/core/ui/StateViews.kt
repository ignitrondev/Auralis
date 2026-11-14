/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.component.GlassButton
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.domain.repository.CatalogException

/** Full-area loading state. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = AuralisTheme.colors.contentSecondary,
            trackColor = AuralisTheme.colors.selection,
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp),
        )
    }
}

/** Full-area error state with a reason-specific message and a retry button (icon plus text, no red). */
@Composable
fun ErrorState(
    reason: CatalogException.Reason,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageState(message = reason.message(), modifier = modifier) {
        GlassButton(text = stringResource(R.string.core_ui_retry), onClick = onRetry, icon = AuralisIcons.Retry)
    }
}

/** Full-area message, for empty results. */
@Composable
fun MessageState(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AuralisTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = AuralisTheme.typography.body,
            color = AuralisTheme.colors.contentSecondary,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}
