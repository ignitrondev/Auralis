/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.provider.api.StreamException
import com.ignitrondev.auralis.provider.youtube.YouTubeMusicProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF090B11)
private val Muted = Color(0xFF989EAD)
private val Accent = Color(0xFFB9F2DD)
private val GlassShape = RoundedCornerShape(24.dp)

@Composable
internal fun AuralisDesktopWindow(onCloseRequest: () -> Unit) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val provider = remember { YouTubeMusicProvider() }
        val playerState = remember { DesktopPlayerState() }
        val audioPlayer = remember {
            DesktopAudioPlayer(
                onPlayingChanged = { playerState.isPlaying = it },
                onError = { playerState.error = it },
            )
        }
        val tracks = remember { androidx.compose.runtime.mutableStateListOf<MusicTrack>() }
        var searchText by remember { mutableStateOf("") }
        val searchFocusRequester = remember { FocusRequester() }
        var loading by remember { mutableStateOf(false) }
        var pageTitle by remember { mutableStateOf("Good evening") }

        fun showTracks(newTracks: List<MusicTrack>, title: String) {
            tracks.clear()
            tracks.addAll(newTracks.distinctBy { it.id })
            pageTitle = title
        }

        fun loadHome() {
            loading = true
            playerState.error = null
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { provider.home() }
                }.onSuccess { feed ->
                    val homeTracks = feed.sections.flatMap { section -> section.items.filterIsInstance<MusicTrack>() }
                    showTracks(homeTracks, "Good evening")
                }.onFailure { error ->
                    playerState.error = error.message ?: "Could not load music right now."
                }
                loading = false
            }
        }

        fun search() {
            val query = searchText.trim()
            if (query.isEmpty()) return
            loading = true
            playerState.error = null
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { provider.search(query) }
                }.onSuccess { results ->
                    showTracks(results.tracks, "Results for “$query”")
                }.onFailure { error ->
                    playerState.error = error.message ?: "Search failed. Please try again."
                }
                loading = false
            }
        }

        fun play(track: MusicTrack) {
            playerState.queue = tracks.toList()
            playerState.currentIndex = playerState.queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            startPlayback(track, playerState, provider, audioPlayer, scope)
        }

        fun playAdjacent(offset: Int) {
            val nextIndex = playerState.currentIndex + offset
            playerState.queue.getOrNull(nextIndex)?.let { track ->
                playerState.currentIndex = nextIndex
                startPlayback(track, playerState, provider, audioPlayer, scope)
            }
        }

        androidx.compose.ui.window.Window(
            onCloseRequest = {
                onCloseRequest()
            },
            title = "Auralis",
        ) {
            DisposableEffect(Unit) {
                loadHome()
                onDispose {
                    audioPlayer.close()
                    provider.close()
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(0xFF202738), Ink, Color(0xFF101C20)))),
            ) {
                Glow(Modifier.align(Alignment.TopEnd).padding(end = 90.dp, top = 36.dp))
                Row(Modifier.fillMaxSize().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Sidebar(onHome = ::loadHome, onSearchFocus = { searchFocusRequester.requestFocus() })
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SearchTopBar(
                            value = searchText,
                            onValueChange = { searchText = it },
                            onSearch = ::search,
                            focusRequester = searchFocusRequester,
                        )
                        GlassPanel(Modifier.weight(1f).fillMaxWidth()) {
                            Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp)) {
                                Text(pageTitle, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text("A little room for the music you love.", color = Muted, fontSize = 14.sp)
                                Spacer(Modifier.height(24.dp))
                                if (loading && tracks.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = Accent)
                                    }
                                } else if (tracks.isEmpty()) {
                                    EmptyState(isLoading = loading, onRetry = ::loadHome)
                                } else {
                                    TrackList(
                                        tracks = tracks,
                                        currentTrack = playerState.currentTrack,
                                        onPlay = ::play,
                                    )
                                }
                            }
                        }
                        playerState.error?.let { ErrorBanner(it) }
                        NowPlayingBar(
                            state = playerState,
                            onPrevious = { playAdjacent(-1) },
                            onToggle = audioPlayer::toggle,
                            onNext = { playAdjacent(1) },
                        )
                    }
                }
            }
        }
    }
}

private class DesktopPlayerState {
    var currentTrack by mutableStateOf<MusicTrack?>(null)
    var isPlaying by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var queue by mutableStateOf<List<MusicTrack>>(emptyList())
    var currentIndex by mutableStateOf(-1)
    var pendingPlayback: Job? = null
}

private fun startPlayback(
    track: MusicTrack,
    state: DesktopPlayerState,
    provider: YouTubeMusicProvider,
    audioPlayer: DesktopAudioPlayer,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    state.error = null
    state.currentTrack = track
    state.isPlaying = false
    state.pendingPlayback?.cancel()
    state.pendingPlayback = scope.launch {
        runCatching {
            withContext(Dispatchers.IO) { provider.resolveStream(track.id) }
        }.onSuccess { stream ->
            audioPlayer.play(stream.url)
        }.onFailure { error ->
            if (error is CancellationException) throw error
            state.isPlaying = false
            state.error = when ((error as? StreamException)?.reason) {
                StreamException.Reason.Blocked -> "YouTube blocked playback while signed out. Desktop sign-in is not available yet."
                StreamException.Reason.Unavailable -> "This track is not available for playback."
                StreamException.Reason.NoStream -> "No playable audio stream was returned for this track."
                StreamException.Reason.Network -> "Network error while starting this track. Please try again."
                null -> error.message ?: "Could not start this track."
            }
        }
    }
}

@Composable
private fun Sidebar(onHome: () -> Unit, onSearchFocus: () -> Unit) {
    GlassPanel(Modifier.width(220.dp).fillMaxHeight()) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(Accent), contentAlignment = Alignment.Center) {
                    Text("A", color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text("AURALIS", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("MUSIC PLAYER", color = Muted, fontSize = 9.sp, letterSpacing = 1.4.sp)
                }
            }
            Spacer(Modifier.height(40.dp))
            NavItem("Home", Icons.Rounded.Home, onHome)
            Spacer(Modifier.height(8.dp))
            NavItem("Search", Icons.Rounded.Search, onSearchFocus)
            Spacer(Modifier.weight(1f))
            Text("DESIGNED TO FEEL QUIET", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Text("Your music, in its own space.", color = Color.White.copy(alpha = .68f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SearchTopBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    focusRequester: FocusRequester,
) {
    GlassPanel(Modifier.fillMaxWidth().height(58.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = Muted)
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box {
                        if (value.isBlank()) Text("Search songs, artists, albums…", color = Muted, fontSize = 15.sp)
                        inner()
                    }
                },
            )
            Text("ENTER", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
        }
    }
}

@Composable
private fun TrackList(tracks: List<MusicTrack>, currentTrack: MusicTrack?, onPlay: (MusicTrack) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#", Modifier.width(38.dp), color = Muted, fontSize = 11.sp)
            Text("TITLE", Modifier.weight(1f), color = Muted, fontSize = 10.sp, letterSpacing = 1.1.sp)
            Text("ALBUM", Modifier.width(180.dp), color = Muted, fontSize = 10.sp, letterSpacing = 1.1.sp)
            Text("LENGTH", Modifier.width(70.dp), color = Muted, fontSize = 10.sp, letterSpacing = 1.1.sp)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                TrackRow(
                    index = index,
                    track = track,
                    selected = currentTrack?.id == track.id,
                    onClick = { onPlay(track) },
                )
            }
        }
    }
}

@Composable
private fun TrackRow(index: Int, track: MusicTrack, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected) Color.White.copy(alpha = .09f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${index + 1}", Modifier.width(38.dp), color = if (selected) Accent else Muted, fontSize = 13.sp)
        Artwork(track.artworkUri, Modifier.size(42.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = if (selected) Accent else Color.White, fontSize = 13.sp, maxLines = 1)
            Text(track.artistNames.ifBlank { "Unknown artist" }, color = Muted, fontSize = 11.sp, maxLines = 1)
        }
        Text(track.album?.title.orEmpty(), Modifier.width(180.dp), color = Muted, fontSize = 11.sp, maxLines = 1)
        Text(formatDuration(track.durationMs), Modifier.width(70.dp), color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun Artwork(uri: String?, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF384859), Color(0xFF17242A)))), contentAlignment = Alignment.Center) {
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text("♪", color = Accent, fontSize = 20.sp)
        }
    }
}

@Composable
private fun NowPlayingBar(state: DesktopPlayerState, onPrevious: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().height(82.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(state.currentTrack?.artworkUri, Modifier.size(48.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(state.currentTrack?.title ?: "Choose a song to start", color = Color.White, fontSize = 13.sp, maxLines = 1)
                Text(state.currentTrack?.artistNames ?: "Music for this moment", color = Muted, fontSize = 11.sp, maxLines = 1)
            }
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = Muted, modifier = Modifier.size(22.dp).clickable(onClick = onPrevious))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(42.dp).clip(CircleShape).background(Accent).clickable(enabled = state.currentTrack != null, onClick = onToggle), contentAlignment = Alignment.Center) {
                Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (state.isPlaying) "Pause" else "Play", tint = Ink)
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Muted, modifier = Modifier.size(22.dp).clickable(onClick = onNext))
        }
    }
}

@Composable
private fun EmptyState(isLoading: Boolean, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isLoading) "Finding your sound…" else "Nothing here yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text("Search for a song or artist to get started.", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Text("REFRESH HOME", color = Accent, fontSize = 10.sp, letterSpacing = 1.1.sp, modifier = Modifier.clickable(onClick = onRetry).padding(8.dp))
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF472E35)).padding(14.dp)) {
        Text(message, color = Color(0xFFFFD5DD), fontSize = 12.sp)
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.clip(GlassShape)
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = .095f), Color.White.copy(alpha = .035f))))
            .border(BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .17f), Color.White.copy(alpha = .045f)))), GlassShape),
    ) { content() }
}

@Composable
private fun Glow(modifier: Modifier = Modifier) {
    Box(modifier.size(280.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF4E8E80).copy(alpha = .18f), Color.Transparent))))
}

private fun formatDuration(durationMs: Long?): String {
    if (durationMs == null || durationMs <= 0) return "—"
    val totalSeconds = durationMs / 1_000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}
