package com.paisa.najarine.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.paisa.najarine.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest
import java.util.Date
import java.util.Locale

object CertificateDiagnostics {

    const val REGISTERED_FIREBASE_SHA1 = "BF:4E:06:ED:31:E0:52:EA:92:55:3B:56:84:42:D7:75:CF:9F:1F:3A"
    const val FIREBASE_PROJECT_ID = "paisa-finance-bd"
    const val FIREBASE_ANDROID_APP_ID = "1:366887012350:android:c5bf15e6613031773962af"

    fun getWebClientId(context: Context): String {
        return try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            "Configured in google-services.json"
        }
    }

    var lastAuthExceptionClass: String? = null
    var lastAuthExceptionMessage: String? = null
    var lastAuthErrorCode: String? = null
    var lastAuthTimestamp: Long = 0L

    var lastAuthSuccessUid: String? = null
    var lastAuthSuccessEmail: String? = null
    var lastAuthSuccessTimestamp: Long = 0L

    fun recordAuthException(exceptionClass: String, message: String, errorCode: String) {
        lastAuthExceptionClass = exceptionClass
        lastAuthExceptionMessage = message
        lastAuthErrorCode = errorCode
        lastAuthTimestamp = System.currentTimeMillis()
    }

    fun recordAuthSuccess(uid: String, email: String?) {
        lastAuthSuccessUid = uid
        lastAuthSuccessEmail = email
        lastAuthSuccessTimestamp = System.currentTimeMillis()
        lastAuthExceptionClass = null
        lastAuthExceptionMessage = null
        lastAuthErrorCode = null
    }

    data class SigningCertInfo(
        val packageName: String,
        val sha1: String,
        val sha256: String,
        val buildType: String,
        val versionName: String,
        val versionCode: Long,
        val registeredSha1: String = REGISTERED_FIREBASE_SHA1,
        val isSha1Matching: Boolean = false
    )

    fun getInstalledSigningCertificates(context: Context): SigningCertInfo {
        val packageName = context.packageName
        var sha1 = "Unable to compute"
        var sha256 = "Unable to compute"
        var versionName = "1.0"
        var versionCode = 1L

        try {
            val pm = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            versionName = packageInfo.versionName ?: "1.0"
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (!signatures.isNullOrEmpty()) {
                val cert = signatures[0].toByteArray()
                sha1 = computeFingerprint(cert, "SHA-1")
                sha256 = computeFingerprint(cert, "SHA-256")
            }
        } catch (e: Exception) {
            sha1 = "Error: ${e.localizedMessage}"
            sha256 = "Error: ${e.localizedMessage}"
        }

        val cleanSha1 = sha1.replace(":", "").uppercase(Locale.US)
        val cleanExpected = REGISTERED_FIREBASE_SHA1.replace(":", "").uppercase(Locale.US)
        val isMatch = cleanSha1 == cleanExpected

        return SigningCertInfo(
            packageName = packageName,
            sha1 = sha1,
            sha256 = sha256,
            buildType = if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) "Debug" else "Release",
            versionName = versionName,
            versionCode = versionCode,
            registeredSha1 = REGISTERED_FIREBASE_SHA1,
            isSha1Matching = isMatch
        )
    }

    private fun computeFingerprint(cert: ByteArray, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(cert)
        return digest.joinToString(":") { String.format(Locale.US, "%02X", it) }
    }

    fun getFullDiagnosticDump(context: Context): String {
        val info = getInstalledSigningCertificates(context)
        val isFirebaseInit = try { FirebaseApp.getApps(context).isNotEmpty() } catch (_: Exception) { false }
        val authUser = try { FirebaseAuth.getInstance().currentUser?.uid ?: "None (Signed Out)" } catch (_: Exception) { "Error reading auth" }
        val webClientId = getWebClientId(context)

        return buildString {
            appendLine("=== PAISA DIAGNOSTIC DUMP ===")
            appendLine("Package: ${info.packageName}")
            appendLine("Version: ${info.versionName} (${info.versionCode}) - ${info.buildType}")
            appendLine("Firebase Project: $FIREBASE_PROJECT_ID")
            appendLine("Firebase App ID: $FIREBASE_ANDROID_APP_ID")
            appendLine("Firebase Initialized: $isFirebaseInit")
            appendLine("Current Auth UID: $authUser")
            appendLine("Web Client ID: $webClientId")
            appendLine("Installed Build SHA-1: ${info.sha1}")
            appendLine("Registered Firebase SHA-1: ${info.registeredSha1}")
            appendLine("SHA-1 Fingerprint Match: ${if (info.isSha1Matching) "YES (MATCH)" else "NO (MISMATCH - ADD TO CONSOLE)"}")
            appendLine("Installed Build SHA-256: ${info.sha256}")
            if (lastAuthExceptionClass != null) {
                appendLine("Last Auth Exception Class: $lastAuthExceptionClass")
                appendLine("Last Auth Exception Message: $lastAuthExceptionMessage")
                appendLine("Last Auth Error Code: $lastAuthErrorCode")
                appendLine("Last Auth Event Time: ${Date(lastAuthTimestamp)}")
            } else if (lastAuthSuccessUid != null) {
                appendLine("Last Auth Success UID: $lastAuthSuccessUid ($lastAuthSuccessEmail) at ${Date(lastAuthSuccessTimestamp)}")
            }
            appendLine("=============================")
        }
    }
}
