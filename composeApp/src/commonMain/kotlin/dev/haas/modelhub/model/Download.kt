package dev.haas.modelhub.model

import kotlinx.coroutines.Job

data class DownloadFile(
    val filename: String,
    var transferredBytes: Long = 0,
    var blobHash: String? = null
)

class Download(
    val repoID: String,
    var totalBytes: Long,
    var sha: String? = null,
    var files: List<DownloadFile> = emptyList(),
    var currentIndex: Int = 0,
    var completedFileBytes: Long = 0,
    var currentFileBytes: Long = 0,
    var job: Job? = null,
    var speedSamples: MutableList<Pair<Long, Long>> = mutableListOf(),
    var state: DownloadState = DownloadState.Queued
) {
    val bytesDownloaded: Long get() = completedFileBytes + currentFileBytes
    
    val progress: Double get() = if (totalBytes > 0) (bytesDownloaded.toDouble() / totalBytes).coerceIn(0.0, 1.0) else 0.0

    val bytesPerSecond: Double get() {
        val now = System.currentTimeMillis()
        val cutoff = now - 3000
        val recent = speedSamples.filter { it.first >= cutoff }
        if (recent.size < 2) return 0.0
        val first = recent.first()
        val last = recent.last()
        val bytes = last.second - first.second
        val seconds = (last.first - first.first) / 1000.0
        return if (seconds > 0) bytes / seconds else 0.0
    }

    fun recordSample() {
        val now = System.currentTimeMillis()
        speedSamples.add(now to bytesDownloaded)
        val cutoff = now - 5000
        speedSamples.removeAll { it.first < cutoff }
    }
}
