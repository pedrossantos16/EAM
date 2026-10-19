package com.pedro.eam.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String
)

object UpdateManager {
    // URL atualizada para o branch 'master' e nome exato do repositório 'EAM'
    private const val VERSION_JSON_URL = "https://raw.githubusercontent.com/pedrossantos16/EAM/master/version.json"

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(VERSION_JSON_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                Log.d("UpdateManager", "Response Code: ${connection.responseCode}")

                if (connection.responseCode == 200) {
                    val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)
                    val remoteVersionCode = json.getInt("versionCode")
                    val versionName = json.getString("versionName")
                    val apkUrl = json.getString("apkUrl")
                    val releaseNotes = json.optString("releaseNotes", "Nova atualização disponível!")

                    Log.d("UpdateManager", "Remote Version: $remoteVersionCode, Current Version: $currentVersionCode")

                    if (remoteVersionCode > currentVersionCode) {
                        return@withContext UpdateInfo(remoteVersionCode, versionName, apkUrl, releaseNotes)
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Erro ao verificar atualização: ${e.message}")
            }
            null
        }
    }

    fun openDownloadUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Erro ao abrir link de download: ${e.message}")
        }
    }
}
