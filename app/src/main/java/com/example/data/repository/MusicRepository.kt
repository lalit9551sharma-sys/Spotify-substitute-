package com.example.data.repository

import com.example.data.local.MusicDao
import com.example.data.model.HistoryEntry
import com.example.data.model.Playlist
import com.example.data.model.PlaylistTrack
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicRepository(private val musicDao: MusicDao) {

    val allTracks: Flow<List<Track>> = musicDao.getAllTracks()
    val favoriteTracks: Flow<List<Track>> = musicDao.getFavoriteTracks()
    val allPlaylists: Flow<List<Playlist>> = musicDao.getAllPlaylists()
    val recentHistory: Flow<List<Track>> = musicDao.getRecentHistory()

    init {
        // Trigger seeding in DB asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            seedDatabaseIfEmpty()
        }
    }

    fun getTracksByCategory(category: String): Flow<List<Track>> =
        musicDao.getTracksByCategory(category)

    fun searchTracks(query: String): Flow<List<Track>> =
        musicDao.searchTracks(query)

    fun getTracksForPlaylist(playlistId: Int): Flow<List<Track>> =
        musicDao.getTracksForPlaylist(playlistId)

    suspend fun getTrackById(id: String): Track? = withContext(Dispatchers.IO) {
        musicDao.getTrackById(id)
    }

    suspend fun toggleLike(trackId: String, isLiked: Boolean) = withContext(Dispatchers.IO) {
        musicDao.updateTrackLikeState(trackId, isLiked)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        val count = musicDao.getAllPlaylists().first().size + 1
        val coverUrl = "https://picsum.photos/id/${(100 + count * 23) % 1000}/300/300"
        val newPlaylist = Playlist(name = name, coverUrl = coverUrl)
        musicDao.insertPlaylist(newPlaylist)
    }

    suspend fun deletePlaylist(playlistId: Int) = withContext(Dispatchers.IO) {
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Int, trackId: String) = withContext(Dispatchers.IO) {
        musicDao.addTrackToPlaylist(PlaylistTrack(playlistId, trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: String) = withContext(Dispatchers.IO) {
        musicDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun recordPlayHistory(trackId: String) = withContext(Dispatchers.IO) {
        musicDao.insertHistoryEntry(HistoryEntry(trackId = trackId))
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        musicDao.clearHistory()
    }

    private suspend fun seedDatabaseIfEmpty() {
        try {
            val existingTracks = musicDao.getAllTracks().first()
            if (existingTracks.isEmpty()) {
                val seedTracks = listOf(
                    Track(
                        id = "track_1",
                        title = "Midnight Forest",
                        artist = "Melo-Fi",
                        album = "Nature Chill",
                        durationMs = 372000,
                        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                        coverUrl = "https://picsum.photos/id/10/400/400",
                        category = "Lofi",
                        lyrics = """[00:00] (A gentle morning birdsong opens)
[00:10] Wind starts blowing softly through leaves...
[00:25] Lost inside the emerald canopy, tracing branches above
[00:40] A midnight whisper in the woods, no track of time
[00:55] Let your busy thoughts dissolve, breathe with the breeze
[01:15] Stars peek through the thick clouds, painting silver light
[01:34] Safe and sound beneath towering boughs
[01:55] Distant owls calling out to the moon...
[02:15] (Peaceful acoustic keys start fading)
[02:40] Rest your mind, float in the silence..."""
                    ),
                    Track(
                        id = "track_2",
                        title = "Cyber Neon Grid",
                        artist = "AuraByte",
                        album = "Glitch World Synth",
                        durationMs = 423000,
                        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                        coverUrl = "https://picsum.photos/id/100/400/400",
                        category = "Electronic",
                        lyrics = """[00:00] (Heavy synthesized bass begins to loop)
[00:15] Welcome to the neural digital matrix
[00:25] Flying past glowing emerald grid lines at midnight
[00:40] Virtual realities sync directly to your pulse
[00:55] Cybernetic visions, lines of glowing neon light
[01:15] We are travelers of the chrome century
[01:32] Golden visors reading digital lines in realtime
[01:50] Accelerating into hyperdrive velocity!
[02:10] (A synthetic synthesizer guitar solo bursts)
[02:40] Floating above the high-tech code city..."""
                    ),
                    Track(
                        id = "track_3",
                        title = "Azure Coast Breeze",
                        artist = "Solar Waves",
                        album = "Endless Summer Vibes",
                        durationMs = 302000,
                        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                        coverUrl = "https://picsum.photos/id/1000/400/400",
                        category = "Chillout",
                        lyrics = """[00:05] (Waves crashing on soft white sand dunes)
[00:18] Sea breeze carrying memories of long summers
[00:30] Flying high above the glistening turquoise surf
[00:48] Under deep azure horizons, setting sail to the wild
[01:05] Sun-washed cedar wood, salt crystals on your skin
[01:25] Floating weightless on the oceanic temperature
[01:45] Island paradise found under the equatorial gold
[02:10] No emails, no deadlines, just endless waves
[02:35] (Gentle ukelele chords finish with the waves...)"""
                    ),
                    Track(
                        id = "track_4",
                        title = "Starlit Mind Focus",
                        artist = "Deep Brains",
                        album = "Mental Sanctuary",
                        durationMs = 502000,
                        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                        coverUrl = "https://picsum.photos/id/101/400/400",
                        category = "Ambient",
                        lyrics = """[00:00] (Deep, low atmospheric bell resonating)
[00:15] Focus the neural pathways, clear away the digital noise
[00:35] Each chime brings standard relaxation and focus
[00:55] Deeply inhale the oxygen, hold, and slowly exhale
[01:20] Cosmic tides vibrating in silent space frequencies
[01:45] Align with your inner focus, thoughts flow freely
[02:15] Solitary study, a mental temple of pure light
[02:50] Floating on a cosmic nebula..."""
                    ),
                    Track(
                        id = "track_5",
                        title = "Retro Coffee Shop",
                        artist = "Rhythm Grid",
                        album = "Tape Deck Beats",
                        durationMs = 384000,
                        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                        coverUrl = "https://picsum.photos/id/1011/400/400",
                        category = "Jazz",
                        lyrics = """[00:00] (Soft vinyl record dust crackling)
[00:12] Double bass slide begins, very mellow
[00:25] Rainy morning in a cozy Seattle brick street
[00:40] Hot espresso brew steam curling in the warm light
[00:58] Gentle saxophone melodies dancing with the rain drops
[01:20] Whispers of old jazz records on a wooden shelf
[01:42] Jazz piano solo begins under dim lamps
[02:10] Pages turning, thoughts relaxing in the afternoon
[02:45] Cozy warmth of a rainy jazz haven..."""
                    ),
                    Track(
                        id = "track_6",
                        title = "Midnight Outrun",
                        artist = "Drive Wave",
                        album = "Outrun Horizons",
                        durationMs = 459000,
                        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                        coverUrl = "https://picsum.photos/id/1015/400/400",
                        category = "Synthwave",
                        lyrics = """[00:00] (Fast pacing electronic retro beat)
[00:15] 120 beats of pure high octane speed
[00:30] Sweeping headlights carving through dark empty highways
[00:48] Retro-futuristic synthesizers scream through the twin exhausts
[00:05] Chasing the endless neon horizontal sunset lines
[01:25] Dashboard glowing violet, engine humming in harmony
[01:48] Past midnight, outrunning the storm in our dreams
[02:15] Dynamic guitar synth solo breaks the horizon limit
[02:50] Keep driving till the morning sun arises..."""
                    ),
                    Track(
                        id = "track_7",
                        title = "Ethereal Nebula Echoes",
                        artist = "Nebula Orchestra",
                        album = "Interstellar Horizons",
                        durationMs = 520000,
                        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                        coverUrl = "https://picsum.photos/id/1018/400/400",
                        category = "Space",
                        lyrics = """[00:00] (Low hum of modular interstellar oscillators)
[00:20] Echoes of ancient satellites repeating a long-lost code
[00:45] Swimming floating weightless in high outer orbit
[01:10] Seeing the beautiful blue planet spin in total silent peace
[01:40] Constellations lighting up paths into deeper space fields
[02:10] Planetary rings playing an cosmic music loop
[02:45] Disappearing into a deep violet nebula cluster..."""
                    ),
                    Track(
                        id = "track_8",
                        title = "Sunset Campfire Lounge",
                        artist = "Chillout Co.",
                        album = "Golden Solstice",
                        durationMs = 312000,
                        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                        coverUrl = "https://picsum.photos/id/1020/400/400",
                        category = "Acoustic",
                        lyrics = """[00:00] (Campfire logs popping and ocean winds blowing)
[00:10] Warm classical finger-picked guitar begins
[00:28] Golden solar rays melting into the desert mountain dunes
[00:45] Fireside sparks rising up to meet the first cold stars
[01:08] Rest your head, wrap in blankets, forget the world
[01:30] Acoustic strings play a sweet familiar theme
[01:55] Whispering shadows casting outlines on the rocks
[02:20] Ending in total quiet, peaceful guitar fading out..."""
                    )
                )
                musicDao.insertTracks(seedTracks)
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Failed to seed default catalog tracks", e)
        }
    }
}
