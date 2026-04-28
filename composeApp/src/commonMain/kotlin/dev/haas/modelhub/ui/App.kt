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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@Composable
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF007AFF), // macOS Blue
            onPrimary = Color.White,
            surface = Color(0x991C1C1E), // Translucent Dark
            background = Color.Transparent, // Root is transparent
            onSurface = Color(0xFFE5E5E7),
            onSurfaceVariant = Color(0xFFA1A1A6)
        ),
        typography = Typography() // Use default which is clean
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC1C1C1E)) // Glass effect
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
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
        // Minimal Header
        TabSwitcher(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            placeholder = { Text("Search", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(10.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0x1AFFFFFF),
                unfocusedContainerColor = Color(0x0DFFFFFF),
                focusedBorderColor = Color(0x33FFFFFF),
                unfocusedBorderColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            Crossfade(targetState = selectedTab) { tab ->
                when (tab) {
                    Tab.MODELS -> ModelList(localModels) { model -> }
                    Tab.EXPLORE -> ModelList(remoteModels) { model ->
                        DownloadManager.startDownload(model.repoID, 0)
                    }
                }
            }
            
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
fun ModelList(models: List<ParsedModel>, onAction: (ParsedModel) -> Unit) {
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
                ModelRow(model = model, onAction = { onAction(model) })
            }
        }
    }
}
