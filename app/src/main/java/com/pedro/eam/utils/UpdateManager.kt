package com.pedro.eam.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val apkSize: Long,
    val minimumSupportedVersionCode: Int,
    val releaseNotes: String
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    object PermissionRequired : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    object Verifying : UpdateState()
    object ReadyToInstall : UpdateState()
    object Installing : UpdateState()
    object Installed : UpdateState()
    object Cancelled : UpdateState()
    data class Failed(val errorCode: String, val message: String) : UpdateState()
}

object UpdateManager {
    private const val VERSION_JSON_URL = "https://raw.githubusercontent.com/pedrossantos16/EAM/master/version.json"

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    private var isDownloading = false

    suspend fun checkForUpdate(context: Context) {
        _state.value = UpdateState.Checking
        withContext(Dispatchers.IO) {
            try {
                val currentVersionCode = getAppVersionCode(context)
                val url = URL(VERSION_JSON_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode == 200) {
                    val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)

                    val versionCode = json.getInt("versionCode")
                    val versionName = json.getString("versionName")
                    val apkUrl = json.getString("apkUrl")
                    val sha256 = json.optString("sha256", "")
                    val apkSize = json.optLong("apkSize", 0L)
                    val minSupported = json.optInt("minimumSupportedVersionCode", 1)
                    val releaseNotes = json.optString("releaseNotes", "Nova atualização disponível!")

                    if (currentVersionCode < minSupported) {
                        _state.value = UpdateState.Failed(
                            "MIN_VERSION_REQUIRED",
                            "Sua versão atual está descontinuada. É necessário reinstalar o aplicativo."
                        )
                        return@withContext
                    }

                    if (versionCode > currentVersionCode && apkUrl.isNotBlank()) {
                        _state.value = UpdateState.UpdateAvailable(
                            UpdateInfo(versionCode, versionName, apkUrl, sha256, apkSize, minSupported, releaseNotes)
                        )
                    } else {
                        _state.value = UpdateState.Idle
                    }
                } else {
                    _state.value = UpdateState.Failed("HTTP_ERROR", "Erro ao verificar atualizações (Código ${connection.responseCode})")
                }
            } catch (e: Exception) {
                _state.value = UpdateState.Failed("NETWORK_ERROR", "Falha de conexão: ${e.localizedMessage}")
            }
        }
    }

    suspend fun startUpdate(context: Context, info: UpdateInfo) {
        if (isDownloading) return
        isDownloading = true

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    _state.value = UpdateState.PermissionRequired
                    isDownloading = false
                    return
                }
            }

            _state.value = UpdateState.Downloading(0f)

            val partFile = File(context.cacheDir, "update.apk.part")
            val targetFile = File(context.cacheDir, "update.apk")
            if (partFile.exists()) partFile.delete()
            if (targetFile.exists()) targetFile.delete()

            val success = withContext(Dispatchers.IO) {
                try {
                    val url = URL(info.apkUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connect()

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        return@withContext false
                    }

                    val fileLength = connection.contentLength
                    connection.inputStream.use { input ->
                        partFile.outputStream().use { output ->
                            val data = ByteArray(8192)
                            var total: Long = 0
                            var count: Int
                            while (input.read(data).also { count = it } != -1) {
                                total += count.toLong()
                                if (fileLength > 0) {
                                    val progress = total.toFloat() / fileLength.toFloat()
                                    _state.value = UpdateState.Downloading(progress)
                                }
                                output.write(data, 0, count)
                            }
                            output.flush()
                        }
                    }
                    true
                } catch (e: Exception) {
                    Log.e("UpdateManager", "Download failed: ${e.message}")
                    false
                }
            }

            if (!success || !partFile.exists()) {
                _state.value = UpdateState.Failed("DOWNLOAD_FAILED", "Não foi possível baixar o arquivo de atualização.")
                isDownloading = false
                return
            }

            if (!partFile.renameTo(targetFile)) {
                _state.value = UpdateState.Failed("FILE_ERROR", "Erro ao processar o arquivo baixado.")
                isDownloading = false
                return
            }

            _state.value = UpdateState.Verifying
            val validationError = validateApk(context, targetFile, info)
            if (validationError != null) {
                _state.value = validationError
                targetFile.delete()
                isDownloading = false
                return
            }

            _state.value = UpdateState.ReadyToInstall
            installApk(context, targetFile)

        } catch (e: Exception) {
            _state.value = UpdateState.Failed("UNKNOWN_ERROR", "Erro inesperado: ${e.localizedMessage}")
        } finally {
            isDownloading = false
        }
    }

    private fun validateApk(context: Context, file: File, info: UpdateInfo): UpdateState.Failed? {
        if (info.apkSize > 0 && Math.abs(file.length() - info.apkSize) > 1024 * 1024) {
            return UpdateState.Failed("INVALID_SIZE", "O tamanho do APK baixado difere do esperado.")
        }

        if (info.sha256.isNotBlank()) {
            val fileHash = calculateSha256(file)
            if (!fileHash.equals(info.sha256, ignoreCase = true)) {
                return UpdateState.Failed("INVALID_HASH", "A validação de integridade (SHA-256) falhou.")
            }
        }

        val pm = context.packageManager
        val archiveInfo = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_SIGNATURES)
            ?: return UpdateState.Failed("INVALID_APK", "O arquivo baixado não é um pacote APK válido.")

        val pkgName = archiveInfo.packageName
        if (pkgName != context.packageName) {
            return UpdateState.Failed("INVALID_PACKAGE", "O pacote do APK ($pkgName) não confere com o aplicativo.")
        }

        val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode
        }

        if (apkVersionCode < info.versionCode) {
            return UpdateState.Failed("OUTDATED_APK", "A versão do APK baixado é inferior à esperada.")
        }

        if (!isSignatureCompatible(context, file)) {
            return UpdateState.Failed(
                "SIGNATURE_MISMATCH",
                "Este APK foi assinado com uma chave diferente da versão instalada. O Android não permite atualizar diretamente. Faça backup dos dados antes de desinstalar e instalar a versão assinada com a chave definitiva."
            )
        }

        return null
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read != -1) {
                if (read > 0) digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun isSignatureCompatible(context: Context, apkFile: File): Boolean {
        return try {
            val pm = context.packageManager
            val installedSignatures = getAppSignatures(pm, context.packageName)
            val apkSignatures = getApkSignatures(pm, apkFile.absolutePath)
            
            if (installedSignatures.isEmpty() || apkSignatures.isEmpty()) return true
            installedSignatures == apkSignatures
        } catch (e: Exception) {
            true
        }
    }

    private fun getAppSignatures(pm: PackageManager, packageName: String): Set<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signatures = pkgInfo.signingInfo?.apkContentsSigners ?: return emptySet()
            signatures.map { it.toCharsString() }.toSet()
        } else {
            @Suppress("DEPRECATION")
            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            val signatures = pkgInfo.signatures ?: return emptySet()
            signatures.map { it.toCharsString() }.toSet()
        }
    }

    private fun getApkSignatures(pm: PackageManager, apkPath: String): Set<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNING_CERTIFICATES)
            val signatures = pkgInfo?.signingInfo?.apkContentsSigners ?: return emptySet()
            signatures.map { it.toCharsString() }.toSet()
        } else {
            @Suppress("DEPRECATION")
            val pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNATURES)
            val signatures = pkgInfo?.signatures ?: return emptySet()
            signatures.map { it.toCharsString() }.toSet()
        }
    }

    private fun getAppVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (_: Exception) {
            1
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
            _state.value = UpdateState.Failed("INSTALL_ERROR", "Erro ao iniciar instalador: ${e.localizedMessage}")
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("UpdateManager", "Failed to open settings: ${e.message}")
            }
        }
    }

    fun resetState() {
        _state.value = UpdateState.Idle
    }
}
