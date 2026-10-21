package com.pedro.eam.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
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
    private const val VERSION_JSON_URL = "https://raw.githubusercontent.com/pedrossantos16/EAM/master/version.json"

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(VERSION_JSON_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)
                    val remoteVersionCode = json.getInt("versionCode")
                    val versionName = json.getString("versionName")
                    val apkUrl = json.getString("apkUrl")
                    val releaseNotes = json.optString("releaseNotes", "Nova atualização disponível!")

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

                // Validar se o arquivo baixado é um APK válido (maior que 500KB)
                if (outputFile.length() < 500 * 1024) {
                    Log.e("UpdateManager", "O arquivo baixado é inválido (provavelmente HTML de erro).")
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
