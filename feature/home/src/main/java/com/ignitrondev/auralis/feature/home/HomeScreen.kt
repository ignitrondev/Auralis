/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignitrondev.auralis.core.designsystem.component.Artwork
import com.ignitrondev.auralis.core.designsystem.component.GlassIconButton
import com.ignitrondev.auralis.core.designsystem.component.GlassLayers
import com.ignitrondev.auralis.core.designsystem.component.GlassSurface
import com.ignitrondev.auralis.core.designsystem.component.LocalBottomChromePadding
import com.ignitrondev.auralis.core.designsystem.component.rememberArtworkWash
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.core.ui.CatalogActions
import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.core.ui.ErrorState
import com.ignitrondev.auralis.core.ui.MessageState
import com.ignitrondev.auralis.core.ui.SectionCarousel
import com.ignitrondev.auralis.core.ui.SectionTitle
import com.ignitrondev.auralis.domain.model.CatalogSection
import com.ignitrondev.auralis.domain.model.HomeFeed
import com.ignitrondev.auralis.domain.model.Mood
import com.ignitrondev.auralis.domain.model.MusicTrack
import java.time.LocalTime
import com.ignitrondev.auralis.core.designsystem.R as DesignR

@Composable
fun HomeDestination(
    actions: CatalogActions,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        actions = actions,
        onRetry = viewModel::retry,
        onSelectMood = viewModel::selectMood,
        onTogglePlay = viewModel::togglePlayPause,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

/**
 * Home: a greeting, mood chips, the song to continue, then the feed. The feed's first all-song
 * shelf is shown as a compact two-column grid ("Quick picks"); other shelves are carousels.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: CatalogActions,
    onRetry: () -> Unit,
    onSelectMood: (Mood) -> Unit,
    onTogglePlay: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AuralisTheme.spacing
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 0.dp, bottom = LocalBottomChromePadding.current + spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        item(key = "top") {
            TopGroup(
                state = state,
                topInset = top,
                onSelectMood = onSelectMood,
                onTogglePlay = onTogglePlay,
                onOpenSettings = onOpenSettings,
            )
        }
        when (val feed = state.feed) {
            CatalogUiState.Loading -> item(key = "loading") { FeedLoading() }
            is CatalogUiState.Error -> item(key = "error") {
                ErrorState(feed.reason, onRetry, Modifier.height(FeedMessageHeight))
            }
            is CatalogUiState.Content -> feedItems(feed.data, state.selectedMood != null, actions)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.feedItems(feed: HomeFeed, mood: Boolean, actions: CatalogActions) {
    if (feed.sections.isEmpty()) {
        item(key = "empty") {
            MessageState(
                stringResource(if (mood) R.string.feature_home_empty_mood else R.string.feature_home_empty),
                Modifier.height(FeedMessageHeight),
            )
        }
        return
    }
    val quick = feed.sections.firstOrNull { section -> section.items.size >= QUICK_MIN && section.items.all { it is MusicTrack } }
    quick?.let { section ->
        item(key = "quick") { QuickPicks(section, actions) }
    }
    itemsIndexed(feed.sections.filter { it !== quick }, key = { index, section -> "$index:${section.title}" }) { _, section ->
        SectionCarousel(section = section, actions = actions)
    }
}

/** Greeting, chips and the continue card, over a wash of the playing song's artwork. */
@Composable
private fun TopGroup(
    state: HomeUiState,
    topInset: androidx.compose.ui.unit.Dp,
    onSelectMood: (Mood) -> Unit,
    onTogglePlay: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val spacing = AuralisTheme.spacing
    val colors = AuralisTheme.colors
    val playing = state.continueListening
    val wash by rememberArtworkWash(playing?.track?.artworkUri)
    val washAlpha by animateFloatAsState(if (wash != null) WASH_ALPHA else 0f, AuralisTheme.motion.fade(), label = "home wash")
    Box(modifier = Modifier.fillMaxWidth()) {
        // Artwork is the only colour on the screen (§8.2.1): the wash fades into the ground.
        wash?.let { image ->
            Box(Modifier.matchParentSize()) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = washAlpha },
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background))),
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = topInset + spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Header(playing != null, state.accountInitial, onOpenSettings)
            if (state.moods.isNotEmpty()) MoodChips(state.moods, state.selectedMood, onSelectMood)
            if (playing != null) ContinueCard(playing, onTogglePlay)
        }
    }
}

@Composable
private fun Header(hasPlayer: Boolean, initial: String?, onOpenSettings: () -> Unit) {
    val spacing = AuralisTheme.spacing
    val greeting = stringResource(
        when (LocalTime.now().hour) {
            in 5..11 -> R.string.feature_home_greeting_morning
            in 12..17 -> R.string.feature_home_greeting_afternoon
            else -> R.string.feature_home_greeting_evening
        },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenGutter),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = greeting,
                style = AuralisTheme.typography.screenTitle,
                color = AuralisTheme.colors.contentPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(if (hasPlayer) R.string.feature_home_subtitle_continue else R.string.feature_home_subtitle),
                style = AuralisTheme.typography.body,
                color = AuralisTheme.colors.contentSecondary,
            )
        }
        // Account: the initial when signed in, the settings icon otherwise. Opens Settings.
        GlassIconButton(
            icon = AuralisIcons.Settings,
            contentDescription = stringResource(R.string.feature_home_account),
            onClick = onOpenSettings,
            content = initial?.let { letter ->
                {
                    Text(text = letter, style = AuralisTheme.typography.sectionTitle, color = AuralisTheme.colors.contentPrimary)
                }
            },
        )
    }
}

@Composable
private fun MoodChips(moods: List<Mood>, selected: String?, onSelect: (Mood) -> Unit) {
    val spacing = AuralisTheme.spacing
    val colors = AuralisTheme.colors
    LazyRow(
        contentPadding = PaddingValues(horizontal = spacing.screenGutter),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        items(moods, key = { it.id }) { mood ->
            val isSelected = mood.id == selected
            Text(
                text = mood.label,
                style = AuralisTheme.typography.label,
                // Selected is a filled chip with inverted text: shape and contrast, not hue (§8.2.1).
                color = if (isSelected) colors.background else colors.contentPrimary,
                maxLines = 1,
                modifier = Modifier
                    .clip(AuralisTheme.shapes.capsule)
                    .background(if (isSelected) colors.contentPrimary else colors.selection)
                    .border(1.dp, if (isSelected) colors.contentPrimary else colors.borderSubtle, AuralisTheme.shapes.capsule)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(mood) })
                    .padding(horizontal = spacing.lg, vertical = spacing.sm + spacing.xxs),
            )
        }
    }
}

/** The song in the player as a card: its cover fills it, controls sit on glass over the cover. */
@Composable
private fun ContinueCard(playing: ContinueListening, onTogglePlay: () -> Unit) {
    val spacing = AuralisTheme.spacing
    val track = playing.track
    GlassLayers(
        modifier = Modifier
            .padding(horizontal = spacing.screenGutter)
            .fillMaxWidth()
            .height(ContinueCardHeight)
            .clip(AuralisTheme.shapes.extraLarge),
        overlay = {
            GlassSurface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(spacing.md),
                shape = AuralisTheme.shapes.capsule,
            ) {
                Text(
                    text = stringResource(R.string.feature_home_continue_listening),
                    style = AuralisTheme.typography.labelSmall,
                    color = AuralisTheme.colors.contentPrimary,
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs + spacing.xxs),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(spacing.lg),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = AuralisTheme.typography.nowPlayingTitle,
                        color = AuralisTheme.colors.contentPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artistNames,
                        style = AuralisTheme.typography.body,
                        color = AuralisTheme.colors.contentSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                GlassIconButton(
                    icon = if (playing.isPlaying) AuralisIcons.Pause else AuralisIcons.Play,
                    contentDescription = stringResource(if (playing.isPlaying) DesignR.string.core_designsystem_pause else DesignR.string.core_designsystem_play),
                    onClick = onTogglePlay,
                    size = 56.dp,
                )
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            Artwork(uri = track.artworkUri, contentDescription = null, shape = AuralisTheme.shapes.extraLarge, modifier = Modifier.fillMaxSize())
            // Darkens the lower part so the title reads over any cover.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)), startY = 120f)),
            )
        }
    }
}

/** Songs as compact rows in two columns. Tapping one plays it with the rest of the grid as the queue. */
@Composable
private fun QuickPicks(section: CatalogSection, actions: CatalogActions) {
    val spacing = AuralisTheme.spacing
    val tracks = section.items.filterIsInstance<MusicTrack>().take(QUICK_MAX)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        SectionTitle(section.title ?: stringResource(R.string.feature_home_quick_picks))
        Column(
            modifier = Modifier.padding(horizontal = spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            tracks.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    pair.forEach { track ->
                        QuickPick(track, actions.onClick(track, tracks), Modifier.weight(1f))
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickPick(track: MusicTrack, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val spacing = AuralisTheme.spacing
    val colors = AuralisTheme.colors
    Row(
        modifier = modifier
            .clip(AuralisTheme.shapes.medium)
            .background(colors.selection)
            .border(1.dp, colors.borderSubtle, AuralisTheme.shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(spacing.xs + spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm + spacing.xxs),
    ) {
        Artwork(uri = track.artworkUri, contentDescription = null, shape = AuralisTheme.shapes.small, modifier = Modifier.size(QuickArtworkSize))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = AuralisTheme.typography.itemSubtitle.copy(color = colors.contentPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artistNames,
                style = AuralisTheme.typography.labelSmall,
                color = colors.contentSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FeedLoading() {
    Box(Modifier.fillMaxWidth().height(FeedMessageHeight), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = AuralisTheme.colors.contentSecondary,
            trackColor = AuralisTheme.colors.selection,
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp),
        )
    }
}

private val FeedMessageHeight = 320.dp
private val ContinueCardHeight = 214.dp
private val QuickArtworkSize = 48.dp
private const val QUICK_MIN = 4
private const val QUICK_MAX = 8
private const val WASH_ALPHA = 0.45f
