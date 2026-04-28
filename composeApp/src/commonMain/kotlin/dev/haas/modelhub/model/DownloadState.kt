package dev.haas.modelhub.model

sealed class DownloadState {
    object NotStarted : DownloadState()
    object Queued : DownloadState()
    data class Downloading(
        val progress: Double,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val bytesPerSecond: Double
    ) : DownloadState()
    data class Paused(
        val progress: Double,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState()
    object Completed : DownloadState()
    data class Failed(val message: String) : DownloadState()
}
