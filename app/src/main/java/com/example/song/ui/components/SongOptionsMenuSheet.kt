package com.example.song.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.song.data.model.Song
import com.example.song.viewmodel.SongViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsMenuSheet(
    song: Song,
    viewModel: SongViewModel,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF141A16).copy(alpha = 0.95f),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Song Header Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.88f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.60f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Options List
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Add to Queue
                    OptionRowItem(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        title = "Add to Queue",
                        subtitle = "Append track to the end of the active queue",
                        onClick = {
                            viewModel.addToQueue(listOf(song), playNext = false)
                            onDismissRequest()
                        }
                    )

                    // Play Next
                    OptionRowItem(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        title = "Play Next",
                        subtitle = "Insert track immediately after current song",
                        onClick = {
                            viewModel.addToQueue(listOf(song), playNext = true)
                            onDismissRequest()
                        }
                    )

                    // Go to Queue
                    OptionRowItem(
                        icon = Icons.Default.Queue,
                        title = "Go to Queue",
                        subtitle = "Open full queue manager",
                        onClick = {
                            onDismissRequest()
                            viewModel.openQueueSheet()
                        }
                    )

                    // Favorite / Unfavorite
                    OptionRowItem(
                        icon = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        iconTint = if (song.isFavorite) Color.Red else Color.White.copy(alpha = 0.88f),
                        title = if (song.isFavorite) "Remove from Favorites" else "Add to Favorites",
                        subtitle = null,
                        onClick = {
                            viewModel.updateFavorite(song, !song.isFavorite)
                            onDismissRequest()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun OptionRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    iconTint: Color = Color.White.copy(alpha = 0.88f),
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Color(0xFF000000).copy(alpha = 0.40f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.50f)
                        )
                    )
                }
            }
        }
    }
}
