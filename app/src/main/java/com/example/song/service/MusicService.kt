package com.example.song.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.*
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.song.MainActivity
import com.example.song.SongApplication
import com.example.song.data.model.Song
import com.example.song.util.PulseLogger
import com.example.song.util.YoutubeStreamHandler
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@UnstableApi
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var idleJob: Job? = null
    
    private lateinit var httpDataSourceFactory: OkHttpDataSource.Factory
    private var currentQueue: List<Song> = emptyList()
    private var lastSavedSongId: Int = -1

    private val resolvedCache = ConcurrentHashMap<String, ResolvedData>()
    private val artworkCache = ConcurrentHashMap<String, ByteArray>()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    data class ResolvedData(val url: String, val headers: Map<String, String>, val artwork: ByteArray?)

    companion object {
        private const val TAG = "PulseDebug"
        private const val PREFS_NAME = "pulse_prefs"
        private const val KEY_LAST_SONG_ID = "last_played_song_id"
        private const val KEY_LAST_QUEUE_IDS = "last_queue_ids"
        private const val KEY_LAST_INDEX = "last_played_index"
    }

    override fun onCreate() {
        super.onCreate()

        val app = SongApplication.getInstance()
        val cache = app.playerCache
        
        httpDataSourceFactory = OkHttpDataSource.Factory(app.okHttpClient)
        val baseFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        val resolvingDataSourceFactory = ResolvingDataSource.Factory(
            baseFactory,
            object : ResolvingDataSource.Resolver {
                override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                    val uriStr = dataSpec.uri.toString()
                    
                    if (uriStr.contains("pulse.music/resolve")) {
                        val mediaId = dataSpec.uri.pathSegments.lastOrNull() ?: "unknown"
                        val query = dataSpec.uri.getQueryParameter("query") ?: return dataSpec
                        val artworkUrl = dataSpec.uri.getQueryParameter("artwork_url") ?: ""
                        
                        resolvedCache[query]?.let { cached ->
                            PulseLogger.log("JIT Cache Hit: Instant skip enabled.")
                            return dataSpec.buildUpon()
                                .setUri(Uri.parse(cached.url))
                                .setHttpRequestHeaders(cached.headers)
                                .setKey(mediaId)
                                .build()
                        }

                        PulseLogger.log("JIT Resolution started for: $query")
                        
                        return runBlocking(Dispatchers.IO) { 
                            val resolved = performResolution(query, artworkUrl, isPriority = true)
                            if (resolved != null) {
                                resolvedCache[query] = resolved
                                withContext(Dispatchers.Main) {
                                    updateActiveMetadata(query, resolved.artwork)
                                }

                                dataSpec.buildUpon()
                                    .setUri(Uri.parse(resolved.url))
                                    .setHttpRequestHeaders(resolved.headers)
                                    .setKey(mediaId)
                                    .build()
                            } else {
                                PulseLogger.log("JIT Resolution FAILED for: $query", isError = true)
                                dataSpec
                            }
                        }
                    }
                    return dataSpec
                }
            }
        )

        val dataSourceFactory = if (cache != null) {
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(resolvingDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            DefaultMediaSourceFactory(cacheDataSourceFactory)
        } else {
            DefaultMediaSourceFactory(resolvingDataSourceFactory)
        }

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(dataSourceFactory)
            .setAudioAttributes(audioAttributes, true) // true handles audio focus automatically
            .setHandleAudioBecomingNoisy(true) // handle Bluetooth/Headphone disconnect
            .build()

        // 🛡️ Guarantee 1.0x normal playback speed to prevent sample rate/fast-forward bugs
        player.playbackParameters = PlaybackParameters(1.0f, 1.0f)
        
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // 🛡️ Avoid feedback loop: Ignore transitions triggered by metadata/playlist updates
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return

                // 🛡️ Cancel stale resolution jobs immediately
                activeJobs.forEach { (key, job) ->
                    val currentQuery = mediaItem?.localConfiguration?.uri?.getQueryParameter("query")
                    if (currentQuery != key) {
                        job.cancel()
                        activeJobs.remove(key)
                    }
                }

                if (mediaItem != null) {
                    val mediaId = mediaItem.mediaId
                    
                    // 🔋 Debounced State Save (Avoid I/O thrashing)
                    val songId = mediaId.toIntOrNull()
                    if (songId != null && songId != lastSavedSongId) {
                        lastSavedSongId = songId
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                            .putInt(KEY_LAST_SONG_ID, songId)
                            .putInt(KEY_LAST_INDEX, player.currentMediaItemIndex)
                            .apply()
                    }

                    val query = mediaItem.localConfiguration?.uri?.getQueryParameter("query")
                    val artworkUrl = mediaItem.mediaMetadata.extras?.getString("custom_artwork_url")
                    
                    val cached = query?.let { resolvedCache[it] }
                    if (cached != null) {
                        serviceScope.launch { updateActiveMetadata(mediaId, cached.artwork) }
                    } else if (!artworkUrl.isNullOrEmpty() && mediaItem.mediaMetadata.artworkData == null) {
                        // 🖼️ Independent Artwork Fetch for cached items or new sessions
                        serviceScope.launch {
                            val art = fetchImageAsByteArray(artworkUrl)
                            if (art != null) {
                                updateActiveMetadata(mediaId, art)
                            }
                        }
                    }
                }
                preResolveNextItems()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (player.currentPosition > 1000) player.seekToNext()
                }
                val stateName = when(playbackState) {
                    Player.STATE_READY -> "Ready"
                    Player.STATE_BUFFERING -> "Buffering"
                    Player.STATE_IDLE -> "Idle"
                    Player.STATE_ENDED -> "Ended"
                    else -> "Unknown"
                }
                PulseLogger.log("Player State: $stateName")
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                PulseLogger.log("Playback: ${if(isPlaying) "Playing" else "Paused"}")
                idleJob?.cancel()
                if (!isPlaying) {
                    idleJob = serviceScope.launch {
                        delay(30 * 1000L) // 30 seconds idle timeout when paused
                        if (!player.isPlaying && player.playbackState != Player.STATE_BUFFERING) {
                            PulseLogger.log("Idle for 30 seconds. Stopping service.")
                            stopSelf()
                        }
                    }
                }
            }
        })

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(MediaSessionCallback())
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.isPlaying) {
            PulseLogger.log("App swiped away while paused. Graceful termination triggered.")
            
            // 🧹 Instant cleanup
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(true)
            }
            
            stopSelf()
            mediaSession?.release()
        }
        super.onTaskRemoved(rootIntent)
    }

    private suspend fun performResolution(query: String, artworkUrl: String, isPriority: Boolean): ResolvedData? = withContext(Dispatchers.IO) {
        val processId = UUID.randomUUID().toString()
        val isLocalFile = query.startsWith("/") || query.startsWith("file://") || File(query).exists()
        
        if (isLocalFile) {
            val artworkBytes = fetchImageAsByteArray(artworkUrl)
            return@withContext ResolvedData(Uri.fromFile(File(query)).toString(), emptyMap(), artworkBytes)
        }

        val job = Job()
        activeJobs[query] = job
        
        try {
            YoutubeStreamHandler.ytDlMutex.withLock {
                if (!isPriority && !job.isActive) return@withLock null
                
                val streamDeferred = async { YoutubeStreamHandler.getStreamInfo(query, processId) }
                val artworkDeferred = async { fetchImageAsByteArray(artworkUrl) }
                
                val info = withTimeoutOrNull(35000L) { streamDeferred.await() }
                val art = artworkDeferred.await()
                
                if (info != null) {
                    ResolvedData(info.url, info.headers, art)
                } else {
                    null
                }
            }
        } catch (_: Exception) { null } finally {
            com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(processId)
            activeJobs.remove(query)
            job.cancel()
        }
    }

    private fun preResolveNextItems() {
        serviceScope.launch(Dispatchers.IO) {
            val (currentIndex, count) = withContext(Dispatchers.Main) {
                player.currentMediaItemIndex to player.mediaItemCount
            }
            if (count == 0) return@launch

            // 🔋 Active Sliding Window Pre-Resolution: 2 tracks ahead, 1 track behind
            val windowIndices = listOf(currentIndex + 1, currentIndex + 2, currentIndex - 1)
            for (i in windowIndices) {
                val item = withContext(Dispatchers.Main) {
                    runCatching {
                        if (i in 0 until player.mediaItemCount) {
                            player.getMediaItemAt(i)
                        } else null
                    }.getOrNull()
                } ?: continue

                val query = item.localConfiguration?.uri?.getQueryParameter("query")
                val artUrl = item.localConfiguration?.uri?.getQueryParameter("artwork_url")

                if (query != null && !resolvedCache.containsKey(query)) {
                    val resolved = performResolution(query, artUrl ?: "", isPriority = false)
                    if (resolved != null) {
                        resolvedCache[query] = resolved
                        withContext(Dispatchers.Main) {
                            updateMetadataInQueue(i, item.mediaId, resolved.artwork)
                        }
                    }
                }
            }
        }
    }

    private fun updateMetadataInQueue(index: Int, mediaId: String, artworkBytes: ByteArray?) {
        if (artworkBytes == null) return
        runCatching {
            if (index in 0 until player.mediaItemCount) {
                val item = player.getMediaItemAt(index)
                if (item.mediaId == mediaId && item.mediaMetadata.artworkData == null) {
                    val updatedMetadata = item.mediaMetadata.buildUpon()
                        .setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build()
                    val updatedItem = item.buildUpon().setMediaMetadata(updatedMetadata).build()
                    
                    if (index == player.currentMediaItemIndex) {
                        val pos = player.currentPosition
                        player.replaceMediaItem(index, updatedItem)
                        player.seekTo(index, pos)
                    } else {
                        player.replaceMediaItem(index, updatedItem)
                    }
                }
            }
        }
    }

    private fun updateActiveMetadata(mediaId: String, artworkBytes: ByteArray?) {
        runCatching {
            if (player.mediaItemCount > 0) {
                updateMetadataInQueue(player.currentMediaItemIndex, mediaId, artworkBytes)
            }
        }
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .build()
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand("PLAY_QUEUE", Bundle.EMPTY))
                .add(SessionCommand("UPDATE_QUEUE", Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(playerCommands)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onPlaybackResumption(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val savedQueue = prefs.getString(KEY_LAST_QUEUE_IDS, null)
            val lastIndex = prefs.getInt(KEY_LAST_INDEX, 0)
            
            if (savedQueue.isNullOrEmpty()) {
                return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
            }

            val settableFuture = com.google.common.util.concurrent.SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            
            serviceScope.launch {
                val repository = SongApplication.getInstance().repository
                val ids = savedQueue.split(",").mapNotNull { it.toIntOrNull() }
                
                if (ids.isEmpty()) {
                    settableFuture.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
                    return@launch
                }

                val streamingIds = ids.filter { it >= 1_000_000 }.map { it - 1_000_000 }
                val localIds = ids.filter { it < 1_000_000 }
                
                val streamingItems = if (streamingIds.isNotEmpty()) repository.getStreamingItemsByIdsSync(streamingIds) else emptyList()
                val localSongs = if (localIds.isNotEmpty()) repository.getSongsByIdsSync(localIds) else emptyList()
                
                val allItemsMap = mutableMapOf<Int, Song>()
                localSongs.forEach { allItemsMap[it.id] = it }
                streamingItems.forEach { item ->
                    val song = item.toSong().copy(id = 1_000_000 + item.id)
                    allItemsMap[1_000_000 + item.id] = song
                }
                
                val songs = ids.mapNotNull { allItemsMap[it] }
                
                if (songs.isNotEmpty()) {
                    val mediaItems = songs.map { mapSongToMediaItem(it) }
                    currentQueue = songs
                    val startIndex = if (lastIndex in songs.indices) lastIndex else 0
                    
                    PulseLogger.log("Restoring Queue: Song ${startIndex + 1} of ${songs.size}")
                    settableFuture.set(MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, 0L))
                } else {
                    settableFuture.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
                }
            }

            return settableFuture
        }

        override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
            idleJob?.cancel()
            if (customCommand.customAction == "PLAY_QUEUE") {
                val index = args.getInt("index", 0)
                val ids = args.getIntegerArrayList("ids") ?: return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
                
                // 🛡️ Save queue and index for persistence
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putString(KEY_LAST_QUEUE_IDS, ids.joinToString(","))
                    .putInt(KEY_LAST_INDEX, index)
                    .apply()
                
                serviceScope.launch {
                    val repository = SongApplication.getInstance().repository
                    
                    val parcelableSongs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        args.getParcelableArrayList("songs", Song::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        args.getParcelableArrayList("songs")
                    }

                    val songs = if (!parcelableSongs.isNullOrEmpty()) {
                        parcelableSongs
                    } else {
                        val streamingIds = ids.filter { it >= 1_000_000 }.map { it - 1_000_000 }
                        val localIds = ids.filter { it < 1_000_000 }
                        val streamingItems = if (streamingIds.isNotEmpty()) repository.getStreamingItemsByIdsSync(streamingIds) else emptyList()
                        val localSongs = if (localIds.isNotEmpty()) repository.getSongsByIdsSync(localIds) else emptyList()
                        val allItemsMap = mutableMapOf<Int, Song>()
                        localSongs.forEach { allItemsMap[it.id] = it }
                        streamingItems.forEach { item ->
                            val song = item.toSong().copy(id = 1_000_000 + item.id)
                            allItemsMap[1_000_000 + item.id] = song
                        }
                        ids.mapNotNull { allItemsMap[it] }
                    }
                    
                    if (songs.isNotEmpty()) {
                        PulseLogger.log("Queue mapping: ${songs.size} items JIT-Ready")
                        val mediaItems = songs.map { mapSongToMediaItem(it) }
                        
                        currentQueue = songs
                        withContext(Dispatchers.Main) {
                            player.setMediaItems(mediaItems, index, 0L)
                            player.prepare()
                            player.play()
                        }
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            } else if (customCommand.customAction == "UPDATE_QUEUE") {
                val index = args.getInt("index", if (player.mediaItemCount > 0) player.currentMediaItemIndex else 0)
                val position = if (player.playbackState != Player.STATE_IDLE) player.currentPosition else 0L
                val isPlaying = player.isPlaying
                val ids = args.getIntegerArrayList("ids") ?: return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))

                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putString(KEY_LAST_QUEUE_IDS, ids.joinToString(","))
                    .putInt(KEY_LAST_INDEX, index)
                    .apply()

                serviceScope.launch {
                    val repository = SongApplication.getInstance().repository

                    val parcelableSongs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        args.getParcelableArrayList("songs", Song::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        args.getParcelableArrayList("songs")
                    }

                    val songs = if (!parcelableSongs.isNullOrEmpty()) {
                        parcelableSongs
                    } else {
                        val streamingIds = ids.filter { it >= 1_000_000 }.map { it - 1_000_000 }
                        val localIds = ids.filter { it < 1_000_000 }
                        val streamingItems = if (streamingIds.isNotEmpty()) repository.getStreamingItemsByIdsSync(streamingIds) else emptyList()
                        val localSongs = if (localIds.isNotEmpty()) repository.getSongsByIdsSync(localIds) else emptyList()
                        val allItemsMap = mutableMapOf<Int, Song>()
                        localSongs.forEach { allItemsMap[it.id] = it }
                        streamingItems.forEach { item ->
                            val song = item.toSong().copy(id = 1_000_000 + item.id)
                            allItemsMap[1_000_000 + item.id] = song
                        }
                        ids.mapNotNull { allItemsMap[it] }
                    }

                    if (songs.isNotEmpty()) {
                        val mediaItems = songs.map { mapSongToMediaItem(it) }
                        currentQueue = songs
                        withContext(Dispatchers.Main) {
                            val safeIndex = index.coerceIn(0, mediaItems.size - 1)
                            val currentItem = player.currentMediaItem
                            val targetItem = mediaItems[safeIndex]

                            if (currentItem != null && currentItem.mediaId == targetItem.mediaId) {
                                // 🛡️ Currently playing track is unchanged: update playlist timeline seamlessly
                                player.setMediaItems(mediaItems, safeIndex, position)
                            } else {
                                player.setMediaItems(mediaItems, safeIndex, position)
                                if (isPlaying) player.play()
                            }
                            preResolveNextItems()
                        }
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            if (customCommand.customAction == "SKIP_TO_PREVIOUS") {
                serviceScope.launch(Dispatchers.Main) {
                    if (player.currentPosition > 3000) {
                        player.seekTo(0)
                    } else if (player.hasPreviousMediaItem()) {
                        player.seekToPreviousMediaItem()
                    } else {
                        player.seekTo(0)
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            if (customCommand.customAction == "SKIP_TO_NEXT") {
                serviceScope.launch(Dispatchers.Main) {
                    if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    private fun mapSongToMediaItem(song: Song): MediaItem {
        val isLocal = song.audioUri.startsWith("/") || song.audioUri.startsWith("file://")
        val uri = if (isLocal) Uri.fromFile(File(song.audioUri)) else {
            val encodedQuery = Uri.encode(song.audioUri)
            val encodedArt = Uri.encode(song.imageUrl ?: "")
            Uri.parse("https://pulse.music/resolve/${song.id}?query=$encodedQuery&artwork_url=$encodedArt")
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setArtworkUri(song.imageUrl?.let { Uri.parse(it) })
            .setExtras(Bundle().apply { 
                putString("custom_artwork_url", song.imageUrl)
                putString("search_query", song.audioUri)
            })
            .build()
        
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(uri)
            .setCustomCacheKey(song.id.toString()) 
            .setMediaMetadata(metadata)
            .build()
    }

    private suspend fun fetchImageAsByteArray(url: String?): ByteArray? {
        if (url.isNullOrEmpty()) return null
        
        // 🚀 Cache Hit check
        artworkCache[url]?.let { return it }

        return try {
            val app = SongApplication.getInstance()
            val request = ImageRequest.Builder(app).data(url).allowHardware(false).build()
            val result = app.imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream) // 🔋 Low CPU compression
                    val bytes = stream.toByteArray()
                    
                    // 🚀 Cache Store (limit size)
                    if (artworkCache.size > 20) artworkCache.clear() 
                    artworkCache[url] = bytes
                    bytes
                } else null
            } else null
        } catch (_: Exception) { null }
    }

    private fun com.example.song.data.model.StreamingItem.toSong(): Song {
        return Song(id = id, title = title, artist = artist ?: "Unknown Artist", audioUri = youtubeUrl, imageUrl = thumbnailUrl, duration = duration)
    }



    override fun onDestroy() {
        serviceScope.cancel()
        player.release()
        mediaSession?.release()
        super.onDestroy()
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession
}
