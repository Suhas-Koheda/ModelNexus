package dev.haas.modelhub.model

import java.io.File

data class ParsedModel(
    val publisher: String,
    val repo: String,
    val familyTag: String,
    val displayName: String,
    val typeLabel: String?,
    val fullPath: String,
    val source: Source
) {
    enum class Source {
        LM_STUDIO,
        HUGGING_FACE,
        OLLAMA,
        LOCAL
    }

    val copyableID: String get() = "${publisher.lowercase()}/${repo.lowercase()}"
    
    val canonicalID: String get() = "$publisher/$repo"

    val sortKey: String get() = "$familyTag ${displayName.lowercase()}"

    fun matches(query: String): Boolean {
        val q = query.lowercase()
        if (q.isEmpty()) return true
        if (publisher.lowercase().contains(q)) return true
        if (repo.lowercase().contains(q)) return true
        if (displayName.lowercase().contains(q)) return true
        if (familyTag.contains(q)) return true
        if ((typeLabel ?: "").lowercase().contains(q)) return true
        if (copyableID.contains(q)) return true
        return false
    }
}
