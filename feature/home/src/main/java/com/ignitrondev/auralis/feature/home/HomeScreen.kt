/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme

/** Placeholder home screen. Real content arrives with the YouTube Music integration (Phase 3). */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Auralis",
            style = MaterialTheme.typography.displaySmall,
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    AuralisTheme {
        HomeScreen()
    }
}
