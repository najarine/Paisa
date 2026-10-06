package com.paisa.najarine.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.ui.theme.*
import com.paisa.najarine.util.CertificateDiagnostics

class AuthDebugActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this
        setContent {
            MyApplicationTheme {
                val certInfo = remember { CertificateDiagnostics.getInstalledSigningCertificates(context) }
                val deviceModel = Build.MODEL
                val deviceManufacturer = Build.MANUFACTURER
                val androidVersion = Build.VERSION.RELEASE

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Auth & SHA-256 Debugger", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = PaisaSurface)
                        )
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PaisaBackground)
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Device & App Info Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Device & Build Environment", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    DebugRow("Device Model", "$deviceManufacturer $deviceModel")
                                    DebugRow("Android Version", "Android $androidVersion (SDK ${Build.VERSION.SDK_INT})")
                                    DebugRow("Package Name", certInfo.packageName)
                                    DebugRow("Build Type", certInfo.buildType)
                                    DebugRow("Version Name", "${certInfo.versionName} (${certInfo.versionCode})")
                                }
                            }
                        }

                        // SHA-256 Fingerprint Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = PaisaTealPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Installed APK SHA-256 Fingerprint", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = certInfo.sha256,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = PaisaTextPrimary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(PaisaSurfaceVariant, RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("SHA-256", certInfo.sha256))
                                            Toast.makeText(context, "SHA-256 copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy SHA-256 to Clipboard", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // SHA-1 & Firebase Console Comparison Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (certInfo.isSha1Matching) PaisaIncomeLight else PaisaExpenseLight),
                                border = if (MaterialTheme.colorScheme.outline == Color.Transparent) null else BorderStroke(1.dp, if (certInfo.isSha1Matching) PaisaIncomeGreen.copy(alpha = 0.5f) else PaisaExpenseRed.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (certInfo.isSha1Matching) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (certInfo.isSha1Matching) PaisaIncomeGreen else PaisaExpenseRed
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (certInfo.isSha1Matching) "SHA-1 Match: SUCCESS" else "SHA-1 Mismatch: DEVELOPER ERROR 10",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (certInfo.isSha1Matching) PaisaIncomeGreen else PaisaExpenseRed
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Installed Build SHA-1:", style = MaterialTheme.typography.labelSmall, color = PaisaTextSecondary)
                                    Text(
                                        text = certInfo.sha1,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = PaisaTextPrimary,
                                        modifier = Modifier.fillMaxWidth().background(PaisaSurface, RoundedCornerShape(6.dp)).padding(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Registered Firebase Console SHA-1 (google-services.json):", style = MaterialTheme.typography.labelSmall, color = PaisaTextSecondary)
                                    Text(
                                        text = certInfo.registeredSha1,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = PaisaTextPrimary,
                                        modifier = Modifier.fillMaxWidth().background(PaisaSurface, RoundedCornerShape(6.dp)).padding(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (certInfo.isSha1Matching)
                                            "Your installed APK signing certificate matches Firebase Console. Google Sign-In should succeed."
                                        else
                                            "MISMATCH DETECTED: The installed build on your iQOO Z9 Turbo is signed with a different keystore than the SHA-1 registered in Firebase Console. Copy your installed SHA-1 above and add it to Firebase Console -> Project Settings -> Android App.",
                                        fontSize = 12.sp,
                                        color = PaisaTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DebugRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = PaisaTextSecondary)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PaisaTextPrimary)
        }
    }
}
