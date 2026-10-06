package com.paisa.najarine.ui.screens.sync

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Sync Center", fontWeight = FontWeight.Bold) },
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
                                    .size(40.dp)
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

            item {
                Button(
                    onClick = {
                        scope.launch {
                            currentUser?.let {
                                viewModel.syncManager.initializeUserAccount(it.uid, it.email, it.displayName)
                            }
                            Toast.makeText(context, "ক্লাউড সিঙ্ক রিফ্রেশ করা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সিঙ্ক রিফ্রেশ করুন", fontWeight = FontWeight.Bold)
                }
            }
        }
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
