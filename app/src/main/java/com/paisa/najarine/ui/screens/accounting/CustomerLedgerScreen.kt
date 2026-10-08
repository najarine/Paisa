package com.paisa.najarine.ui.screens.accounting

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.paisa.najarine.data.local.CustomerLedgerEntity
import com.paisa.najarine.ui.PaisaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val ledgerEntries by viewModel.customerLedger.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val totalAssetSummary by viewModel.totalAssetSummary.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val totalBaki = ledgerEntries.filter { it.type == "BAKI" }.sumOf { it.amount }
    val totalJoma = ledgerEntries.filter { it.type == "JOMA" }.sumOf { it.amount }
    val netDueReceivable = totalAssetSummary.customerReceivables

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("খদ্দের ও কাস্টমার লেজার", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "হিসাব যোগ করুন")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "যোগ করুন")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Net Dues Summary Card
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
                        Text("কাস্টমারদের মোট বাকি (পাওনা সম্পদ)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF166534))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "৳ ${String.format(Locale.US, "%,.0f", netDueReceivable)}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF15803D)
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color(0xFF15803D)
                    )
                }
            }

            if (ledgerEntries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ImportContacts, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("কোন খদ্দের বা কাস্টমার লেজার এন্ট্রি নেই", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ব্যবসায়িক বা ফ্রিল্যান্স ক্লায়েন্টদের বাকি ও জমার হিসাব ডিজিটাল খতিয়ানে সংরক্ষণ করুন।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("এন্ট্রি যোগ করুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ledgerEntries, key = { it.id }) { entry ->
                        val isBaki = entry.type == "BAKI"
                        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(entry.dateMillis))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (entry.phone.isNotBlank()) {
                                        Text(entry.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    if (entry.note.isNotBlank()) {
                                        Text(entry.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("তারিখ: $dateStr", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (isBaki) "+" else "-"}৳ ${String.format(Locale.US, "%,.0f", entry.amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBaki) Color(0xFFEF4444) else Color(0xFF10B981)
                                    )
                                    Surface(
                                        color = if (isBaki) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = if (isBaki) "বাকি (Due)" else "জমা (Paid)",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isBaki) Color(0xFF991B1B) else Color(0xFF166534),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                IconButton(onClick = {
                                    viewModel.deleteCustomerLedger(entry.id)
                                    Toast.makeText(context, "লেজার এন্ট্রি মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var customerName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("BAKI") } // BAKI, JOMA
        var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
        var depositToWallet by remember { mutableStateOf(true) }
        var note by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
            title = { Text("নতুন কাস্টমার এন্ট্রি") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = type == "BAKI",
                            onClick = { if (!isSubmitting) type = "BAKI" },
                            label = { Text("বাকি (বিক্রয়/পাওনা)") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = type == "JOMA",
                            onClick = { if (!isSubmitting) type = "JOMA" },
                            label = { Text("জমা (আদায়/পরিশোধ)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("খদ্দের / কাস্টমারের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("মোবাইল নম্বর (ঐচ্ছিক)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("টাকার পরিমাণ (৳)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (type == "JOMA" && wallets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("টাকা জমার ওয়ালেট / অ্যাকাউন্ট:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(wallets) { w ->
                                FilterChip(
                                    selected = selectedWalletId == w.id,
                                    onClick = { if (!isSubmitting) selectedWalletId = w.id },
                                    label = { Text(w.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("বিবরণ / পণ্যের বিবরণ") },
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
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (customerName.isBlank() || amt <= 0) {
                            Toast.makeText(context, "নাম ও সঠিক টাকার পরিমাণ দিন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val entry = CustomerLedgerEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = viewModel.activeWorkspaceId.value,
                            customerName = customerName,
                            phone = phone,
                            amount = amt,
                            type = type,
                            dateMillis = System.currentTimeMillis(),
                            note = note
                        )
                        val depositWallet = if (type == "JOMA" && depositToWallet) selectedWalletId else null
                        viewModel.addCustomerLedger(entry, depositToWalletId = depositWallet)
                        showAddDialog = false
                        Toast.makeText(context, "খতিয়ানে সফলভাবে সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isSubmitting) "সংরক্ষণ হচ্ছে..." else "সংরক্ষণ")
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
