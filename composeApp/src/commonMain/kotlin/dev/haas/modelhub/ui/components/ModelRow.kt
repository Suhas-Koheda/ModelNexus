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
    onAction: () -> Unit
) {
    val downloadState by DownloadManager.downloads.collectAsState()
    val currentDownload = downloadState[model.repoID]
    val state = currentDownload?.state ?: DownloadManager.getDownloadState(model.repoID)
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMenu = true }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minimal Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (model.source == ParsedModel.Source.LOCAL) Color(0x22FFFFFF) else Color(0x44007AFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (model.source == ParsedModel.Source.LOCAL) Icons.Default.Folder else Icons.Default.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Text(
                    text = "${model.publisher} • ${model.typeLabel ?: "Model"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (state is DownloadState.Downloading) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        LinearProgressIndicator(
                            progress = state.progress.toFloat(),
                            modifier = Modifier.weight(1f).height(2.dp).clip(RoundedCornerShape(1.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color(0x33FFFFFF)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Minimal Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state is DownloadState.Downloading) {
                    IconButton(
                        onClick = { DownloadManager.stopDownload(model.repoID) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp), tint = Color.Red)
                    }
                } else {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(16.dp))
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF2C2C2E)).border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy Absolute Path", fontSize = 12.sp) },
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
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0x1AFFFFFF))
                    DropdownMenuItem(
                        text = { 
                            Text(
                                if (state is DownloadState.Completed || model.source == ParsedModel.Source.LOCAL) "Open" else "Download",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            ) 
                        },
                        onClick = {
                            onAction()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
