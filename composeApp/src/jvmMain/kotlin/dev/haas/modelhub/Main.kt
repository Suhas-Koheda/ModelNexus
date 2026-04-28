package dev.haas.modelhub

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import dev.haas.modelhub.ui.App

fun main() = application {
    val state = rememberWindowState(width = 800.dp, height = 600.dp)
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Model Hub",
        state = state
    ) {
        App()
    }
}
