package com.example.data.sync

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.model.Client
import com.example.data.model.Order
import com.example.data.model.Visit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object GoogleSyncManager {
    private const val TAG = "GoogleSyncManager"
    private const val BACKUP_FILENAME = "visita_facil_backup.json"
    private val client = OkHttpClient()

    // Key to simulate cloud storage if the user is in demo mode / no real oauth token
    private const val PREFS_NAME = "visita_facil_sync_prefs"
    private const val KEY_SIMULATED_CLOUD_DATA = "simulated_cloud_backup_data"

    /**
     * Backs up database to Google Drive (real REST API or simulated backup if token is demo).
     */
    fun backupToDrive(
        context: Context,
        token: String,
        clients: List<Client>,
        visits: List<Visit>,
        orders: List<Order>,
        email: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val jsonContent = DatabaseJsonSerializer.serialize(clients, visits, orders, email, System.currentTimeMillis())

        // If it's a simulated token, we store in SharedPreferences under a separate key to emulate Drive storage
        if (token.startsWith("demo_") || token.isEmpty()) {
            // Simulate network latency of 1.5s
            Handler(Looper.getMainLooper()).postDelayed({
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_SIMULATED_CLOUD_DATA, jsonContent).apply()
                onComplete(true, "Dados salvos com sucesso no Google Drive simulado da conta $email!")
            }, 1500)
            return
        }

        // Real Google Drive REST API Backup
        findBackupFile(token) { fileId ->
            if (fileId != null) {
                // Update existing file
                uploadFileContent(token, fileId, jsonContent) { success ->
                    if (success) {
                        onComplete(true, "Backup atualizado no seu Google Drive com sucesso!")
                    } else {
                        onComplete(false, "Falha ao atualizar o arquivo de backup no Google Drive.")
                    }
                }
            } else {
                // Create new file metadata first
                createFileMetadata(token) { newFileId ->
                    if (newFileId != null) {
                        uploadFileContent(token, newFileId, jsonContent) { success ->
                            if (success) {
                                onComplete(true, "Novo backup criado com sucesso no seu Google Drive!")
                            } else {
                                onComplete(false, "Falha ao enviar conteúdo do novo backup para o Google Drive.")
                            }
                        }
                    } else {
                        onComplete(false, "Falha ao registrar metadados do arquivo no Google Drive.")
                    }
                }
            }
        }
    }

    /**
     * Restores database from Google Drive.
     */
    fun restoreFromDrive(
        context: Context,
        token: String,
        email: String,
        onComplete: (Boolean, String, String?) -> Unit
    ) {
        // If it's simulated, we restore from SharedPreferences
        if (token.startsWith("demo_") || token.isEmpty()) {
            Handler(Looper.getMainLooper()).postDelayed({
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val data = prefs.getString(KEY_SIMULATED_CLOUD_DATA, null)
                if (data != null) {
                    onComplete(true, "Dados restaurados com sucesso do Google Drive simulado!", data)
                } else {
                    onComplete(false, "Nenhum backup em nuvem foi encontrado para esta conta.", null)
                }
            }, 1500)
            return
        }

        // Real Google Drive REST API Restore
        findBackupFile(token) { fileId ->
            if (fileId != null) {
                downloadFileContent(token, fileId) { jsonContent ->
                    if (jsonContent != null) {
                        onComplete(true, "Backup sincronizado e restaurado do Google Drive!", jsonContent)
                    } else {
                        onComplete(false, "Falha ao baixar os dados do backup do Google Drive.", null)
                    }
                }
            } else {
                onComplete(false, "Nenhum arquivo de backup encontrado no seu Google Drive AppData.", null)
            }
        }
    }

    /**
     * Finds the backup file ID in the appDataFolder.
     */
    private fun findBackupFile(token: String, callback: (String?) -> Unit) {
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='$BACKUP_FILENAME'"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "findBackupFile failure", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        try {
                            val body = response.body?.string() ?: ""
                            val json = JSONObject(body)
                            val files = json.getJSONArray("files")
                            if (files.length() > 0) {
                                val file = files.getJSONObject(0)
                                callback(file.getString("id"))
                            } else {
                                callback(null)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "findBackupFile parse error", e)
                            callback(null)
                        }
                    } else {
                        Log.e(TAG, "findBackupFile unsuccessful: ${response.code} ${response.message}")
                        callback(null)
                    }
                }
            }
        })
    }

    /**
     * Creates empty file metadata in the appDataFolder.
     */
    private fun createFileMetadata(token: String, callback: (String?) -> Unit) {
        val url = "https://www.googleapis.com/drive/v3/files"
        val jsonPayload = JSONObject().apply {
            put("name", BACKUP_FILENAME)
            put("parents", listOf("appDataFolder").let { org.json.JSONArray(it) })
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "createFileMetadata failure", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        try {
                            val body = response.body?.string() ?: ""
                            val json = JSONObject(body)
                            callback(json.getString("id"))
                        } catch (e: Exception) {
                            Log.e(TAG, "createFileMetadata parse error", e)
                            callback(null)
                        }
                    } else {
                        Log.e(TAG, "createFileMetadata unsuccessful: ${response.code} ${response.message}")
                        callback(null)
                    }
                }
            }
        })
    }

    /**
     * Uploads the media content to Google Drive.
     */
    private fun uploadFileContent(token: String, fileId: String, content: String, callback: (Boolean) -> Unit) {
        val url = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .patch(content.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "uploadFileContent failure", e)
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        callback(true)
                    } else {
                        Log.e(TAG, "uploadFileContent unsuccessful: ${response.code} ${response.message}")
                        callback(false)
                    }
                }
            }
        })
    }

    /**
     * Downloads file content from Google Drive.
     */
    private fun downloadFileContent(token: String, fileId: String, callback: (String?) -> Unit) {
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "downloadFileContent failure", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        callback(response.body?.string())
                    } else {
                        Log.e(TAG, "downloadFileContent unsuccessful: ${response.code} ${response.message}")
                        callback(null)
                    }
                }
            }
        })
    }
}
