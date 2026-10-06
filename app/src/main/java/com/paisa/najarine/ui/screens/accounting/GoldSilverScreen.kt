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
fun GoldSilverScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val assets by viewModel.assets.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val totalAssetSummary by viewModel.totalAssetSummary.collectAsState()

    val metalAssets = remember(assets) {
        assets.filter { it.category in listOf("GOLD", "SILVER") }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    val totalGoldValuation = metalAssets.filter { it.category == "GOLD" }.sumOf { it.quantity * it.currentPrice }
    val totalSilverValuation = metalAssets.filter { it.category == "SILVER" }.sumOf { it.quantity * it.currentPrice }
    val totalMetalsValuation = totalAssetSummary.preciousMetals

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("স্বর্ণ ও রৌপ্য (Gold & Silver)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Metal")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFD97706)
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
            // Valuation Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("মোট মূল্যবান ধাতু সম্পদ (Total Assets এ যুক্ত)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF92400E))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "৳ ${String.format(Locale.US, "%,.0f", totalMetalsValuation)}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFB45309)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("স্বর্ণ মোট: ৳ ${String.format(Locale.US, "%,.0f", totalGoldValuation)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD97706), fontWeight = FontWeight.SemiBold)
                        Text("রৌপ্য মোট: ৳ ${String.format(Locale.US, "%,.0f", totalSilverValuation)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (metalAssets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFD97706).copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("কোন স্বর্ণ বা রৌপ্য যুক্ত করা হয়নি", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("আপনার পরিবারের স্বর্ণালঙ্কার বা রৌপ্যের পরিমাণ যুক্ত করুন। এগুলো স্বয়ংক্রিয়ভাবে মোট সম্পদে যোগ হবে।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("স্বর্ণ / রৌপ্য যুক্ত করুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(metalAssets, key = { it.id }) { item ->
                        val isGold = item.category == "GOLD"
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Diamond,
                                            contentDescription = null,
                                            tint = if (isGold) Color(0xFFD97706) else Color(0xFF64748B),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("ওজন: ${item.quantity} ${item.unit} | যোগ: $dateStr", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                    Surface(
                                        color = if (isGold) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (isGold) "স্বর্ণ" else "রৌপ্য",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isGold) Color(0xFFB45309) else Color(0xFF475569),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteAsset(item.id)
                                        Toast.makeText(context, "আইটেম মুছে ফেলা হয়েছে (মোট সম্পদ আপডেট হয়েছে)", Toast.LENGTH_SHORT).show()
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
                                        Text("বর্তমান বাজার মূল্য (সম্পদ)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text("৳ ${String.format(Locale.US, "%,.0f", value)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("প্রতি ${item.unit} দর", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text("৳ ${String.format(Locale.US, "%,.0f", item.currentPrice)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
        var metalType by remember { mutableStateOf("GOLD") } // GOLD, SILVER
        var itemName by remember { mutableStateOf("") }
        var weight by remember { mutableStateOf("") }
        var unit by remember { mutableStateOf("ভরি") } // ভরি, গ্রাম
        var ratePerUnit by remember { mutableStateOf("140000") }
        var isSubmitting by remember { mutableStateOf(false) }

        LaunchedEffect(metalType) {
            ratePerUnit = if (metalType == "GOLD") "140000" else "2500"
        }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
            title = { Text("স্বর্ণ / রৌপ্য যুক্ত করুন") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = metalType == "GOLD",
                            onClick = { metalType = "GOLD" },
                            label = { Text("স্বর্ণ (Gold)") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = metalType == "SILVER",
                            onClick = { metalType = "SILVER" },
                            label = { Text("রৌপ্য (Silver)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("বিবরণ / অলংকার") },
                        placeholder = { Text("যেমন: ২২ ক্যারেট চেইন, বালা, কয়েন") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("ওজন") },
                            placeholder = { Text("যেমন: 2.5") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        var expandedUnit by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(0.7f).padding(top = 8.dp)) {
                            OutlinedButton(onClick = { expandedUnit = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(unit)
                            }
                            DropdownMenu(expanded = expandedUnit, onDismissRequest = { expandedUnit = false }) {
                                DropdownMenuItem(text = { Text("ভরি (Vori)") }, onClick = { unit = "ভরি"; expandedUnit = false })
                                DropdownMenuItem(text = { Text("গ্রাম (Gram)") }, onClick = { unit = "গ্রাম"; expandedUnit = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ratePerUnit,
                        onValueChange = { ratePerUnit = it },
                        label = { Text("প্রতি $unit এর বর্তমান বাজার দর (৳)") },
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
                        val wt = weight.toDoubleOrNull() ?: 0.0
                        val rate = ratePerUnit.toDoubleOrNull() ?: 0.0
                        if (itemName.isBlank() || wt <= 0 || rate <= 0) {
                            Toast.makeText(context, "নাম, সঠিক ওজন ও দর লিখুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val asset = AssetEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = viewModel.activeWorkspaceId.value,
                            name = itemName,
                            category = metalType,
                            quantity = wt,
                            unit = unit,
                            buyPrice = rate,
                            currentPrice = rate,
                            dateMillis = System.currentTimeMillis()
                        )
                        viewModel.addAsset(asset)
                        showAddDialog = false
                        Toast.makeText(context, "স্বর্ণ/রৌপ্য সফলভাবে সম্পদে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
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
