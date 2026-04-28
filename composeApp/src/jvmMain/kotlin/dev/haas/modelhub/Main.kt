package dev.haas.modelhub

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.window.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import dev.haas.modelhub.ui.App

fun main() = application {
    var isVisible by remember { mutableStateOf(true) }
    
    val trayState = rememberTrayState()
    val windowState = rememberWindowState(
        width = 400.dp,
        height = 600.dp,
        position = WindowPosition.Aligned(androidx.compose.ui.Alignment.TopEnd)
    )

    Tray(
        icon = rememberVectorPainter(Icons.Default.Hub),
        state = trayState,
        tooltip = "ModelNexus",
        onAction = { isVisible = !isVisible },
        menu = {
            Item("Toggle Widget", onClick = { isVisible = !isVisible })
            Separator()
            Item("Exit", onClick = { exitApplication() })
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
            // Close on focus lost to behave like a tray popup
            LaunchedEffect(Unit) {
                window.type = java.awt.Window.Type.UTILITY
                window.addWindowFocusListener(object : java.awt.event.WindowFocusListener {
                    override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {}
                    override fun windowLostFocus(e: java.awt.event.WindowEvent?) {
                        isVisible = false
                    }
                })
            }
            App()
        }
    }
}
