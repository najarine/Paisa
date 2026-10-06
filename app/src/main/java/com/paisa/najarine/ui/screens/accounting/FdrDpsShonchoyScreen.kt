package com.paisa.najarine.ui.screens.accounting

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.local.AssetEntity
import com.paisa.najarine.ui.PaisaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FdrDpsShonchoyScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val assets by viewModel.assets.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val totalAssetSummary by viewModel.totalAssetSummary.collectAsState()

    // Filter fixed term investment assets
    val fixedInvestments = remember(assets) {
        assets.filter { it.category in listOf("FDR", "DPS", "SHONCHOYPOTRO") }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    val totalInvestmentValuation = totalAssetSummary.fdrDpsSavings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("এফডিআর, ডিপিএস ও সঞ্চয়পত্র", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Investment")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Asset Impact Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("মোট স্থায়ী বিনিয়োগ সম্পদ (Total Assets এ যুক্ত)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF166534))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "৳ ${String.format(Locale.US, "%,.0f", totalInvestmentValuation)}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF15803D)
                        )
                    }
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color(0xFF15803D)
                    )
                }
            }

            if (fixedInvestments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("কোন এফডিআর, ডিপিএস বা সঞ্চয়পত্র নেই", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ব্যাংক ডিপিএস, এফডিআর ও জাতীয় সঞ্চয়পত্রের হিসাব রাখুন। এগুলো আপনার মোট সম্পদে স্বয়ংক্রিয়ভাবে যোগ হবে।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("বিনিয়োগ যুক্ত করুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(fixedInvestments, key = { it.id }) { item ->
                        val value = item.quantity * item.currentPrice
                        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(item.dateMillis))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("শুরুর তারিখ: $dateStr", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = item.category,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF166534),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteAsset(item.id)
                                        Toast.makeText(context, "বিনিয়োগ মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("বর্তমান মূল্যমান (সম্পদ)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text("৳ ${String.format(Locale.US, "%,.0f", value)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("প্রারম্ভিক বিনিয়োগ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text("৳ ${String.format(Locale.US, "%,.0f", item.buyPrice)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var invType by remember { mutableStateOf("DPS") } // DPS, FDR, SHONCHOYPOTRO
        var name by remember { mutableStateOf("") }
        var principalAmount by remember { mutableStateOf("") }
        var currentAccruedValue by remember { mutableStateOf("") }
        var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
        var deductFromWallet by remember { mutableStateOf(false) }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
            title = { Text("নতুন বিনিয়োগ যুক্ত করুন") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(selected = invType == "DPS", onClick = { invType = "DPS" }, label = { Text("DPS") })
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(selected = invType == "FDR", onClick = { invType = "FDR" }, label = { Text("FDR") })
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(selected = invType == "SHONCHOYPOTRO", onClick = { invType = "SHONCHOYPOTRO" }, label = { Text("সঞ্চয়পত্র") })
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("বিনিয়োগের নাম / ব্যাংক") },
                        placeholder = { Text("যেমন: ব্র্যাক ব্যাংক ৫ বছর মেয়াদী ডিপিএস") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = principalAmount,
                        onValueChange = { principalAmount = it },
                        label = { Text("প্রারম্ভিক বিনিয়োগ / কিস্তি মোট (৳)") },
                        placeholder = { Text("যেমন: 100000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = currentAccruedValue,
                        onValueChange = { currentAccruedValue = it },
                        label = { Text("বর্তমান অর্জিত মূল্য (৳)") },
                        placeholder = { Text("খালি রাখলে প্রারম্ভিক পরিমাণই গণ্য হবে") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = deductFromWallet, onCheckedChange = { deductFromWallet = it })
                        Text("ওয়ালেট থেকে টাকা কর্তন করুন", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        if (isSubmitting) return@Button
                        val principal = principalAmount.toDoubleOrNull() ?: 0.0
                        val currentVal = currentAccruedValue.toDoubleOrNull() ?: principal
                        if (name.isBlank() || principal <= 0) {
                            Toast.makeText(context, "নাম ও সঠিক বিনিয়োগ পরিমাণ দিন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val asset = AssetEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = viewModel.activeWorkspaceId.value,
                            name = name,
                            category = invType,
                            quantity = 1.0,
                            unit = "certificate",
                            buyPrice = principal,
                            currentPrice = currentVal,
                            dateMillis = System.currentTimeMillis()
                        )
                        viewModel.addAsset(asset, deductFromWallet = deductFromWallet, walletId = selectedWalletId)
                        showAddDialog = false
                        Toast.makeText(context, "বিনিয়োগ সফলভাবে সম্পদে যোগ হয়েছে!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !isSubmitting,
                    onClick = { showAddDialog = false }
                ) {
                    Text("বাতিল")
                }
            }
        )
    }
}
