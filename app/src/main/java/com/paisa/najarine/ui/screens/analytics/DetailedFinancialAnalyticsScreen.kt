package com.paisa.najarine.ui.screens.analytics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.components.PaisaLogoBadge
import com.paisa.najarine.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedFinancialAnalyticsScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    val wallets by viewModel.wallets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val totalAssetSummary by viewModel.totalAssetSummary.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedTimeframe by remember { mutableStateOf("এই মাস") }
    val timeframes = listOf("এই মাস", "গত ৩০ দিন", "চলতি বছর", "সকল সময়")

    val currencyFormat = remember {
        NumberFormat.getNumberInstance(Locale("bn", "BD")).apply {
            maximumFractionDigits = 0
        }
    }

    // Filter transactions based on timeframe
    val currentTime = System.currentTimeMillis()
    val filteredTransactions = remember(transactions, selectedTimeframe) {
        when (selectedTimeframe) {
            "এই মাস" -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                val startOfMonth = cal.timeInMillis
                transactions.filter { it.dateMillis >= startOfMonth }
            }
            "গত ৩০ দিন" -> {
                val thirtyDaysAgo = currentTime - (30L * 24 * 60 * 60 * 1000)
                transactions.filter { it.dateMillis >= thirtyDaysAgo }
            }
            "চলতি বছর" -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
                val startOfYear = cal.timeInMillis
                transactions.filter { it.dateMillis >= startOfYear }
            }
            else -> transactions
        }
    }

    // Calculations
    val totalIncome = filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = filteredTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val netSavings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).coerceIn(-100.0, 100.0) else 0.0

    // Expenses grouped by Category
    val categoryExpenses = remember(filteredTransactions, categories) {
        val catMapByName = categories.associateBy { it.name.lowercase() }
        val catMapById = categories.associateBy { it.id.lowercase() }
        filteredTransactions.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .map { (catKey, txList) ->
                val total = txList.sumOf { it.amount }
                val catObj = catMapByName[catKey.lowercase()] ?: catMapById[catKey.lowercase()]
                val name = catObj?.name ?: catKey.ifBlank { "সাধারণ খরচ" }
                val colorHex = catObj?.colorHex ?: "#EF4444"
                Triple(name, total, colorHex)
            }
            .sortedByDescending { it.second }
    }

    // Dena vs Paona
    val totalPayable = debts.filter { it.type == "DENA" && !it.isSettled }.sumOf { it.amount }
    val totalReceivable = debts.filter { it.type == "PONA" && !it.isSettled }.sumOf { it.amount }

    // Net Worth & Emergency Runway
    val netWorth = totalAssetSummary.netTotalAsset
    val liquidCash = totalAssetSummary.liquidCashBank
    val monthlyBurnRate = max(totalExpense, 1.0)
    val emergencyRunwayMonths = if (monthlyBurnRate > 0) (liquidCash / monthlyBurnRate) else 0.0

    // Financial Health Score Calculation (0 - 100)
    val healthScore = remember(savingsRate, emergencyRunwayMonths, totalPayable, netWorth) {
        var score = 50
        if (savingsRate >= 20.0) score += 20 else if (savingsRate > 0) score += 10 else score -= 15
        if (emergencyRunwayMonths >= 6.0) score += 20 else if (emergencyRunwayMonths >= 3.0) score += 10
        if (totalPayable == 0.0 || totalPayable < (netWorth * 0.2)) score += 10 else score -= 10
        score.coerceIn(10, 100)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PaisaLogoBadge(
                            size = 32.dp,
                            fontSize = 16.sp,
                            cornerRadius = 10.dp,
                            borderWidth = 1.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("আর্থিক অ্যানালিটিক্স", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("বিশদ ইনসাইটস ও হেলথ রিপোর্ট", fontSize = 11.sp, color = PaisaTextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaisaSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(PaisaBackground)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timeframe Selector Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(timeframes) { tf ->
                        FilterChip(
                            selected = selectedTimeframe == tf,
                            onClick = { selectedTimeframe = tf },
                            label = { Text(tf, fontSize = 12.sp, fontWeight = if (selectedTimeframe == tf) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PaisaTealPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 1. FINANCIAL HEALTH SCORE CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "আর্থিক স্বাস্থ্য স্কোর (FINANCIAL HEALTH)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PaisaTealPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                val scoreLabel = when {
                                    healthScore >= 80 -> "চমৎকার ও স্থিতিশীল"
                                    healthScore >= 60 -> "সন্তোষজনক"
                                    else -> "উন্নতি প্রয়োজন"
                                }
                                val scoreColor = when {
                                    healthScore >= 80 -> PaisaIncomeGreen
                                    healthScore >= 60 -> Color(0xFFD97706)
                                    else -> PaisaExpenseRed
                                }
                                Text(
                                    text = scoreLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = scoreColor
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(PaisaTealContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$healthScore",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = PaisaTealPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { healthScore / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (healthScore >= 70) PaisaIncomeGreen else Color(0xFFD97706),
                            trackColor = PaisaSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "আপনার সঞ্চয় হার, লিকুইডিটি রানওয়ে এবং দেনা-সম্পদ অনুপাতের ভিত্তিতে এই স্কোর স্বয়ংক্রিয়ভাবে নির্ধারিত হয়।",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = PaisaTextSecondary
                        )
                    }
                }
            }

            // 2. CASH FLOW OVERVIEW (Income vs Expense vs Net Savings)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "নগদ প্রবাহ ($selectedTimeframe)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PaisaTextPrimary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Total Income
                            CashFlowPill(
                                label = "মোট আয়",
                                amount = "৳${currencyFormat.format(totalIncome)}",
                                color = PaisaIncomeGreen,
                                icon = Icons.Default.ArrowDownward,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            // Total Expense
                            CashFlowPill(
                                label = "মোট ব্যয়",
                                amount = "৳${currencyFormat.format(totalExpense)}",
                                color = PaisaExpenseRed,
                                icon = Icons.Default.ArrowUpward,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Net Savings & Rate Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PaisaBackground)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "নিট সঞ্চয় (বা উদ্বৃত্ত)", fontSize = 11.sp, color = PaisaTextSecondary)
                                Text(
                                    text = "${if (netSavings >= 0) "+" else ""}৳${currencyFormat.format(netSavings)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (netSavings >= 0) PaisaIncomeGreen else PaisaExpenseRed
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "সঞ্চয়ের হার (Savings Rate)", fontSize = 11.sp, color = PaisaTextSecondary)
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", savingsRate),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (savingsRate >= 20.0) PaisaIncomeGreen else if (savingsRate > 0) Color(0xFFD97706) else PaisaExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            // 3. CATEGORY-WISE EXPENSE BREAKDOWN
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ক্যাটাগরি অনুযায়ী ব্যয় বিশ্লেষণ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PaisaTextPrimary
                            )
                            Text(
                                text = "${categoryExpenses.size} টি খাত",
                                fontSize = 12.sp,
                                color = PaisaTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (categoryExpenses.isEmpty()) {
                            Text(
                                text = "এই সময়ের মধ্যে কোনো ব্যয়ের লেনদেন পাওয়া যায়নি।",
                                fontSize = 13.sp,
                                color = PaisaTextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            categoryExpenses.forEach { (catName, catTotal, colorHex) ->
                                val pct = if (totalExpense > 0) (catTotal / totalExpense) else 0.0
                                val parsedColor = try {
                                    Color(android.graphics.Color.parseColor(colorHex))
                                } catch (_: Exception) {
                                    PaisaTealPrimary
                                }

                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(parsedColor)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = catName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PaisaTextPrimary)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "৳${currencyFormat.format(catTotal)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = PaisaTextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = String.format(Locale.US, "(%.1f%%)", pct * 100),
                                                fontSize = 11.sp,
                                                color = PaisaTextSecondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    LinearProgressIndicator(
                                        progress = { pct.toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = parsedColor,
                                        trackColor = PaisaSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. LIQUIDITY, EMERGENCY RUNWAY & DEBT RATIO
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "লিকুইডিটি ও আর্থিক নিরাপত্তা সূচক",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PaisaTextPrimary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            AnalyticsMetricBox(
                                label = "জরুরী রানওয়ে",
                                value = String.format(Locale.US, "%.1f মাস", emergencyRunwayMonths),
                                subtext = "বর্তমান মাসিক খরচে",
                                icon = Icons.Default.Shield,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            AnalyticsMetricBox(
                                label = "মোট দেনা (ঋণ)",
                                value = "৳${currencyFormat.format(totalPayable)}",
                                subtext = "পাওনা: ৳${currencyFormat.format(totalReceivable)}",
                                icon = Icons.Default.Handshake,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            AnalyticsMetricBox(
                                label = "সচল ওয়ালেট",
                                value = "${wallets.size} টি",
                                subtext = "ক্যাশ, ব্যাংক ও MFS",
                                icon = Icons.Default.AccountBalanceWallet,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            AnalyticsMetricBox(
                                label = "বাজেট সীমা",
                                value = "${budgets.size} টি খাত",
                                subtext = "সক্রিয় খরচের সীমা",
                                icon = Icons.Default.TrackChanges,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. ACTIONABLE FINANCIAL RECOMMENDATIONS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaTealContainer.copy(alpha = 0.4f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("স্মার্ট আর্থিক পরামর্শ (Recommendations)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PaisaTealDark)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val adviceList = remember(savingsRate, emergencyRunwayMonths, totalPayable) {
                            val list = mutableListOf<String>()
                            if (savingsRate < 20.0) {
                                list.add("আপনার সঞ্চয়ের হার বাড়ানো প্রয়োজন। চেষ্টা করুন আয়ের অন্তত ২০% সঞ্চয় বা নিরাপদ ইসলামিক সম্পদে বিনিয়োগ করতে।")
                            } else {
                                list.add("অভিনন্দন! আপনার সঞ্চয়ের হার ২০% বা তার বেশি, যা একটি চমৎকার আর্থিক অভ্যাসের প্রমাণ।")
                            }
                            if (emergencyRunwayMonths < 3.0) {
                                list.add("আপনার জরুরী তহবিলের স্থিতি ৩ মাসের কম। অপ্রত্যাশিত খরচের সুরক্ষায় লিকুইড ক্যাশ বা ব্যাংকে ৩-৬ মাসের খরচের টাকা রাখুন।")
                            } else {
                                list.add("জরুরী তহবিলের নিরাপত্তা সন্তোষজনক। অতিরিক্ত অর্থ দীর্ঘমেয়াদী হালাল সম্পদে (সোনা/রুপা, সুদমুক্ত ডিপিএস) বিনিয়োগ করতে পারেন।")
                            }
                            if (totalPayable > 0.0) {
                                list.add("আপনার দেনা-পাওনা দ্রুত নিষ্পত্তি করুন। ইসলামে ঋণ পরিশোধকে সর্বোচ্চ অগ্রাধিকার দেওয়া হয়েছে।")
                            }
                            list
                        }

                        adviceList.forEach { adv ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", fontWeight = FontWeight.Bold, color = PaisaTealPrimary)
                                Text(
                                    text = adv,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = PaisaTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CashFlowPill(
    label: String,
    amount: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, PaisaBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, fontSize = 12.sp, color = PaisaTextSecondary, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = amount, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        }
    }
}

@Composable
fun AnalyticsMetricBox(
    label: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PaisaBackground)
            .border(1.dp, PaisaBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, fontSize = 11.sp, color = PaisaTextSecondary, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
            Text(text = subtext, fontSize = 10.sp, color = PaisaTextSecondary)
        }
    }
}
