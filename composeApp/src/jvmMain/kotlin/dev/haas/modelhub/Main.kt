package dev.haas.modelhub

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.window.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Folder
import dev.haas.modelhub.ui.App
import java.io.File
import java.io.PrintStream
import java.io.FileOutputStream

fun setupLogging() {
    try {
        val logDir = File(System.getProperty("user.home"), ".cache/modelnexus")
        if (!logDir.exists()) logDir.mkdirs()
        val logFile = File(logDir, "logs.txt")
        val out = PrintStream(FileOutputStream(logFile, true))
        System.setOut(out)
        System.setErr(out)
        println("\n--- Session Started: ${java.util.Date()} ---")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun main() = application {
    var isVisible by remember { mutableStateOf(false) }
    
    val trayState = rememberTrayState()
    val windowState = rememberWindowState(
        width = 400.dp,
        height = 600.dp,
        position = WindowPosition.Aligned(androidx.compose.ui.Alignment.TopEnd)
    )

    LaunchedEffect(Unit) {
        setupLogging()
    }

    Tray(
        icon = rememberVectorPainter(Icons.Default.Inventory),
        state = trayState,
        tooltip = "ModelNexus",
        onAction = { isVisible = !isVisible },
        menu = {
            Item("Show Hub", onClick = { isVisible = true })
            Item("Hide Hub", onClick = { isVisible = false })
            Separator()
            Item("Scan for Models", onClick = { /* Force re-scan logic if needed */ })
            Separator()
            Item("Quit ModelNexus", onClick = { exitApplication() })
        }
    )

    if (isVisible) {
        Window(
            onCloseRequest = { isVisible = false },
            title = "ModelNexus",
            state = windowState,
            undecorated = true,
            transparent = true,
            resizable = false,
            alwaysOnTop = true,
            focusable = true
        ) {
            App()
        }
    }
}
