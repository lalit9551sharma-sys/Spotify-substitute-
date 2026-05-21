package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val allTracks by viewModel.tracks.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val selectedPlaylistTracks by viewModel.selectedPlaylistTracks.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var showAddToPlaylistDialog by remember { mutableStateOf<Track?>(null) }

    // Navigation inside Library (either showing list of playlists, Liked Songs sub-panel, or a specific Custom Playlist sub-panel)
    var activeSubView by remember { mutableStateOf<LibrarySubView>(LibrarySubView.MainList) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        when (activeSubView) {
            LibrarySubView.MainList -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 90.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // --- Library Title & Action row ---
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.LibraryMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Your Library",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    )
                                )
                            }

                            IconButton(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier
                                    .testTag("create_playlist_fab")
                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Playlist", tint = Color.White)
                            }
                        }
                    }

                    // --- Liked Songs Card Highlight ---
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                                .height(130.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF6366F1).copy(alpha = 0.35f), // Indigo-500 low opacity
                                            Color(0xFFEC4899).copy(alpha = 0.15f)  // Pink-500 low opacity
                                        )
                                    )
                                )
                                .clickable {
                                    activeSubView = LibrarySubView.LikedSongs
                                }
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                Text(
                                    text = "Liked Songs",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${favorites.size} songs saved",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.LightGray
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFFF4081),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Playlists",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        )
                    }

                    // --- Playlists List ---
                    if (playlists.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No custom playlists yet",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Tap the plus icon above to create your first.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    } else {
                        items(playlists) { playlist ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = playlist.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                },
                                supportingContent = {
                                    Text(text = "Playlist • Click to open", color = Color.Gray)
                                },
                                leadingContent = {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(playlist.coverUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Delete playlist",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.White.copy(alpha = 0.03f)
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        viewModel.selectPlaylist(playlist)
                                        activeSubView = LibrarySubView.PlaylistView
                                    }
                            )
                        }
                    }
                }
            }

            LibrarySubView.LikedSongs -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // --- Subheader ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { activeSubView = LibrarySubView.MainList }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Liked Songs",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (favorites.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Songs you favorite will appear here",
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(favorites) { track ->
                                val isPlayingThis = currentTrack?.id == track.id
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                        .clickable { viewModel.playTrack(track, favorites) },
                                    color = Color.White.copy(alpha = 0.03f)
                                ) {
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
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isPlayingThis) MaterialTheme.colorScheme.primary else Color.White
                                            )
                                            Text(
                                                text = track.artist,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.LightGray
                                            )
                                        }

                                        IconButton(onClick = { viewModel.toggleLike(track) }) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                tint = Color(0xFFFF4081),
                                                contentDescription = "Unlike"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LibrarySubView.PlaylistView -> {
                selectedPlaylist?.let { playlist ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        // --- Playlist Header ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { activeSubView = LibrarySubView.MainList }) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(playlist.coverUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${selectedPlaylistTracks.size} tracks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                            }
                        }

                        // --- Content Sections ---
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp)
                        ) {
                            if (selectedPlaylistTracks.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "This playlist is empty.",
                                            color = Color.Gray,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            } else {
                                items(selectedPlaylistTracks) { track ->
                                    val isPlayingThis = currentTrack?.id == track.id
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                            .clickable { viewModel.playTrack(track, selectedPlaylistTracks) },
                                        color = Color.White.copy(alpha = 0.03f)
                                    ) {
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
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = track.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isPlayingThis) MaterialTheme.colorScheme.primary else Color.White
                                                )
                                                Text(
                                                    text = track.artist,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.LightGray
                                                )
                                            }

                                            IconButton(onClick = { viewModel.removeTrackFromPlaylist(playlist.id, track.id) }) {
                                                Icon(
                                                    imageVector = Icons.Default.RemoveCircleOutline,
                                                    tint = Color.LightGray,
                                                    contentDescription = "Remove track from playlist"
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // --- Add Songs Row ---
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Add tracks to this playlist",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }

                            // List songs NOT in the playlist to allow clicking to add them
                            val tracksNotInPlaylist = allTracks.filter { track ->
                                selectedPlaylistTracks.none { it.id == track.id }
                            }

                            if (tracksNotInPlaylist.isEmpty()) {
                                item {
                                    Text(
                                        "All catalog track entries are already added!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                items(tracksNotInPlaylist) { track ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(track.coverUrl)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(track.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(track.artist, color = Color.Gray, fontSize = 12.sp)
                                        }

                                        IconButton(onClick = { viewModel.addTrackToPlaylist(playlist.id, track.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                tint = MaterialTheme.colorScheme.primary,
                                                contentDescription = "Add"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } // Ended PlaylistView
        }

        // --- Dialog: Create Playlist ---
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCreateDialog = false
                    playlistNameInput = ""
                },
                title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Give your playlist a descriptive name", color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        TextField(
                            value = playlistNameInput,
                            onValueChange = { playlistNameInput = it },
                            placeholder = { Text("My playlist #1") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier.testTag("playlist_dialog_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        onClick = {
                            if (playlistNameInput.isNotBlank()) {
                                viewModel.createPlaylist(playlistNameInput)
                                showCreateDialog = false
                                playlistNameInput = ""
                            }
                        }
                    ) {
                        Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCreateDialog = false
                        playlistNameInput = ""
                    }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }
    }
}

enum class LibrarySubView {
    MainList,
    LikedSongs,
    PlaylistView
}
