package com.paisa.najarine.ui.screens.home

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.local.WalletEntity
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.components.BankMfsLogo
import com.paisa.najarine.ui.components.DebtPayoffWidget
import com.paisa.najarine.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PaisaViewModel,
    onNavigateToWallets: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToIslamic: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onTransferClick: () -> Unit,
    onAddWalletClick: () -> Unit,
    onNavigateToFinancialAnalytics: () -> Unit = {},
    onNavigateToBudgetPlanning: () -> Unit = {},
    onNavigateToSavingsGoals: () -> Unit = {},
    onNavigateToBillsSubscriptions: () -> Unit = {},
    onNavigateToDenaPaona: () -> Unit = {},
    onNavigateToCreditCardEmi: () -> Unit = {},
    onNavigateToFdrDpsShonchoy: () -> Unit = {},
    onNavigateToGoldSilver: () -> Unit = {},
    onNavigateToStockMarket: () -> Unit = {},
    onNavigateToCryptoAssets: () -> Unit = {},
    onNavigateToCustomerLedger: () -> Unit = {},
    onNavigateToProjectInvoice: () -> Unit = {},
    onNavigateToMessManager: () -> Unit = {},
    onNavigateToBazarShodai: () -> Unit = {},
    onNavigateToCurrencyConverter: () -> Unit = {},
    onNavigateToMasterSettings: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val wallets by viewModel.wallets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val prayerTimings by viewModel.prayerTimings.collectAsState()
    val totalAssetSummary by viewModel.totalAssetSummary.collectAsState()

    // Deterministic Financial Calculations from live summary
    val netTotalWorth = totalAssetSummary.netTotalAsset
    val liquidAssets = totalAssetSummary.liquidCashBank
    val cardLiabilities = totalAssetSummary.totalLiabilities
    val totalInvestments = totalAssetSummary.totalInvestments
    val totalReceivables = totalAssetSummary.totalReceivables

    var selectedFilterCategory by remember { mutableStateOf("All") }
    val filterCategories = listOf("All", "Cash", "Banks", "MFS / Mobile", "Cards")

    val filteredWallets = remember(wallets, selectedFilterCategory) {
        when (selectedFilterCategory) {
            "Cash" -> wallets.filter { it.type == "CASH" }
            "Banks" -> wallets.filter { it.type == "BANK" }
            "MFS / Mobile" -> wallets.filter { it.type == "MFS" }
            "Cards" -> wallets.filter { it.type == "CARD" }
            else -> wallets
        }
    }

    val currencyFormat = remember {
        NumberFormat.getNumberInstance(Locale("bn", "BD")).apply {
            maximumFractionDigits = 0
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaisaBackground),
        contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp)
    ) {
        // Main Wealth Hub Overview Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "মোট সম্পদ (NET WORTH)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PaisaTextSecondary,
                            letterSpacing = 1.sp
                        )

                        Surface(
                            onClick = onNavigateToWallets,
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF3F4F6)
                        ) {
                            Text(
                                text = "Active Wallets (${wallets.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PaisaTextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "৳ ${currencyFormat.format(netTotalWorth)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = if (netTotalWorth >= 0) Color(0xFF0F766E) else Color(0xFFB91C1C)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = PaisaBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Liquid Assets
                        Column {
                            Text(
                                text = "লিকুইড ক্যাশ",
                                style = MaterialTheme.typography.bodySmall,
                                color = PaisaTextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳ ${currencyFormat.format(liquidAssets)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F766E)
                            )
                        }

                        // Receivables (Customer Ledger + Loan Paona)
                        Column {
                            Text(
                                text = "পাওনা/বাকি",
                                style = MaterialTheme.typography.bodySmall,
                                color = PaisaTextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳ ${currencyFormat.format(totalReceivables)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0284C7)
                            )
                        }

                        // Investments & Metals
                        Column {
                            Text(
                                text = "বিনিয়োগ ও সম্পদ",
                                style = MaterialTheme.typography.bodySmall,
                                color = PaisaTextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳ ${currencyFormat.format(totalInvestments)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFD97706)
                            )
                        }

                        // Liabilities
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "দায় ও দেনা",
                                style = MaterialTheme.typography.bodySmall,
                                color = PaisaTextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳ ${currencyFormat.format(cardLiabilities)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = PaisaBorder.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToFinancialAnalytics() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = PaisaTealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "বিশদ আর্থিক অ্যানালিটিক্স ও ইনসাইটস",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = PaisaTealPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = PaisaTealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Debt Payoff & Liabilities Progress Dashboard Widget
        item {
            DebtPayoffWidget(
                debts = debts,
                goals = goals,
                wallets = wallets,
                transactions = transactions,
                onNavigateToDebtGoals = onNavigateToSavingsGoals,
                onNavigateToDenaPaona = onNavigateToDenaPaona
            )
        }

        // 3. Category Filter Chips (All (5), Cash, Banks, MFS / Mobile, Cards)
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterCategories) { cat ->
                    val isSelected = selectedFilterCategory == cat
                    val countLabel = if (cat == "All") " (${wallets.size})" else ""
                    val bg = if (isSelected) Color(0xFF14532D) else Color(0xFFF3F4F6)
                    val textCol = if (isSelected) Color.White else PaisaTextPrimary

                    Surface(
                        onClick = { selectedFilterCategory = cat },
                        shape = RoundedCornerShape(16.dp),
                        color = bg,
                        border = if (!isSelected && MaterialTheme.colorScheme.outline != Color.Transparent) CardDefaults.outlinedCardBorder() else null
                    ) {
                        Text(
                            text = "$cat$countLabel",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textCol,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 4. Clean Empty State (Section 2 & 3) OR Active Wallets List
        if (wallets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PaisaTealContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = PaisaTealPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "আপনার এখনো কোনো ওয়ালেট বা লেনদেন নেই।",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PaisaTextPrimary,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Paisa শুরু হয়েছে সম্পূর্ণ শূন্য (৳০) অবস্থা থেকে। আপনার দৈনন্দিন ক্যাশ, ব্যাংক বা বিকাশ/নগদ ওয়ালেট যুক্ত করে আয়-ব্যয়ের হিসাব রাখা শুরু করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = PaisaTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onAddWalletClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("+ ওয়ালেট যোগ করুন", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onAddExpenseClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("খরচ যোগ করুন", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = onAddIncomeClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("আয় যোগ করুন", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Wallets Cards matching Snapshot 1 (Cash Pocket, bKash Personal, etc.)
            items(filteredWallets) { wallet ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onNavigateToWallets() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BankMfsLogo(
                                institutionId = wallet.institutionId,
                                size = 42.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = wallet.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = if (wallet.accountNumber.isNotEmpty()) "${wallet.type} •• ${wallet.accountNumber.takeLast(4)}" else wallet.type,
                                    fontSize = 12.sp,
                                    color = PaisaTextSecondary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "৳ ${currencyFormat.format(wallet.balance)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (wallet.balance < 0) Color(0xFFB91C1C) else Color(0xFF0F766E)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = PaisaTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4B. QUICK ACCESS FINANCIAL TOOLS HUB (Unified tools access)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                border = CardDefaults.outlinedCardBorder()
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
                                    .background(PaisaTealContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Widgets, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "আর্থিক সেবা ও প্রয়োজনীয় টুলস",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = "বাজেট, সঞ্চয়, দেনা-পাওনা ও সম্পদ ব্যবস্থাপনা",
                                    fontSize = 11.sp,
                                    color = PaisaTextSecondary
                                )
                            }
                        }

                        IconButton(onClick = onNavigateToMasterSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "সেটিংস", tint = PaisaTealPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2-Column Responsive Grid of Primary Tools
                    val quickTools = listOf(
                        Triple("বাজেট পরিকল্পনা", Icons.Default.PieChart, onNavigateToBudgetPlanning),
                        Triple("সঞ্চয় লক্ষ্য", Icons.Default.Savings, onNavigateToSavingsGoals),
                        Triple("বিল ও সাবস্ক্রিপশন", Icons.Default.CalendarMonth, onNavigateToBillsSubscriptions),
                        Triple("দেনা-পাওনা ও ঋণ", Icons.Default.Handshake, onNavigateToDenaPaona),
                        Triple("ক্রেডিট কার্ড ও EMI", Icons.Default.CreditCard, onNavigateToCreditCardEmi),
                        Triple("FDR ও সঞ্চয়পত্র", Icons.Default.AccountBalance, onNavigateToFdrDpsShonchoy),
                        Triple("স্বর্ণ ও রৌপ্য সম্পদ", Icons.Default.MonetizationOn, onNavigateToGoldSilver),
                        Triple("শেয়ার ও স্টক মার্কেট", Icons.Default.ShowChart, onNavigateToStockMarket),
                        Triple("ডিজিটাল ক্রিপ্টো", Icons.Default.CurrencyBitcoin, onNavigateToCryptoAssets),
                        Triple("কাস্টমার বাকি খাতা", Icons.Default.BusinessCenter, onNavigateToCustomerLedger),
                        Triple("মেস ও হোস্টেল ম্যানেজার", Icons.Default.Restaurant, onNavigateToMessManager),
                        Triple("বাজার সদাই তালিকা", Icons.Default.ShoppingCart, onNavigateToBazarShodai),
                        Triple("মুদ্রা রূপান্তর ও ফরেক্স", Icons.Default.CurrencyExchange, onNavigateToCurrencyConverter),
                        Triple("প্রজেক্ট ইনভয়েস", Icons.Default.Receipt, onNavigateToProjectInvoice),
                        Triple("মাস্টার সেটিংস", Icons.Default.Settings, onNavigateToMasterSettings)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        quickTools.chunked(2).forEach { rowTools ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTools.forEach { (label, icon, onClick) ->
                                    Surface(
                                        onClick = onClick,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = PaisaSurfaceVariant.copy(alpha = 0.5f),
                                        border = CardDefaults.outlinedCardBorder()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = PaisaTealPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PaisaTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (rowTools.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Islamic Prayer Summary Link Card
        item {
            prayerTimings?.let { timings ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clickable { onNavigateToIslamic() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD1FAE5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mosque, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "পরবর্তী নামাজ: ${timings.nextPrayerNameBn} (${timings.nextPrayerTime} PM)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = "ঢাকা • ${timings.hijriDateFormatted} • আযান ও কিবলা",
                                    fontSize = 12.sp,
                                    color = PaisaTextSecondary
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PaisaTextTertiary)
                    }
                }
            }
        }
    }
}
