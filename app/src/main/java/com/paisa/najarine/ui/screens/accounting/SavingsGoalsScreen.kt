package com.paisa.najarine.ui.screens.accounting

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.local.DebtEntity
import com.paisa.najarine.data.local.GoalVaultEntity
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private const val DEBT_GOAL_PREFIX = "[ঋণ পরিশোধ]"

val GoalVaultEntity.isDebtPayoff: Boolean
    get() = name.startsWith(DEBT_GOAL_PREFIX) || colorHex == "#EF4444"

val GoalVaultEntity.displayCleanName: String
    get() = if (name.startsWith(DEBT_GOAL_PREFIX)) {
        name.removePrefix(DEBT_GOAL_PREFIX).trim()
    } else {
        name
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    initialTab: Int = 0
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val goals by viewModel.goals.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val totalAssetSummary by viewModel.totalAssetSummary.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab) } // 0: Savings Goals, 1: Debt Payoff Goals
    var showAddDialog by remember { mutableStateOf(false) }
    var preselectedDebtForGoal by remember { mutableStateOf<DebtEntity?>(null) }
    var contributingGoal by remember { mutableStateOf<GoalVaultEntity?>(null) }

    val activeDebts = remember(debts) {
        debts.filter { it.type == "DENA" && !it.isSettled }
    }
    val totalDenaLiability = remember(activeDebts) {
        activeDebts.sumOf { it.amount }
    }

    val savingsGoals = remember(goals) { goals.filter { !it.isDebtPayoff } }
    val debtPayoffGoals = remember(goals) { goals.filter { it.isDebtPayoff } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedTab == 0) "সঞ্চয় লক্ষ্য (Savings Goals)" else "ঋণ পরিশোধ লক্ষ্য (Debt Payoff)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (selectedTab == 0) "ভবিষ্যত সম্পদ সৃষ্টির পরিকল্পনা" else "দায় মুক্তি ও দেনা পরিশোধ ট্র্যাকার",
                            fontSize = 11.sp,
                            color = PaisaTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        preselectedDebtForGoal = null
                        showAddDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Goal")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaisaSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    preselectedDebtForGoal = null
                    showAddDialog = true
                },
                containerColor = if (selectedTab == 0) PaisaTealPrimary else PaisaExpenseRed
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PaisaBackground)
                .padding(padding)
        ) {
            // Tab Row: Savings vs Debt Payoff
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = PaisaSurface,
                contentColor = PaisaTealPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("সঞ্চয় লক্ষ্য (${savingsGoals.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ঋণ পরিশোধ (${debtPayoffGoals.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                // TAB 0: REGULAR SAVINGS GOALS
                val totalTarget = savingsGoals.sumOf { it.targetAmount }
                val totalSaved = savingsGoals.sumOf { it.currentAmount }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("মোট সঞ্চয় প্রবৃদ্ধি (Savings Overview)", style = MaterialTheme.typography.titleSmall, color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "৳ ${String.format(Locale.US, "%,.0f", totalSaved)}",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF047857)
                                )
                                Text("বর্তমান মোট জমার পরিমাণ", fontSize = 11.sp, color = Color(0xFF065F46))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "টার্গেট: ৳ ${String.format(Locale.US, "%,.0f", totalTarget)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF065F46)
                                )
                                val overallProgress = if (totalTarget > 0) ((totalSaved / totalTarget) * 100).toInt() else 0
                                Text("অগ্রগতি: $overallProgress%", fontSize = 11.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (savingsGoals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.TrackChanges, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("কোন সঞ্চয় লক্ষ্য যুক্ত করা হয়নি", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("হজ্জ ফান্ড, জরুরি তহবিল, যানবাহন বা গৃহনির্মাণের জন্য লক্ষ্য তৈরি করুন।", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                preselectedDebtForGoal = null
                                showAddDialog = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("নতুন সঞ্চয় লক্ষ্য তৈরি করুন")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(savingsGoals, key = { it.id }) { goal ->
                            GoalCardItem(
                                goal = goal,
                                isDebtGoal = false,
                                onDepositClick = { contributingGoal = goal },
                                onDeleteClick = {
                                    viewModel.deleteGoal(goal.id)
                                    Toast.makeText(context, "লক্ষ্য মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            } else {
                // TAB 1: DEBT PAYOFF GOALS & LIABILITY BALANCE TRACKER
                val totalDebtPaidOff = debtPayoffGoals.sumOf { it.currentAmount }
                val remainingTotalLiability = (totalDenaLiability - totalDebtPaidOff).coerceAtLeast(0.0)
                val debtPayoffProgress = if (totalDenaLiability > 0) {
                    ((totalDebtPaidOff / totalDenaLiability) * 100.0).coerceIn(0.0, 100.0)
                } else {
                    100.0
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Overall Debt Payoff Banner
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("debt_payoff_summary_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = PaisaExpenseLight),
                            border = BorderStroke(1.dp, PaisaExpenseRed.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(PaisaExpenseRed.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = PaisaExpenseRed, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("সামগ্রিক ঋণ পরিশোধ অগ্রগতি", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PaisaExpenseRed)
                                            Text("দায়মুক্তি প্রবৃদ্ধি ট্র্যাকার", fontSize = 11.sp, color = PaisaTextSecondary)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (remainingTotalLiability == 0.0) Color(0xFFDCFCE7) else Color.White
                                    ) {
                                        Text(
                                            text = if (remainingTotalLiability == 0.0) "ঋণমুক্ত আলহামদুলিল্লাহ!" else "${debtPayoffProgress.toInt()}% পরিশোধিত",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (remainingTotalLiability == 0.0) Color(0xFF166534) else PaisaExpenseRed,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("মোট দেনা (Liability)", fontSize = 11.sp, color = PaisaTextSecondary)
                                        Text("৳ ${String.format(Locale.US, "%,.0f", totalDenaLiability)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PaisaExpenseRed)
                                    }
                                    Column {
                                        Text("পরিশোধিত অগ্রগতি", fontSize = 11.sp, color = PaisaTextSecondary)
                                        Text("৳ ${String.format(Locale.US, "%,.0f", totalDebtPaidOff)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PaisaIncomeGreen)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("অবশিষ্ট দেনা", fontSize = 11.sp, color = PaisaTextSecondary)
                                        Text("৳ ${String.format(Locale.US, "%,.0f", remainingTotalLiability)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = PaisaTextPrimary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { (debtPayoffProgress / 100f).toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = PaisaIncomeGreen,
                                    trackColor = Color.White
                                )
                            }
                        }
                    }

                    // 2. Individual Liabilities Section: Visualizing Remaining Balance
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "প্রতিটি দায়ের অবশিষ্ট ব্যালেন্স (${activeDebts.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PaisaTextPrimary
                            )
                            if (activeDebts.isNotEmpty()) {
                                Text(
                                    text = "লাইভ ব্যালেন্স",
                                    fontSize = 11.sp,
                                    color = PaisaTextSecondary
                                )
                            }
                        }
                    }

                    if (activeDebts.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                                border = BorderStroke(1.dp, PaisaBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PaisaIncomeGreen, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("কোন দেনা বা ঋণ সক্রিয় নেই!", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("আপনার সকল দেনা পরিশোধিত অথবা কোনো ঋণ রেকর্ড এন্ট্রি করা হয়নি।", fontSize = 11.sp, color = PaisaTextSecondary)
                                    }
                                }
                            }
                        }
                    } else {
                        items(activeDebts, key = { it.id }) { debt ->
                            // Find any payoff goal associated with this liability
                            val matchingGoal = debtPayoffGoals.find {
                                it.name.contains(debt.personName, ignoreCase = true)
                            }
                            val paidForThisLiability = matchingGoal?.currentAmount ?: 0.0
                            val remainingBalance = (debt.amount - paidForThisLiability).coerceAtLeast(0.0)
                            val progressRatio = if (debt.amount > 0) {
                                (paidForThisLiability / debt.amount).toFloat().coerceIn(0f, 1f)
                            } else {
                                1f
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("liability_card_${debt.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                                border = BorderStroke(1.dp, if (remainingBalance == 0.0) PaisaIncomeGreen.copy(alpha = 0.4f) else PaisaBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = debt.personName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = PaisaTextPrimary
                                            )
                                            if (debt.phoneNumber.isNotBlank()) {
                                                Text(text = debt.phoneNumber, fontSize = 11.sp, color = PaisaTextSecondary)
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (remainingBalance == 0.0) Color(0xFFDCFCE7) else PaisaExpenseLight
                                        ) {
                                            Text(
                                                text = if (remainingBalance == 0.0) "পরিশোধিত" else "অবশিষ্ট: ৳ ${String.format(Locale.US, "%,.0f", remainingBalance)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (remainingBalance == 0.0) Color(0xFF166534) else PaisaExpenseRed,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Visual Progress & Breakdown
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("মূল দেনা: ৳ ${String.format(Locale.US, "%,.0f", debt.amount)}", fontSize = 12.sp, color = PaisaTextSecondary)
                                        Text("পরিশোধিত: ৳ ${String.format(Locale.US, "%,.0f", paidForThisLiability)}", fontSize = 12.sp, color = PaisaIncomeGreen, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    LinearProgressIndicator(
                                        progress = { progressRatio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = if (remainingBalance == 0.0) PaisaIncomeGreen else PaisaTealPrimary,
                                        trackColor = PaisaSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Quick actions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (matchingGoal != null) {
                                            OutlinedButton(
                                                onClick = { contributingGoal = matchingGoal },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("টাকা পরিশোধ করুন", fontSize = 12.sp)
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    preselectedDebtForGoal = debt
                                                    showAddDialog = true
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = PaisaExpenseRed)
                                            ) {
                                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("পরিশোধের লক্ষ্য তৈরি করুন", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Active Debt Payoff Goals List
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "সক্রিয় ঋণ পরিশোধ লক্ষ্যসমূহ (${debtPayoffGoals.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PaisaTextPrimary
                            )
                            IconButton(onClick = {
                                preselectedDebtForGoal = null
                                showAddDialog = true
                            }) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "নতুন লক্ষ্য", tint = PaisaExpenseRed)
                            }
                        }
                    }

                    if (debtPayoffGoals.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                                border = BorderStroke(1.dp, PaisaBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.TrackChanges, contentDescription = null, tint = PaisaTextTertiary, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("এখনও কোনো ঋণ পরিশোধ লক্ষ্য তৈরি করা হয়নি", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("উপরের দায় তালিকা থেকে যেকোনো দেনার জন্য নির্দিষ্ট Debt Payoff Goal তৈরি করুন।", fontSize = 11.sp, color = PaisaTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                    } else {
                        items(debtPayoffGoals, key = { it.id }) { goal ->
                            GoalCardItem(
                                goal = goal,
                                isDebtGoal = true,
                                onDepositClick = { contributingGoal = goal },
                                onDeleteClick = {
                                    viewModel.deleteGoal(goal.id)
                                    Toast.makeText(context, "ঋণ পরিশোধ লক্ষ্য মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Goal Dialog (Supports both Savings & Debt Payoff goals)
    if (showAddDialog) {
        var isDebtGoalMode by remember { mutableStateOf(selectedTab == 1 || preselectedDebtForGoal != null) }
        var name by remember {
            mutableStateOf(
                if (preselectedDebtForGoal != null) {
                    "${preselectedDebtForGoal!!.personName} এর ঋণ পরিশোধ"
                } else ""
            )
        }
        var targetAmount by remember {
            mutableStateOf(
                if (preselectedDebtForGoal != null) {
                    preselectedDebtForGoal!!.amount.toInt().toString()
                } else ""
            )
        }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
            title = {
                Text(
                    text = if (isDebtGoalMode) "নতুন ঋণ পরিশোধ লক্ষ্য (Debt Payoff)" else "নতুন সঞ্চয় লক্ষ্য (Savings Goal)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    // Type Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PaisaSurfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isDebtGoalMode) PaisaTealPrimary else Color.Transparent)
                                .clickable { isDebtGoalMode = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "সঞ্চয় লক্ষ্য",
                                fontSize = 12.sp,
                                fontWeight = if (!isDebtGoalMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isDebtGoalMode) Color.White else PaisaTextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDebtGoalMode) PaisaExpenseRed else Color.Transparent)
                                .clickable { isDebtGoalMode = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ঋণ পরিশোধ",
                                fontSize = 12.sp,
                                fontWeight = if (isDebtGoalMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDebtGoalMode) Color.White else PaisaTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isDebtGoalMode && activeDebts.isNotEmpty()) {
                        Text("দেনার তালিকা থেকে স্বয়ংক্রিয় নির্বাচন:", style = MaterialTheme.typography.labelSmall, color = PaisaTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        var expandedDebtMenu by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedDebtMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (name.isNotBlank()) name else "দায় নির্বাচন করুন (ঐচ্ছিক)",
                                        maxLines = 1,
                                        fontSize = 12.sp
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = expandedDebtMenu,
                                onDismissRequest = { expandedDebtMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("সামগ্রিক মোট দেনা (৳ ${totalDenaLiability.toInt()})") },
                                    onClick = {
                                        name = "সকল দেনা পরিশোধের সামগ্রিক লক্ষ্য"
                                        targetAmount = totalDenaLiability.toInt().toString()
                                        expandedDebtMenu = false
                                    }
                                )
                                activeDebts.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text("${d.personName} (দেনা: ৳ ${d.amount.toInt()})") },
                                        onClick = {
                                            name = "${d.personName} এর ঋণ পরিশোধ"
                                            targetAmount = d.amount.toInt().toString()
                                            expandedDebtMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isDebtGoalMode) "ঋণের শিরোনাম / ব্যক্তির নাম" else "লক্ষ্যের নাম") },
                        placeholder = { Text(if (isDebtGoalMode) "যেমন: রহিম ভাইয়ের ঋণ পরিশোধ" else "যেমন: হজ্জ ফান্ড, ল্যাপটপ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { targetAmount = it },
                        label = { Text(if (isDebtGoalMode) "পরিশোধের টার্গেট পরিমাণ (৳)" else "টার্গেট টাকার পরিমাণ (৳)") },
                        placeholder = { Text("যেমন: 50000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
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
                        val finalGoalName = if (isDebtGoalMode && !name.startsWith(DEBT_GOAL_PREFIX)) {
                            "$DEBT_GOAL_PREFIX $name"
                        } else {
                            name
                        }
                        val goal = GoalVaultEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = viewModel.activeWorkspaceId.value,
                            name = finalGoalName,
                            targetAmount = target,
                            currentAmount = 0.0,
                            targetDateMillis = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000),
                            colorHex = if (isDebtGoalMode) "#EF4444" else "#10B981"
                        )
                        viewModel.addGoal(goal)
                        showAddDialog = false
                        Toast.makeText(
                            context,
                            if (isDebtGoalMode) "ঋণ পরিশোধ লক্ষ্য যুক্ত করা হয়েছে!" else "সঞ্চয় লক্ষ্য যুক্ত করা হয়েছে!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDebtGoalMode) PaisaExpenseRed else PaisaTealPrimary
                    )
                ) {
                    Text("সংরক্ষণ করুন")
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

    // Deposit to Goal / Pay Debt Dialog
    contributingGoal?.let { goal ->
        var depositAmount by remember { mutableStateOf("") }
        var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
        var isDepositSubmitting by remember { mutableStateOf(false) }
        val isDebt = goal.isDebtPayoff

        AlertDialog(
            onDismissRequest = { if (!isDepositSubmitting) contributingGoal = null },
            title = {
                Text(
                    text = if (isDebt) "${goal.displayCleanName} এ পরিশোধ" else "${goal.displayCleanName} এ টাকা জমা",
                    fontWeight = FontWeight.Bold
                )
            },
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

                    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
                    Text(
                        text = if (isDebt) "অবশিষ্ট দায়: ৳ ${String.format(Locale.US, "%,.0f", remaining)}" else "টার্গেট পৌঁছাতে বাকি: ৳ ${String.format(Locale.US, "%,.0f", remaining)}",
                        fontSize = 11.sp,
                        color = if (isDebt) PaisaExpenseRed else PaisaTealPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = depositAmount,
                        onValueChange = { depositAmount = it },
                        label = { Text(if (isDebt) "পরিশোধের পরিমাণ (৳)" else "জমার পরিমাণ (৳)") },
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
                        Toast.makeText(
                            context,
                            if (isDebt) "৳ $amount দেনা পরিশোধ তহবিলে জমা করা হয়েছে!" else "৳ $amount সঞ্চয় লক্ষ্যে জমা হয়েছে!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDebt) PaisaExpenseRed else PaisaTealPrimary
                    )
                ) {
                    Text(if (isDebt) "পরিশোধ নিশ্চিত করুন" else "জমা নিশ্চিত করুন")
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

@Composable
private fun GoalCardItem(
    goal: GoalVaultEntity,
    isDebtGoal: Boolean,
    onDepositClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val isDone = goal.currentAmount >= goal.targetAmount
    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isDebtGoal) PaisaExpenseRed.copy(alpha = 0.2f) else PaisaBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isDebtGoal) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PaisaExpenseLight,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "ঋণ পরিশোধ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaisaExpenseRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(goal.displayCleanName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(goal.targetDateMillis))
                    Text("টার্গেট তারিখ: $dateStr", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                if (isDone) {
                    Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = if (isDebtGoal) "দায়মুক্ত!" else "সম্পন্ন!",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isDebtGoal) "পরিশোধিত: ৳ ${String.format(Locale.US, "%,.0f", goal.currentAmount)}" else "জমা: ৳ ${String.format(Locale.US, "%,.0f", goal.currentAmount)}",
                    fontWeight = FontWeight.Bold,
                    color = if (isDebtGoal) PaisaExpenseRed else Color(0xFF10B981)
                )
                Text(
                    text = if (isDebtGoal) "অবশিষ্ট দেনা: ৳ ${String.format(Locale.US, "%,.0f", remaining)}" else "টার্গেট: ৳ ${String.format(Locale.US, "%,.0f", goal.targetAmount)}",
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDebtGoal) PaisaTextPrimary else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isDone) Color(0xFF10B981) else if (isDebtGoal) PaisaExpenseRed else MaterialTheme.colorScheme.primary,
                trackColor = PaisaSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onDepositClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDebtGoal) PaisaExpenseRed else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isDebtGoal) Icons.Default.Payment else Icons.Default.Savings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isDebtGoal) "ওয়ালেট থেকে দেনা পরিশোধ করুন" else "ওয়ালেট থেকে টাকা জমা করুন")
            }
        }
    }
}
