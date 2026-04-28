package dev.haas.modelhub.model

import kotlinx.serialization.Serializable

@Serializable
data class HFModelSummary(
    val id: String,
    val author: String? = null,
    val lastModified: String? = null,
    val downloads: Int = 0,
    val pipeline_tag: String? = null,
    val tags: List<String> = emptyList(),
    val siblings: List<HFSibling> = emptyList()
)

@Serializable
data class HFSibling(
    val rfilename: String
)

@Serializable
data class HFModelDetail(
    val id: String,
    val sha: String? = null,
    val siblings: List<HFSibling>? = null
)
