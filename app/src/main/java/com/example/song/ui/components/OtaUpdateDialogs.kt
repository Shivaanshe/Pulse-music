package com.example.song.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.shivaansh.pulseplayer.BuildConfig
import com.example.song.viewmodel.OtaUpdateViewModel
import java.util.Locale

/**
 * Shared Glassmorphic Dialog Surface.
 * Renders a translucent dark green tinted background with a thin vertical gradient glass border
 * and a subtle top horizontal highlight glow.
 */
@Composable
fun GlassmorphicDialogSurface(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(28.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                ),
                shape = shape
            ),
        shape = shape,
        color = Color(0xFF141A16).copy(alpha = 0.88f),
        tonalElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle top highlight glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF4CAF50).copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                content()
            }
        }
    }
}

/**
 * Smoked Glass Inner Container for release notes and content boxes.
 */
@Composable
fun SmokedGlassContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp, max = 320.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF000000).copy(alpha = 0.40f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            content()
        }
    }
}

/**
 * Lightweight Composable Markdown text renderer.
 * Converts headers (###, ##, #), bullet points (*, -), and **bold** text spans
 * into styled typography.
 */
@Composable
fun FormattedMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White.copy(alpha = 0.88f)
) {
    val lines = remember(text) { text.lines() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@forEach

            when {
                // Headers (#, ##, ###)
                trimmed.startsWith("#") -> {
                    val headerText = trimmed.removePrefix("#").removePrefix("#").removePrefix("#").trim()
                    val formattedHeader = parseInlineBold(headerText)
                    Text(
                        text = formattedHeader,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                // Bullet points (* or -)
                trimmed.startsWith("* ") || trimmed.startsWith("- ") -> {
                    val bulletContent = trimmed.substring(2).trim()
                    val formattedContent = parseInlineBold(bulletContent)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = formattedContent,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 20.sp,
                                color = textColor
                            )
                        )
                    }
                }
                // Regular Paragraph
                else -> {
                    val formattedParagraph = parseInlineBold(trimmed)
                    Text(
                        text = formattedParagraph,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp,
                            color = textColor
                        )
                    )
                }
            }
        }
    }
}

private fun parseInlineBold(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        while (currentIndex < text.length) {
            val boldStart = text.indexOf("**", currentIndex)
            if (boldStart == -1) {
                append(text.substring(currentIndex))
                break
            }
            append(text.substring(currentIndex, boldStart))

            val boldEnd = text.indexOf("**", boldStart + 2)
            if (boldEnd == -1) {
                append(text.substring(boldStart))
                break
            }

            val boldText = text.substring(boldStart + 2, boldEnd)
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(boldText)
            }
            currentIndex = boldEnd + 2
        }
    }
}

@Composable
fun OtaUpdateAvailableDialog(
    viewModel: OtaUpdateViewModel
) {
    val release by viewModel.activeRelease.collectAsState()
    val asset by viewModel.activeAsset.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val progress by viewModel.downloadProgress.collectAsState()

    if (release == null) return

    val formattedSize = asset?.let {
        val mb = it.size.toDouble() / (1024 * 1024)
        String.format(Locale.US, "%.1f MB", mb)
    } ?: ""

    Dialog(onDismissRequest = { if (!isDownloading) viewModel.dismissUpdateModal() }) {
        GlassmorphicDialogSurface(
            modifier = Modifier.padding(16.dp)
        ) {
            // Hero Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update Hero Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "New Update Available",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Version ${release?.tagName ?: ""} ${if (formattedSize.isNotBlank()) "• $formattedSize" else ""}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.60f)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Changelog / Release Notes with Smoked Glass Container
            Text(
                text = "Release Notes",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            SmokedGlassContainer {
                val releaseBody = release?.body?.takeIf { it.isNotBlank() } ?: "No release notes provided."
                FormattedMarkdownText(text = releaseBody)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Downloading Progress
            if (isDownloading) {
                val progressFraction = progress?.progressFraction ?: 0f
                val percentage = (progressFraction * 100).toInt()

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Downloading update...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Actions with Polished Remind Later Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isDownloading) {
                    var dropdownExpanded by remember { mutableStateOf(false) }

                    Box {
                        TextButton(onClick = { dropdownExpanded = true }) {
                            Text("Remind Later", color = Color.White.copy(alpha = 0.80f))
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .width(220.dp)
                                .background(Color(0xFF1A221D).copy(alpha = 0.95f), RoundedCornerShape(20.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Tomorrow (24 hrs)",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    viewModel.remindLater(86_400_000L)
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "In 1 Week",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    viewModel.remindLater(604_800_000L)
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "In 1 Month",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    viewModel.remindLater(2_592_000_000L)
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = Color.White.copy(alpha = 0.12f)
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Skip This Release",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFEF5350))
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    viewModel.remindLater(-1L)
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = {
                        if (!isDownloading) {
                            viewModel.startDownloadOrInstall()
                        }
                    },
                    enabled = !isDownloading
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isDownloading) "Downloading..." else "Update Now")
                }
            }
        }
    }
}

@Composable
fun OtaWhatsNewDialog(
    viewModel: OtaUpdateViewModel
) {
    val changelog by viewModel.whatsNewChangelog.collectAsState()

    Dialog(onDismissRequest = { viewModel.dismissWhatsNewModal() }) {
        GlassmorphicDialogSurface(
            modifier = Modifier.padding(16.dp)
        ) {
            // Success Hero Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.18f))
                        .border(
                            width = 1.dp,
                            color = Color(0xFF4CAF50).copy(alpha = 0.40f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success Icon",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Updated Successfully!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Now running v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.60f)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "What's New in this Version",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            SmokedGlassContainer {
                val releaseBody = changelog?.takeIf { it.isNotBlank() } ?: "Enjoy performance enhancements, bug fixes, and new features!"
                FormattedMarkdownText(text = releaseBody)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = { viewModel.dismissWhatsNewModal() }) {
                    Text("Awesome!")
                }
            }
        }
    }
}

@Composable
fun OtaInstallPermissionDialog(
    viewModel: OtaUpdateViewModel
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = { viewModel.dismissPermissionModal() }) {
        GlassmorphicDialogSurface(
            modifier = Modifier.padding(16.dp)
        ) {
            // Permission Hero Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.40f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.50f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Permission Security",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Permission Required",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Install Unknown Apps",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.60f)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SmokedGlassContainer(
                modifier = Modifier.heightIn(max = 120.dp)
            ) {
                Text(
                    text = "To complete installing the downloaded update, Android requires permission to allow app installations from this source.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { viewModel.dismissPermissionModal() }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.80f))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.openPermissionSettings(context) }) {
                    Text("Open Settings")
                }
            }
        }
    }
}

@Composable
fun OtaSettingsDialog(
    viewModel: OtaUpdateViewModel
) {
    val isChecking by viewModel.isChecking.collectAsState()

    Dialog(onDismissRequest = { viewModel.dismissSettingsModal() }) {
        GlassmorphicDialogSurface(
            modifier = Modifier.padding(16.dp)
        ) {
            // Settings Hero Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "About & Updates",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Song App v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.60f)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF000000).copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "In-App OTA Updates",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "Direct updates from GitHub Releases",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.60f)
                        )
                    }

                    Button(
                        onClick = { viewModel.checkForUpdates(force = true) },
                        enabled = !isChecking
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Check for Updates",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { viewModel.dismissSettingsModal() }) {
                    Text("Close", color = Color.White.copy(alpha = 0.80f))
                }
            }
        }
    }
}
