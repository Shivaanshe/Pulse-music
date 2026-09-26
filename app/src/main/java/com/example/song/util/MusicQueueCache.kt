package com.example.song.util

import com.example.song.data.model.Song
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe in-memory cache for the active playback queue.
 * Eliminates Binder IPC TransactionTooLargeException and Room DB latency
 * during playback resumption and queue updates.
 */
object MusicQueueCache {

    private val _queue = CopyOnWriteArrayList<Song>()

    fun setQueue(songs: List<Song>) {
        _queue.clear()
        _queue.addAll(songs)
    }

    fun getQueue(): List<Song> {
        return _queue.toList()
    }

    fun isEmpty(): Boolean = _queue.isEmpty()

    fun clear() {
        _queue.clear()
    }
}
