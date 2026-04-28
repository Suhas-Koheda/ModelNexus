package dev.haas.modelhub.service

import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner
import kotlinx.serialization.json.*

object LiveLoadedChecker {
    /**
     * Returns the lowercased `publisher/repo` IDs of models currently
     * loaded by LM Studio. Empty if the server isn't reachable.
     */
    fun loadedCopyableIDs(): Set<String> {
        return try {
            val url = URL("http://127.0.0.1:1234/v1/models")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 400
            connection.readTimeout = 400

            if (connection.responseCode == 200) {
                val scanner = Scanner(connection.inputStream).useDelimiter("\\A")
                val response = if (scanner.hasNext()) scanner.next() else ""
                val json = Json.parseToJsonElement(response).jsonObject
                val data = json["data"]?.jsonArray ?: return emptySet()
                
                val ids = mutableSetOf<String>()
                for (element in data) {
                    val id = element.jsonObject["id"]?.jsonPrimitive?.content
                    if (!id.isNullOrEmpty()) {
                        ids.add(id.lowercase())
                    }
                }
                ids
            } else {
                emptySet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }
}
