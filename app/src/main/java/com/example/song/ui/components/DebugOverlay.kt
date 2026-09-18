package com.example.song.ui.components

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.util.UnstableApi
import com.example.song.SongApplication
import com.example.song.data.preferences.CachePreferences
import com.example.song.ui.spotlight.SpotlightController
import com.example.song.ui.spotlight.TourStep
import com.example.song.ui.spotlight.spotlightTarget
import kotlinx.coroutines.launch
import com.example.song.viewmodel.SongViewModel

@OptIn(UnstableApi::class)
@Composable
fun DebugOverlay(
    viewModel: SongViewModel,
    spotlightController: SpotlightController? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    var showFullError by remember { mutableStateOf(false) }
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val playbackError by viewModel.playbackError.collectAsState()
    val extractionError by viewModel.extractionError.collectAsState()
    val isExtracting by viewModel.isExtracting.collectAsState()
    val resolvingId by viewModel.resolvingUrlId.collectAsState()
    val systemLogs by viewModel.systemLogs.collectAsState()
    val currentQueue by viewModel.currentQueue.collectAsState()
    val currentSong by viewModel.currentPlayingSong.collectAsState()
    val currentTask by viewModel.currentTask.collectAsState()
    val cachedKeys by viewModel.cachedKeys.collectAsState()

    val isArrangeModeEnabled by viewModel.isArrangeModeEnabled.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        SmallFloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 90.dp)
                .size(32.dp)
                .spotlightTarget(TourStep.STEP_7_ARRANGE_MODE, spotlightController),
            containerColor = Color.Black.copy(alpha = 0.4f),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.BugReport, contentDescription = "Debug", modifier = Modifier.size(16.dp))
        }
    }

    AnimatedVisibility(
        visible = showDialog,
        enter = fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.92f, animationSpec = tween(250, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.92f, animationSpec = tween(200, easing = FastOutSlowInEasing))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 680.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Prevent dismiss on card tap */ },
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF14161A).copy(alpha = 0.96f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pulse Debugger", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(onClick = { showDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        // Section 1: Engine Status
                        item {
                            DebugSection("Engine & Task Status") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DebugRowFixed("Live Task", currentTask ?: "Idle", Color(0xFF81C784))
                                    DebugRowFixed("Extracting", isExtracting.toString(), if(isExtracting) Color.Yellow else Color.White)
                                    DebugRowFixed("Resolving ID", resolvingId?.toString() ?: "None", if(resolvingId != null) Color.Cyan else Color.White)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Arrange Mode", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Switch(
                                            checked = isArrangeModeEnabled,
                                            onCheckedChange = { 
                                                viewModel.toggleArrangeMode(it)
                                                if (it) showDialog = false // Close debugger if enabling
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFFE91E63),
                                                checkedTrackColor = Color(0xFFE91E63).copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.scale(0.7f)
                                        )
                                    }

                                    currentSong?.let {
                                        DebugRowFixed("Active Song", it.title, Color(0xFFFF4081))
                                    }
                                }
                            }
                        }

                        // Section 2: Active Errors (Red Alert)
                        if (playbackError != null || extractionError != null) {
                            item {
                                DebugSection("🔥 Technical Errors") {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val combinedError = extractionError ?: playbackError
                                        Text(
                                            combinedError ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Red,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        TextButton(onClick = { showFullError = !showFullError }) {
                                            Text(if (showFullError) "Hide Full Report" else "View Technical Report", color = Color.Gray)
                                        }
                                        if (showFullError) {
                                            Box(modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 200.dp)
                                                .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                                .verticalScroll(rememberScrollState())
                                            ) {
                                                Text(combinedError ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Red.copy(alpha = 0.8f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section: Dynamic Cache Allocation & Storage Config
                        item {
                            val primaryGreen = Color(0xFF00E676)
                            val cachePrefs = remember { CachePreferences(context) }
                            val storedTotalMb by cachePrefs.songCacheSizeMb.collectAsState(initial = CachePreferences.DEFAULT_SONG_CACHE_MB)
                            
                            var inputString by remember(storedTotalMb) { mutableStateOf(storedTotalMb.toString()) }
                            val totalLimitMb = inputString.toIntOrNull() ?: 0

                            val usableSpaceBytes = remember { context.cacheDir.usableSpace }
                            val usableSpaceMb = (usableSpaceBytes / (1024 * 1024L)).toInt()
                            val usableSpaceGb = usableSpaceBytes / (1024 * 1024 * 1024f)

                            val safetyBufferMb = CachePreferences.SAFETY_BUFFER_MB // 1024 MB (1 GB)
                            
                            // Footprint breakdown math: totalLimit = calculatedSongMb + calculatedImageMb
                            val calculatedImageMb = maxOf(20, totalLimitMb / 6)
                            val calculatedSongMb = maxOf(80, totalLimitMb - calculatedImageMb)
                            val totalFootprintMb = calculatedSongMb + calculatedImageMb

                            val maxAllowedTotalMb = maxOf(
                                CachePreferences.MIN_SONG_CACHE_MB,
                                usableSpaceMb - safetyBufferMb
                            )

                            val isExceedingSafeLimit = totalLimitMb > maxAllowedTotalMb
                            val isBelowMinimum = totalLimitMb in 1..99

                            DebugSection("Dynamic Cache & Storage Allocation") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 1. Live Storage Telemetry Grid (No Text Collisions)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Audio Cache", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Text("${calculatedSongMb} MB", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = primaryGreen)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Covers (1:5)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Text("${calculatedImageMb} MB", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White.copy(alpha = 0.9f))
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Total Footprint", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Text("${totalFootprintMb} MB", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                        }
                                    }

                                    // 2. Smoked Glass Text Field & "Max Safe" Helper
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = inputString,
                                            onValueChange = { newValue ->
                                                if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                                                    inputString = newValue
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            placeholder = { Text("Cache MB") },
                                            trailingIcon = {
                                                Text("MB", modifier = Modifier.padding(end = 12.dp), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.Gray)
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.Black.copy(alpha = 0.40f),
                                                unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                                focusedBorderColor = Color.White.copy(alpha = 0.35f),
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )

                                        Surface(
                                            onClick = { inputString = maxAllowedTotalMb.toString() },
                                            shape = RoundedCornerShape(16.dp),
                                            color = primaryGreen.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, primaryGreen.copy(alpha = 0.35f)),
                                            modifier = Modifier.height(56.dp)
                                        ) {
                                            Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                                Text("Max Safe", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = primaryGreen)
                                            }
                                        }
                                    }

                                    // Input Validation Warning Banners
                                    if (isBelowMinimum) {
                                        Text(
                                            text = "⚠️ Minimum required cache size is 100 MB.",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFFFB74D)
                                        )
                                    } else if (isExceedingSafeLimit) {
                                        Text(
                                            text = "⚠️ Exceeds safe limit. Max possible safe storage is ${maxAllowedTotalMb} MB.",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFFFB74D)
                                        )
                                    }

                                    // 3. Quick Select Preset Chips (Primary Green)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val presets = listOf(
                                            100 to "100M (Min)",
                                            300 to "300M (Def)",
                                            500 to "500M",
                                            1000 to "1GB"
                                        )
                                        presets.forEach { (mb, label) ->
                                            val isSelected = totalLimitMb == mb
                                            val isEnabled = mb <= maxAllowedTotalMb
                                            Surface(
                                                onClick = { if (isEnabled) inputString = mb.toString() },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) primaryGreen else Color.White.copy(alpha = 0.08f),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) primaryGreen else Color.White.copy(alpha = 0.15f)
                                                ),
                                                enabled = isEnabled,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold),
                                                    color = if (isSelected) Color.Black else if (isEnabled) Color.White.copy(alpha = 0.9f) else Color.Gray,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Live Device Storage Indicator
                                    Text(
                                        text = "Available device storage: ${"%.1f".format(usableSpaceGb)} GB (Safety buffer: 1 GB free)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )

                                    // 4. Apply & Restart Cache Action Button (Primary Green)
                                    Button(
                                        onClick = {
                                            val finalTotalMb = totalLimitMb.coerceIn(100, maxAllowedTotalMb)
                                            inputString = finalTotalMb.toString()
                                            val finalImageMb = maxOf(20, finalTotalMb / 6)
                                            val finalAudioMb = maxOf(80, finalTotalMb - finalImageMb)
                                            scope.launch {
                                                cachePrefs.setSongCacheSizeMb(finalTotalMb)
                                                try {
                                                    SongApplication.getInstance().updateCacheConfig(finalTotalMb)
                                                } catch (e: Exception) {
                                                    // Safe catch
                                                }
                                                Toast.makeText(
                                                    context,
                                                    "Cache updated: ${finalAudioMb}MB Audio + ${finalImageMb}MB Covers (${finalTotalMb}MB Total)",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen, contentColor = Color.Black),
                                        enabled = !isExceedingSafeLimit && !isBelowMinimum && totalLimitMb >= 100
                                    ) {
                                        Text("Apply & Restart Cache", fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }

                        // Section 3: Cache Monitor
                        item {
                            DebugSection("Cache Monitor (${cachedKeys.size} spans)") {
                                Box(
                                    modifier = Modifier
                                        .heightIn(max = 120.dp)
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    if (cachedKeys.isEmpty()) {
                                        Text("Cache is empty", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    } else {
                                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                            cachedKeys.forEach { key ->
                                                Text(
                                                    text = "• $key",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = Color(0xFF81C784),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 4: Playback Queue
                        item {
                            DebugSection("Playback Queue (${currentQueue.size})") {
                                Column(
                                    modifier = Modifier
                                        .heightIn(max = 150.dp)
                                        .verticalScroll(rememberScrollState())
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    currentQueue.forEach { song ->
                                        val isPlaying = song.id == currentSong?.id
                                        // Check if this song's ID or URI is in the cache keys
                                        val isCached = cachedKeys.any { it.contains(song.audioUri) || (song.id.toString() in cachedKeys) }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = (if (isPlaying) "▶ " else "• "),
                                                color = if (isPlaying) Color(0xFFE91E63) else Color.White.copy(alpha = 0.4f)
                                            )
                                            Text(
                                                text = song.title,
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isPlaying) Color.White else Color.White.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isCached) {
                                                Surface(
                                                    color = Color(0xFF81C784).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text("CACHED", modifier = Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp), color = Color(0xFF81C784))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 5: System Logs
                        item {
                            DebugSection(
                                title = "System Logs (Live)",
                                action = {
                                    IconButton(
                                        onClick = {
                                            val allLogs = systemLogs.joinToString("\n")
                                            clipboardManager.setText(AnnotatedString(allLogs))
                                            Toast.makeText(context, "Logs copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy Logs",
                                            tint = Color(0xFF81C784),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(200.dp)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    val verticalScrollState = rememberScrollState()
                                    val horizontalScrollState = rememberScrollState()
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(verticalScrollState)
                                            .horizontalScroll(horizontalScrollState)
                                    ) {
                                        systemLogs.forEach { log ->
                                            Text(
                                                text = log,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = if (log.contains("[ERROR]")) Color(0xFFFF5252) else Color.Gray,
                                                fontFamily = FontFamily.Monospace,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.clearPlaybackError() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333), contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Clear Logs & Errors", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DebugSection(
    title: String, 
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF81C784), fontWeight = FontWeight.ExtraBold)
            action?.invoke()
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
fun DebugRowFixed(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label, 
            style = MaterialTheme.typography.labelSmall, 
            color = Color.Gray,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
