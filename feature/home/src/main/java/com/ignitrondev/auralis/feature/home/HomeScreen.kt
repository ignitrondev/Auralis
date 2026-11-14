/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ignitrondev.auralis.core.designsystem.component.ScreenPlaceholder
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    ScreenPlaceholder(
        title = stringResource(R.string.feature_home_title),
        message = stringResource(R.string.feature_home_placeholder),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun HomeScreenPreview() {
    AuralisTheme {
        HomeScreen()
    }
}
