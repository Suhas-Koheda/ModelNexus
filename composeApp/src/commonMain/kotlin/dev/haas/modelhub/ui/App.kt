package dev.haas.modelhub.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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
    // Light Mac Style Theme
    val lightColorScheme = lightColorScheme(
        primary = Color(0xFF007AFF),
        onPrimary = Color.White,
        surface = Color(0xCCFBFBFB),
        background = Color.Transparent,
        onSurface = Color(0xFF1E1E1E),
        onSurfaceVariant = Color(0xFF8E8E93)
    )

    MaterialTheme(
        colorScheme = lightColorScheme,
        typography = Typography(
            bodyLarge = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif),
            bodyMedium = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif),
            labelSmall = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xEEF2F2F7)) // Mac Light Gray
                .border(0.5.dp, Color(0x1A000000), RoundedCornerShape(12.dp))
        ) {
            ModelHubWidget()
        }
    }
}

@Composable
fun ModelHubWidget() {
    var selectedTab by remember { mutableStateOf(Tab.MODELS) }
    var searchQuery by remember { mutableStateOf("") }
    var localModels by remember { mutableStateOf<List<ParsedModel>>(emptyList()) }
    var remoteModels by remember { mutableStateOf<List<ParsedModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Initial Scan
    LaunchedEffect(Unit) {
        localModels = ModelScanner.scanAll()
    }

    // Remote Search
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
        TabSwitcher(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            placeholder = { Text("Search models...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0x0A000000),
                unfocusedContainerColor = Color(0x05000000),
                focusedBorderColor = Color(0x1A000000),
                unfocusedBorderColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.weight(1f)) {
            Crossfade(targetState = selectedTab) { tab ->
                when (tab) {
                    Tab.MODELS -> ModelList(localModels, 
                        onAction = { model -> openFolder(model.fullPath) },
                        onDelete = { model -> 
                            deleteModel(model)
                            localModels = ModelScanner.scanAll()
                        }
                    )
                    Tab.EXPLORE -> ModelList(remoteModels, 
                        onAction = { model -> DownloadManager.startDownload(model.repoID, 0) }
                    )
                }
            }
            
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter),
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp)
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
