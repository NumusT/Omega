package com.omega.ordencompra.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String = ""
)

class UpdateChecker(private val context: Context) {

    companion object {
        private const val UPDATE_URL =
            "https://github.com/NumusT/Omega/releases/latest/download/version.json"
    }

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(UPDATE_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val json = conn.inputStream.bufferedReader().readText()
            val obj = JSONObject(json)
            val remoteCode = obj.getInt("versionCode")
            val currentCode = getCurrentVersionCode()
            if (remoteCode > currentCode) {
                UpdateInfo(
                    versionCode = remoteCode,
                    versionName = obj.getString("versionName"),
                    apkUrl = obj.getString("apkUrl"),
                    changelog = obj.optString("changelog", "")
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                pInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }

    fun openDownloadInBrowser(apkUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
