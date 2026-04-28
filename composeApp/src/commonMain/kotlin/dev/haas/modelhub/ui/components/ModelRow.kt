package dev.haas.modelhub.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.haas.modelhub.model.ParsedModel
import dev.haas.modelhub.model.DownloadState
import dev.haas.modelhub.service.DownloadManager
import dev.haas.modelhub.service.SizeUtil

@Composable
fun ModelRow(
    model: ParsedModel,
    onAction: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val downloadState by DownloadManager.downloads.collectAsState()
    val currentDownload = downloadState[model.repoID]
    val state = currentDownload?.state ?: DownloadManager.getDownloadState(model.repoID)
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }

    val isLight = !MaterialTheme.colorScheme.surface.isDark()
    val cardBgColor = if (isLight) Color.White else Color(0xFF1C1C1E)
    val textColor = if (isLight) Color(0xFF1E1E1E) else Color.White
    val subTextColor = if (isLight) Color(0xFF8E8E93) else Color(0xFF98989D)

    val animatedBgColor by animateColorAsState(
        targetValue = if (showMenu) {
            if (isLight) Color(0xFFE8E8ED) else Color(0xFF3C3C3E)
        } else cardBgColor
        ,
        label = "bg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { showMenu = true },
        color = animatedBgColor,
        shadowElevation = if (isLight) 0.5.dp else 0.dp,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )

                if (state is DownloadState.Downloading) {
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { state.progress.toFloat() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp)),
                                color = if (isLight) Color(0xFF007AFF) else Color(0xFF0A84FF),
                                trackColor = if (isLight) Color(0x1A000000) else Color(0x33FFFFFF)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${(state.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (isLight) Color(0xFF007AFF) else Color(0xFF0A84FF)
                            )
                        }
                        Text(
                            text = "${SizeUtil.format(state.bytesDownloaded)} / ${SizeUtil.format(state.totalBytes)} • ${String.format("%.1f", state.bytesPerSecond / (1024 * 1024))} MB/s",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = subTextColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    val sizeText = if (model.sizeBytes > 0) " • ${SizeUtil.format(model.sizeBytes)}" else ""
                    Text(
                        text = "${model.publisher} • ${model.typeLabel ?: "Model"}$sizeText",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = subTextColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Box(modifier = Modifier.padding(start = 8.dp)) {
                if (state is DownloadState.Downloading) {
                    IconButton(
                        onClick = { DownloadManager.stopDownload(model.repoID) },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isLight) Color(0xFFF5F5F7) else Color(0xFF2C2C2E))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFFFF3B30)
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = subTextColor.copy(alpha = 0.5f)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(if (isLight) Color(0xFFFEFEFE) else Color(0xFF1C1C1E))
                        .border(0.5.dp, if (isLight) Color(0x1A000000) else Color(0x33FFFFFF), RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy Path", fontSize = 13.sp) },
                        onClick = {
                            clipboardManager.setText(AnnotatedString(model.fullPath))
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy Repo ID", fontSize = 13.sp) },
                        onClick = {
                            clipboardManager.setText(AnnotatedString(model.repoID))
                            showMenu = false
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = if (isLight) Color(0x1A000000) else Color(0x1AFFFFFF)
                    )

                    val actionText = when {
                        state is DownloadState.Downloading -> "Cancel Download"
                        state is DownloadState.Completed || model.source == ParsedModel.Source.LOCAL || model.source == ParsedModel.Source.LM_STUDIO || model.source == ParsedModel.Source.HUGGING_FACE || model.source == ParsedModel.Source.OLLAMA -> "Open Folder"
                        else -> "Download Model"
                    }

                    DropdownMenuItem(
                        text = {
                            Text(
                                actionText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (actionText == "Delete Local Files") Color(0xFFFF3B30) else MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            if (state is DownloadState.Downloading) DownloadManager.stopDownload(model.repoID)
                            else onAction()
                            showMenu = false
                        }
                    )

                    if ((model.source == ParsedModel.Source.LOCAL || model.source == ParsedModel.Source.LM_STUDIO || model.source == ParsedModel.Source.HUGGING_FACE || model.source == ParsedModel.Source.OLLAMA) && onDelete != null) {
                        DropdownMenuItem(
                            text = { Text("Delete Local Files", fontSize = 13.sp, color = Color(0xFFFF3B30)) },
                            onClick = {
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun Color.isDark(): Boolean {
    val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
    return luminance < 0.5
}
