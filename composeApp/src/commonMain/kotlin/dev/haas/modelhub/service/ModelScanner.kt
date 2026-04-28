package dev.haas.modelhub.service

import dev.haas.modelhub.model.ParsedModel
import java.io.File

object ModelScanner {
    private val userHome = System.getProperty("user.home")
    
    val lmStudioRoot = File(userHome, ".lmstudio/models")
    val huggingFaceRoot = File(userHome, ".cache/huggingface/hub")
    val ollamaRoot = File(userHome, ".ollama/models/manifests/registry.ollama.ai/library")

    fun scanAll(customPaths: List<String> = emptyList()): List<ParsedModel> {
        val out = mutableListOf<ParsedModel>()
        out.addAll(scanLMStudio())
        out.addAll(scanHuggingFace())
        out.addAll(scanOllama())
        out.addAll(scanCommonFolders())
        
        customPaths.forEach { path ->
            out.addAll(scanFolder(File(path), ParsedModel.Source.LOCAL))
        }
        
        return out.sortedBy { it.sortKey }
    }

    private fun scanFolder(folder: File, source: ParsedModel.Source): List<ParsedModel> {
        if (!folder.exists() || !folder.isDirectory) return emptyList()
        val out = mutableListOf<ParsedModel>()
        folder.walkTopDown().maxDepth(3).forEach { file ->
            val ext = file.extension.lowercase()
            if (ext == "gguf" || ext == "safetensors" || ext == "task") {
                out.add(
                    ModelParser.parse(
                        publisher = if (folder.name == "models") folder.parentFile?.name ?: "local" else "local",
                        repo = file.name,
                        path = file.absolutePath,
                        source = source
                    )
                )
            }
        }
        return out
    }

    fun scanLMStudio(): List<ParsedModel> {
        if (!lmStudioRoot.exists() || !lmStudioRoot.isDirectory) return emptyList()

        val out = mutableListOf<ParsedModel>()
        lmStudioRoot.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.forEach { pubDir ->
            pubDir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.forEach { repoDir ->
                out.add(
                    ModelParser.parse(
                        publisher = pubDir.name,
                        repo = repoDir.name,
                        path = repoDir.absolutePath,
                        source = ParsedModel.Source.LM_STUDIO
                    )
                )
            }
        }
        return out
    }

    fun scanHuggingFace(): List<ParsedModel> {
        if (!huggingFaceRoot.exists() || !huggingFaceRoot.isDirectory) return emptyList()

        val out = mutableListOf<ParsedModel>()
        huggingFaceRoot.listFiles()?.filter { it.isDirectory && it.name.startsWith("models--") }?.forEach { repoDir ->
            val parts = repoDir.name.split("--")
            if (parts.size >= 3) {
                val publisher = parts[1]
                val repo = parts.drop(2).joinToString("--")
                out.add(
                    ModelParser.parse(
                        publisher = publisher,
                        repo = repo,
                        path = repoDir.absolutePath,
                        source = ParsedModel.Source.HUGGING_FACE
                    )
                )
            }
        }
        return out
    }

    fun scanOllama(): List<ParsedModel> {
        if (!ollamaRoot.exists() || !ollamaRoot.isDirectory) return emptyList()

        val out = mutableListOf<ParsedModel>()
        ollamaRoot.listFiles()?.filter { it.isDirectory || it.isFile }?.forEach { modelEntry ->
            if (modelEntry.isDirectory) {
                modelEntry.listFiles()?.forEach { tagFile ->
                    out.add(
                        ModelParser.parse(
                            publisher = "ollama",
                            repo = "${modelEntry.name}:${tagFile.name}",
                            path = tagFile.absolutePath,
                            source = ParsedModel.Source.OLLAMA
                        )
                    )
                }
            } else if (modelEntry.isFile) {
                out.add(
                    ModelParser.parse(
                        publisher = "ollama",
                        repo = modelEntry.name,
                        path = modelEntry.absolutePath,
                        source = ParsedModel.Source.OLLAMA
                    )
                )
            }
        }
        return out
    }

    fun scanCommonFolders(): List<ParsedModel> {
        val paths = listOf("Downloads", "Documents", "Desktop")
        val out = mutableListOf<ParsedModel>()
        
        paths.forEach { folderName ->
            val folder = File(userHome, folderName)
            if (folder.exists() && folder.isDirectory) {
                folder.walkTopDown().maxDepth(2).forEach { file ->
                    val ext = file.extension.lowercase()
                    if (ext == "gguf" || ext == "safetensors" || ext == "task") {
                        out.add(
                            ModelParser.parse(
                                publisher = "local",
                                repo = file.name,
                                path = file.absolutePath,
                                source = ParsedModel.Source.LOCAL
                            )
                        )
                    }
                }
            }
        }
        return out
    }
}
