package com.example.song.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class QueueItem(
    val queueId: String = UUID.randomUUID().toString(),
    val song: Song,
    val isUserQueued: Boolean = false
) : Parcelable
