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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.local.GoalVaultEntity
import com.paisa.najarine.ui.PaisaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val goals by viewModel.goals.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<GoalVaultEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("সঞ্চয় লক্ষ্য (Savings Goals)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Goal")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val totalTarget = goals.sumOf { it.targetAmount }
            val totalSaved = goals.sumOf { it.currentAmount }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("মোট সঞ্চয় প্রবৃদ্ধি", style = MaterialTheme.typography.titleSmall, color = Color(0xFF065F46))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "৳ ${String.format(Locale.US, "%,.0f", totalSaved)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF047857)
                        )
                        Text(
                            text = "টার্গেট: ৳ ${String.format(Locale.US, "%,.0f", totalTarget)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF065F46)
                        )
                    }
                }
            }

            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TrackChanges, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("কোন সঞ্চয় লক্ষ্য যুক্ত করা হয়নি", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("হজ্জ ফান্ড, জরুরি তহবিল, যানবাহন বা গৃহনির্মাণের জন্য লক্ষ্য তৈরি করুন।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("নতুন লক্ষ্য তৈরি করুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(goals, key = { it.id }) { goal ->
                        val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                        val isDone = goal.currentAmount >= goal.targetAmount

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
                                        Text(goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(goal.targetDateMillis))
                                        Text("টার্গেট তারিখ: $dateStr", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    if (isDone) {
                                        Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(12.dp)) {
                                            Text("সম্পন্ন!", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF166534), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteGoal(goal.id)
                                            Toast.makeText(context, "লক্ষ্য মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("জমা: ৳ ${String.format(Locale.US, "%,.0f", goal.currentAmount)}", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    Text("লক্ষ্য: ৳ ${String.format(Locale.US, "%,.0f", goal.targetAmount)}", color = Color.Gray)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = if (isDone) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { contributingGoal = goal },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ওয়ালেট থেকে টাকা জমা করুন")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Goal Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var targetAmount by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
            title = { Text("নতুন সঞ্চয় লক্ষ্য") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("লক্ষ্যের নাম") },
                        placeholder = { Text("যেমন: হজ্জ ফান্ড, নতুন গাড়ি, ল্যাপটপ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { targetAmount = it },
                        label = { Text("টার্গেট টাকার পরিমাণ (৳)") },
                        placeholder = { Text("যেমন: 500000") },
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
                        val target = targetAmount.toDoubleOrNull() ?: 0.0
                        if (name.isBlank() || target <= 0) {
                            Toast.makeText(context, "নাম ও সঠিক টার্গেট পরিমাণ দিন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val goal = GoalVaultEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = viewModel.activeWorkspaceId.value,
                            name = name,
                            targetAmount = target,
                            currentAmount = 0.0,
                            targetDateMillis = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)
                        )
                        viewModel.addGoal(goal)
                        showAddDialog = false
                        Toast.makeText(context, "সঞ্চয় লক্ষ্য যুক্ত করা হয়েছে!", Toast.LENGTH_SHORT).show()
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

    // Deposit to Goal Dialog
    contributingGoal?.let { goal ->
        var depositAmount by remember { mutableStateOf("") }
        var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
        var isDepositSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isDepositSubmitting) contributingGoal = null },
            title = { Text("${goal.name} তে টাকা জমা") },
            text = {
                Column {
                    Text("উৎস ওয়ালেট নির্বাচন করুন:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    var expanded by remember { mutableStateOf(false) }
                    val currentWallet = wallets.find { it.id == selectedWalletId }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(currentWallet?.let { "${it.name} (৳ ${it.balance})" } ?: "ওয়ালেট নির্বাচন করুন")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text("${w.name} (ব্যালেন্স: ৳ ${w.balance})") },
                                    onClick = {
                                        selectedWalletId = w.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = depositAmount,
                        onValueChange = { depositAmount = it },
                        label = { Text("জমার পরিমাণ (৳)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDepositSubmitting,
                    onClick = {
                        if (isDepositSubmitting) return@Button
                        val amount = depositAmount.toDoubleOrNull() ?: 0.0
                        if (amount <= 0) {
                            Toast.makeText(context, "সঠিক পরিমাণ দিন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isDepositSubmitting = true
                        viewModel.depositToGoal(goal.id, selectedWalletId, amount)
                        contributingGoal = null
                        Toast.makeText(context, "৳ $amount সফলভাবে লক্ষ্য তহবিলে জমা হয়েছে!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("জমা নিশ্চিত করুন")
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !isDepositSubmitting,
                    onClick = { contributingGoal = null }
                ) {
                    Text("বাতিল")
                }
            }
        )
    }
}
