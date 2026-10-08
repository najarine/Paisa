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
import com.paisa.najarine.data.local.WalletEntity
import com.paisa.najarine.ui.PaisaViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardEmiScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val wallets by viewModel.wallets.collectAsState()

    // Filter wallets that are Credit Cards
    val creditCardWallets = remember(wallets) { wallets.filter { it.type == "CREDIT_CARD" } }
    val nonCardWallets = remember(wallets) { wallets.filter { it.type != "CREDIT_CARD" } }

    var showAddCardDialog by remember { mutableStateOf(false) }
    var payingCardWallet by remember { mutableStateOf<WalletEntity?>(null) }

    // Total outstanding liability
    val totalOutstandingDebt = creditCardWallets.sumOf { Math.abs(it.balance) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ক্রেডিট কার্ড ও ইএমআই (Card & EMI)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddCardDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "কার্ড যোগ করুন")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCardDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.AddCard, contentDescription = "কার্ড যোগ করুন")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Liability Summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("মোট ক্রেডিট কার্ড বকেয়া / দায় (Liability)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF991B1B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-৳ ${String.format(Locale.US, "%,.0f", totalOutstandingDebt)}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFDC2626)
                        )
                        Text("এটি আপনার মোট নিট সম্পদ থেকে স্বয়ংক্রিয়ভাবে বিয়োগ হয়।", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color(0xFFDC2626)
                    )
                }
            }

            if (creditCardWallets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CreditCardOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("কোন ক্রেডিট কার্ড যুক্ত করা হয়নি", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ব্যাংক ক্রেডিট কার্ড ও কিস্তি (EMI) হিসাব সহজে ট্র্যাক করুন।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showAddCardDialog = true }) {
                            Icon(Icons.Default.AddCard, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ক্রেডিট কার্ড যুক্ত করুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(creditCardWallets, key = { it.id }) { card ->
                        val outstanding = Math.abs(card.balance)
                        val limit = card.creditLimit
                        val available = (limit - outstanding).coerceAtLeast(0.0)
                        val utilizationPercent = if (limit > 0) ((outstanding / limit) * 100).toInt() else 0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(card.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (card.accountNumber.isNotBlank()) {
                                            Text("কার্ড নং: **** ${card.accountNumber.takeLast(4)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                    Surface(
                                        color = if (utilizationPercent > 80) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "ব্যবহার: $utilizationPercent%",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (utilizationPercent > 80) Color.Red else Color(0xFF0369A1),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("বর্তমান বকেয়া (দেনা)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text("৳ ${String.format(Locale.US, "%,.0f", outstanding)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                    }
                                    Column {
                                        Text("ক্রেডিট লিমিট", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text("৳ ${String.format(Locale.US, "%,.0f", limit)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("অবশিষ্ট লিমিট", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text("৳ ${String.format(Locale.US, "%,.0f", available)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { payingCardWallet = card },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("বিল পরিশোধ করুন")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.deleteWallet(card.id)
                                            Toast.makeText(context, "কার্ড মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Credit Card Dialog
    if (showAddCardDialog) {
        var cardName by remember { mutableStateOf("") }
        var cardLast4 by remember { mutableStateOf("") }
        var limitAmount by remember { mutableStateOf("") }
        var outstandingAmount by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddCardDialog = false },
            title = { Text("নতুন ক্রেডিট কার্ড যুক্ত করুন") },
            text = {
                Column {
                    OutlinedTextField(
                        value = cardName,
                        onValueChange = { cardName = it },
                        label = { Text("কার্ডের নাম / ব্যাংক") },
                        placeholder = { Text("যেমন: City Bank Amex, Standard Chartered") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cardLast4,
                        onValueChange = { cardLast4 = it },
                        label = { Text("কার্ডের শেষ ৪ ডিজিট (ঐচ্ছিক)") },
                        placeholder = { Text("1234") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = limitAmount,
                        onValueChange = { limitAmount = it },
                        label = { Text("ক্রেডিট লিমিট (৳)") },
                        placeholder = { Text("যেমন: 100000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = outstandingAmount,
                        onValueChange = { outstandingAmount = it },
                        label = { Text("বর্তমান বকেয়া / বিল (৳)") },
                        placeholder = { Text("যেমন: 18500") },
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
                        val limit = limitAmount.toDoubleOrNull() ?: 0.0
                        val debt = outstandingAmount.toDoubleOrNull() ?: 0.0
                        if (cardName.isBlank() || limit <= 0) {
                            Toast.makeText(context, "কার্ডের নাম ও লিমিট সঠিকভাবে লিখুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val cardWallet = WalletEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = viewModel.activeWorkspaceId.value,
                            name = cardName,
                            type = "CREDIT_CARD",
                            institutionId = "credit_card",
                            accountNumber = cardLast4,
                            balance = -debt, // stored as negative liability
                            creditLimit = limit,
                            colorHex = "#E11D48"
                        )
                        viewModel.addWallet(cardWallet)
                        showAddCardDialog = false
                        Toast.makeText(context, "ক্রেডিট কার্ড সফলভাবে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isSubmitting) "সংরক্ষণ হচ্ছে..." else "সংরক্ষণ")
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !isSubmitting,
                    onClick = { showAddCardDialog = false }
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Pay Card Bill Dialog
    payingCardWallet?.let { card ->
        var payAmount by remember { mutableStateOf(Math.abs(card.balance).toString()) }
        var fromWalletId by remember { mutableStateOf(nonCardWallets.firstOrNull()?.id ?: "") }
        var isPaying by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isPaying) payingCardWallet = null },
            title = { Text("${card.name} এর বিল পরিশোধ") },
            text = {
                Column {
                    Text("পরিশোধের উৎস ওয়ালেট:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    var expanded by remember { mutableStateOf(false) }
                    val currentFromWallet = nonCardWallets.find { it.id == fromWalletId }
                    Box {
                        OutlinedButton(
                            enabled = !isPaying,
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currentFromWallet?.let { "${it.name} (৳ ${it.balance})" } ?: "ওয়ালেট নির্বাচন করুন")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            nonCardWallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text("${w.name} (৳ ${w.balance})") },
                                    onClick = {
                                        fromWalletId = w.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text("পরিশোধের পরিমাণ (৳)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isPaying,
                    onClick = {
                        if (isPaying) return@Button
                        val amount = payAmount.toDoubleOrNull() ?: 0.0
                        if (amount <= 0 || fromWalletId.isBlank()) {
                            Toast.makeText(context, "সঠিক পরিমাণ ও ওয়ালেট নির্বাচন করুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isPaying = true
                        viewModel.payCreditCardBill(card.id, fromWalletId, amount)
                        payingCardWallet = null
                        Toast.makeText(context, "৳ $amount কার্ড বিল পরিশোধ সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isPaying) "পরিশোধ হচ্ছে..." else "বিল পরিশোধ নিশ্চিত করুন")
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !isPaying,
                    onClick = { payingCardWallet = null }
                ) {
                    Text("বাতিল")
                }
            }
        )
    }
}
