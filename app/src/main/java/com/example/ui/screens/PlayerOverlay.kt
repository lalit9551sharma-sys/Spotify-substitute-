package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.MusicViewModel
import kotlinx.coroutines.launch

@Composable
fun PlayerOverlay(
    viewModel: MusicViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val isRepeat by viewModel.isRepeat.collectAsState()
    val visualizerAmplitudes by viewModel.visualizerAmplitudes.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    if (currentTrack == null) return

    val track = currentTrack!!

    // Parse lyrics
    val parsedLyrics = remember(track.lyrics) {
        parseLyrics(track.lyrics)
    }

    // Active Tab in Player (0 = Visualizer / Speed, 1 = Synchronized Lyrics)
    var selectedTab by remember { mutableStateOf(0) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }

    // Breathing Animation Scale for Disk Cover Art based on playback
    val infiniteTransition = rememberInfiniteTransition(label = "Disk Scale")
    val animScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Disk Scale"
    )
    val finalScale = if (isPlaying) animScale else 1.0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .frostedMeshBackground()
    ) {
        // --- 1. Immersive Ambient Background Blur Cover ---
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(track.coverUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.25f
        )

        // Gradient shielding layer for high contrast text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black
                        )
                    )
                )
        )

        // --- 2. Main Visual Layout Content Scrollable/Grid ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // --- HEADER ACTION ROW ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = track.album,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { showAddToPlaylistSheet = true }) {
                    Icon(
                        imageVector = Icons.Outlined.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- DECORATIVE DYNAMIC VINYL OR ALPIN ALBUM CONTAINER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .size(280.dp)
                        .scale(finalScale)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .testTag("full_player_cover"),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Art cover large",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TITLE, ARTIST, LIKE ACTION ROW ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleLike(track) },
                    modifier = Modifier.testTag("full_player_like_button")
                ) {
                    Icon(
                        imageVector = if (track.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like Song",
                        tint = if (track.isLiked) Color(0xFFFF4081) else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TAB SELECTOR (VISUALIZER VS LYRICS) ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty() && selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                ) {
                    Text("Visualizer", modifier = Modifier.padding(vertical = 10.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                ) {
                    Text("Live Lyrics", modifier = Modifier.padding(vertical = 10.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // --- TAB CONTENT ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                if (selectedTab == 0) {
                    // --- Dynamic Neon Visualizer Block ---
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Buffering audio network nodes...", color = Color.Gray, fontSize = 12.sp)
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                visualizerAmplitudes.forEachIndexed { i, amp ->
                                    val barHeight = (amp * 110.dp.value).dp
                                    val animHeight by animateDpAsState(
                                        targetValue = barHeight,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                                        label = "Visualizer Bar #$i"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(10.dp)
                                            .height(animHeight)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0xFF818CF8), // Glowing Indigo
                                                        Color(0xFF6366F1)  // Classic Indigo gradient
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Acoustic Spectrum Stream • Free play in high fidelity",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    // --- Live Scrolling Lyrics Engine ---
                    if (parsedLyrics.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Instrumental Session.", color = Color.Gray)
                        }
                    } else {
                        val activeLineIndex = remember(currentPosition, parsedLyrics) {
                            var index = -1
                            for (i in parsedLyrics.indices) {
                                if (currentPosition >= parsedLyrics[i].timestampMs) {
                                    index = i
                                } else {
                                    break
                                }
                            }
                            index
                        }

                        val listState = rememberLazyListState()
                        LaunchedEffect(activeLineIndex) {
                            if (activeLineIndex >= 0 && activeLineIndex < parsedLyrics.size) {
                                try {
                                    listState.animateScrollToItem(activeLineIndex)
                                } catch (e: Exception) {
                                    android.util.Log.e("PlayerOverlay", "Error scrolling to active lyic line", e)
                                }
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 40.dp)
                        ) {
                            itemsIndexed(parsedLyrics) { index, line ->
                                val isActive = index == activeLineIndex
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isActive) 24.sp else 18.sp,
                                        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Gray.copy(alpha = 0.5f)
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. TIMELINE ADJUSTMENT SLIDER SLOTS ---
            Column(modifier = Modifier.fillMaxWidth()) {
                var sliderValue by remember { mutableStateOf<Float?>(null) }
                val displayPosition = sliderValue?.toLong() ?: currentPosition
                val ratio = if (duration > 0) displayPosition.toFloat() / duration.toFloat() else 0f

                Slider(
                    value = ratio.coerceIn(0f, 1f),
                    onValueChange = { percent ->
                        sliderValue = percent * duration.toFloat()
                    },
                    onValueChangeFinished = {
                        sliderValue?.let {
                            viewModel.seekTo(it.toLong())
                        }
                        sliderValue = null
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_timeline_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMs(displayPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Text(
                        text = formatMs(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. PLAYBACK MEDIA CONTROL TRIGGERS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.testTag("shuffle_control")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White
                    )
                }

                IconButton(
                    onClick = { viewModel.skipToPrevious() },
                    modifier = Modifier.testTag("prev_control")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Song",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF818CF8), Color(0xFF6366F1))
                            )
                        )
                        .clickable { viewModel.togglePlayPause() }
                        .testTag("play_pause_control"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.skipToNext() },
                    modifier = Modifier.testTag("next_control")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Song",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleRepeat() },
                    modifier = Modifier.testTag("repeat_control")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }
        }

        // --- Bottom Sheet/Popup: Add track to selection lists ---
        if (showAddToPlaylistSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showAddToPlaylistSheet = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) {}
                        .padding(top = 90.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xE60F0F13))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Add to Playlist",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (playlists.isEmpty()) {
                            Text(
                                "No custom playlists found. Go to Library to build one!",
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(playlists) { _, playlist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.addTrackToPlaylist(playlist.id, track.id)
                                                showAddToPlaylistSheet = false
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(playlist.coverUrl)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            playlist.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showAddToPlaylistSheet = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Close", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Lyrics Parser helpers
data class LyricLine(
    val timestampMs: Long,
    val text: String
)

private fun parseLyrics(lyricsText: String): List<LyricLine> {
    val lines = lyricsText.split("\n")
    val list = mutableListOf<LyricLine>()
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            val endBracketIndex = trimmed.indexOf("]")
            val timeString = trimmed.substring(1, endBracketIndex)
            val lyricContent = trimmed.substring(endBracketIndex + 1).trim()
            val timeParts = timeString.split(":")
            if (timeParts.size == 2) {
                val min = timeParts[0].toLongOrNull() ?: 0
                val sec = timeParts[1].toLongOrNull() ?: 0
                val totalMs = (min * 60 + sec) * 1000
                list.add(LyricLine(totalMs, lyricContent))
            }
        }
    }
    return list
}

private fun formatMs(positionMs: Long): String {
    val totalSec = positionMs / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
