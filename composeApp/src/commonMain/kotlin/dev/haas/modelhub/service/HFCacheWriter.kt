package dev.haas.modelhub.service

import dev.haas.modelhub.model.ModelPaths
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object HFCacheWriter {
    fun modelDirectory(repoID: String): File {
        val parts = repoID.split("/")
        val dirName = "models--" + parts.joinToString("--")
        return File(ModelPaths.huggingFaceRoot, dirName)
    }

    fun isDownloaded(repoID: String): Boolean {
        val refFile = File(modelDirectory(repoID), "refs/main")
        return refFile.exists()
    }

    fun prepareDirectories(repoID: String, sha: String): File {
        val root = modelDirectory(repoID)
        File(root, "blobs").mkdirs()
        File(root, "refs").mkdirs()
        File(root, "snapshots/$sha").mkdirs()
        return root
    }

    fun placeBlob(tempFile: File, repoID: String, blobHash: String): File {
        val blobFile = File(modelDirectory(repoID), "blobs/$blobHash")
        if (blobFile.exists()) {
            tempFile.delete()
            return blobFile
        }
        tempFile.renameTo(blobFile)
        return blobFile
    }

    fun createSnapshotSymlink(repoID: String, sha: String, filename: String, blobHash: String) {
        val root = modelDirectory(repoID)
        val linkFile = File(root, "snapshots/$sha/$filename")
        linkFile.parentFile.mkdirs()

        if (linkFile.exists()) linkFile.delete()

        val depth = filename.split("/").count() - 1
        val upLevels = "../".repeat(depth + 2)
        val target = "${upLevels}blobs/$blobHash"

        try {
            Files.createSymbolicLink(linkFile.toPath(), Paths.get(target))
        } catch (e: Exception) {
            // On some systems/filesystems symlinks might fail, fallback to copy or just log
            e.printStackTrace()
        }
    }

    fun writeRef(repoID: String, sha: String) {
        val refFile = File(modelDirectory(repoID), "refs/main")
        refFile.writeText(sha)
    }
}
