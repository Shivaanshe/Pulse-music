package com.example.song.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.song.data.model.Song
import com.example.song.viewmodel.SongViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    viewModel: SongViewModel,
    onDismissRequest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentQueue by viewModel.currentQueue.collectAsState()
    val manualQueue by viewModel.manualQueue.collectAsState()
    val parentQueue by viewModel.parentQueue.collectAsState()
    val currentSong by viewModel.currentPlayingSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val contextTitle by viewModel.queueContextTitle.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsState()

    var showTimerDialog by remember { mutableStateOf(false) }

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
                .fillMaxHeight(0.85f)
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
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Top Grab Handle & Header
                Spacer(modifier = Modifier.height(12.dp))
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
                Spacer(modifier = Modifier.height(12.dp))

                // Header Title & Dismiss Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Playing Queue",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        )
                        Text(
                            text = contextTitle,
                            style = MaterialTheme.typography.bodySmall.copy(
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
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Queue",
                            tint = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // --- NOW PLAYING SECTION ---
                    item {
                        Text(
                            text = "Now Playing",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        if (currentSong != null) {
                            SmokedGlassNowPlayingCard(
                                song = currentSong!!,
                                isPlaying = isPlaying,
                                onTogglePlay = { viewModel.togglePlayPause() }
                            )
                        } else {
                            SmokedGlassEmptyCard(text = "No track currently playing.")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // --- SECTION 2: ADDED TO QUEUE (MANUAL QUEUE) ---
                    if (manualQueue.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Added to Queue (${manualQueue.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.White.copy(alpha = 0.88f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )

                                TextButton(
                                    onClick = { viewModel.clearManualQueue() },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "Clear",
                                        color = Color.White.copy(alpha = 0.60f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        itemsIndexed(
                            items = manualQueue,
                            key = { _, item -> item.queueId }
                        ) { index, item ->
                            SmokedGlassQueueRow(
                                song = item.song,
                                onPlayNow = {
                                    viewModel.playSong(item.song, currentQueue)
                                },
                                onRemove = {
                                    viewModel.removeFromQueueByQueueId(item.queueId)
                                },
                                onMoveUp = if (index > 0) {
                                    { viewModel.reorderManualQueue(index, index - 1) }
                                } else null,
                                onMoveDown = if (index < manualQueue.size - 1) {
                                    { viewModel.reorderManualQueue(index, index + 1) }
                                } else null,
                                onMoveToTop = if (index > 0) {
                                    { viewModel.reorderManualQueue(index, 0) }
                                } else null
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // --- SECTION 3: CONTINUE PLAYING (PARENT PLAYLIST QUEUE) ---
                    if (parentQueue.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Continue Playing from $contextTitle (${parentQueue.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.White.copy(alpha = 0.88f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (manualQueue.isEmpty()) {
                                    TextButton(
                                        onClick = { viewModel.clearQueue() },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = "Clear All",
                                            color = Color.White.copy(alpha = 0.60f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        itemsIndexed(
                            items = parentQueue,
                            key = { _, item -> item.queueId }
                        ) { index, item ->
                            SmokedGlassQueueRow(
                                song = item.song,
                                onPlayNow = {
                                    viewModel.playSong(item.song, currentQueue)
                                },
                                onRemove = {
                                    viewModel.removeFromQueueByQueueId(item.queueId)
                                },
                                onMoveUp = if (index > 0) {
                                    { viewModel.reorderParentQueue(index, index - 1) }
                                } else null,
                                onMoveDown = if (index < parentQueue.size - 1) {
                                    { viewModel.reorderParentQueue(index, index + 1) }
                                } else null,
                                onMoveToTop = if (index > 0) {
                                    { viewModel.reorderParentQueue(index, 0) }
                                } else null
                            )
                        }
                    }

                    if (manualQueue.isEmpty() && parentQueue.isEmpty()) {
                        item {
                            SmokedGlassEmptyCard(text = "Queue is empty. Add songs to get started!")
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // --- BOTTOM UTILITY BAR ---
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF000000).copy(alpha = 0.50f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Shuffle Button
                        val shuffleOffsetX = remember { Animatable(0f) }
                        val shuffleAlpha = remember { Animatable(1f) }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    launch { shuffleOffsetX.animateTo(50f, animationSpec = tween(160, easing = FastOutSlowInEasing)) }
                                    launch { shuffleAlpha.animateTo(0f, animationSpec = tween(120)) }
                                    delay(160)
                                    shuffleOffsetX.snapTo(-50f)
                                    launch { shuffleOffsetX.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                    launch { shuffleAlpha.animateTo(1f, animationSpec = tween(160)) }
                                }
                                viewModel.toggleShuffle()
                            },
                            modifier = Modifier
                                .background(
                                    if (isShuffleEnabled) Color(0xFF4CAF50).copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.08f),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (isShuffleEnabled) Color(0xFF4CAF50) else Color.Transparent,
                                    CircleShape
                                )
                                .size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Toggle Shuffle",
                                tint = if (isShuffleEnabled) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.60f),
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        translationX = shuffleOffsetX.value
                                        alpha = shuffleAlpha.value
                                    }
                            )
                        }

                        // Sleep Timer Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (sleepTimerRemainingMs != null) Color(0xFF4CAF50).copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .clickable { showTimerDialog = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerRemainingMs != null) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.88f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (sleepTimerRemainingMs != null) {
                                    val ms = sleepTimerRemainingMs!!
                                    val minutes = (ms / 1000) / 60
                                    val seconds = (ms / 1000) % 60
                                    String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                                } else "Sleep Timer",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (sleepTimerRemainingMs != null) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.88f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        // Clear Queue Button
                        IconButton(
                            onClick = { viewModel.clearQueue() },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                .size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Queue",
                                tint = Color.White.copy(alpha = 0.60f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showTimerDialog = false },
            onSelectMinutes = { minutes ->
                viewModel.setSleepTimer(minutes)
                showTimerDialog = false
            }
        )
    }
}

@Composable
fun SmokedGlassNowPlayingCard(
    song: Song,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF000000).copy(alpha = 0.40f),
        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.40f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = song.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                // Equalizer animated soundwave icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
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
                onClick = onTogglePlay,
                modifier = Modifier
                    .background(Color(0xFF4CAF50), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun SmokedGlassQueueRow(
    song: Song,
    onPlayNow: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToTop: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayNow() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF000000).copy(alpha = 0.40f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag / Reorder Indicator
            Column(verticalArrangement = Arrangement.Center) {
                if (onMoveUp != null) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move Up",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onMoveUp() }
                    )
                }
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Move to Top of Queue",
                    tint = if (onMoveToTop != null) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(enabled = onMoveToTop != null) { onMoveToTop?.invoke() }
                )
                if (onMoveDown != null) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move Down",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onMoveDown() }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            AsyncImage(
                model = song.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.88f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.60f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from Queue",
                    tint = Color.White.copy(alpha = 0.50f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SmokedGlassEmptyCard(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF000000).copy(alpha = 0.30f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.60f)
                )
            )
        }
    }
}

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSelectMinutes: (Int) -> Unit
) {
    val options = listOf(
        "Off" to 0,
        "15 minutes" to 15,
        "30 minutes" to 30,
        "45 minutes" to 45,
        "60 minutes" to 60
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp)),
            color = Color(0xFF141A16).copy(alpha = 0.96f),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            ),
            tonalElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Sleep Timer",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Pause audio automatically",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.60f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Options List
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    options.forEach { (label, minutes) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectMinutes(minutes) },
                            color = Color(0xFF000000).copy(alpha = 0.40f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White.copy(alpha = 0.88f),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.40f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cancel Button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
