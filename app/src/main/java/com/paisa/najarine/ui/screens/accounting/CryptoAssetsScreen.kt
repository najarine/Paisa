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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoAssetsScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val assets by viewModel.assets.collectAsState()
    val cryptoRates by viewModel.cryptoRates.collectAsState()
    val isFetchingRates by viewModel.isFetchingCryptoRates.collectAsState()
    val totalAssetSummary by viewModel.totalAssetSummary.collectAsState()

    val cryptoAssets = remember(assets) {
        assets.filter { it.category == "CRYPTO" }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    // Live valuation in BDT using live rates
    val totalCryptoBdtValuation = totalAssetSummary.cryptoDigital

    LaunchedEffect(Unit) {
        viewModel.refreshCryptoRates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ডিজিটাল সম্পদ ও ক্রিপ্টো", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.refreshCryptoRates()
                        Toast.makeText(context, "লাইভ ক্রিপ্টো দর আপডেট করা হচ্ছে...", Toast.LENGTH_SHORT).show()
                    }) {
                        if (isFetchingRates) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Rates")
                        }
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Crypto")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFF59E0B)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Live Valuation Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("মোট ক্রিপ্টো সম্পদ (লাইভ কনভার্শন)", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB45309))
                        Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(10.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(12.dp))
                                Text("লাইভ এপিআই", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "৳ ${String.format(Locale.US, "%,.0f", totalCryptoBdtValuation)}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFB45309)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "এই মূল্যায়ন সরাসরি আপনার মোট নিট সম্পদে যুক্ত হয়।",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            // Live Price Bar
            if (cryptoRates.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        cryptoRates.values.take(4).forEach { rate ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(rate.symbol, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text("$${String.format(Locale.US, "%,.0f", rate.priceUsd)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (cryptoAssets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CurrencyBitcoin, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFF59E0B).copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("কোন ক্রিপ্টো বা ডিজিটাল সম্পদ নেই", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("BTC, ETH, USDT, SOL ইত্যাদি যুক্ত করুন। লাইভ কনভার্শন রেটে অটোমেটিক সম্পদে যোগ হবে।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ক্রিপ্টো যুক্ত করুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cryptoAssets, key = { it.id }) { item ->
                        val sym = item.name.uppercase()
                        val liveRate = cryptoRates[sym]
                        val livePriceBdt = liveRate?.priceBdt ?: item.currentPrice
                        val livePriceUsd = liveRate?.priceUsd ?: (item.currentPrice / 122.5)
                        val totalBdt = item.quantity * livePriceBdt

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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CurrencyBitcoin,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(sym, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("হোল্ডিং: ${item.quantity} $sym", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteAsset(item.id)
                                        Toast.makeText(context, "ক্রিপ্টো সম্পদ মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
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
                                        Text("৳ ${String.format(Locale.US, "%,.0f", totalBdt)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("বর্তমান দর (Live)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text("$${String.format(Locale.US, "%,.2f", livePriceUsd)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
        var symbol by remember { mutableStateOf("USDT") }
        var coinAmount by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        val commonCoins = listOf("USDT", "BTC", "ETH", "SOL", "BNB")

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
            title = { Text("নতুন ক্রিপ্টো সম্পদ যোগ করুন") },
            text = {
                Column {
                    Text("কয়েন / টোকেন নির্বাচন করুন:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        commonCoins.forEach { c ->
                            FilterChip(
                                selected = symbol == c,
                                onClick = { symbol = c },
                                label = { Text(c) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = coinAmount,
                        onValueChange = { coinAmount = it },
                        label = { Text("$symbol এর পরিমাণ (Quantity)") },
                        placeholder = { Text("যেমন: 100") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val rate = cryptoRates[symbol]
                    if (rate != null) {
                        Text(
                            text = "বর্তমান রেট: $${String.format(Locale.US, "%,.2f", rate.priceUsd)} (≈ ৳ ${String.format(Locale.US, "%,.0f", rate.priceBdt)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        if (isSubmitting) return@Button
                        val count = coinAmount.toDoubleOrNull() ?: 0.0
                        if (count <= 0) {
                            Toast.makeText(context, "সঠিক পরিমাণ লিখুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val rate = cryptoRates[symbol]?.priceBdt ?: 122.5
                        val asset = AssetEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = viewModel.activeWorkspaceId.value,
                            name = symbol,
                            category = "CRYPTO",
                            quantity = count,
                            unit = symbol,
                            buyPrice = rate,
                            currentPrice = rate,
                            dateMillis = System.currentTimeMillis()
                        )
                        viewModel.addAsset(asset)
                        showAddDialog = false
                        Toast.makeText(context, "ক্রিপ্টো সফলভাবে সম্পদে যোগ হয়েছে!", Toast.LENGTH_SHORT).show()
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
