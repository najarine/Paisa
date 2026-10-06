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
fun StockMarketScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val assets by viewModel.assets.collectAsState()
    val totalAssetSummary by viewModel.totalAssetSummary.collectAsState()

    val stockAssets = remember(assets) {
        assets.filter { it.category == "STOCK" }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    val totalPortfolioValuation = totalAssetSummary.stockInvestments
    val totalInvestmentCost = stockAssets.sumOf { it.quantity * it.buyPrice }
    val totalProfitLoss = totalPortfolioValuation - totalInvestmentCost

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("শেয়ার বাজার বিনিয়োগ (Stocks)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Stock")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF2563EB)
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
            // Portfolio Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("মোট শেয়ার পোর্টফোলিও মূল্যায়ন (Total Assets এ যুক্ত)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E40AF))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "৳ ${String.format(Locale.US, "%,.0f", totalPortfolioValuation)}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1D4ED8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ক্রয়মূল্য: ৳ ${String.format(Locale.US, "%,.0f", totalInvestmentCost)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(
                            text = "লাভ/ক্ষতি: ${if (totalProfitLoss >= 0) "+" else ""}৳ ${String.format(Locale.US, "%,.0f", totalProfitLoss)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (totalProfitLoss >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (stockAssets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFF2563EB).copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("কোন শেয়ার বিনিয়োগ যুক্ত করা হয়নি", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ডিএসই, সিএসই বা বৈশ্বিক শেয়ার বাজারের পোর্টফোলিও যুক্ত করুন। এটি আপনার মোট সম্পদে যুক্ত হবে।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("শেয়ার যুক্ত করুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(stockAssets, key = { it.id }) { item ->
                        val value = item.quantity * item.currentPrice
                        val cost = item.quantity * item.buyPrice
                        val diff = value - cost

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
                                        Text("শেয়ার সংখ্যা: ${item.quantity.toInt()} টি | গড় ক্রয়: ৳ ${item.buyPrice}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteAsset(item.id)
                                        Toast.makeText(context, "শেয়ার রেকর্ড মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
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
                                        Text("বর্তমান মূল্যায়ন (সম্পদ)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text("৳ ${String.format(Locale.US, "%,.0f", value)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("লাভ / ক্ষতি", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text(
                                            text = "${if (diff >= 0) "+" else ""}৳ ${String.format(Locale.US, "%,.0f", diff)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (diff >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
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
        var ticker by remember { mutableStateOf("") }
        var shares by remember { mutableStateOf("") }
        var buyPrice by remember { mutableStateOf("") }
        var currentPrice by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
            title = { Text("নতুন শেয়ার যুক্ত করুন") },
            text = {
                Column {
                    OutlinedTextField(
                        value = ticker,
                        onValueChange = { ticker = it.uppercase() },
                        label = { Text("কোম্পানি / টিকার প্রতীক") },
                        placeholder = { Text("যেমন: GP, BEXIMCO, BRACBANK, SQURPHARMA") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = shares,
                        onValueChange = { shares = it },
                        label = { Text("শেয়ারের সংখ্যা (টি)") },
                        placeholder = { Text("যেমন: 500") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = buyPrice,
                        onValueChange = { buyPrice = it },
                        label = { Text("গড় ক্রয়মূল্য (৳)") },
                        placeholder = { Text("যেমন: 280.50") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = currentPrice,
                        onValueChange = { currentPrice = it },
                        label = { Text("বর্তমান বাজার দর (৳)") },
                        placeholder = { Text("যেমন: 310.00") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        if (isSubmitting) return@Button
                        val count = shares.toDoubleOrNull() ?: 0.0
                        val bPrice = buyPrice.toDoubleOrNull() ?: 0.0
                        val cPrice = currentPrice.toDoubleOrNull() ?: bPrice
                        if (ticker.isBlank() || count <= 0 || bPrice <= 0) {
                            Toast.makeText(context, "টিকার, সংখ্যা ও ক্রয়মূল্য সঠিকভাবে দিন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val asset = AssetEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = viewModel.activeWorkspaceId.value,
                            name = ticker,
                            category = "STOCK",
                            quantity = count,
                            unit = "share",
                            buyPrice = bPrice,
                            currentPrice = cPrice,
                            dateMillis = System.currentTimeMillis()
                        )
                        viewModel.addAsset(asset)
                        showAddDialog = false
                        Toast.makeText(context, "শেয়ার পোর্টফোলিওতে যোগ হয়েছে!", Toast.LENGTH_SHORT).show()
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
