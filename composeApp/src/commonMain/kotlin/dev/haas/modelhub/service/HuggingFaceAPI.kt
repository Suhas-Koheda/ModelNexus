package dev.haas.modelhub.service

import dev.haas.modelhub.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object HuggingFaceAPI {
    private const val BASE_URL = "https://huggingface.co/api"
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun searchModels(query: String?, limit: Int = 30): List<ParsedModel> {
        return try {
            val urlString = StringBuilder("$BASE_URL/models?pipeline_tag=text-generation&limit=$limit")
            if (!query.isNullOrBlank()) {
                urlString.append("&search=${query.trim()}")
            } else {
                urlString.append("&sort=downloads&direction=-1")
            }

            val summaries: List<HFModelSummary> = client.get(urlString.toString()).body()
            
            summaries.map { summary ->
                ModelParser.parse(
                    publisher = summary.publisher,
                    repo = summary.repo,
                    path = "", 
                    source = ParsedModel.Source.HUGGING_FACE,
                    tags = summary.tags,
                    sizeBytes = summary.usedStorage ?: 0L
                )
            }
        } catch (e: Throwable) {
            if (e::class.simpleName?.contains("Cancellation") == true) throw e
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun modelDetail(repoID: String): HFModelDetail {
        val url = "$BASE_URL/models/$repoID"
        return client.get(url).body()
    }

    fun resolveURL(repoID: String, sha: String, filename: String): String {
        return "https://huggingface.co/$repoID/resolve/$sha/$filename"
    }
}
