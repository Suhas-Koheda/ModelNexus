package dev.haas.modelhub.service

import java.io.File
import java.text.DecimalFormat

object SizeUtil {
    fun format(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    fun getDirectorySize(file: File): Long {
        if (!file.exists()) return 0
        if (file.isFile) return file.length()
        
        var size: Long = 0
        file.listFiles()?.forEach {
            // Skips symlinks to avoid double-counting in HF cache
            if (!isSymlink(it)) {
                size += if (it.isDirectory) getDirectorySize(it) else it.length()
            }
        }
        return size
    }

    private fun isSymlink(file: File): Boolean {
        return java.nio.file.Files.isSymbolicLink(file.toPath())
    }
}
