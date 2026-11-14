/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignitrondev.auralis.core.auth.SignInWebView
import com.ignitrondev.auralis.core.designsystem.component.Artwork
import com.ignitrondev.auralis.core.designsystem.component.GlassButton
import com.ignitrondev.auralis.core.designsystem.component.GlassCard
import com.ignitrondev.auralis.core.designsystem.component.GlassIconButton
import com.ignitrondev.auralis.core.designsystem.component.LocalBottomChromePadding
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.core.ui.LoadingState
import com.ignitrondev.auralis.core.ui.MessageState
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.core.ui.R as UiR

@Composable
fun SettingsScreen(
    onSignIn: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.authState.collectAsStateWithLifecycle()
    val spacing = AuralisTheme.spacing
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = spacing.screenGutter,
                end = spacing.screenGutter,
                top = top + spacing.xl,
                bottom = LocalBottomChromePadding.current + spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Text(stringResource(R.string.feature_settings_title), style = AuralisTheme.typography.screenTitle, color = AuralisTheme.colors.contentPrimary)
        Text(stringResource(R.string.feature_settings_account), style = AuralisTheme.typography.sectionTitle, color = AuralisTheme.colors.contentPrimary)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            AccountContent(state = state, onSignIn = onSignIn, onSignOut = viewModel::signOut)
        }
        Text(stringResource(R.string.feature_settings_updates), style = AuralisTheme.typography.sectionTitle, color = AuralisTheme.colors.contentPrimary)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            val update by viewModel.update.collectAsStateWithLifecycle()
            UpdatesContent(
                installedVersion = viewModel.installedVersion,
                update = update,
                onCheck = viewModel::checkForUpdates,
            )
        }
        Text(
            stringResource(R.string.feature_settings_privacy),
            style = AuralisTheme.typography.bodySmall,
            color = AuralisTheme.colors.contentTertiary,
        )
    }
}

@Composable
private fun AccountContent(state: AuthState, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    when (state) {
        AuthState.Restoring -> Text(stringResource(R.string.feature_settings_checking_account), style = AuralisTheme.typography.body, color = colors.contentSecondary)
        AuthState.SignedOut, AuthState.SessionExpired -> Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Text(
                stringResource(if (state == AuthState.SignedOut) R.string.feature_settings_signed_out else R.string.feature_settings_session_expired),
                style = AuralisTheme.typography.body,
                color = colors.contentSecondary,
            )
            GlassButton(
                text = stringResource(if (state == AuthState.SignedOut) R.string.feature_settings_sign_in else R.string.feature_settings_sign_in_again),
                onClick = onSignIn,
            )
        }
        is AuthState.SignedIn -> Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                Artwork(uri = state.avatarUrl, contentDescription = null, shape = CircleShape, modifier = Modifier.size(48.dp))
                Column {
                    Text(state.name, style = AuralisTheme.typography.itemTitle, color = colors.contentPrimary)
                    state.handle?.let { Text(it, style = AuralisTheme.typography.itemSubtitle, color = colors.contentSecondary) }
                }
            }
            GlassButton(text = stringResource(R.string.feature_settings_sign_out), onClick = onSignOut)
        }
    }
}

/** Google's sign-in page; leaves as soon as the session is confirmed. */
@Composable
fun SignInScreen(
    onDone: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state == SignInState.Done) onDone() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        when (state) {
            SignInState.Browsing -> SignInWebView(
                onSession = viewModel::onSession,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp, bottom = LocalBottomChromePadding.current),
            )
            SignInState.Checking, SignInState.Done -> LoadingState()
            SignInState.Failed -> MessageState(stringResource(R.string.feature_settings_sign_in_failed)) {
                GlassButton(text = stringResource(R.string.feature_settings_sign_in_again), onClick = viewModel::retry)
            }
        }
        GlassIconButton(
            icon = AuralisIcons.Back,
            contentDescription = stringResource(UiR.string.core_ui_back),
            onClick = onDone,
            modifier = Modifier.padding(AuralisTheme.spacing.sm),
        )
    }
}

@Composable
private fun UpdatesContent(installedVersion: String, update: UpdateState, onCheck: () -> Unit) {
    val colors = AuralisTheme.colors
    val context = LocalContext.current
    val open = { url: String -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    Column(verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.md)) {
        Text(
            stringResource(R.string.feature_settings_installed_version, installedVersion),
            style = AuralisTheme.typography.body,
            color = colors.contentPrimary,
        )
        val message = when (update) {
            UpdateState.Idle -> null
            UpdateState.Checking -> stringResource(R.string.feature_settings_checking_updates)
            UpdateState.UpToDate -> stringResource(R.string.feature_settings_up_to_date)
            is UpdateState.Available -> stringResource(R.string.feature_settings_update_available, update.version)
            UpdateState.Failed -> stringResource(R.string.feature_settings_update_failed)
        }
        message?.let {
            Text(
                it,
                style = AuralisTheme.typography.body,
                // An available update reads as news, like the dot on the tab; other results are quiet.
                color = if (update is UpdateState.Available) colors.contentPrimary else colors.contentSecondary,
            )
        }
        if (update is UpdateState.Available) {
            GlassButton(
                text = stringResource(R.string.feature_settings_download_update),
                icon = AuralisIcons.Download,
                onClick = { open(update.downloadUrl) },
            )
            GlassButton(text = stringResource(R.string.feature_settings_release_notes), onClick = { open(update.pageUrl) })
        } else {
            GlassButton(
                text = stringResource(R.string.feature_settings_check_for_updates),
                icon = AuralisIcons.Retry,
                onClick = onCheck,
                enabled = update != UpdateState.Checking,
            )
        }
        Text(
            stringResource(R.string.feature_settings_update_auto_check),
            style = AuralisTheme.typography.bodySmall,
            color = colors.contentTertiary,
        )
    }
}
