package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.data.repository.MusicRepository
import com.example.player.MusicPlayerManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MusicViewModel(
    private val repository: MusicRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    // --- Master Data Streams ---
    val tracks = repository.allTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favorites = repository.favoriteTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlists = repository.allPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentHistory = repository.recentHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Search Stream ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchTracks(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Selected Playlist Tracks ---
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist = _selectedPlaylist.asStateFlow()

    val selectedPlaylistTracks = _selectedPlaylist
        .flatMapLatest { playlist ->
            if (playlist == null) {
                flowOf(emptyList())
            } else {
                repository.getTracksForPlaylist(playlist.id)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Live Player States ---
    val currentTrack = playerManager.currentTrack
    val isPlaying = playerManager.isPlaying
    val currentPosition = playerManager.currentPosition
    val duration = playerManager.duration
    val playbackQueue = playerManager.playbackQueue
    val isShuffle = playerManager.isShuffle
    val isRepeat = playerManager.isRepeat
    val visualizerAmplitudes = playerManager.visualizerAmplitudes
    val isBuffering = playerManager.isBuffering

    // --- UI Interactions ---

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
    }

    fun playTrack(track: Track, customQueue: List<Track>? = null) {
        val activeQueue = customQueue ?: tracks.value
        playerManager.playTrack(track, activeQueue)
        viewModelScope.launch {
            repository.recordPlayHistory(track.id)
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun skipToNext() {
        playerManager.playNextTrack()
    }

    fun skipToPrevious() {
        playerManager.playPreviousTrack()
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playerManager.toggleRepeat()
    }

    fun toggleLike(track: Track) {
        viewModelScope.launch {
            repository.toggleLike(track.id, !track.isLiked)
            // If the currently playing track's like state is changed, update the active instance
            if (currentTrack.value?.id == track.id) {
                // Update player manager's internal Track object if applicable
                // Since _currentTrack was update in DB, reload it
                val updatedTrack = repository.getTrackById(track.id)
                if (updatedTrack != null) {
                    // Let the player manager know if needed, or rely on flow trigger
                }
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = null
            }
            repository.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: Int, trackId: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Int, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun clearPlaybackHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            playerManager.release()
        } catch (e: Exception) {
            android.util.Log.e("MusicViewModel", "Error releasing playerManager in onCleared", e)
        }
    }
}

// --- ViewModel Factory ---
class MusicViewModelFactory(
    private val repository: MusicRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            val playerManager = MusicPlayerManager(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return MusicViewModel(repository, playerManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
