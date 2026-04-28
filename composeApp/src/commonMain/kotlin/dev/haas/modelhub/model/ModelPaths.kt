package dev.haas.modelhub.model

import java.io.File

object ModelPaths {
    val userHome = System.getProperty("user.home")
    val lmStudioRoot = File(userHome, ".lmstudio/models")
    val huggingFaceRoot = File(userHome, ".cache/huggingface/hub")
    val ollamaRoot = File(userHome, ".ollama/models/manifests/registry.ollama.ai/library")
}
