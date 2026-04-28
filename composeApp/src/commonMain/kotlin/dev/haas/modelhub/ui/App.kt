package dev.haas.modelhub.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.haas.modelhub.model.ParsedModel
import dev.haas.modelhub.service.ModelScanner
import dev.haas.modelhub.service.SizeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

@Composable
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7C4DFF), // Deep Purple
            secondary = Color(0xFF00E5FF), // Cyan
            surface = Color(0xFF1E1E1E),
            background = Color(0xFF0A0A0A)
        ),
        typography = Typography(
            headlineMedium = androidx.compose.material3.Typography().headlineMedium.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold
            )
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Subtle background gradient
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF7C4DFF).copy(alpha = 0.05f),
                        radius = 400.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(0f, 0f)
                    )
                }
                ModelHubContent()
            }
        }
    }
}

@Composable
fun ModelHubContent() {
    var query by remember { mutableStateOf("") }
    var currentTab by remember { mutableStateOf("Local") }
    var customFolders by remember { mutableStateOf(emptyList<String>()) }
    var localModels by remember { mutableStateOf(emptyList<ParsedModel>()) }
    var remoteModels by remember { mutableStateOf(emptyList<ParsedModel>()) }
    var loadedIds by remember { mutableStateOf(emptySet<String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    val scanLocal = suspend {
        withContext(Dispatchers.IO) {
            localModels = ModelScanner.scanAll(customFolders)
            loadedIds = LiveLoadedChecker.loadedCopyableIDs()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        while(true) {
            scanLocal()
            kotlinx.coroutines.delay(30000) // Auto-refresh every 30s
        }
    }

    LaunchedEffect(query, currentTab) {
        if (currentTab == "Explore") {
            isLoading = true
            withContext(Dispatchers.IO) {
                remoteModels = HuggingFaceAPI.searchModels(query)
                isLoading = false
            }
        }
    }

    val filteredLocalModels = remember(query, localModels) {
        localModels.filter { it.matches(query) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "ModelNexus",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            
            // Refresh Button
            IconButton(onClick = { 
                isLoading = true
                kotlinx.coroutines.MainScope().launch { scanLocal() }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
            }

            // Settings Button
            IconButton(onClick = { isSettingsOpen = !isSettingsOpen }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isSettingsOpen) {
            SettingsPanel(customFolders) { newList ->
                customFolders = newList
                isLoading = true
                kotlinx.coroutines.MainScope().launch { scanLocal() }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tabs
        Row {
            TabButton("Local", currentTab == "Local") { currentTab = "Local" }
            Spacer(modifier = Modifier.width(8.dp))
            TabButton("Explore", currentTab == "Explore") { currentTab = "Explore" }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (currentTab == "Local") "Search local models..." else "Search on Hugging Face...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        val modelsToShow = if (currentTab == "Local") filteredLocalModels else remoteModels

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(modelsToShow) { model ->
                ModelRow(
                    model, 
                    isLoaded = loadedIds.contains(model.copyableID),
                    isRemote = currentTab == "Explore"
                ) {
                    // On action (delete for local, download for remote)
                    if (currentTab == "Local") {
                        val file = File(model.fullPath)
                        if (file.exists()) {
                            file.deleteRecursively()
                            localModels = localModels.filter { it.fullPath != model.fullPath }
                        }
                    } else {
                        // Handle download (placeholder for now)
                        println("Downloading ${model.copyableID}")
                    }
                }
                Divider(color = Color.Gray.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
            contentColor = if (isSelected) Color.Black else Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ModelRow(model: ParsedModel, isLoaded: Boolean, isRemote: Boolean, onAction: () -> Unit) {
    var isHovered by remember { mutableStateOf(false) }
    var showCopied by remember { mutableStateOf(false) }
    val size = remember(model.fullPath) { 
        if (model.fullPath.isNotEmpty()) SizeUtil.format(SizeUtil.getDirectorySize(File(model.fullPath))) else ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHovered) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable {
                copyToClipboard(model.copyableID)
                showCopied = true
            }
            .onHover { isHovered = it }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Tag
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "[${model.familyTag}]",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Source Badge
            Surface(
                color = when(model.source) {
                    ParsedModel.Source.LM_STUDIO -> Color(0xFF64B5F6).copy(alpha = 0.2f)
                    ParsedModel.Source.HUGGING_FACE -> Color(0xFFFFB74D).copy(alpha = 0.2f)
                    ParsedModel.Source.OLLAMA -> Color(0xFF81C784).copy(alpha = 0.2f)
                    ParsedModel.Source.LOCAL -> Color(0xFF90A4AE).copy(alpha = 0.2f)
                },
                shape = CircleShape
            ) {
                Text(
                    when(model.source) {
                        ParsedModel.Source.LM_STUDIO -> "LM"
                        ParsedModel.Source.HUGGING_FACE -> "HF"
                        ParsedModel.Source.OLLAMA -> "OL"
                        ParsedModel.Source.LOCAL -> "PC"
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (showCopied) "Copied to clipboard!" else model.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (showCopied) MaterialTheme.colorScheme.secondary else Color.White
                    ),
                    maxLines = 1
                )
                LaunchedEffect(showCopied) {
                    if (showCopied) {
                        kotlinx.coroutines.delay(2000)
                        showCopied = false
                    }
                }
            }

            // Pulsing Dot
            if (isLoaded) {
                PulsingDot()
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Type
            model.typeLabel?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Size / Trash Animation
            Box(contentAlignment = Alignment.CenterEnd) {
                AnimatedVisibility(
                    visible = !isHovered,
                    enter = fadeIn() + slideInHorizontally { it / 2 },
                    exit = fadeOut() + slideOutHorizontally { it / 2 }
                ) {
                    Text(
                        size,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    )
                }

                AnimatedVisibility(
                    visible = isHovered,
                    enter = fadeIn() + slideInHorizontally { it },
                    exit = fadeOut() + slideOutHorizontally { it }
                ) {
                    IconButton(onClick = onAction, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (isRemote) Icons.Default.KeyboardArrowDown else Icons.Default.Delete, 
                            contentDescription = if (isRemote) "Download" else "Delete", 
                            tint = if (isRemote) MaterialTheme.colorScheme.primary else Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color(0xFF81C784).copy(alpha = alpha))
    )
}

fun copyToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}

@Composable
fun SettingsPanel(folders: List<String>, onUpdate: (List<String>) -> Unit) {
    var newFolder by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Custom Scan Folders", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            
            folders.forEach { folder ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(folder, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    IconButton(onClick = { onUpdate(folders.filter { it != folder }) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newFolder,
                    onValueChange = { newFolder = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add folder path...", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (newFolder.isNotBlank()) {
                        onUpdate(folders + newFolder)
                        newFolder = ""
                    }
                }) {
                    Text("Add")
                }
            }
        }
    }
}

// Extension for hover detection
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun Modifier.onHover(onHover: (Boolean) -> Unit): Modifier {
    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    androidx.compose.ui.input.pointer.PointerEventType.Enter -> onHover(true)
                    androidx.compose.ui.input.pointer.PointerEventType.Exit -> onHover(false)
                }
            }
        }
    }
}
