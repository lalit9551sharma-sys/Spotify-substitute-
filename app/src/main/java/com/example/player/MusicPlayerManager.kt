package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.data.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class MusicPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var visualizerJob: Job? = null

    // --- Observable Player States ---
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackQueue = MutableStateFlow<List<Track>>(emptyList())
    val playbackQueue: StateFlow<List<Track>> = _playbackQueue.asStateFlow()

    private var currentQueueIndex = -1

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    // Amplitudes for the live music visualizer (values between 0.1 and 1.0)
    private val _visualizerAmplitudes = MutableStateFlow(listOf(0.2f, 0.1f, 0.15f, 0.2f, 0.1f, 0.15f, 0.2f, 0.1f))
    val visualizerAmplitudes: StateFlow<List<Float>> = _visualizerAmplitudes.asStateFlow()

    // Helper state for lyrics tracking
    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    init {
        initializeMediaPlayer()
    }

    private fun initializeMediaPlayer() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener { mp ->
                    try {
                        _isBuffering.value = false
                        _duration.value = mp.duration.toLong()
                        mp.start()
                        _isPlaying.value = true
                        startTickingProgress()
                        startVisualizerAnimation()
                    } catch (e: Exception) {
                        Log.e("MusicPlayerManager", "Error in setOnPreparedListener callback", e)
                    }
                }
                setOnCompletionListener {
                    try {
                        playNextTrack(autoPlay = true)
                    } catch (e: Exception) {
                        Log.e("MusicPlayerManager", "Error in setOnCompletionListener callback", e)
                    }
                }
                setOnErrorListener { _, what, extra ->
                    try {
                        _isBuffering.value = false
                        _isPlaying.value = false
                        Log.e("MusicPlayerManager", "MediaPlayer error: what=$what, extra=$extra")
                    } catch (e: Exception) {
                        Log.e("MusicPlayerManager", "Error in setOnErrorListener callback", e)
                    }
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("MusicPlayerManager", "Failed to initialize MediaPlayer", e)
        }
    }

    fun playTrack(track: Track, queue: List<Track>) {
        if (_currentTrack.value?.id == track.id) {
            // Already active track, just play/pause toggled
            togglePlayPause()
            return
        }

        // Set queue and active index
        _playbackQueue.value = queue
        currentQueueIndex = queue.indexOfFirst { it.id == track.id }
        if (currentQueueIndex == -1) {
            _playbackQueue.value = queue + track
            currentQueueIndex = queue.size
        }

        prepareAndStartPlayback(track)
    }

    private fun prepareAndStartPlayback(track: Track) {
        _currentTrack.value = track
        _isBuffering.value = true
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L

        stopTickingProgress()
        stopVisualizerAnimation()

        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(track.url)
            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            _isBuffering.value = false
            Log.e("MusicPlayerManager", "Failed to play track source", e)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        try {
            if (trackActiveState()) {
                if (player.isPlaying) {
                    player.pause()
                    _isPlaying.value = false
                    stopTickingProgress()
                    stopVisualizerAnimation()
                } else {
                    player.start()
                    _isPlaying.value = true
                    startTickingProgress()
                    startVisualizerAnimation()
                }
            } else {
                // If play is pressed but queue has tracks, play first element
                val currentQ = _playbackQueue.value
                if (currentQ.isNotEmpty()) {
                    playTrack(currentQ.first(), currentQ)
                }
            }
        } catch (e: Exception) {
            Log.e("MusicPlayerManager", "Error in togglePlayPause", e)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.let { player ->
                if (trackActiveState()) {
                    val safePosition = positionMs.coerceIn(0, _duration.value)
                    player.seekTo(safePosition.toInt())
                    _currentPosition.value = safePosition
                }
            }
        } catch (e: Exception) {
            Log.e("MusicPlayerManager", "Error in seekTo", e)
        }
    }

    fun playNextTrack(autoPlay: Boolean = true) {
        val q = _playbackQueue.value
        if (q.isEmpty()) return

        if (_isRepeat.value) {
            _currentTrack.value?.let { prepareAndStartPlayback(it) }
            return
        }

        if (_isShuffle.value) {
            val randomIndex = Random.nextInt(q.size)
            currentQueueIndex = randomIndex
        } else {
            currentQueueIndex = (currentQueueIndex + 1) % q.size
        }

        val nextTrack = q.getOrNull(currentQueueIndex)
        if (nextTrack != null) {
            prepareAndStartPlayback(nextTrack)
        }
    }

    fun playPreviousTrack() {
        val q = _playbackQueue.value
        if (q.isEmpty()) return

        if (currentPosition.value > 5000L) {
            // Restart track if over 5 seconds
            seekTo(0)
            return
        }

        currentQueueIndex = if (currentQueueIndex > 0) {
            currentQueueIndex - 1
        } else {
            q.size - 1
        }

        val prevTrack = q.getOrNull(currentQueueIndex)
        if (prevTrack != null) {
            prepareAndStartPlayback(prevTrack)
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
    }

    private fun trackActiveState(): Boolean {
        return _currentTrack.value != null
    }

    private fun startTickingProgress() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive) {
                try {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            _currentPosition.value = player.currentPosition.toLong()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MusicPlayerManager", "Error in progress tick", e)
                }
                delay(300)
            }
        }
    }

    private fun stopTickingProgress() {
        progressJob?.cancel()
    }

    private fun startVisualizerAnimation() {
        visualizerJob?.cancel()
        visualizerJob = coroutineScope.launch {
            while (isActive) {
                if (_isPlaying.value) {
                    val list = List(8) { Random.nextFloat().coerceIn(0.15f, 1.0f) }
                    _visualizerAmplitudes.value = list
                } else {
                    _visualizerAmplitudes.value = List(8) { 0.1f }
                }
                delay(120)
            }
        }
    }

    private fun stopVisualizerAnimation() {
        visualizerJob?.cancel()
        _visualizerAmplitudes.value = List(8) { 0.1f }
    }

    fun release() {
        coroutineScope.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
