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
import com.paisa.najarine.data.local.DebtEntity
import com.paisa.najarine.ui.PaisaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DenaPaonaScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val debts by viewModel.debts.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val totalAssetSummary by viewModel.totalAssetSummary.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Paona (Receivable), 2: Dena (Payable)
    var showAddDialog by remember { mutableStateOf(false) }

    val activeDebts = remember(debts) { debts.filter { !it.isSettled } }
    val totalPaona = totalAssetSummary.loanReceivables
    val totalDena = totalAssetSummary.loanLiabilities
    val netDebtBalance = totalPaona - totalDena

    val filteredList = when (selectedTab) {
        1 -> debts.filter { it.type == "PONA" }
        2 -> debts.filter { it.type == "DENA" }
        else -> debts
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("দেনা-পাওনা (Debt & Loan)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Debt/Loan")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Net Impact Banner on Total Assets
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("মোট সম্পদে দেনা-পাওনার প্রভাব:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("মোট পাওনা (সম্পদ)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF047857))
                            Text("৳ ${String.format(Locale.US, "%,.0f", totalPaona)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Column {
                            Text("মোট দেনা (দায়)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF991B1B))
                            Text("৳ ${String.format(Locale.US, "%,.0f", totalDena)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                        Column {
                            Text("নিট প্রভাব", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                text = "${if (netDebtBalance >= 0) "+" else ""}৳ ${String.format(Locale.US, "%,.0f", netDebtBalance)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (netDebtBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("সকল (${debts.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("পাওনা (${debts.count { it.type == "PONA" }})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("দেনা (${debts.count { it.type == "DENA" }})") })
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("কোন দেনা বা পাওনা হিসাব নেই", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("বন্ধুবান্ধব বা পরিচিতদের ঋণ দেওয়া বা নেওয়া হিসাব রাখুন।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("নতুন হিসাব যুক্ত করুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { debt ->
                        val isPaona = debt.type == "PONA"
                        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(debt.dueDateMillis))

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
                                        Text(debt.personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (debt.phoneNumber.isNotBlank()) {
                                            Text(debt.phoneNumber, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                        Text("পরিশোধের তারিখ: $dateStr", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isPaona) "+" else "-"}৳ ${String.format(Locale.US, "%,.0f", debt.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPaona) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                        Surface(
                                            color = if (debt.isSettled) Color(0xFFE2E8F0) else if (isPaona) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = if (debt.isSettled) "পরিশোধিত" else if (isPaona) "পাওনা (সম্পদ)" else "দেনা (দায়)",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (debt.isSettled) Color.Gray else if (isPaona) Color(0xFF166534) else Color(0xFF991B1B),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    IconButton(onClick = {
                                        viewModel.deleteDebt(debt.id)
                                        Toast.makeText(context, "হিসাব মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
                                    }
                                }

                                if (!debt.isSettled) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.settleDebt(debt, wallets.firstOrNull()?.id ?: "")
                                            Toast.makeText(context, "হিসাব নিষ্পত্তি (Settled) হিসেবে সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("নিষ্পত্তি / পরিশোধ সম্পন্ন চিহ্নিত করুন")
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
        var personName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var debtType by remember { mutableStateOf("PONA") } // PONA = They owe me, DENA = I owe them
        var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
        var updateWalletBalance by remember { mutableStateOf(true) }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
            title = { Text("নতুন দেনা / পাওনা যুক্ত করুন") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = debtType == "PONA",
                            onClick = { debtType = "PONA" },
                            label = { Text("আমি পাবো (পাওনা)") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = debtType == "DENA",
                            onClick = { debtType = "DENA" },
                            label = { Text("আমি দেবো (দেনা)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text("ব্যক্তি / প্রতিষ্ঠানের নাম") },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = updateWalletBalance, onCheckedChange = { updateWalletBalance = it })
                        Text(
                            text = if (debtType == "PONA") "ওয়ালেট থেকে টাকা কর্তন করুন (ঋণ প্রদান)" else "ওয়ালেটে টাকা যোগ করুন (ঋণ গ্রহণ)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        if (isSubmitting) return@Button
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (personName.isBlank() || amt <= 0) {
                            Toast.makeText(context, "নাম ও সঠিক টাকার পরিমাণ দিন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val debt = DebtEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = viewModel.activeWorkspaceId.value,
                            walletId = selectedWalletId,
                            personName = personName,
                            phoneNumber = phone,
                            amount = amt,
                            type = debtType,
                            dueDateMillis = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
                        )
                        viewModel.addDebt(debt, updateWallet = updateWalletBalance)
                        showAddDialog = false
                        Toast.makeText(context, "দেনা-পাওনা রেকর্ড সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
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
