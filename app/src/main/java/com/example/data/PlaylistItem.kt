package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class PlaylistItem(
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null
)

object M3uParser {
    private const val TAG = "M3uParser"
    private val client = OkHttpClient()

    suspend fun fetchAndParseM3u(url: String): List<PlaylistItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<PlaylistItem>()
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to download file: $response")
                
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val lines = bodyString.lines()

                var currentMetaMap: Map<String, String>? = null
                var currentName: String? = null

                for (line in lines) {
                    val trimmedLine = line.trim()
                    if (trimmedLine.isEmpty()) continue

                    if (trimmedLine.startsWith("#EXTINF:")) {
                        // Extract attributes: key="value"
                        currentMetaMap = parseAttributes(trimmedLine)
                        
                        // Extract channel name (everything after the last comma)
                        currentName = trimmedLine.substringAfterLast(",").trim()
                        if (currentName.startsWith("#EXTINF:")) {
                            // No comma found, or fallback
                            currentName = "Dizi"
                        }
                    } else if (!trimmedLine.startsWith("#")) {
                        // This must be the stream URL!
                        val streamUrl = trimmedLine
                        val name = currentName ?: "Bilinməyən Dizi"
                        val logo = currentMetaMap?.get("tvg-logo") ?: currentMetaMap?.get("logo")
                        val group = currentMetaMap?.get("group-title")

                        items.add(
                            PlaylistItem(
                                name = name,
                                url = streamUrl,
                                logo = logo?.takeIf { it.isNotEmpty() },
                                group = group?.takeIf { it.isNotEmpty() }
                            )
                        )

                        // Reset temp placeholder states
                        currentMetaMap = null
                        currentName = null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching and parsing M3U list", e)
        }

        return@withContext items
    }

    private fun parseAttributes(extInfLine: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        // Regular expression to match pattern key="value"
        val regex = """([a-zA-Z0-9_-]+)="([^"]*)"""".toRegex()
        val matches = regex.findAll(extInfLine)
        for (match in matches) {
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            map[key] = value
        }
        return map
    }
}
