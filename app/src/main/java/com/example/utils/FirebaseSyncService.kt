package com.example.utils

import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object FirebaseSyncService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    // Setup Moshi with Reflection
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Helper to get Firebase REST json URL
    private fun getJsonUrl(url: String, prefix: String, table: String): String {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        cleanUrl = cleanUrl.removeSuffix("/")
        val cleanPrefix = prefix.trim().ifEmpty { "shreeshyam_sync" }
        return "$cleanUrl/$cleanPrefix/$table.json"
    }

    suspend fun testFirebaseConnection(url: String): Boolean = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext false
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        cleanUrl = cleanUrl.removeSuffix("/")
        val checkUrl = "$cleanUrl/.json?shallow=true"
        val request = Request.Builder().url(checkUrl).get().build()
        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun <T> uploadTable(url: String, prefix: String, tableName: String, list: List<T>, clazz: Class<T>): Boolean = withContext(Dispatchers.IO) {
        val targetUrl = getJsonUrl(url, prefix, tableName)
        val type = Types.newParameterizedType(List::class.java, clazz)
        val adapter = moshi.adapter<List<T>>(type)
        // Set serializeNulls() to handle any null database cells cleanly in JSON
        val json = adapter.serializeNulls().toJson(list)
        
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(targetUrl).put(body).build()
        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun <T> downloadTable(url: String, prefix: String, tableName: String, clazz: Class<T>): List<T> = withContext(Dispatchers.IO) {
        val targetUrl = getJsonUrl(url, prefix, tableName)
        val request = Request.Builder().url(targetUrl).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isBlank() || bodyStr == "null") {
                        emptyList()
                    } else {
                        val type = Types.newParameterizedType(List::class.java, clazz)
                        val adapter = moshi.adapter<List<T>>(type)
                        // Ignore potential null values that might cause crashes during parsing
                        adapter.fromJson(bodyStr) ?: emptyList()
                    }
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
