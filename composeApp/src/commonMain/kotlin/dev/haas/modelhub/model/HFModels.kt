package dev.haas.modelhub.model

import kotlinx.serialization.Serializable

@Serializable
data class HFModelSummary(
    val id: String,
    val downloads: Int? = null,
    val likes: Int? = null,
    val tags: List<String>? = null,
    val pipeline_tag: String? = null,
    val library_name: String? = null
) {
    val publisher: String get() {
        val slash = id.indexOf("/")
        return if (slash == -1) "" else id.substring(0, slash)
    }
    
    val repo: String get() {
        val slash = id.indexOf("/")
        return if (slash == -1) id else id.substring(slash + 1)
    }
}

@Serializable
data class HFSibling(
    val rfilename: String
)

@Serializable
data class HFModelDetail(
    val id: String,
    val sha: String? = null,
    val siblings: List<HFSibling>? = null,
    val usedStorage: Long? = null
)
