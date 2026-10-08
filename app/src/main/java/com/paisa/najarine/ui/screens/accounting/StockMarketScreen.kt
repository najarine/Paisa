package com.paisa.najarine.ui.screens.accounting

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.paisa.najarine.data.local.AssetEntity
import com.paisa.najarine.data.remote.*
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockMarketScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val assets by viewModel.assets.collectAsState()
    val totalAssetSummary by viewModel.totalAssetSummary.collectAsState()

    val stockAssets = remember(assets) {
        assets.filter { it.category == "STOCK" }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Portfolio, 1: Share Market, 2: Forex Rates
    var showAddDialog by remember { mutableStateOf(false) }
    var prefilledTicker by remember { mutableStateOf("") }
    var prefilledPrice by remember { mutableStateOf("") }

    val marketService = remember { MarketDataService() }
    var shareMarketOverview by remember { mutableStateOf<ShareMarketOverview?>(null) }
    var forexRates by remember { mutableStateOf<List<ForexCurrencyRate>>(emptyList()) }
    var isLoadingData by remember { mutableStateOf(false) }

    fun refreshMarketData() {
        scope.launch {
            isLoadingData = true
            try {
                shareMarketOverview = marketService.fetchShareMarketOverview()
                forexRates = marketService.fetchForexRates()
            } catch (_: Exception) {
            } finally {
                isLoadingData = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshMarketData()
    }

    val totalPortfolioValuation = totalAssetSummary.stockInvestments
    val totalInvestmentCost = stockAssets.sumOf { it.quantity * it.buyPrice }
    val totalProfitLoss = totalPortfolioValuation - totalInvestmentCost

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("শেয়ার বাজার ও ফরেক্স (Market & Forex)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("ডিএসই ইনডেক্স, শেয়ার পোর্টফোলিও ও মুদ্রা বিনিময়", fontSize = 11.sp, color = PaisaTextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshMarketData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "রিফ্রেশ করুন", tint = PaisaTealPrimary)
                    }
                    IconButton(onClick = {
                        prefilledTicker = ""
                        prefilledPrice = ""
                        showAddDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "স্টক যোগ করুন")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaisaSurface)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        prefilledTicker = ""
                        prefilledPrice = ""
                        showAddDialog = true
                    },
                    containerColor = Color(0xFF2563EB)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "যোগ করুন", tint = Color.White)
                }
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
                contentColor = Color(0xFF2563EB)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("পোর্টফোলিও (${stockAssets.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("শেয়ার বাজার (DSE)", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CurrencyExchange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ফরেক্স রেট (Forex)", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: PERSONAL PORTFOLIO
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Portfolio Summary Card
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stock_portfolio_card"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text("মোট শেয়ার পোর্টফোলিও মূল্যায়ন (সম্পদ)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "৳ ${String.format(Locale.US, "%,.0f", totalPortfolioValuation)}",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF1D4ED8)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("মোট ক্রয়মূল্য", fontSize = 11.sp, color = Color.Gray)
                                            Text("৳ ${String.format(Locale.US, "%,.0f", totalInvestmentCost)}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("লাভ / ক্ষতি (P&L)", fontSize = 11.sp, color = Color.Gray)
                                            Text(
                                                text = "${if (totalProfitLoss >= 0) "+" else ""}৳ ${String.format(Locale.US, "%,.0f", totalProfitLoss)}",
                                                fontSize = 14.sp,
                                                color = if (totalProfitLoss >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (stockAssets.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(54.dp), tint = Color(0xFF2563EB).copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("কোন শেয়ার বিনিয়োগ যুক্ত করা হয়নি", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("ডিএসই, সিএসই বা বৈশ্বিক শেয়ার বাজারের পোর্টফোলিও যুক্ত করুন। এটি আপনার মোট সম্পদে যুক্ত হবে।", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                prefilledTicker = ""
                                                prefilledPrice = ""
                                                showAddDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("শেয়ার যুক্ত করুন")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(stockAssets, key = { it.id }) { item ->
                                val value = item.quantity * item.currentPrice
                                val cost = item.quantity * item.buyPrice
                                val diff = value - cost

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, PaisaBorder)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("শেয়ার: ${item.quantity.toInt()} টি | গড় ক্রয়: ৳ ${item.buyPrice} | বর্তমান: ৳ ${item.currentPrice}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }
                                            IconButton(onClick = {
                                                viewModel.deleteAsset(item.id)
                                                Toast.makeText(context, "শেয়ার রেকর্ড মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("বর্তমান মূল্যায়ন (সম্পদ)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                Text("৳ ${String.format(Locale.US, "%,.0f", value)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("লাভ / ক্ষতি", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                Text(
                                                    text = "${if (diff >= 0) "+" else ""}৳ ${String.format(Locale.US, "%,.0f", diff)}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (diff >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: LIVE SHARE MARKET (DSE INDICES & TOP TRADED STOCKS)
                    val overview = shareMarketOverview
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Market Status Banner
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("share_market_status_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (overview?.isMarketOpen == true) Color(0xFFDCFCE7) else PaisaSurfaceVariant
                                ),
                                border = BorderStroke(1.dp, if (overview?.isMarketOpen == true) Color(0xFF86EFAC) else PaisaBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (overview?.isMarketOpen == true) PaisaIncomeGreen else Color.Gray)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = overview?.marketStatusText ?: "ঢাকা স্টক এক্সচেঞ্জ (DSE)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (overview?.isMarketOpen == true) Color(0xFF166534) else PaisaTextPrimary
                                        )
                                    }

                                    Text(
                                        text = "রবি - বৃহঃ ১০:০০ - ২:৩০",
                                        fontSize = 11.sp,
                                        color = PaisaTextSecondary
                                    )
                                }
                            }
                        }

                        // DSE Indices Grid
                        item {
                            Text("প্রধান সূচকসমূহ (Major Indices)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
                        }

                        item {
                            val indices = overview?.indices ?: emptyList()
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    indices.take(2).forEach { idx ->
                                        StockIndexCard(index = idx, modifier = Modifier.weight(1f))
                                    }
                                }
                                if (indices.size > 2) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        indices.drop(2).take(2).forEach { idx ->
                                            StockIndexCard(index = idx, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        // Top Traded Shares
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("শীর্ষ লেনদেনকৃত শেয়ার (Top Active Stocks)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
                                Text("বর্তমান মূল্য (LTP)", fontSize = 11.sp, color = PaisaTextSecondary)
                            }
                        }

                        items(overview?.topStocks ?: emptyList(), key = { it.ticker }) { stock ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stock_item_${stock.ticker}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                                border = BorderStroke(1.dp, PaisaBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stock.ticker, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E40AF))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(shape = RoundedCornerShape(4.dp), color = PaisaSurfaceVariant) {
                                                Text(stock.sector, fontSize = 10.sp, color = PaisaTextSecondary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                        Text(stock.companyName, fontSize = 12.sp, color = PaisaTextSecondary, maxLines = 1)
                                        Text("ভলিউম: ${stock.volume}", fontSize = 10.sp, color = PaisaTextTertiary)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("৳ ${String.format(Locale.US, "%.2f", stock.ltp)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
                                        val isUp = stock.change >= 0
                                        Text(
                                            text = "${if (isUp) "+" else ""}${String.format(Locale.US, "%.2f (%.2f%%)", stock.change, stock.changePercent)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUp) PaisaIncomeGreen else PaisaExpenseRed
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEFF6FF),
                                            modifier = Modifier.clickable {
                                                prefilledTicker = stock.ticker
                                                prefilledPrice = stock.ltp.toString()
                                                showAddDialog = true
                                            }
                                        ) {
                                            Text(
                                                text = "+ যুক্ত করুন",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2563EB),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: FOREX RATES & CURRENCY CONVERTER
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Currency Converter Card
                        item {
                            ForexConverterCard(rates = forexRates)
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("বৈদেশিক মুদ্রা বিনিময় হার (Forex to BDT)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
                                Text("১ ইউনিট = BDT ৳", fontSize = 11.sp, color = PaisaTextSecondary)
                            }
                        }

                        items(forexRates, key = { it.code }) { rate ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forex_rate_${rate.code}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                                border = BorderStroke(1.dp, PaisaBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(rate.countryFlag, fontSize = 28.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(rate.code, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PaisaTextPrimary)
                                            Text(rate.name, fontSize = 12.sp, color = PaisaTextSecondary)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "৳ ${String.format(Locale.US, "%.2f", rate.rateInBdt)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF0D9488)
                                        )
                                        val isUp = rate.change24hPct >= 0
                                        Text(
                                            text = "${if (isUp) "+" else ""}${String.format(Locale.US, "%.2f%%", rate.change24hPct)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isUp) PaisaIncomeGreen else PaisaExpenseRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Stock Dialog
    if (showAddDialog) {
        var ticker by remember { mutableStateOf(prefilledTicker) }
        var shares by remember { mutableStateOf("") }
        var buyPrice by remember { mutableStateOf(prefilledPrice) }
        var currentPrice by remember { mutableStateOf(prefilledPrice) }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
            title = { Text("শেয়ার যুক্ত করুন (Add Stock)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = ticker,
                        onValueChange = { ticker = it.uppercase() },
                        label = { Text("শেয়ারের প্রতীক / কোম্পানি (Ticker)") },
                        placeholder = { Text("যেমন: GP, SQURPHARMA, BRACBANK") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = shares,
                        onValueChange = { shares = it },
                        label = { Text("শেয়ারের সংখ্যা (Quantity)") },
                        placeholder = { Text("যেমন: 100") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = buyPrice,
                        onValueChange = { buyPrice = it },
                        label = { Text("গড় ক্রয়মূল্য (৳)") },
                        placeholder = { Text("যেমন: 280.50") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = currentPrice,
                        onValueChange = { currentPrice = it },
                        label = { Text("বর্তমান বাজার দর / LTP (৳)") },
                        placeholder = { Text("যেমন: 312.40") },
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
                        val qty = shares.toDoubleOrNull() ?: 0.0
                        val buy = buyPrice.toDoubleOrNull() ?: 0.0
                        val current = currentPrice.toDoubleOrNull() ?: buy
                        if (ticker.isBlank() || qty <= 0 || buy <= 0) {
                            Toast.makeText(context, "সঠিক শেয়ার প্রতীক, সংখ্যা ও মূল্য দিন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val asset = AssetEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = viewModel.activeWorkspaceId.value,
                            name = ticker,
                            category = "STOCK",
                            quantity = qty,
                            unit = "shares",
                            buyPrice = buy,
                            currentPrice = current
                        )
                        viewModel.addAsset(asset)
                        showAddDialog = false
                        Toast.makeText(context, "$ticker পোর্টফোলিওতে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
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
}

@Composable
private fun StockIndexCard(index: StockMarketIndex, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.testTag("index_${index.symbol}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
        border = BorderStroke(1.dp, PaisaBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(index.symbol, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E40AF))
            Text(index.name, fontSize = 10.sp, color = PaisaTextSecondary, maxLines = 1)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = String.format(Locale.US, "%,.2f", index.currentValue),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = PaisaTextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (index.isPositive) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (index.isPositive) PaisaIncomeGreen else PaisaExpenseRed,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${if (index.isPositive) "+" else ""}${String.format(Locale.US, "%.2f (%.2f%%)", index.changeValue, index.changePercent)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (index.isPositive) PaisaIncomeGreen else PaisaExpenseRed
                )
            }
        }
    }
}

@Composable
private fun ForexConverterCard(rates: List<ForexCurrencyRate>) {
    var inputAmount by remember { mutableStateOf("100") }
    var selectedCurrencyCode by remember { mutableStateOf("USD") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val selectedRate = rates.find { it.code == selectedCurrencyCode }
        ?: rates.firstOrNull()
        ?: ForexCurrencyRate("USD", "মার্কিন ডলার", "🇺🇸", 122.50)

    val amountDouble = inputAmount.toDoubleOrNull() ?: 0.0
    val convertedBdt = amountDouble * selectedRate.rateInBdt

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("forex_converter_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaTealContainer.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, PaisaTealPrimary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("লাইভ মুদ্রা রূপান্তরকারী (Forex Converter)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PaisaTealDark)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputAmount,
                    onValueChange = { inputAmount = it },
                    label = { Text("পরিমাণ") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box {
                    OutlinedButton(
                        onClick = { expandedDropdown = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("${selectedRate.countryFlag} ${selectedRate.code}", fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        rates.forEach { r ->
                            DropdownMenuItem(
                                text = { Text("${r.countryFlag} ${r.code} - ${r.name}") },
                                onClick = {
                                    selectedCurrencyCode = r.code
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaisaSurface)
                    .border(1.dp, PaisaBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("বাংলাদেশী টাকায় সমমূল্য:", fontSize = 11.sp, color = PaisaTextSecondary)
                    Text(
                        text = "৳ ${String.format(Locale.US, "%,.2f", convertedBdt)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = PaisaTealPrimary
                    )
                }

                Text(
                    text = "১ ${selectedRate.code} = ৳ ${selectedRate.rateInBdt}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PaisaTextSecondary
                )
            }
        }
    }
}
