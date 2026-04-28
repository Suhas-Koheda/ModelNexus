package dev.haas.modelhub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    val textColor = if (isLight) Color(0xFF1E1E1E) else Color.White
    val subTextColor = if (isLight) Color(0xFF8E8E93) else Color(0xFFA1A1A6)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMenu = true }
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )
                
                if (state is DownloadState.Downloading) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        LinearProgressIndicator(
                            progress = { state.progress.toFloat() },
                            modifier = Modifier.weight(1f).height(2.dp).clip(RoundedCornerShape(1.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = if (isLight) Color(0x1A000000) else Color(0x33FFFFFF)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = "${model.publisher} • ${model.typeLabel ?: "Model"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = subTextColor
                    )
                }
            }

            // Minimal Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state is DownloadState.Downloading) {
                    IconButton(
                        onClick = { DownloadManager.stopDownload(model.repoID) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(14.dp), tint = Color.Red)
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(if (isLight) Color(0xFFF2F2F7) else Color(0xFF2C2C2E))
                        .border(0.5.dp, if (isLight) Color(0x1A000000) else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy Path", fontSize = 12.sp) },
                        onClick = {
                            clipboardManager.setText(AnnotatedString(model.fullPath))
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy Repo ID", fontSize = 12.sp) },
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
                        state is DownloadState.Completed || model.source == ParsedModel.Source.LOCAL -> "Open Folder"
                        else -> "Download Model"
                    }
                    
                    DropdownMenuItem(
                        text = { Text(actionText, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            if (state is DownloadState.Downloading) DownloadManager.stopDownload(model.repoID)
                            else onAction()
                            showMenu = false
                        }
                    )

                    if (model.source == ParsedModel.Source.LOCAL && onDelete != null) {
                        DropdownMenuItem(
                            text = { Text("Delete Local Files", fontSize = 12.sp, color = Color.Red) },
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
