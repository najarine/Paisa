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
import com.paisa.najarine.data.local.BudgetEntity
import com.paisa.najarine.ui.PaisaViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPlanningScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    // Calculate current month's expenses per category
    val currentCal = Calendar.getInstance()
    val currentMonth = currentCal.get(Calendar.MONTH) + 1
    val currentYear = currentCal.get(Calendar.YEAR)

    val expenseTransactionsThisMonth = remember(transactions, currentMonth, currentYear) {
        transactions.filter { tx ->
            if (tx.type != "EXPENSE") return@filter false
            val cal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
            cal.get(Calendar.MONTH) + 1 == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
    }

    val spentPerCategory = remember(expenseTransactionsThisMonth) {
        expenseTransactionsThisMonth.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("বাজেট পরিকল্পনা (Budgeting)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "বাজেট যোগ করুন")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "বাজেট যোগ করুন")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Card
            val totalBudget = budgets.sumOf { it.amountLimit }
            val totalSpent = spentPerCategory.values.sum()
            val totalRemaining = (totalBudget - totalSpent).coerceAtLeast(0.0)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("চলতি মাসের সামগ্রিক বাজেট", style = MaterialTheme.typography.titleSmall)
                        Surface(
                            color = if (totalSpent > totalBudget && totalBudget > 0) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (totalSpent > totalBudget && totalBudget > 0) "বাজেট অতিক্রম!" else "নিয়ন্ত্রণে আছে",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (totalSpent > totalBudget && totalBudget > 0) Color(0xFF991B1B) else Color(0xFF166534),
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
                            Text("মোট বাজেট", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("৳ ${String.format(Locale.US, "%,.0f", totalBudget)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("মোট ব্যয়", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("৳ ${String.format(Locale.US, "%,.0f", totalSpent)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                        Column {
                            Text("অবশিষ্ট", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("৳ ${String.format(Locale.US, "%,.0f", totalRemaining)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val overallProgress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (overallProgress >= 1f) Color(0xFFEF4444) else if (overallProgress >= 0.8f) Color(0xFFF59E0B) else Color(0xFF10B981)
                    )
                }
            }

            if (budgets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("কোন বাজেট পরিকল্পনা নির্ধারণ করা হয়নি", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("খাবার, বাজার, বিল ইত্যাদির জন্য মাসিক সর্বোচ্চ সীমা ঠিক করুন।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("বাজেট যোগ করুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(budgets, key = { it.id }) { budget ->
                        val spent = spentPerCategory[budget.categoryName] ?: 0.0
                        val progress = if (budget.amountLimit > 0) (spent / budget.amountLimit).toFloat().coerceIn(0f, 1f) else 0f
                        val isOver = spent > budget.amountLimit

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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Category,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(budget.categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteBudget(budget.id)
                                            Toast.makeText(context, "বাজেট মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ব্যয় হয়েছে: ৳ ${String.format(Locale.US, "%,.0f", spent)}", style = MaterialTheme.typography.bodySmall, color = if (isOver) Color.Red else Color.Gray)
                                    Text("সীমা: ৳ ${String.format(Locale.US, "%,.0f", budget.amountLimit)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = if (isOver) Color(0xFFEF4444) else if (progress >= 0.8f) Color(0xFFF59E0B) else Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var selectedCategory by remember { mutableStateOf(categories.firstOrNull { it.type == "EXPENSE" }?.name ?: "Food & Dining") }
        var limitAmount by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("নতুন বাজেট নির্ধারণ") },
            text = {
                Column {
                    Text("খাত (Category) নির্বাচন করুন:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedCategory)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.filter { it.type == "EXPENSE" }.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCategory = cat.name
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = limitAmount,
                        onValueChange = { limitAmount = it },
                        label = { Text("মাসিক সর্বোচ্চ ব্যয় সীমা (৳)") },
                        placeholder = { Text("যেমন: 15000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = limitAmount.toDoubleOrNull() ?: 0.0
                        if (limit <= 0) {
                            Toast.makeText(context, "সঠিক পরিমাণ লিখুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val budget = BudgetEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = "personal_default",
                            categoryName = selectedCategory,
                            amountLimit = limit,
                            month = currentMonth,
                            year = currentYear
                        )
                        viewModel.addBudget(budget)
                        showAddDialog = false
                        Toast.makeText(context, "বাজেট যোগ করা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("নির্ধারণ করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
