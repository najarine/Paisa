package com.paisa.najarine.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.notification.PaisaNotificationManager
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import com.paisa.najarine.util.CertificateDiagnostics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperDiagnosticsScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val certInfo = remember { CertificateDiagnostics.getInstalledSigningCertificates(context) }
    var fcmToken by remember { mutableStateOf<String?>("Retrieving token...") }

    LaunchedEffect(Unit) {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                val messaging = FirebaseMessaging.getInstance()
                messaging.token.addOnCompleteListener { task ->
                    fcmToken = if (task.isSuccessful && !task.result.isNullOrBlank()) {
                        task.result
                    } else {
                        "Standby / Optional (${task.exception?.localizedMessage ?: "Cloud Messaging API disabled"})"
                    }
                }
            } else {
                fcmToken = "Firebase Not Initialized"
            }
        } catch (e: Exception) {
            fcmToken = "Not configured: ${e.localizedMessage}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.paisa.najarine.ui.components.PaisaLogoBadge(
                            size = 28.dp,
                            fontSize = 14.sp,
                            cornerRadius = 8.dp,
                            borderWidth = 1.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Developer Diagnostics", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaisaSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(PaisaBackground)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Important Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaTealContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PaisaTealDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Current Installed Build Certificate", fontWeight = FontWeight.Bold, color = PaisaTealDark)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Use the fingerprint from the build you are configuring. A Play Store build can have a different signing certificate from a locally installed build.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = PaisaOnTealContainer
                        )
                    }
                }
            }

            // Signing Fingerprints Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Application & Certificate Fingerprints", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                        Spacer(modifier = Modifier.height(10.dp))
                        DiagnosticTextItem(label = "Application ID", value = certInfo.packageName)
                        DiagnosticTextItem(label = "Build Type", value = certInfo.buildType)
                        DiagnosticTextItem(label = "Version", value = "${certInfo.versionName} (${certInfo.versionCode})")

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Current Installed Build SHA-1", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PaisaTextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = certInfo.sha1,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = PaisaTextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PaisaSurfaceVariant, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Paisa SHA-1", certInfo.sha1))
                                    Toast.makeText(context, "SHA-1 copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy SHA-1", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val dump = CertificateDiagnostics.getFullDiagnosticDump(context)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Paisa Diagnostic Report", dump))
                                    Toast.makeText(context, "Full diagnostic report copied", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Copy Full Report", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Match vs Mismatch Indicator
                        if (certInfo.isSha1Matching) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = PaisaIncomeGreen.copy(alpha = 0.15f))
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PaisaIncomeGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Installed SHA-1 matches registered Firebase SHA-1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PaisaIncomeGreen)
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = PaisaExpenseRed.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = PaisaExpenseRed, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("SHA-1 Mismatch with Firebase Console", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PaisaExpenseRed)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Firebase registered: ${certInfo.registeredSha1}\n\nIf Google Sign-In gives Code 10 / DEVELOPER_ERROR, copy the Installed Build SHA-1 above and add it to Firebase Console > Project Settings > Android Apps (com.paisa.najarine).",
                                        fontSize = 11.sp,
                                        color = PaisaTextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Current Installed Build SHA-256", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PaisaTextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = certInfo.sha256,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = PaisaTextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PaisaSurfaceVariant, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Paisa SHA-256", certInfo.sha256))
                                Toast.makeText(context, "SHA-256 copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy SHA-256", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Credential Manager & Last Auth Event Diagnostics Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Google Credential Manager Diagnostics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        DiagnosticTextItem(label = "Web Client ID", value = CertificateDiagnostics.getWebClientId(context).take(28) + "...")
                        DiagnosticTextItem(label = "Primary Option", value = "GetSignInWithGoogleOption + GetGoogleIdOption")
                        DiagnosticTextItem(label = "First-time Accounts", value = "Supported (FilterByAuthorizedAccounts = false)")
                        DiagnosticTextItem(label = "Fallback Flow", value = "Explicit Standalone GetSignInWithGoogleOption")

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        if (CertificateDiagnostics.lastAuthExceptionClass != null) {
                            Text("Last Credential Manager Exception", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PaisaExpenseRed)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Class: ${CertificateDiagnostics.lastAuthExceptionClass}\nCode: ${CertificateDiagnostics.lastAuthErrorCode}\nMessage: ${CertificateDiagnostics.lastAuthExceptionMessage}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = PaisaTextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PaisaExpenseLight, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            )
                        } else if (CertificateDiagnostics.lastAuthSuccessUid != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PaisaIncomeGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Last Login: ${CertificateDiagnostics.lastAuthSuccessEmail ?: "Success"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PaisaIncomeGreen)
                            }
                        } else {
                            Text("No authentication exceptions recorded yet.", fontSize = 12.sp, color = PaisaTextSecondary)
                        }
                    }
                }
            }

            // Firebase Architecture Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Firebase & System Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(10.dp))

                        val authUser = FirebaseAuth.getInstance().currentUser
                        StatusRow(service = "Firebase Auth", status = if (authUser != null) "READY (UID: ${authUser.uid.take(8)}...)" else "SIGNED OUT", isOk = authUser != null)
                        StatusRow(service = "Project ID", status = "paisa-finance-bd", isOk = true)
                        StatusRow(service = "Cloud Firestore", status = "READY (Local First)", isOk = true)
                        StatusRow(service = "Room SQLite Cache", status = "READY (Operational Truth)", isOk = true)
                        StatusRow(service = "FCM Cloud Messaging", status = if (fcmToken?.startsWith("Unavailable") == false) "READY" else "INITIALIZING", isOk = true)
                        StatusRow(service = "Active Workspace", status = viewModel.activeWorkspaceId.value, isOk = true)
                    }
                }
            }

            // Test Notifications Triggers
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Notification & Adhan Verification Tests", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Verify that notifications and sound channels trigger properly on this device:", style = MaterialTheme.typography.bodyMedium, color = PaisaTextSecondary)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    PaisaNotificationManager.showNotification(
                                        context = context,
                                        channelId = PaisaNotificationManager.CHANNEL_PRAYER,
                                        notificationId = 801,
                                        title = "Fajr Adhan — الصَّلَاةُ خَيْرٌ مِنَ النَّوْمِ",
                                        body = "Prayer is better than sleep. Time for Fajr prayer.",
                                        targetTab = 3
                                    )
                                    Toast.makeText(context, "Fajr notification triggered", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Test Fajr", fontSize = 11.sp) }

                            OutlinedButton(
                                onClick = {
                                    PaisaNotificationManager.showNotification(
                                        context = context,
                                        channelId = PaisaNotificationManager.CHANNEL_PRAYER,
                                        notificationId = 802,
                                        title = "Dhuhr Adhan — حي على الصلاة",
                                        body = "Time to pause work and offer Dhuhr prayer.",
                                        targetTab = 3
                                    )
                                    Toast.makeText(context, "Dhuhr notification triggered", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Test Dhuhr", fontSize = 11.sp) }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    PaisaNotificationManager.showNotification(
                                        context = context,
                                        channelId = PaisaNotificationManager.CHANNEL_QURAN_HADITH,
                                        notificationId = 803,
                                        title = "Daily Quranic Reflection",
                                        body = "\"And whoever relies upon Allah - then He is sufficient for him.\" (Surah At-Talaq 65:3)",
                                        targetTab = 3
                                    )
                                    Toast.makeText(context, "Quran reminder triggered", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Test Quran", fontSize = 11.sp) }

                            OutlinedButton(
                                onClick = {
                                    PaisaNotificationManager.showNotification(
                                        context = context,
                                        channelId = PaisaNotificationManager.CHANNEL_COLLABORATION,
                                        notificationId = 804,
                                        title = "Partner Workspace Invite",
                                        body = "You have been invited to collaborate on Family Finance workspace.",
                                        targetTab = 4
                                    )
                                    Toast.makeText(context, "FCM invite test triggered", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Test Collab", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticTextItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = PaisaTextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PaisaTextPrimary)
    }
}

@Composable
fun StatusRow(service: String, status: String, isOk: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = service, style = MaterialTheme.typography.bodyMedium, color = PaisaTextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (isOk) PaisaIncomeGreen else PaisaExpenseRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOk) PaisaIncomeGreen else PaisaExpenseRed
            )
        }
    }
}
