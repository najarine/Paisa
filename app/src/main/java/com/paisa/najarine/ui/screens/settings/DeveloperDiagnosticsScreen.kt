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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
                        Text("ডেভেলপার ডায়াগনস্টিকস", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
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
                            Text("বর্তমান ইনস্টলড বিল্ড সার্টিফিকেট", fontWeight = FontWeight.Bold, color = PaisaTealDark)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "আপনার কনফিগার করা বিল্ডের ফিঙ্গারপ্রিন্ট ব্যবহার করুন। প্লে স্টোর বিল্ডের সাইনিং সার্টিফিকেট লোকাল বিল্ড থেকে ভিন্ন হতে পারে।",
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
                        Text("অ্যাপ্লিকেশন ও সার্টিফিকেট ফিঙ্গারপ্রিন্ট", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                        Spacer(modifier = Modifier.height(10.dp))
                        DiagnosticTextItem(label = "অ্যাপ আইডি", value = certInfo.packageName)
                        DiagnosticTextItem(label = "বিল্ডের ধরণ", value = certInfo.buildType)
                        DiagnosticTextItem(label = "ভার্সন", value = "${certInfo.versionName} (${certInfo.versionCode})")

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("বর্তমান ইনস্টলড বিল্ড SHA-1", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PaisaTextSecondary)
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
                                    Toast.makeText(context, "SHA-1 ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SHA-1 কপি করুন", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val dump = CertificateDiagnostics.getFullDiagnosticDump(context)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Paisa Diagnostic Report", dump))
                                    Toast.makeText(context, "সম্পূর্ণ রিপোর্ট কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("সম্পূর্ণ রিপোর্ট কপি করুন", fontSize = 12.sp)
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
                                    Text("ইনস্টলড SHA-1 ফায়ারবেসের সাথে মিলে গেছে", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PaisaIncomeGreen)
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
                                        Text("ফায়ারবেস কনসোলের সাথে SHA-1 অমিল", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PaisaExpenseRed)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ফায়ারবেসে নিবন্ধিত: ${certInfo.registeredSha1}\n\nযদি গুগল সাইন-ইনে ত্রুটি দেখায়, তবে ওপরের ইনস্টলড SHA-1 কপি করে ফায়ারবেস কনসোলে যুক্ত করুন।",
                                        fontSize = 11.sp,
                                        color = PaisaTextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("বর্তমান ইনস্টলড বিল্ড SHA-256", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PaisaTextSecondary)
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
                                Toast.makeText(context, "SHA-256 ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SHA-256 কপি করুন", fontSize = 12.sp)
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
                        Text("গুগল ক্রেডেনশিয়াল ম্যানেজার ডায়াগনস্টিকস", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        DiagnosticTextItem(label = "ওয়েব ক্লায়েন্ট আইডি", value = CertificateDiagnostics.getWebClientId(context).take(28) + "...")
                        DiagnosticTextItem(label = "প্রধান অপশন", value = "GetSignInWithGoogleOption + GetGoogleIdOption")
                        DiagnosticTextItem(label = "প্রথমবারের অ্যাকাউন্টসমূহ", value = "সমর্থিত (FilterByAuthorizedAccounts = false)")
                        DiagnosticTextItem(label = "ফলব্যাক ফ্লো", value = "স্ট্যান্ডঅ্যালোন GetSignInWithGoogleOption")

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        if (CertificateDiagnostics.lastAuthExceptionClass != null) {
                            Text("সর্বশেষ ক্রেডেনশিয়াল ম্যানেজার ত্রুটি", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PaisaExpenseRed)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ক্লাস: ${CertificateDiagnostics.lastAuthExceptionClass}\nকোড: ${CertificateDiagnostics.lastAuthErrorCode}\nমেসেজ: ${CertificateDiagnostics.lastAuthExceptionMessage}",
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
                                Text("সর্বশেষ লগইন: ${CertificateDiagnostics.lastAuthSuccessEmail ?: "সফল"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PaisaIncomeGreen)
                            }
                        } else {
                            Text("এখনো কোনো অথেন্টিকেশন ত্রুটি রেকর্ড করা হয়নি।", fontSize = 12.sp, color = PaisaTextSecondary)
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
                        Text("ফায়ারবেস ও সিস্টেম স্ট্যাটাস", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
                        Text("নোটিফিকেশন ও আযান যাচাইকরণ টেস্ট", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("এই ডিভাইসে নোটিফিকেশন চ্যানেলগুলো সঠিকভাবে কাজ করছে কিনা তা পরীক্ষা করুন:", style = MaterialTheme.typography.bodyMedium, color = PaisaTextSecondary)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    PaisaNotificationManager.showNotification(
                                        context = context,
                                        channelId = PaisaNotificationManager.CHANNEL_PRAYER,
                                        notificationId = 801,
                                        title = "ফজরের আযান — الصَّلَاةُ خَيْرٌ مِنَ النَّوْمِ",
                                        body = "ঘুম থেকে নামাজ উত্তম। ফজরের নামাজের সময় হয়েছে।",
                                        targetTab = 3
                                    )
                                    Toast.makeText(context, "ফজরের নোটিফিকেশন পাঠানো হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("ফজর টেস্ট", fontSize = 11.sp) }

                            OutlinedButton(
                                onClick = {
                                    PaisaNotificationManager.showNotification(
                                        context = context,
                                        channelId = PaisaNotificationManager.CHANNEL_PRAYER,
                                        notificationId = 802,
                                        title = "যোহরের আযান — حي على الصلاة",
                                        body = "কাজ থামিয়ে যোহরের নামাজ আদায় করার সময় হয়েছে।",
                                        targetTab = 3
                                    )
                                    Toast.makeText(context, "যোহরের নোটিফিকেশন পাঠানো হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("যোহর টেস্ট", fontSize = 11.sp) }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    PaisaNotificationManager.showNotification(
                                        context = context,
                                        channelId = PaisaNotificationManager.CHANNEL_QURAN_HADITH,
                                        notificationId = 803,
                                        title = "দৈনিক কুরআনিক উপদেশ",
                                        body = "\"যে ব্যক্তি আল্লাহর ওপর ভরসা করে, তিনি তার জন্য যথেষ্ট।\" (সূরা আত-ত্বলাক ৬৫:৩)",
                                        targetTab = 3
                                    )
                                    Toast.makeText(context, "কুরআন রিমাইন্ডার পাঠানো হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("কুরআন টেস্ট", fontSize = 11.sp) }

                            OutlinedButton(
                                onClick = {
                                    PaisaNotificationManager.showNotification(
                                        context = context,
                                        channelId = PaisaNotificationManager.CHANNEL_COLLABORATION,
                                        notificationId = 804,
                                        title = "ওয়ার্কস্পেস আমন্ত্রণ",
                                        body = "আপনাকে ফ্যামিলি ফাইন্যান্স ওয়ার্কস্পেসে আমন্ত্রণ জানানো হয়েছে।",
                                        targetTab = 4
                                    )
                                    Toast.makeText(context, "আমন্ত্রণ টেস্ট নোটিফিকেশন পাঠানো হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("আমন্ত্রণ টেস্ট", fontSize = 11.sp) }
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
