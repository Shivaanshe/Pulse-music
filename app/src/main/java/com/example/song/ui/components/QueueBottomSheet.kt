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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import com.example.song.util.dragGestureHandler
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

    val listState = rememberLazyListState()

    var draggedQueueId by remember { mutableStateOf<String?>(null) }
    var activeDraggedSong by remember { mutableStateOf<Song?>(null) }
    var targetQueueIndex by remember { mutableStateOf<Int?>(null) }
    var isDraggingManual by remember { mutableStateOf(true) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }
    var measuredItemHeightPx by remember { mutableFloatStateOf(0f) }

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
                    state = listState,
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
                            val isDragging = draggedQueueId == item.queueId
                            val itemHeightPx = measuredItemHeightPx
                            val draggedIdx = manualQueue.indexOfFirst { it.queueId == draggedQueueId }

                            val targetDisplacement = when {
                                isDragging -> 0f
                                draggedQueueId == null || targetQueueIndex == null || itemHeightPx == 0f || !isDraggingManual -> 0f
                                draggedIdx != -1 && draggedIdx < targetQueueIndex!! && index > draggedIdx && index <= targetQueueIndex!! -> -itemHeightPx
                                draggedIdx != -1 && draggedIdx > targetQueueIndex!! && index < draggedIdx && index >= targetQueueIndex!! -> itemHeightPx
                                else -> 0f
                            }

                            val itemTranslationY by animateFloatAsState(
                                targetValue = targetDisplacement,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                label = "DragTranslation"
                            )

                            val isGhostSlot = !isDragging && isDraggingManual && targetQueueIndex == index && draggedQueueId != null

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                                    .zIndex(if (isGhostSlot) 1f else 0f)
                                    .onGloballyPositioned {
                                        if (measuredItemHeightPx == 0f) measuredItemHeightPx = it.size.height.toFloat()
                                    }
                                    .graphicsLayer { translationY = itemTranslationY }
                            ) {
                                if (isGhostSlot) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(with(LocalDensity.current) { measuredItemHeightPx.toDp() })
                                            .graphicsLayer { translationY = -itemTranslationY }
                                            .padding(vertical = 4.dp)
                                            .border(
                                                width = 2.dp,
                                                brush = Brush.linearGradient(
                                                    colors = listOf(Color(0xFF4CAF50).copy(alpha = 0.6f), Color(0xFF4CAF50).copy(alpha = 0.2f))
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "DROP SONG HERE",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF4CAF50).copy(alpha = 0.8f),
                                                letterSpacing = 2.sp
                                            )
                                        )
                                    }
                                }

                                Box(modifier = Modifier.graphicsLayer { alpha = if (isDragging) 0f else 1f }) {
                                    SmokedGlassQueueRow(
                                        song = item.song,
                                        onPlayNow = { viewModel.playSong(item.song, currentQueue) },
                                        onRemove = { viewModel.removeFromQueueByQueueId(item.queueId) },
                                        onDragStart = { localY ->
                                            draggedQueueId = item.queueId
                                            activeDraggedSong = item.song
                                            targetQueueIndex = index
                                            isDraggingManual = true
                                            accumulatedDragY = 0f
                                        },
                                        onDragUpdate = { dragAmount ->
                                            if (draggedQueueId == item.queueId) {
                                                accumulatedDragY += dragAmount
                                                val steps = (accumulatedDragY / itemHeightPx.coerceAtLeast(1f)).roundToInt()
                                                val newTarget = (index + steps).coerceIn(0, manualQueue.size - 1)
                                                if (newTarget != targetQueueIndex) {
                                                    targetQueueIndex = newTarget
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val currentDragged = draggedQueueId
                                            val targetIdx = targetQueueIndex
                                            if (currentDragged != null && targetIdx != null) {
                                                val fromIdx = manualQueue.indexOfFirst { it.queueId == currentDragged }
                                                if (fromIdx != -1 && fromIdx != targetIdx) {
                                                    viewModel.reorderManualQueue(fromIdx, targetIdx)
                                                }
                                            }
                                            draggedQueueId = null
                                            activeDraggedSong = null
                                            targetQueueIndex = null
                                            accumulatedDragY = 0f
                                        }
                                    )
                                }
                            }
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
                            val isDragging = draggedQueueId == item.queueId
                            val itemHeightPx = measuredItemHeightPx
                            val draggedIdx = parentQueue.indexOfFirst { it.queueId == draggedQueueId }

                            val targetDisplacement = when {
                                isDragging -> 0f
                                draggedQueueId == null || targetQueueIndex == null || itemHeightPx == 0f || isDraggingManual -> 0f
                                draggedIdx != -1 && draggedIdx < targetQueueIndex!! && index > draggedIdx && index <= targetQueueIndex!! -> -itemHeightPx
                                draggedIdx != -1 && draggedIdx > targetQueueIndex!! && index < draggedIdx && index >= targetQueueIndex!! -> itemHeightPx
                                else -> 0f
                            }

                            val itemTranslationY by animateFloatAsState(
                                targetValue = targetDisplacement,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                label = "DragTranslation"
                            )

                            val isGhostSlot = !isDragging && !isDraggingManual && targetQueueIndex == index && draggedQueueId != null

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                                    .zIndex(if (isGhostSlot) 1f else 0f)
                                    .onGloballyPositioned {
                                        if (measuredItemHeightPx == 0f) measuredItemHeightPx = it.size.height.toFloat()
                                    }
                                    .graphicsLayer { translationY = itemTranslationY }
                            ) {
                                if (isGhostSlot) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(with(LocalDensity.current) { measuredItemHeightPx.toDp() })
                                            .graphicsLayer { translationY = -itemTranslationY }
                                            .padding(vertical = 4.dp)
                                            .border(
                                                width = 2.dp,
                                                brush = Brush.linearGradient(
                                                    colors = listOf(Color(0xFF4CAF50).copy(alpha = 0.6f), Color(0xFF4CAF50).copy(alpha = 0.2f))
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "DROP SONG HERE",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF4CAF50).copy(alpha = 0.8f),
                                                letterSpacing = 2.sp
                                            )
                                        )
                                    }
                                }

                                Box(modifier = Modifier.graphicsLayer { alpha = if (isDragging) 0f else 1f }) {
                                    SmokedGlassQueueRow(
                                        song = item.song,
                                        onPlayNow = { viewModel.playSong(item.song, currentQueue) },
                                        onRemove = { viewModel.removeFromQueueByQueueId(item.queueId) },
                                        onDragStart = { localY ->
                                            draggedQueueId = item.queueId
                                            activeDraggedSong = item.song
                                            targetQueueIndex = index
                                            isDraggingManual = false
                                            accumulatedDragY = 0f
                                        },
                                        onDragUpdate = { dragAmount ->
                                            if (draggedQueueId == item.queueId) {
                                                accumulatedDragY += dragAmount
                                                val steps = (accumulatedDragY / itemHeightPx.coerceAtLeast(1f)).roundToInt()
                                                val newTarget = (index + steps).coerceIn(0, parentQueue.size - 1)
                                                if (newTarget != targetQueueIndex) {
                                                    targetQueueIndex = newTarget
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val currentDragged = draggedQueueId
                                            val targetIdx = targetQueueIndex
                                            if (currentDragged != null && targetIdx != null) {
                                                val fromIdx = parentQueue.indexOfFirst { it.queueId == currentDragged }
                                                if (fromIdx != -1 && fromIdx != targetIdx) {
                                                    viewModel.reorderParentQueue(fromIdx, targetIdx)
                                                }
                                            }
                                            draggedQueueId = null
                                            activeDraggedSong = null
                                            targetQueueIndex = null
                                            accumulatedDragY = 0f
                                        }
                                    )
                                }
                            }
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
                        val shuffleScale by animateFloatAsState(
                            targetValue = if (isShuffleEnabled) 1.15f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "ShuffleScale"
                        )

                        IconButton(
                            onClick = {
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
                            AnimatedShuffleIcon(
                                isShuffleEnabled = isShuffleEnabled,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = shuffleScale
                                    scaleY = shuffleScale
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

            // Floating Active Dragged Item Snapshot Overlay (placed inside root Box at zIndex 100f)
            activeDraggedSong?.let { draggedSong ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .offset { IntOffset(0, accumulatedDragY.roundToInt()) }
                        .zIndex(100f)
                ) {
                    val itemScale by animateFloatAsState(
                        targetValue = 1.05f,
                        label = "FloatingScale",
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                    Box(
                        modifier = Modifier.graphicsLayer {
                            scaleX = itemScale
                            scaleY = itemScale
                            shadowElevation = 24.dp.toPx()
                            shape = RoundedCornerShape(16.dp)
                            clip = true
                        }
                    ) {
                        SmokedGlassQueueRow(
                            song = draggedSong,
                            onPlayNow = {},
                            onRemove = {}
                        )
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
    onDragStart: (Float) -> Unit = {},
    onDragUpdate: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {}
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
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Touch & Drag Reorder Grip Handle
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                onDragStart(offset.y)
                            },
                            onDragEnd = {
                                onDragEnd()
                            },
                            onDragCancel = {
                                onDragEnd()
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                onDragUpdate(dragAmount)
                            }
                        )
                    }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Hold and Drag to Reorder",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(22.dp)
                )
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

@Composable
fun AnimatedShuffleIcon(
    isShuffleEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (isShuffleEnabled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ShuffleAnim"
    )

    val activeColor = Color(0xFF4CAF50)
    val inactiveColor = Color.White.copy(alpha = 0.60f)
    val color = if (isShuffleEnabled) activeColor else inactiveColor

    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()

        // Top line crossing to bottom line
        val p1 = Path().apply {
            moveTo(w * 0.15f, h * 0.30f)
            cubicTo(
                w * (0.35f + 0.10f * progress), h * (0.30f + 0.10f * progress),
                w * (0.65f - 0.10f * progress), h * (0.70f - 0.10f * progress),
                w * 0.85f, h * 0.70f
            )
        }

        // Bottom line crossing to top line
        val p2 = Path().apply {
            moveTo(w * 0.15f, h * 0.70f)
            cubicTo(
                w * (0.35f + 0.10f * progress), h * (0.70f - 0.10f * progress),
                w * (0.65f - 0.10f * progress), h * (0.30f + 0.10f * progress),
                w * 0.85f, h * 0.30f
            )
        }

        drawPath(p1, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawPath(p2, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))

        val arrowSize = 4.dp.toPx()
        // Top right arrow head
        drawPath(
            path = Path().apply {
                moveTo(w * 0.85f - arrowSize, h * 0.30f - arrowSize)
                lineTo(w * 0.85f, h * 0.30f)
                lineTo(w * 0.85f - arrowSize, h * 0.30f + arrowSize)
            },
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        // Bottom right arrow head
        drawPath(
            path = Path().apply {
                moveTo(w * 0.85f - arrowSize, h * 0.70f - arrowSize)
                lineTo(w * 0.85f, h * 0.70f)
                lineTo(w * 0.85f - arrowSize, h * 0.70f + arrowSize)
            },
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
