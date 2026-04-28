package dev.haas.modelhub.service

import dev.haas.modelhub.model.Download
import dev.haas.modelhub.model.DownloadFile
import dev.haas.modelhub.model.DownloadState
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.io.RandomAccessFile

object DownloadManager {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
    val downloads: StateFlow<Map<String, Download>> = _downloads

    fun getDownloadState(repoID: String): DownloadState {
        val active = _downloads.value[repoID]
        if (active != null) return active.state
        
        // Check if already downloaded
        if (HFCacheWriter.isDownloaded(repoID)) return DownloadState.Completed
        
        return DownloadState.NotStarted
    }

    fun startDownload(repoID: String, estimatedTotalBytes: Long) {
        if (getDownloadState(repoID) == DownloadState.Completed) return
        if (_downloads.value.containsKey(repoID)) return

        val download = Download(repoID, estimatedTotalBytes)
        _downloads.value = _downloads.value + (repoID to download)
        
        download.job = scope.launch {
            try {
                prepareAndStart(download)
            } catch (e: Exception) {
                e.printStackTrace()
                updateState(download, DownloadState.Failed(e.message ?: "Unknown error"))
            }
        }
    }

    fun stopDownload(repoID: String) {
        val download = _downloads.value[repoID]
        download?.job?.cancel()
        _downloads.value = _downloads.value - repoID
    }

    private suspend fun prepareAndStart(download: Download) {
        val detail = HuggingFaceAPI.modelDetail(download.repoID)
        val sha = detail.sha ?: throw Exception("Missing commit SHA")
        val siblings = detail.siblings ?: throw Exception("No files found")
        
        download.sha = sha
        download.files = siblings.map { DownloadFile(it.rfilename) }
        
        HFCacheWriter.prepareDirectories(download.repoID, sha)
        
        updateState(download, DownloadState.Downloading(0.0, 0, download.totalBytes, 0.0))
        
        startCurrentFile(download)
    }

    private suspend fun startCurrentFile(download: Download) {
        if (download.currentIndex >= download.files.size) {
            finalizeDownload(download)
            return
        }

        val file = download.files[download.currentIndex]
        val sha = download.sha!!
        val url = HuggingFaceAPI.resolveURL(download.repoID, sha, file.filename)
        
        val response = client.get(url) {
            // Handle redirects if necessary, CIO handles them by default
        }

        if (response.status.value !in 200..299) {
            throw Exception("HTTP ${response.status.value}")
        }

        val channel = response.bodyAsChannel()
        val blobHash = extractBlobHash(response)
        file.blobHash = blobHash

        val tempFile = File.createTempFile("modelhub_", ".tmp")
        val raf = RandomAccessFile(tempFile, "rw")
        
        try {
            val buffer = ByteArray(8192)
            while (!channel.isClosedForRead) {
                val read = channel.readAvailable(buffer)
                if (read == -1) break
                raf.write(buffer, 0, read)
                download.currentFileBytes += read
                download.recordSample()
                
                updateState(download, DownloadState.Downloading(
                    download.progress,
                    download.bytesDownloaded,
                    download.totalBytes,
                    download.bytesPerSecond
                ))
            }
            
            raf.close()
            
            HFCacheWriter.placeBlob(tempFile, download.repoID, blobHash)
            HFCacheWriter.createSnapshotSymlink(download.repoID, sha, file.filename, blobHash)
            
            download.completedFileBytes += download.currentFileBytes
            download.currentFileBytes = 0
            download.currentIndex++
            
            startCurrentFile(download)
            
        } finally {
            raf.close()
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun finalizeDownload(download: Download) {
        HFCacheWriter.writeRef(download.repoID, download.sha!!)
        updateState(download, DownloadState.Completed)
        _downloads.value = _downloads.value - download.repoID
    }

    private fun updateState(download: Download, state: DownloadState) {
        download.state = state
        _downloads.value = _downloads.value.toMutableMap().apply { put(download.repoID, download) }
    }

    private fun extractBlobHash(response: HttpResponse): String {
        val xLinked = response.headers["X-Linked-Etag"] ?: response.headers["x-linked-etag"]
        val etag = response.headers["ETag"] ?: response.headers["etag"]
        return stripQuotes(xLinked ?: etag ?: java.util.UUID.randomUUID().toString())
    }

    private fun stripQuotes(s: String): String {
        return s.removePrefix("W/").removePrefix("\"").removeSuffix("\"")
    }
}
