package dev.haas.modelhub.service

import dev.haas.modelhub.model.ParsedModel
import java.io.File

object ModelParser {
    fun parse(
        publisher: String,
        repo: String,
        path: String,
        source: ParsedModel.Source,
        tags: List<String>? = null
    ): ParsedModel {
        var format = detectFormatFromName(publisher, repo)

        val rawTokens = repo.split("-").filter { it.isNotEmpty() }

        val keptTokens = mutableListOf<String>()
        var quant: String? = null
        for (tok in rawTokens) {
            val l = tok.lowercase()
            if (l == "mlx") {
                format = format ?: "MLX"
                continue
            }
            if (l == "gguf") {
                format = format ?: "GGUF"
                continue
            }
            val q = parseQuant(tok)
            if (q != null) {
                if (quant == null) quant = q
                continue
            }
            keptTokens.append(tok)
        }

        if (format == null && tags != null) {
            format = detectFormatFromTags(tags)
        }

        if (format == null) {
            format = detectFormatFromFiles(path)
        }

        val expanded = keptTokens.flatMap { splitLetterDigit(it) }
        val prettyTokens = expanded.map { prettyToken(it) }
        val displayName = prettyTokens.joinToString(" ")

        val familyTag = extractFamilyTag(expanded.firstOrNull() ?: repo)

        val typeLabel = composeTypeLabel(format, quant)

        return ParsedModel(
            publisher = publisher,
            repo = repo,
            familyTag = familyTag,
            displayName = displayName,
            typeLabel = typeLabel,
            fullPath = path,
            source = source
        )
    }

    private fun extractFamilyTag(token: String): String {
        var t = token
        if (t.lowercase().endsWith(".cpp")) t = t.dropLast(4)

        val letters = t.takeWhile { it.isLetter() }.lowercase()
        return if (letters.isEmpty()) "model" else letters
    }

    private fun detectFormatFromName(publisher: String, repo: String): String? {
        val combined = (publisher + " " + repo).lowercase()
        if (combined.contains("gguf")) return "GGUF"
        if (combined.contains("mlx")) return "MLX"
        return null
    }

    private fun detectFormatFromTags(tags: List<String>): String? {
        val lowered = tags.map { it.lowercase() }.toSet()
        if (lowered.contains("gguf")) return "GGUF"
        if (lowered.contains("mlx")) return "MLX"
        if (lowered.contains("safetensors")) return "safetensors"
        return null
    }

    private fun detectFormatFromFiles(path: String): String? {
        val file = File(path)
        if (!file.exists()) return null
        
        var sawSafetensors = false
        var count = 0
        file.walkTopDown().maxDepth(2).forEach { f ->
            count++
            if (count > 400) return@forEach
            val ext = f.extension.lowercase()
            if (ext == "gguf") return "GGUF"
            if (ext == "safetensors") sawSafetensors = true
        }
        return if (sawSafetensors) "safetensors" else null
    }

    private fun splitLetterDigit(tok: String): List<String> {
        if (tok.isEmpty() || !tok[0].isLetter()) return listOf(tok)

        val firstDigitIdx = tok.indexOfFirst { it.isDigit() }
        if (firstDigitIdx == -1) return listOf(tok)

        val prefix = tok.substring(0, firstDigitIdx)
        val suffix = tok.substring(firstDigitIdx)
        if (prefix.length >= 3) return listOf(prefix, suffix)
        return listOf(tok)
    }

    private fun parseQuant(tok: String): String? {
        val l = tok.lowercase()
        if (l.matches(Regex("""^\d{1,2}bit$"""))) return l
        if (l.matches(Regex("""^q\d+(_[a-z0-9]+)*$"""))) return tok.uppercase()
        if (listOf("bf16", "fp16", "f16", "f32", "b16").contains(l)) return l.uppercase()
        if (l.matches(Regex("""^int\d+$"""))) return l.uppercase()
        return null
    }

    private fun prettyToken(tok: String): String {
        val lower = tok.lowercase()

        val upperCaseAbbrevs = setOf(
            "it", "vl", "moe", "sft", "dpo", "rm", "rlhf", "ft", "gguf", "mlx", "vlm"
        )
        if (upperCaseAbbrevs.contains(lower)) return lower.uppercase()

        if (tok.matches(Regex("""^[0-9]+(\.[0-9]+)?$"""))) return tok

        if (tok.matches(Regex("""^[0-9]+[a-zA-Z]+$"""))) return tok.uppercase()
        if (tok.matches(Regex("""^[a-zA-Z]\d+[a-zA-Z]+$"""))) return tok.uppercase()

        var t = tok
        if (t.lowercase().endsWith(".cpp")) t = t.dropLast(4)

        if (t.length > 1 && t.drop(1).any { it.isUpperCase() }) return t

        if (t.isEmpty()) return ""
        return t.replaceFirstChar { it.uppercase() }
    }

    private fun composeTypeLabel(format: String?, quant: String?): String? {
        return when {
            format != null && quant != null -> "$format $quant"
            format != null -> format
            quant != null -> quant
            else -> null
        }
    }

    // Helper for Swift's append which was used on a list in my translation thought process but it's add in Kotlin
    private fun MutableList<String>.append(element: String) = add(element)
}
