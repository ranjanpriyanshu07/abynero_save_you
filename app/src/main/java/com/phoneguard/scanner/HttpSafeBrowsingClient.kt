package com.phoneguard.scanner

import com.phoneguard.di.IoDispatcher
import com.phoneguard.di.SafeBrowsingApiKey
import com.phoneguard.domain.model.SafeBrowsingResult
import com.phoneguard.domain.model.SafeBrowsingStatus
import com.phoneguard.domain.model.ThreatType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import javax.inject.Inject

/**
 * Google Safe Browsing v4 Lookup API client.
 *
 * Only the URL being checked is sent. Missing key, no internet and server errors are all
 * reported as a status instead of an exception.
 */
class HttpSafeBrowsingClient @Inject constructor(
    @SafeBrowsingApiKey private val apiKey: String,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SafeBrowsingClient {

    override suspend fun lookup(url: String): SafeBrowsingResult {
        if (apiKey.isBlank()) return SafeBrowsingResult(SafeBrowsingStatus.NOT_CONFIGURED)
        return withContext(ioDispatcher) {
            var connection: HttpURLConnection? = null
            try {
                val endpoint = URI.create("$ENDPOINT?key=${URLEncoder.encode(apiKey, "UTF-8")}").toURL()
                connection = (endpoint.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                connection.outputStream.use { it.write(requestBody(url).toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                if (code !in 200..299) {
                    SafeBrowsingResult(SafeBrowsingStatus.ERROR)
                } else {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    parseResponse(body)
                }
            } catch (e: IOException) {
                SafeBrowsingResult(SafeBrowsingStatus.OFFLINE)
            } catch (e: JSONException) {
                SafeBrowsingResult(SafeBrowsingStatus.ERROR)
            } catch (e: RuntimeException) {
                SafeBrowsingResult(SafeBrowsingStatus.ERROR)
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun requestBody(url: String): String = JSONObject()
        .put(
            "client",
            JSONObject().put("clientId", CLIENT_ID).put("clientVersion", CLIENT_VERSION),
        )
        .put(
            "threatInfo",
            JSONObject()
                .put("threatTypes", JSONArray(ThreatType.entries.filter { it != ThreatType.OTHER }.map { it.name }))
                .put("platformTypes", JSONArray(listOf("ANY_PLATFORM")))
                .put("threatEntryTypes", JSONArray(listOf("URL")))
                .put("threatEntries", JSONArray().put(JSONObject().put("url", url))),
        )
        .toString()

    private fun parseResponse(body: String): SafeBrowsingResult {
        if (body.isBlank()) return SafeBrowsingResult(SafeBrowsingStatus.NO_THREAT)
        val matches = JSONObject(body).optJSONArray("matches")
            ?: return SafeBrowsingResult(SafeBrowsingStatus.NO_THREAT)
        if (matches.length() == 0) return SafeBrowsingResult(SafeBrowsingStatus.NO_THREAT)
        val threats = (0 until matches.length()).map { index ->
            val type = matches.optJSONObject(index)?.optString("threatType").orEmpty()
            ThreatType.entries.firstOrNull { it.name == type } ?: ThreatType.OTHER
        }.distinct()
        return SafeBrowsingResult(SafeBrowsingStatus.THREAT_FOUND, threats)
    }

    private companion object {
        const val ENDPOINT = "https://safebrowsing.googleapis.com/v4/threatMatches:find"
        const val CLIENT_ID = "phoneguard-android"
        const val CLIENT_VERSION = "1.0.0"
        const val TIMEOUT_MS = 8_000
    }
}
