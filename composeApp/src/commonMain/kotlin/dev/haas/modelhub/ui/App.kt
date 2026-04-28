package dev.haas.modelhub.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.haas.modelhub.model.ParsedModel
import dev.haas.modelhub.model.Tab
import dev.haas.modelhub.service.DownloadManager
import dev.haas.modelhub.service.HuggingFaceAPI
import dev.haas.modelhub.service.ModelScanner
import dev.haas.modelhub.ui.components.ModelRow
import dev.haas.modelhub.ui.components.TabSwitcher
import kotlinx.coroutines.delay
import java.io.File
import java.awt.Desktop

@Composable
fun App() {
    var isDarkTheme by remember { mutableStateOf(false) }

    val lightColorScheme = lightColorScheme(
        primary = Color(0xFF007AFF),
        onPrimary = Color.White,
        surface = Color(0xFFFBFBFB),
        background = Color.Transparent,
        onSurface = Color(0xFF1E1E1E),
        onSurfaceVariant = Color(0xFF8E8E93),
        surfaceVariant = Color(0xFFF5F5F7)
    )

    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF0A84FF),
        onPrimary = Color.White,
        surface = Color(0xFF1C1C1E),
        background = Color.Transparent,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFF98989D),
        surfaceVariant = Color(0xFF2C2C2E)
    )

    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme else lightColorScheme,
        typography = Typography(
            bodyLarge = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif),
            bodyMedium = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif),
            labelSmall = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDarkTheme) Color(0xFF000000) else Color(0xFFF5F5F7))
                .border(0.5.dp, if (isDarkTheme) Color(0x33FFFFFF) else Color(0x1A000000), RoundedCornerShape(16.dp))
        ) {
            ModelHubWidget(isDarkTheme) { isDarkTheme = it }
        }
    }
}

@Composable
fun ModelHubWidget(isDarkTheme: Boolean, onThemeToggle: (Boolean) -> Unit) {
    var selectedTab by remember { mutableStateOf(Tab.MODELS) }
    var searchQuery by remember { mutableStateOf("") }
    var localModels by remember { mutableStateOf<List<ParsedModel>>(emptyList()) }
    var remoteModels by remember { mutableStateOf<List<ParsedModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        localModels = ModelScanner.scanAll()
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            isLoading = true
            delay(500)
            remoteModels = HuggingFaceAPI.searchModels(searchQuery)
            isLoading = false
        } else if (searchQuery.isEmpty()) {
            remoteModels = HuggingFaceAPI.searchModels(null)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ModelNexus",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(
                onClick = { onThemeToggle(!isDarkTheme) },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        TabSwitcher(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = {
                Text(
                    "Search models...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = RoundedCornerShape(10.dp),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            Crossfade(targetState = selectedTab) { tab ->
                when (tab) {
                    Tab.MODELS -> ModelList(
                        localModels,
                        onAction = { model -> openFolder(model.fullPath) },
                        onDelete = { model ->
                            deleteModel(model)
                            localModels = ModelScanner.scanAll()
                        }
                    )
                    Tab.EXPLORE -> ModelList(
                        remoteModels,
                        onAction = { model -> DownloadManager.startDownload(model.repoID, model.sizeBytes) }
                    )
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
fun ModelList(
    models: List<ParsedModel>,
    onAction: (ParsedModel) -> Unit,
    onDelete: ((ParsedModel) -> Unit)? = null
) {
    if (models.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No models",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(models) { model ->
                ModelRow(
                    model = model,
                    onAction = { onAction(model) },
                    onDelete = if (onDelete != null) { { onDelete(model) } } else null
                )
            }
        }
    }
}

fun openFolder(path: String) {
    try {
        val file = File(path)
        val folder = if (file.isDirectory) file else file.parentFile
        if (folder != null && folder.exists()) {
            Desktop.getDesktop().open(folder)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun deleteModel(model: ParsedModel) {
    try {
        val file = File(model.fullPath)
        if (file.exists()) {
            if (file.isDirectory) file.deleteRecursively()
            else file.delete()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
