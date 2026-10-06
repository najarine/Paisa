package com.paisa.najarine.ui.screens.accounting

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import com.paisa.najarine.data.local.BillSubscriptionEntity
import com.paisa.najarine.ui.PaisaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsSubscriptionsScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val bills by viewModel.bills.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    var payingBillId by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("বিল ও সাবস্ক্রিপশন", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Bill")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Bill")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val totalMonthlyBills = bills.filter { !it.isPaid }.sumOf { it.amount }
            val totalPaidBills = bills.filter { it.isPaid }.sumOf { it.amount }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("আসন্ন নিয়মিত বিল (বকেয়া)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "৳ ${String.format(Locale.US, "%,.0f", totalMonthlyBills)}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFEF4444)
                        )
                        if (totalPaidBills > 0) {
                            Text(
                                text = "এই মাসে পরিশোধিত: ৳ ${String.format(Locale.US, "%,.0f", totalPaidBills)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (bills.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("কোন বিল বা সাবস্ক্রিপশন যুক্ত নেই", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("বিদ্যুৎ বিল, ইন্টারনেট, গ্যাস, বাড়ি ভাড়া বা নেটফ্লিক্স যুক্ত করুন।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("নতুন বিল যুক্ত করুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(bills, key = { it.id }) { bill ->
                        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(bill.nextDueDateMillis))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bill.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("পরিশোধের তারিখ: $dateStr (${bill.cycle})", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "৳ ${String.format(Locale.US, "%,.0f", bill.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (bill.isPaid) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                        Surface(
                                            color = if (bill.isPaid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (bill.isPaid) "পরিশোধিত / নিষ্পত্তি" else "বকেয়া",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (bill.isPaid) Color(0xFF166534) else Color(0xFF991B1B),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteBill(bill.id)
                                        Toast.makeText(context, "বিল মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (bill.isPaid) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            color = Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("বিল পরিশোধ সম্পন্ন (Settled)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.resetBillForNextCycle(bill)
                                                Toast.makeText(context, "পরবর্তী মাসের জন্য সক্রিয় করা হয়েছে", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("পুনরায় সক্রিয়", fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    val isThisPaying = payingBillId == bill.id
                                    Button(
                                        enabled = !isThisPaying,
                                        onClick = {
                                            if (isThisPaying) return@Button
                                            payingBillId = bill.id
                                            val walletId = bill.walletId.ifBlank { wallets.firstOrNull()?.id ?: "" }
                                            viewModel.payBill(bill, walletId)
                                            Toast.makeText(context, "${bill.name} পরিশোধ হিসেবে ব্যয়ে যুক্ত হয়েছে ও নিষ্পত্তি হয়েছে!", Toast.LENGTH_SHORT).show()
                                            payingBillId = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isThisPaying) "পরিশোধ হচ্ছে..." else "বিল পরিশোধ করুন (Pay Now)")
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
        var name by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var cycle by remember { mutableStateOf("MONTHLY") }
        var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
            title = { Text("নতুন বিল / সাবস্ক্রিপশন") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("বিলের নাম") },
                        placeholder = { Text("যেমন: বিদ্যুৎ বিল, ওয়াইফাই, বাসা ভাড়া") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("টাকার পরিমাণ (৳)") },
                        placeholder = { Text("যেমন: 2500") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("বিল পরিশোধের ওয়ালেট:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    var expanded by remember { mutableStateOf(false) }
                    val currentWallet = wallets.find { it.id == selectedWalletId }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(currentWallet?.name ?: "ওয়ালেট নির্বাচন করুন")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name) },
                                    onClick = {
                                        selectedWalletId = w.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        if (isSubmitting) return@Button
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (name.isBlank() || amt <= 0) {
                            Toast.makeText(context, "নাম ও সঠিক টাকার পরিমাণ দিন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val bill = BillSubscriptionEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = viewModel.activeWorkspaceId.value,
                            walletId = selectedWalletId,
                            name = name,
                            amount = amt,
                            cycle = cycle,
                            nextDueDateMillis = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
                        )
                        viewModel.addBill(bill)
                        showAddDialog = false
                        Toast.makeText(context, "বিল সফলভাবে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
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
