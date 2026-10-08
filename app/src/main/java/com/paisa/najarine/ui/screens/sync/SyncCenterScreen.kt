package com.paisa.najarine.ui.screens.sync

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.sync.BackupResult
import com.paisa.najarine.sync.SyncState
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncCenterScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val syncStatus by viewModel.syncManager.syncStatus.collectAsState()
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

    val latestBackup by viewModel.latestBackupMetadata.collectAsState()
    val isOperating by viewModel.isBackupOperating.collectAsState()

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshBackupMetadata()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ক্লাউড সিঙ্ক ও ব্যাকআপ", fontWeight = FontWeight.Bold) },
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
            // Status Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaTealContainer),
                    border = if (MaterialTheme.colorScheme.outline == Color.Transparent) null else BorderStroke(1.dp, PaisaTealPrimary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("স্বয়ংক্রিয় ক্লাউড সিঙ্ক", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PaisaTealDark)
                                Text("আপনার ব্যক্তিগত ডিভাইসের সাথে ক্লাউডে সুরক্ষিতভাবে সিঙ্ক হচ্ছে", fontSize = 12.sp, color = PaisaTextSecondary)
                            }
                        }
                    }
                }
            }

            // SECTION: Encrypted Room Database & Settings Cloud Backup
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PaisaGoldAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = PaisaGoldAmber, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("সম্পূর্ণ এনক্রিপ্টেড ব্যাকআপ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
                                    Text("AES-256-GCM এনক্রিপশনযুক্ত Firestore ব্যাকআপ", fontSize = 11.sp, color = PaisaTextSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Backup Details Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PaisaSurfaceVariant)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (latestBackup != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("সর্বশেষ ব্যাকআপ:", fontSize = 12.sp, color = PaisaTextSecondary)
                                        Text(latestBackup!!.formattedDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PaisaTextPrimary)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("সংরক্ষিত রেকর্ড:", fontSize = 12.sp, color = PaisaTextSecondary)
                                        Text("${latestBackup!!.recordsCount} টি ডাটা আইটেম", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PaisaTealPrimary)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("সুরক্ষা পদ্ধতি:", fontSize = 12.sp, color = PaisaTextSecondary)
                                        Text("AES-256-GCM (Zero-Knowledge)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PaisaIncomeGreen)
                                    }
                                } else {
                                    Text("এখনো কোনো ক্লাউড ব্যাকআপ নেওয়া হয়নি। নিচে 'এখনই ব্যাকআপ নিন' চাপুন।", fontSize = 12.sp, color = PaisaTextSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Actions: Export & Restore
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.isBackupOperating.value = true
                                        val result = viewModel.backupManager.createEncryptedBackup()
                                        viewModel.isBackupOperating.value = false
                                        when (result) {
                                            is BackupResult.Success -> {
                                                viewModel.refreshBackupMetadata()
                                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                            }
                                            is BackupResult.Error -> {
                                                Toast.makeText(context, result.error, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isOperating,
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                            ) {
                                if (isOperating) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ব্যাকআপ নিন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = { showRestoreConfirmDialog = true },
                                enabled = !isOperating && latestBackup != null,
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PaisaTealPrimary)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("পুনরুদ্ধার করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Diagnostics Metric Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("অ্যাকাউন্ট ডায়াগনস্টিকস", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        SyncMetricRow(label = "লগইন অ্যাকাউন্ট", value = currentUser?.email ?: "Guest")
                        SyncMetricRow(label = "লোকাল ডাটাবেস", value = "Room on SQLite (Offline First)")
                        SyncMetricRow(label = "সিঙ্ক স্ট্যাটাস", value = "সক্রিয় (Active Auto-Sync)")
                        SyncMetricRow(label = "পেন্ডিং কিউ", value = "${syncStatus.pendingOperations} operations")
                    }
                }
            }

            // Sync Refresh Button
            item {
                Button(
                    onClick = {
                        scope.launch {
                            currentUser?.let {
                                viewModel.syncManager.initializeUserAccount(it.uid, it.email, it.displayName)
                            }
                            viewModel.refreshBackupMetadata()
                            Toast.makeText(context, "ক্লাউড সিঙ্ক রিফ্রেশ করা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaSurfaceVariant, contentColor = PaisaTextPrimary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = PaisaTealPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সিঙ্ক অবস্থা রিফ্রেশ করুন", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Confirmation Dialog for Restore
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("ব্যাকআপ পুনরুদ্ধার করবেন?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "ক্লাউড থেকে সর্বশেষ সংরক্ষিত ব্যাকআপ ডাউনলোড ও ডিক্রিপ্ট করে আপনার ডিভাইসের লোকাল রুম ডাটাবেস এবং সেটিংস আপডেট করা হবে। এটি কি নিশ্চিত?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        scope.launch {
                            viewModel.isBackupOperating.value = true
                            val result = viewModel.backupManager.restoreEncryptedBackup()
                            viewModel.isBackupOperating.value = false
                            when (result) {
                                is BackupResult.Success -> {
                                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                }
                                is BackupResult.Error -> {
                                    Toast.makeText(context, result.error, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                ) {
                    Text("হ্যাঁ, পুনরুদ্ধার করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun SyncMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = PaisaTextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PaisaTextPrimary)
    }
}
