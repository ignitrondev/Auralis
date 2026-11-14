/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme

/**
 * Temporary screen body for destinations whose real content is not built yet.
 * Shows only text, so it never looks like a working control.
 */
@Composable
fun ScreenPlaceholder(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AuralisTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = AuralisTheme.typography.screenTitle,
            color = AuralisTheme.colors.contentPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = message,
            style = AuralisTheme.typography.body,
            color = AuralisTheme.colors.contentSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
