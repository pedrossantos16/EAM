package com.pedro.eam.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String
)

object UpdateManager {
    // Consulta direta à API oficial do GitHub para pegar a última release e o link do APK exato
    private const val GITHUB_API_URL = "https://api.github.com/repos/pedrossantos16/EAM/releases/latest"

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)

                    val tagName = json.getString("tag_name") // ex: "v1.0.12"
                    val releaseNotes = json.optString("body", "Nova atualização disponível!")

                    // Extrair código da versão a partir da tag
                    val remoteVersionCode = extractVersionCode(tagName)

                    // Procurar o link direto do APK nos assets da release
                    var apkUrl = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.getString("name")
                            if (name.endsWith(".apk")) {
                                apkUrl = asset.getString("browser_download_url")
                                break
                            }
                        }
                    }

                    // Fallback se não encontrar o asset direto
                    if (apkUrl.isBlank()) {
                        apkUrl = "https://github.com/pedrossantos16/EAM/releases/latest/download/app-debug.apk"
                    }

                    Log.d("UpdateManager", "GitHub Tag: $tagName, RemoteCode: $remoteVersionCode, Current: $currentVersionCode")

                    if (remoteVersionCode > currentVersionCode && apkUrl.isNotBlank()) {
                        return@withContext UpdateInfo(remoteVersionCode, tagName, apkUrl, releaseNotes)
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Erro ao consultar GitHub API: ${e.message}")
            }
            null
        }
    }

    private fun extractVersionCode(tagName: String): Int {
        return try {
            val digits = tagName.filter { it.isDigit() }
            if (digits.isNotEmpty()) digits.toInt() else 1
        } catch (e: Exception) {
            1
        }
    }

    suspend fun downloadAndInstall(context: Context, apkUrl: String, onProgress: (Float) -> Unit = {}): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(apkUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext false
                }

                val fileLength = connection.contentLength
                val outputFile = File(context.cacheDir, "update.apk")
                if (outputFile.exists()) outputFile.delete()

                connection.inputStream.use { input ->
                    outputFile.outputStream().use { output ->
                        val data = ByteArray(4096)
                        var total: Long = 0
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count.toLong()
                            if (fileLength > 0) {
                                onProgress(total.toFloat() / fileLength.toFloat())
                            }
                            output.write(data, 0, count)
                        }
                        output.flush()
                    }
                }

                if (outputFile.length() < 500 * 1024) {
                    Log.e("UpdateManager", "Arquivo baixado é inválido.")
                    return@withContext false
                }

                withContext(Dispatchers.Main) {
                    installApk(context, outputFile)
                }
                true
            } catch (e: Exception) {
                Log.e("UpdateManager", "Erro ao baixar APK: ${e.message}")
                false
            }
        }
    }

    private fun installApk(context: Context, file: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file),
                    "application/vnd.android.package-archive"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Erro ao iniciar instalação: ${e.message}")
        }
    }
}
