package dev.haas.modelhub.service

import dev.haas.modelhub.model.HFModelSummary
import dev.haas.modelhub.model.ParsedModel
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object HuggingFaceAPI {
    private const val BASE_URL = "https://huggingface.co/api"
    private val json = Json { ignoreUnknownKeys = true }

    fun searchModels(query: String?, limit: Int = 30): List<ParsedModel> {
        return try {
            val urlString = StringBuilder("$BASE_URL/models?pipeline_tag=text-generation&limit=$limit")
            if (!query.isNullOrBlank()) {
                urlString.append("&search=${query.trim()}")
            } else {
                urlString.append("&sort=downloads&direction=-1")
            }

            val url = URL(urlString.toString())
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val scanner = Scanner(connection.inputStream).useDelimiter("\\A")
                val response = if (scanner.hasNext()) scanner.next() else ""
                val summaries = json.decodeFromString<List<HFModelSummary>>(response)
                
                summaries.map { summary ->
                    val parts = summary.id.split("/")
                    val publisher = if (parts.size >= 2) parts[0] else "unknown"
                    val repo = if (parts.size >= 2) parts[1] else summary.id
                    
                    ModelParser.parse(
                        publisher = publisher,
                        repo = repo,
                        path = "", // Remote model
                        source = ParsedModel.Source.HUGGING_FACE,
                        tags = summary.tags
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
