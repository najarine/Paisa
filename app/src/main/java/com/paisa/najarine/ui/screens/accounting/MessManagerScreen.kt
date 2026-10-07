package com.paisa.najarine.ui.screens.accounting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.local.MessEntryEntity
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class MemberMessSummary(
    val memberName: String,
    val totalMeals: Double,
    val totalDeposit: Double,
    val totalBazarSpent: Double,
    val totalFixedShare: Double,
    val mealCost: Double,
    val totalIndividualCost: Double,
    val netBalance: Double // > 0 -> Refund/পাবে, < 0 -> Due/দিতে হবে
)

data class MessCalculationResult(
    val totalMeals: Double,
    val totalBazar: Double,
    val totalFixedBills: Double,
    val totalMessExpense: Double,
    val totalDeposits: Double,
    val mealRate: Double,
    val memberSummaries: List<MemberMessSummary>
)

object MessCalculationEngine {
    fun calculate(entries: List<MessEntryEntity>): MessCalculationResult {
        val totalMeals = entries.sumOf { it.mealsCount }
        val totalBazar = entries.sumOf { it.bazarExpense }
        val totalFixedBills = entries.sumOf { it.fixedExpenseShare }
        val totalMessExpense = totalBazar + totalFixedBills
        val totalDeposits = entries.sumOf { it.depositAmount }

        val mealRate = if (totalMeals > 0) totalBazar / totalMeals else 0.0

        val members = entries.map { it.memberName.trim() }.filter { it.isNotBlank() }.distinct()
        val memberCount = if (members.isNotEmpty()) members.size else 1

        val memberSummaries = members.map { member ->
            val memberEntries = entries.filter { it.memberName.trim().equals(member, ignoreCase = true) }
            val memMeals = memberEntries.sumOf { it.mealsCount }
            val memDeposit = memberEntries.sumOf { it.depositAmount }
            val memBazar = memberEntries.sumOf { it.bazarExpense }
            // If individual entries specify fixed share, use sum, otherwise split evenly
            val explicitFixed = memberEntries.sumOf { it.fixedExpenseShare }
            val memFixedShare = if (explicitFixed > 0) explicitFixed else (totalFixedBills / memberCount)

            val mealCost = memMeals * mealRate
            val totalCost = mealCost + memFixedShare
            val netBalance = (memDeposit + memBazar) - totalCost

            MemberMessSummary(
                memberName = member,
                totalMeals = memMeals,
                totalDeposit = memDeposit,
                totalBazarSpent = memBazar,
                totalFixedShare = memFixedShare,
                mealCost = mealCost,
                totalIndividualCost = totalCost,
                netBalance = netBalance
            )
        }.sortedByDescending { it.totalMeals }

        return MessCalculationResult(
            totalMeals = totalMeals,
            totalBazar = totalBazar,
            totalFixedBills = totalFixedBills,
            totalMessExpense = totalMessExpense,
            totalDeposits = totalDeposits,
            mealRate = mealRate,
            memberSummaries = memberSummaries
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessManagerScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    onNavigateToBazarShodai: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val messEntries by viewModel.messEntries.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Summary, 1: Meals, 2: Bazar, 3: Deposits & Bills
    var showAddEntryDialog by remember { mutableStateOf(false) }
    var dialogEntryType by remember { mutableStateOf("MEAL") } // "MEAL", "BAZAR", "DEPOSIT", "FIXED"

    val calculation = remember(messEntries) {
        MessCalculationEngine.calculate(messEntries)
    }

    val currencyFormat = remember {
        NumberFormat.getNumberInstance(Locale("bn", "BD")).apply {
            maximumFractionDigits = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("মেস ও হোস্টেল মিল ম্যানেজার", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            text = "মিল রেট: ৳${String.format(Locale.US, "%.2f", calculation.mealRate)} • মোট মিল: ${if (calculation.totalMeals % 1.0 == 0.0) calculation.totalMeals.toInt() else calculation.totalMeals}",
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
                    IconButton(onClick = onNavigateToBazarShodai) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Bazar List", tint = PaisaTealPrimary)
                    }

                    IconButton(onClick = {
                        val report = buildString {
                            appendLine("🏢 পয়সা (PAISA) মেস মিল ও হিসাব বিবরণী")
                            appendLine("====================================")
                            appendLine("মোট মিল: ${calculation.totalMeals}")
                            appendLine("মোট বাজার খরচ: ৳${currencyFormat.format(calculation.totalBazar)}")
                            appendLine("অন্যান্য ফিক্সড বিল: ৳${currencyFormat.format(calculation.totalFixedBills)}")
                            appendLine("মোট মেস খরচ: ৳${currencyFormat.format(calculation.totalMessExpense)}")
                            appendLine("হিসাবকৃত মিল রেট: ৳${String.format(Locale.US, "%.2f", calculation.mealRate)}")
                            appendLine("------------------------------------")
                            appendLine("সদস্যভিত্তিক চূড়ান্ত হিসাব:")
                            calculation.memberSummaries.forEach { mem ->
                                val status = if (mem.netBalance >= 0) "পাবে (Refund): +৳${currencyFormat.format(mem.netBalance)}" else "দিতে হবে (Due): -৳${currencyFormat.format(-mem.netBalance)}"
                                appendLine("👤 ${mem.memberName}: মিল=${mem.totalMeals}, জমা=৳${mem.totalDeposit.toInt()}, বাজার=৳${mem.totalBazarSpent.toInt()} => $status")
                            }
                            appendLine("====================================")
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Mess Report", report))
                        Toast.makeText(context, "মেস রিপোর্ট কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = PaisaTealPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaisaSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    dialogEntryType = when (selectedTab) {
                        1 -> "MEAL"
                        2 -> "BAZAR"
                        3 -> "DEPOSIT"
                        else -> "MEAL"
                    }
                    showAddEntryDialog = true
                },
                containerColor = PaisaTealPrimary,
                modifier = Modifier.testTag("add_mess_entry_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PaisaBackground)
                .padding(padding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = PaisaSurface,
                contentColor = PaisaTealPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("সার্বিক সামারি", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("মিল খাতা", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("বাজার খরচ", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("জমা ও বিল", fontSize = 11.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: SUMMARY & MEMBER SETTLEMENT
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Overview Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("বর্তমান মিল রেট (Meal Rate)", fontSize = 12.sp, color = PaisaTextSecondary)
                                            Text(
                                                text = "৳ ${String.format(Locale.US, "%.2f", calculation.mealRate)}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 24.sp,
                                                color = PaisaTealPrimary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFECFDF5),
                                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("মোট মিল", fontSize = 10.sp, color = Color(0xFF047857))
                                                Text(
                                                    text = "${if (calculation.totalMeals % 1.0 == 0.0) calculation.totalMeals.toInt() else calculation.totalMeals}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color(0xFF047857)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = PaisaBorderSubtle)
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("মোট বাজার", fontSize = 11.sp, color = PaisaTextSecondary)
                                            Text("৳ ${currencyFormat.format(calculation.totalBazar)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        Column {
                                            Text("অন্যান্য বিল", fontSize = 11.sp, color = PaisaTextSecondary)
                                            Text("৳ ${currencyFormat.format(calculation.totalFixedBills)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("মোট মেস খরচ", fontSize = 11.sp, color = PaisaTextSecondary)
                                            Text("৳ ${currencyFormat.format(calculation.totalMessExpense)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PaisaExpenseRed)
                                        }
                                    }
                                }
                            }
                        }

                        // Bazar Shortcut Banner
                        item {
                            Surface(
                                onClick = onNavigateToBazarShodai,
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("বাজার সদাই তালিকা খুলুন", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                                            Text("মেসের প্রয়োজনীয় সদাই চেকলিস্ট ও মূল্য ট্র্যাক করুন", fontSize = 11.sp, color = Color(0xFFB45309))
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFD97706))
                                }
                            }
                        }

                        // Member Final Settlement List
                        item {
                            Text(
                                text = "সদস্যদের চূড়ান্ত পাওনা ও দেনা হিসাব (${calculation.memberSummaries.size} জন)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PaisaTextPrimary
                            )
                        }

                        if (calculation.memberSummaries.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("এখনও কোনো মেস সদস্য বা মিল যোগ করা হয়নি।", color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(calculation.memberSummaries, key = { it.memberName }) { mem ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                                    border = BorderStroke(1.dp, if (mem.netBalance >= 0) Color(0xFFA7F3D0) else Color(0xFFFECACA))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(if (mem.netBalance >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (mem.netBalance >= 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                        contentDescription = null,
                                                        tint = if (mem.netBalance >= 0) Color(0xFF166534) else Color(0xFF991B1B),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(mem.memberName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    Text("মোট মিল: ${mem.totalMeals}", fontSize = 11.sp, color = PaisaTextSecondary)
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = if (mem.netBalance >= 0) "+ ৳ ${currencyFormat.format(mem.netBalance)}" else "- ৳ ${currencyFormat.format(-mem.netBalance)}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp,
                                                    color = if (mem.netBalance >= 0) PaisaIncomeGreen else PaisaExpenseRed
                                                )
                                                Text(
                                                    text = if (mem.netBalance >= 0) "পাবে (Refund)" else "দিতে হবে (Due)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (mem.netBalance >= 0) Color(0xFF166534) else Color(0xFF991B1B)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("জমা: ৳${currencyFormat.format(mem.totalDeposit)}", fontSize = 10.sp, color = PaisaTextSecondary)
                                            Text("বাজার খরচ: ৳${currencyFormat.format(mem.totalBazarSpent)}", fontSize = 10.sp, color = PaisaTextSecondary)
                                            Text("মিল খরচ: ৳${currencyFormat.format(mem.mealCost)}", fontSize = 10.sp, color = PaisaTextSecondary)
                                            Text("ফিক্সড বিল: ৳${currencyFormat.format(mem.totalFixedShare)}", fontSize = 10.sp, color = PaisaTextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: MEAL ENTRIES
                    val mealEntries = remember(messEntries) { messEntries.filter { it.mealsCount > 0 } }
                    if (mealEntries.isEmpty()) {
                        EmptyMessSection(
                            title = "কোনো মিল এন্ট্রি নেই",
                            subtitle = "দৈনিক মিল ইনপুট দিতে নিচের + বোতামে চাপুন।",
                            onAddClick = {
                                dialogEntryType = "MEAL"
                                showAddEntryDialog = true
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(mealEntries, key = { it.id }) { entry ->
                                MessEntryCard(
                                    entry = entry,
                                    type = "MEAL",
                                    onDelete = { viewModel.deleteMessEntry(entry.id) }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: BAZAR ENTRIES
                    val bazarEntries = remember(messEntries) { messEntries.filter { it.bazarExpense > 0 } }
                    if (bazarEntries.isEmpty()) {
                        EmptyMessSection(
                            title = "কোনো বাজার খরচ যোগ করা হয়নি",
                            subtitle = "মেস বাজারের ভাউচার যুক্ত করতে নিচের বোতামে চাপুন।",
                            onAddClick = {
                                dialogEntryType = "BAZAR"
                                showAddEntryDialog = true
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(bazarEntries, key = { it.id }) { entry ->
                                MessEntryCard(
                                    entry = entry,
                                    type = "BAZAR",
                                    onDelete = { viewModel.deleteMessEntry(entry.id) }
                                )
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: DEPOSITS & FIXED BILLS
                    val otherEntries = remember(messEntries) { messEntries.filter { it.depositAmount > 0 || it.fixedExpenseShare > 0 } }
                    if (otherEntries.isEmpty()) {
                        EmptyMessSection(
                            title = "কোনো জমা বা ফিক্সড বিল নেই",
                            subtitle = "সদস্যের জমা বা বুয়া/ওয়াইফাই বিল যোগ করতে বোতামে চাপুন।",
                            onAddClick = {
                                dialogEntryType = "DEPOSIT"
                                showAddEntryDialog = true
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(otherEntries, key = { it.id }) { entry ->
                                MessEntryCard(
                                    entry = entry,
                                    type = if (entry.depositAmount > 0) "DEPOSIT" else "FIXED",
                                    onDelete = { viewModel.deleteMessEntry(entry.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Entry Dialog
    if (showAddEntryDialog) {
        AddMessEntryDialog(
            initialType = dialogEntryType,
            existingMembers = calculation.memberSummaries.map { it.memberName },
            onDismiss = { showAddEntryDialog = false },
            onSave = { entry ->
                viewModel.saveMessEntry(entry)
                showAddEntryDialog = false
                Toast.makeText(context, "মেস এন্ট্রি সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun EmptyMessSection(
    title: String,
    subtitle: String,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddClick, colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("নতুন হিসাব যুক্ত করুন")
            }
        }
    }
}

@Composable
private fun MessEntryCard(
    entry: MessEntryEntity,
    type: String,
    onDelete: () -> Unit
) {
    val dateStr = remember(entry.dateMillis) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("bn", "BD")).format(Date(entry.dateMillis))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when (type) {
                                "MEAL" -> Color(0xFFE0F2FE)
                                "BAZAR" -> Color(0xFFFEF3C7)
                                "DEPOSIT" -> Color(0xFFDCFCE7)
                                else -> Color(0xFFF3E8FF)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (type) {
                            "MEAL" -> Icons.Default.Restaurant
                            "BAZAR" -> Icons.Default.ShoppingCart
                            "DEPOSIT" -> Icons.Default.Savings
                            else -> Icons.Default.Receipt
                        },
                        contentDescription = null,
                        tint = when (type) {
                            "MEAL" -> Color(0xFF0369A1)
                            "BAZAR" -> Color(0xFFB45309)
                            "DEPOSIT" -> Color(0xFF15803D)
                            else -> Color(0xFF7E22CE)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(entry.memberName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(dateStr, fontSize = 10.sp, color = PaisaTextSecondary)
                    if (entry.note.isNotBlank()) {
                        Text(entry.note, fontSize = 11.sp, color = Color.DarkGray)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    when (type) {
                        "MEAL" -> {
                            Text("${entry.mealsCount} টি মিল", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0284C7))
                        }
                        "BAZAR" -> {
                            Text("৳ ${entry.bazarExpense.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PaisaExpenseRed)
                        }
                        "DEPOSIT" -> {
                            Text("৳ ${entry.depositAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PaisaIncomeGreen)
                        }
                        else -> {
                            Text("৳ ${entry.fixedExpenseShare.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF7E22CE))
                        }
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun AddMessEntryDialog(
    initialType: String,
    existingMembers: List<String>,
    onDismiss: () -> Unit,
    onSave: (MessEntryEntity) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var memberName by remember { mutableStateOf(existingMembers.firstOrNull() ?: "") }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন মেস এন্ট্রি যুক্ত করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type selector chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("MEAL" to "মিল", "BAZAR" to "বাজার খরচ", "DEPOSIT" to "টাকা জমা", "FIXED" to "ফিক্সড বিল").forEach { (tKey, tLabel) ->
                        FilterChip(
                            selected = type == tKey,
                            onClick = { type = tKey },
                            label = { Text(tLabel, fontSize = 11.sp) }
                        )
                    }
                }

                // Member Name
                OutlinedTextField(
                    value = memberName,
                    onValueChange = { memberName = it },
                    label = { Text("মেস সদস্যের নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (existingMembers.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(existingMembers) { mem ->
                            Surface(
                                onClick = { memberName = mem },
                                shape = RoundedCornerShape(6.dp),
                                color = if (memberName == mem) PaisaTealPrimary else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = mem,
                                    fontSize = 10.sp,
                                    color = if (memberName == mem) Color.White else Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Value field based on type
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = {
                        Text(
                            when (type) {
                                "MEAL" -> "মিলের সংখ্যা (যেমন ১, ২, ২.৫)"
                                "BAZAR" -> "বাজারের মোট খরচ (৳)"
                                "DEPOSIT" -> "জমার পরিমাণ (৳)"
                                else -> "ফিক্সড বিলের পরিমাণ (৳)"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("নোট (যেমন: সকালের বাজার, ওয়াইফাই বিল)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (memberName.isBlank()) return@Button
                    val parsedAmount = amountStr.toDoubleOrNull() ?: 0.0

                    val newEntry = MessEntryEntity(
                        id = UUID.randomUUID().toString(),
                        workspaceId = "personal_default",
                        memberName = memberName.trim(),
                        mealsCount = if (type == "MEAL") parsedAmount else 0.0,
                        depositAmount = if (type == "DEPOSIT") parsedAmount else 0.0,
                        bazarExpense = if (type == "BAZAR") parsedAmount else 0.0,
                        fixedExpenseShare = if (type == "FIXED") parsedAmount else 0.0,
                        dateMillis = System.currentTimeMillis(),
                        note = note.trim()
                    )
                    onSave(newEntry)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
