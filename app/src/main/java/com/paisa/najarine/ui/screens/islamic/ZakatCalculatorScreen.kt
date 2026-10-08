package com.paisa.najarine.ui.screens.islamic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.local.ZakatRecordEntity
import com.paisa.najarine.notification.AutoZakatCalculatorWorker
import com.paisa.najarine.notification.AutoZakatScheduler
import com.paisa.najarine.notification.ZakatSummaryReport
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalculatorScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val totalAssetSummary by viewModel.totalAssetSummary.collectAsState()

    // Mode: Auto-calculate directly from Net Worth (default: true) vs Custom Manual Input
    var autoCalculateFromNetWorth by remember { mutableStateOf(true) }

    var customCashInHand by remember { mutableStateOf("") }
    var customBankBalance by remember { mutableStateOf("") }
    var customGoldSilverValue by remember { mutableStateOf("") }
    var customBusinessInventory by remember { mutableStateOf("") }
    var customLoansGiven by remember { mutableStateOf("") }
    var customDebtsPayable by remember { mutableStateOf("") }

    var familyMembersForFitrah by remember { mutableStateOf("1") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Background Service Report State
    var backgroundReport by remember { mutableStateOf(AutoZakatCalculatorWorker.getLatestReport(context)) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Silver Nisab in Bangladesh (approx 52.5 tola silver = ~৳ 85,000)
    val nisabThreshold = 85000.0

    // Calculations based on mode
    val netZakatPool: Double
    val zakatPayable: Double
    val cCash: Double
    val cBank: Double
    val cGold: Double
    val cInventory: Double
    val cLoans: Double
    val cDebts: Double

    if (autoCalculateFromNetWorth) {
        // AUTOMATICALLY CALCULATED FROM THE NET WORTH
        cCash = totalAssetSummary.liquidCashBank * 0.3
        cBank = totalAssetSummary.liquidCashBank * 0.7
        cGold = totalAssetSummary.preciousMetals
        cInventory = totalAssetSummary.stockInvestments + totalAssetSummary.cryptoDigital
        cLoans = totalAssetSummary.loanReceivables + totalAssetSummary.customerReceivables
        cDebts = totalAssetSummary.totalLiabilities

        // Automatically calculated directly from the live Net Worth
        netZakatPool = totalAssetSummary.netTotalAsset.coerceAtLeast(0.0)
        val isNisabReached = netZakatPool >= nisabThreshold
        zakatPayable = if (isNisabReached) netZakatPool * 0.025 else 0.0
    } else {
        // MANUAL CUSTOM INPUT
        cCash = customCashInHand.toDoubleOrNull() ?: 0.0
        cBank = customBankBalance.toDoubleOrNull() ?: 0.0
        cGold = customGoldSilverValue.toDoubleOrNull() ?: 0.0
        cInventory = customBusinessInventory.toDoubleOrNull() ?: 0.0
        cLoans = customLoansGiven.toDoubleOrNull() ?: 0.0
        cDebts = customDebtsPayable.toDoubleOrNull() ?: 0.0

        val gross = cCash + cBank + cGold + cInventory + cLoans
        netZakatPool = (gross - cDebts).coerceAtLeast(0.0)
        val isNisabReached = netZakatPool >= nisabThreshold
        zakatPayable = if (isNisabReached) netZakatPool * 0.025 else 0.0
    }

    val isNisabReached = netZakatPool >= nisabThreshold

    // Standard Fitrah rate per person in Bangladesh
    val members = familyMembersForFitrah.toIntOrNull() ?: 1
    val fitrahRate = 115.0
    val totalFitrah = members * fitrahRate

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "যাকাত ও ফিতরা ক্যালকুলেটর",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (autoCalculateFromNetWorth) "নিট সম্পদ থেকে স্বয়ংক্রিয় হিসাব (Live Net Worth)" else "কাস্টম ম্যানুয়াল হিসাব",
                            fontSize = 11.sp,
                            color = PaisaTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    // Quick Toggle between Net Worth Auto and Custom
                    IconButton(onClick = { autoCalculateFromNetWorth = !autoCalculateFromNetWorth }) {
                        Icon(
                            imageVector = if (autoCalculateFromNetWorth) Icons.Default.Sync else Icons.Default.Edit,
                            contentDescription = "মোড পরিবর্তন",
                            tint = PaisaTealPrimary
                        )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mode Selector Pill
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = BorderStroke(1.dp, PaisaBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Auto Net Worth Mode Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (autoCalculateFromNetWorth) PaisaTealPrimary else PaisaSurfaceVariant)
                                .clickable { autoCalculateFromNetWorth = true }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = if (autoCalculateFromNetWorth) Color.White else PaisaTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "নিট সম্পদ থেকে অটো",
                                    fontSize = 12.sp,
                                    fontWeight = if (autoCalculateFromNetWorth) FontWeight.Bold else FontWeight.Medium,
                                    color = if (autoCalculateFromNetWorth) Color.White else PaisaTextSecondary
                                )
                            }
                        }

                        // Manual Custom Mode Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!autoCalculateFromNetWorth) PaisaTealPrimary else PaisaSurfaceVariant)
                                .clickable {
                                    // Prepopulate fields when switching to custom mode
                                    if (customCashInHand.isEmpty()) {
                                        customCashInHand = (totalAssetSummary.liquidCashBank * 0.3).toInt().toString()
                                        customBankBalance = (totalAssetSummary.liquidCashBank * 0.7).toInt().toString()
                                        customGoldSilverValue = totalAssetSummary.preciousMetals.toInt().toString()
                                        customBusinessInventory = (totalAssetSummary.stockInvestments + totalAssetSummary.cryptoDigital).toInt().toString()
                                        customLoansGiven = (totalAssetSummary.loanReceivables + totalAssetSummary.customerReceivables).toInt().toString()
                                        customDebtsPayable = totalAssetSummary.totalLiabilities.toInt().toString()
                                    }
                                    autoCalculateFromNetWorth = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = if (!autoCalculateFromNetWorth) Color.White else PaisaTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "কাস্টম ইনপুট মোড",
                                    fontSize = 12.sp,
                                    fontWeight = if (!autoCalculateFromNetWorth) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!autoCalculateFromNetWorth) Color.White else PaisaTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 1. Net Zakat Payable Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("zakat_summary_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNisabReached) PaisaIslamicGreen else Color(0xFF1E293B)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isNisabReached) "আপনার প্রদেয় যাকাত (২.৫%)" else "নেসাব পূরণ হয়নি (যাকাত আবশ্যক নয়)",
                                fontWeight = FontWeight.Bold,
                                color = if (isNisabReached) Color(0xFFD1FAE5) else Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )

                            if (autoCalculateFromNetWorth) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF34D399))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("লাইভ নেট ওর্থ", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "৳ ${String.format(Locale.US, "%,.0f", zakatPayable)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("যাকাতযোগ্য নিট সম্পদ:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                                Text("৳ ${String.format(Locale.US, "%,.0f", netZakatPool)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("রৌপ্য নেসাব (৫২.৫ তোলা):", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                                Text("৳ ${String.format(Locale.US, "%,.0f", nisabThreshold)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE68A))
                            }
                        }
                    }
                }
            }

            // 2. BACKGROUND SERVICE & SUMMARY REPORT CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auto_zakat_background_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaTealContainer.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, PaisaTealPrimary.copy(alpha = 0.3f))
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
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(PaisaTealPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("স্বয়ংক্রিয় ব্যাকগ্রাউন্ড সার্ভিস", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PaisaTealDark)
                                    Text("নিয়মিত নিট সম্পদ ট্র্যাকিং ও রিপোর্ট", fontSize = 10.sp, color = PaisaTextSecondary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PaisaIncomeLight
                            ) {
                                Text(
                                    text = "২৪ ঘণ্টা সক্রিয়",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaisaIncomeGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val report = backgroundReport
                        if (report != null) {
                            val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(report.calculatedAtMillis))
                            Text("সর্বশেষ ব্যাকগ্রাউন্ড মূল্যায়ন: $timeStr", fontSize = 11.sp, color = PaisaTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("মূল্যায়িত নিট সম্পদ: ৳ ${String.format(Locale.US, "%,.0f", report.netWorth)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PaisaTextPrimary)
                                Text("প্রদেয় যাকাত: ৳ ${String.format(Locale.US, "%,.0f", report.zakatPayable)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PaisaTealPrimary)
                            }
                        } else {
                            Text(
                                text = "অ্যাপে সংরক্ষিত ওয়ালেট, সম্পদ ও দেনা ট্র্যাক করে ব্যাকগ্রাউন্ড সার্ভিস স্বয়ংক্রিয়ভাবে যাকাত নির্ধারণ ও সামারি রিপোর্ট তৈরি করে।",
                                fontSize = 11.sp,
                                color = PaisaTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    AutoZakatScheduler.triggerImmediateCalculation(context)
                                    Toast.makeText(context, "ব্যাকগ্রাউন্ড যাকাত হিসাব শুরু হয়েছে...", Toast.LENGTH_SHORT).show()
                                    scope.launch {
                                        delay(1200)
                                        backgroundReport = AutoZakatCalculatorWorker.getLatestReport(context)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("হিসাব রিফ্রেশ", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    if (backgroundReport == null) {
                                        AutoZakatScheduler.triggerImmediateCalculation(context)
                                        scope.launch {
                                            delay(1000)
                                            backgroundReport = AutoZakatCalculatorWorker.getLatestReport(context)
                                            showReportDialog = true
                                        }
                                    } else {
                                        showReportDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("সামারি রিপোর্ট", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (autoCalculateFromNetWorth) {
                // 3. LIVE NET WORTH BREAKDOWN CARD
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("net_worth_breakdown_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                        border = BorderStroke(1.dp, PaisaBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("অ্যাকাউন্টিং ইঞ্জিন লাইভ নিট ওর্থ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
                                }
                                Text("মোট: ৳ ${String.format(Locale.US, "%,.0f", totalAssetSummary.netTotalAsset)}", fontWeight = FontWeight.Bold, color = PaisaTealPrimary, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Asset items breakdown
                            ZakatAssetRow("নগদ টাকা ও ব্যাংক/MFS ওয়ালেট", totalAssetSummary.liquidCashBank, isDeduction = false)
                            ZakatAssetRow("স্বর্ণ ও মূল্যবান ধাতু (Gold/Silver)", totalAssetSummary.preciousMetals, isDeduction = false)
                            ZakatAssetRow("শেয়ার বাজার ও ক্রিপ্টো সম্পদ", totalAssetSummary.stockInvestments + totalAssetSummary.cryptoDigital, isDeduction = false)
                            ZakatAssetRow("ডিপিএস ও অন্যান্য সেভিংস স্কিম", totalAssetSummary.fdrDpsSavings, isDeduction = false)
                            ZakatAssetRow("পাওনা ঋণ ও বাকি কাস্টমার লেজার", totalAssetSummary.loanReceivables + totalAssetSummary.customerReceivables, isDeduction = false)
                            ZakatAssetRow("বাদ: সক্রিয় দেনা ও ক্রেডিট কার্ড দায়", totalAssetSummary.totalLiabilities, isDeduction = true)

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = PaisaBorder)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("সর্বমোট নিট সম্পদ (Net Worth)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PaisaTextPrimary)
                                Text("৳ ${String.format(Locale.US, "%,.0f", totalAssetSummary.netTotalAsset)}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = PaisaTealPrimary)
                            }
                        }
                    }
                }
            } else {
                // 4. CUSTOM MANUAL INPUT FIELDS
                item {
                    Text("যাকাতযোগ্য সম্পদের কাস্টম বিবরণ (টাকা)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
                }

                item {
                    OutlinedTextField(
                        value = customCashInHand,
                        onValueChange = { customCashInHand = it },
                        label = { Text("হাতের নগদ টাকা (Cash in Hand)") },
                        placeholder = { Text("৳ ০") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = customBankBalance,
                        onValueChange = { customBankBalance = it },
                        label = { Text("ব্যাংক ও ওয়ালেট ব্যালেন্স (Bank / MFS)") },
                        placeholder = { Text("৳ ০") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = customGoldSilverValue,
                        onValueChange = { customGoldSilverValue = it },
                        label = { Text("স্বর্ণ ও রৌপ্যের বর্তমান বাজারমূল্য") },
                        placeholder = { Text("৳ ০") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = customBusinessInventory,
                        onValueChange = { customBusinessInventory = it },
                        label = { Text("ব্যবসায়িক পণ্যের বিক্রয়যোগ্য মজুদ (Inventory / Stocks)") },
                        placeholder = { Text("৳ ০") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = customLoansGiven,
                        onValueChange = { customLoansGiven = it },
                        label = { Text("প্রাপ্য পাওনা টাকা (Receivables)") },
                        placeholder = { Text("৳ ০") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = customDebtsPayable,
                        onValueChange = { customDebtsPayable = it },
                        label = { Text("তাত্ক্ষণিক প্রদেয় ঋণ বা দেনা (বাদ যাবে)") },
                        placeholder = { Text("৳ ০") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // 5. Fitrah Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("সাদাকাতুল ফিতর (ফিতরা হিসাব)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = BorderStroke(1.dp, PaisaBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("মোট প্রদেয় ফিতরা:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("৳ ${String.format(Locale.US, "%,.0f", totalFitrah)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PaisaTealPrimary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ইসলামিক ফাউন্ডেশন নির্ধারিত ন্যূনতম গম/আটার হার অনুযায়ী জনপ্রতি ১১৫ টাকা।",
                            style = MaterialTheme.typography.bodySmall,
                            color = PaisaTextSecondary,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = familyMembersForFitrah,
                            onValueChange = { familyMembersForFitrah = it },
                            label = { Text("পরিবারের সদস্য সংখ্যা") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // 6. Save Record Action
            item {
                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        if (isSubmitting) return@Button
                        isSubmitting = true
                        scope.launch {
                            viewModel.islamicRepo.saveZakatCalculation(
                                ZakatRecordEntity(
                                    id = UUID.randomUUID().toString(),
                                    calculatedDateMillis = System.currentTimeMillis(),
                                    cashAmount = cCash,
                                    bankAmount = cBank,
                                    goldValue = cGold,
                                    silverValue = 0.0,
                                    businessStockValue = cInventory,
                                    receivables = cLoans,
                                    liabilitiesDue = cDebts,
                                    totalZakatable = netZakatPool,
                                    zakatPayable = zakatPayable,
                                    isPaid = false
                                )
                            )
                            Toast.makeText(context, "যাকাত হিসাব নিরাপদে সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                            isSubmitting = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSubmitting) "সংরক্ষণ হচ্ছে..." else "যাকাত রেকর্ড সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Full Summary Report Dialog
    if (showReportDialog) {
        val report = backgroundReport
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = PaisaTealPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("যাকাত সামারি রিপোর্ট", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = report?.formattedReportText ?: "সামারি রিপোর্ট প্রস্তুত হচ্ছে...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = PaisaTextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PaisaSurfaceVariant)
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val textToCopy = report?.formattedReportText ?: ""
                        if (textToCopy.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Zakat Summary Report", textToCopy)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "রিপোর্ট ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("কপি করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showReportDialog = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }
}

@Composable
private fun ZakatAssetRow(
    title: String,
    amount: Double,
    isDeduction: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 12.sp, color = PaisaTextSecondary)
        Text(
            text = "${if (isDeduction) "- " else ""}৳ ${String.format(Locale.US, "%,.0f", amount)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDeduction) PaisaExpenseRed else PaisaTextPrimary
        )
    }
}
