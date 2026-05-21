package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.MusicViewModel
import com.example.ui.theme.FrostedIndigo
import com.example.ui.theme.FrostedIndigoLight

fun Modifier.frostedMeshBackground(): Modifier = this.drawBehind {
    // Draw base dark slate color (#0F0F13)
    drawRect(Color(0xFF0F0F13))

    // Top-left purple glow matching Purple-600/20 rounded-full blur
    val topLeftBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.16f), Color.Transparent),
        center = Offset(size.width * -0.1f, size.height * -0.05f),
        radius = size.width * 1.0f
    )
    drawRect(topLeftBrush)

    // Bottom-right blue glow matching Blue-500/10 rounded-full blur
    val bottomRightBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.10f), Color.Transparent),
        center = Offset(size.width * 1.05f, size.height * 0.85f),
        radius = size.width * 1.1f
    )
    drawRect(bottomRightBrush)
}

@Composable
fun MainAppScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.Home) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .frostedMeshBackground()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent, // transparent to allow mesh background to shine through
            bottomBar = {
                // Standard NavigationBar
                Column {
                    // Persistent Mini Player sits directly above the bottom NavigationBar
                    if (currentTrack != null) {
                        val track = currentTrack!!
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                                .clickable { isPlayerExpanded = true }
                                .testTag("mini_player_container"),
                            color = Color(0x336366F1).copy(alpha = 0.25f), // Translucent Indigo backdrop
                            tonalElevation = 0.dp
                        ) {
                            Column {
                                // Mini progress bar along the top
                                val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp),
                                    color = Color.White,
                                    trackColor = Color.Transparent
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(track.coverUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Mini Player Cover Image",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.togglePlayPause() },
                                            modifier = Modifier.testTag("mini_player_play_pause")
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = Color.White
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.skipToNext() },
                                            modifier = Modifier.testTag("mini_player_skip_next")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SkipNext,
                                                contentDescription = "Skip Next",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    NavigationBar(
                        containerColor = Color(0xCC0F0F13), // 90% opacity cosmic dark
                        modifier = Modifier.drawBehind {
                            // Top subtle highlight line
                            drawLine(
                                color = Color.White.copy(alpha = 0.05f),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        },
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.Home,
                            onClick = { selectedTab = NavigationTab.Home },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == NavigationTab.Home) Icons.Default.Home else Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FrostedIndigoLight,
                                selectedTextColor = FrostedIndigoLight,
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                unselectedTextColor = Color.White.copy(alpha = 0.5f),
                                indicatorColor = FrostedIndigo.copy(alpha = 0.20f) // bg-indigo-500/20 active pill
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.Search,
                            onClick = { selectedTab = NavigationTab.Search },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == NavigationTab.Search) Icons.Default.Search else Icons.Outlined.Search,
                                    contentDescription = "Search"
                                )
                            },
                            label = { Text("Search", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FrostedIndigoLight,
                                selectedTextColor = FrostedIndigoLight,
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                unselectedTextColor = Color.White.copy(alpha = 0.5f),
                                indicatorColor = FrostedIndigo.copy(alpha = 0.20f)
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.Library,
                            onClick = { selectedTab = NavigationTab.Library },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == NavigationTab.Library) Icons.Default.LibraryMusic else Icons.Outlined.LibraryMusic,
                                    contentDescription = "Library"
                                )
                            },
                            label = { Text("Your Library", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FrostedIndigoLight,
                                selectedTextColor = FrostedIndigoLight,
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                unselectedTextColor = Color.White.copy(alpha = 0.5f),
                                indicatorColor = FrostedIndigo.copy(alpha = 0.20f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Show corresponding screen with sliding crossfade transitions
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    },
                    label = "Screen Transitions"
                ) { targetTab ->
                    when (targetTab) {
                        NavigationTab.Home -> HomeScreen(viewModel = viewModel)
                        NavigationTab.Search -> SearchScreen(viewModel = viewModel)
                        NavigationTab.Library -> LibraryScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // --- ANIMATED SLIDE UP FULL-SCREEN PLAYER DRAWER OVERLAY ---
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(
                initialOffsetY = { height -> height },
                animationSpec = tween(380)
            ) + fadeIn(tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { height -> height },
                animationSpec = tween(320)
            ) + fadeOut(tween(250))
        ) {
            PlayerOverlay(
                viewModel = viewModel,
                onClose = { isPlayerExpanded = false }
            )
        }
    }
}

enum class NavigationTab {
    Home,
    Search,
    Library
}
