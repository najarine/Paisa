package com.paisa.najarine.ui.screens.accounting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.local.TransactionEntity
import com.paisa.najarine.data.remote.ConversionResult
import com.paisa.najarine.data.remote.MarketDataService
import com.paisa.najarine.data.remote.SupportedCurrency
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val marketService = remember { MarketDataService() }
    val wallets by viewModel.wallets.collectAsState()

    val currencies = remember { marketService.supportedCurrencies }

    var fromCurrency by remember { mutableStateOf(currencies.first { it.code == "USD" }) }
    var toCurrency by remember { mutableStateOf(currencies.first { it.code == "BDT" }) }
    var inputAmountStr by remember { mutableStateOf("100") }

    var conversionResult by remember { mutableStateOf<ConversionResult?>(null) }
    var isConverting by remember { mutableStateOf(false) }
    var isLiveFetching by remember { mutableStateOf(false) }
    var lastUpdatedText by remember { mutableStateOf("লাইভ রেট লোড হচ্ছে...") }

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    var showQuickTransactionDialog by remember { mutableStateOf(false) }
    var transactionType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"

    var ratesSearchQuery by remember { mutableStateOf("") }
    var allRatesMap by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    val currencyFormat = remember {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("bn-BD")).apply {
            maximumFractionDigits = 2
        }
    }

    fun performConversion() {
        val amount = inputAmountStr.toDoubleOrNull() ?: 0.0
        scope.launch {
            isConverting = true
            try {
                val res = marketService.convertCurrency(
                    fromCode = fromCurrency.code,
                    toCode = toCurrency.code,
                    amount = amount
                )
                conversionResult = res
                val timeStr = SimpleDateFormat("hh:mm:ss a", Locale.forLanguageTag("bn-BD")).format(Date(res.timestampMillis))
                lastUpdatedText = "সর্বশেষ আপডেট: $timeStr"
            } catch (_: Exception) {
            } finally {
                isConverting = false
            }
        }
    }

    fun fetchRates() {
        scope.launch {
            isLiveFetching = true
            try {
                val map = marketService.fetchAllExchangeRates()
                allRatesMap = map
                performConversion()
            } catch (_: Exception) {
            } finally {
                isLiveFetching = false
            }
        }
    }

    LaunchedEffect(fromCurrency, toCurrency, inputAmountStr) {
        performConversion()
    }

    LaunchedEffect(Unit) {
        fetchRates()
    }

    var rotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(targetValue = rotationAngle, label = "swap_rotation")

    val filteredRates = remember(currencies, ratesSearchQuery, allRatesMap) {
        currencies.filter { it.code != "BDT" && (it.code.contains(ratesSearchQuery, ignoreCase = true) || it.nameBn.contains(ratesSearchQuery, ignoreCase = true) || it.nameEn.contains(ratesSearchQuery, ignoreCase = true)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("মুদ্রা রূপান্তর ও ফরেক্স (Currency Converter)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(lastUpdatedText, fontSize = 11.sp, color = PaisaTextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { fetchRates() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PaisaTealPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaisaSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PaisaBackground)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Interactive Converter Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("currency_converter_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Section Header
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
                                    Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("মুদ্রা রূপান্তর ক্যালকুলেটর", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
                                    Text("রিয়েল-টাইম লাইভ এক্সচেঞ্জ রেট", fontSize = 11.sp, color = PaisaTextSecondary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("লাইভ API", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // From Currency Row
                        CurrencySelectBox(
                            label = "যে মুদ্রা থেকে রূপান্তর করবেন (From)",
                            currency = fromCurrency,
                            amount = inputAmountStr,
                            onAmountChange = { inputAmountStr = it },
                            isEditable = true,
                            onClick = { showFromPicker = true }
                        )

                        // Quick Amount Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(listOf("10", "50", "100", "500", "1000", "5000", "10000")) { chipVal ->
                                val isSelected = inputAmountStr == chipVal
                                Surface(
                                    onClick = { inputAmountStr = chipVal },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PaisaTealPrimary else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) PaisaTealPrimary else Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "${fromCurrency.symbol}$chipVal",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // Swap Button Center
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            HorizontalDivider(modifier = Modifier.fillMaxWidth().align(Alignment.Center), color = PaisaBorderSubtle)

                            Surface(
                                onClick = {
                                    rotationAngle += 180f
                                    val temp = fromCurrency
                                    fromCurrency = toCurrency
                                    toCurrency = temp
                                },
                                shape = CircleShape,
                                color = PaisaTealPrimary,
                                shadowElevation = 3.dp,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = "Swap Currencies",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .rotate(animatedRotation)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // To Currency Row
                        val convertedDisplay = conversionResult?.convertedAmount?.let {
                            currencyFormat.format(it)
                        } ?: "..."

                        CurrencySelectBox(
                            label = "যে মুদ্রায় রূপান্তর হবে (To)",
                            currency = toCurrency,
                            amount = convertedDisplay,
                            onAmountChange = {},
                            isEditable = false,
                            onClick = { showToPicker = true }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Conversion Summary Box
                        conversionResult?.let { res ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("এক্সচেঞ্জ রেট (Exchange Rate)", fontSize = 11.sp, color = PaisaTextSecondary)
                                            Text(
                                                text = "1 ${res.fromCode} = ${String.format(Locale.US, "%.4f", res.exchangeRate)} ${res.toCode}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = PaisaTealPrimary
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("বিপরীত রেট (Inverse)", fontSize = 11.sp, color = PaisaTextSecondary)
                                            Text(
                                                text = "1 ${res.toCode} = ${String.format(Locale.US, "%.4f", res.inverseRate)} ${res.fromCode}",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                color = PaisaTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Save directly to Transactions / Remittance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    transactionType = "EXPENSE"
                                    showQuickTransactionDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PaisaExpenseRed)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("খরচে যোগ করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    transactionType = "INCOME"
                                    showQuickTransactionDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PaisaIncomeGreen)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("আয়ে যোগ করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    conversionResult?.let { res ->
                                        val text = "${res.fromAmount} ${res.fromCode} = ${currencyFormat.format(res.convertedAmount)} ${res.toCode} (রেট: 1 ${res.fromCode} = ${res.exchangeRate} ${res.toCode})"
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Currency Conversion", text))
                                        Toast.makeText(context, "রূপান্তরিত ফলাফল কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PaisaSurfaceVariant)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = PaisaTextPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // 2. All Rates Against BDT (লাইভ মুদ্রা বিনিময় তালিকা)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "শীর্ষ আন্তর্জাতিক মুদ্রার রেট (টাকা / BDT)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PaisaTextPrimary
                    )
                    Text(
                        text = "ট্যাপ করে রূপান্তর করুন",
                        fontSize = 11.sp,
                        color = PaisaTextSecondary
                    )
                }
            }

            // Search Bar for Rates
            item {
                OutlinedTextField(
                    value = ratesSearchQuery,
                    onValueChange = { ratesSearchQuery = it },
                    placeholder = { Text("মুদ্রা বা দেশের নাম দিয়ে খুঁজুন...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Currency Rates Grid / List
            items(filteredRates, key = { it.code }) { cur ->
                val usdToBdt = allRatesMap["BDT"] ?: 122.50
                val usdToCur = allRatesMap[cur.code] ?: 1.0
                val rateInBdt = if (usdToCur > 0.0) usdToBdt / usdToCur else 0.0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            fromCurrency = cur
                            toCurrency = currencies.first { it.code == "BDT" }
                            performConversion()
                            Toast.makeText(context, "${cur.nameBn} রূপান্তরের জন্য নির্বাচিত হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cur.flag, fontSize = 26.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${cur.code} - ${cur.nameBn}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = cur.nameEn,
                                    fontSize = 11.sp,
                                    color = PaisaTextSecondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "৳ ${String.format(Locale.US, "%.2f", rateInBdt)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = PaisaTealPrimary
                            )
                            Text(
                                text = "১ ${cur.code} এর মান",
                                fontSize = 10.sp,
                                color = PaisaTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    // Currency Selector Bottom Sheets
    if (showFromPicker) {
        CurrencyPickerBottomSheet(
            currencies = currencies,
            selectedCurrency = fromCurrency,
            onSelect = {
                fromCurrency = it
                showFromPicker = false
            },
            onDismiss = { showFromPicker = false }
        )
    }

    if (showToPicker) {
        CurrencyPickerBottomSheet(
            currencies = currencies,
            selectedCurrency = toCurrency,
            onSelect = {
                toCurrency = it
                showToPicker = false
            },
            onDismiss = { showToPicker = false }
        )
    }

    // Quick Add Converted Amount to Transaction Dialog
    if (showQuickTransactionDialog) {
        val convertedBdtAmount = if (toCurrency.code == "BDT") {
            conversionResult?.convertedAmount ?: 0.0
        } else {
            // Convert to BDT if toCurrency is not BDT
            val bdtRate = allRatesMap["BDT"] ?: 122.50
            val fromRate = allRatesMap[fromCurrency.code] ?: 1.0
            (inputAmountStr.toDoubleOrNull() ?: 0.0) * (bdtRate / fromRate)
        }

        var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
        var category by remember { mutableStateOf(if (transactionType == "EXPENSE") "Food & Dining" else "Salary & Income") }
        var note by remember {
            mutableStateOf(
                if (transactionType == "INCOME")
                    "রেমিট্যান্স / বৈদেশিক আয় (${inputAmountStr} ${fromCurrency.code})"
                else
                    "বৈদেশিক লেনদেন / শপিং (${inputAmountStr} ${fromCurrency.code})"
            )
        }

        AlertDialog(
            onDismissRequest = { showQuickTransactionDialog = false },
            title = {
                Text(
                    text = if (transactionType == "EXPENSE") "মুদ্রা রূপান্তর থেকে খরচ সংরক্ষণ" else "বৈদেশিক আয় / রেমিট্যান্স সংরক্ষণ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "রূপান্তরিত পরিমাণ: ৳ ${currencyFormat.format(convertedBdtAmount)} (${inputAmountStr} ${fromCurrency.code})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (transactionType == "EXPENSE") PaisaExpenseRed else PaisaIncomeGreen
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("লেনদেনের বিবরণ / নোট") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("টাকা জমার / কাটার ওয়ালেট:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(wallets) { wallet ->
                            val isSel = selectedWalletId == wallet.id
                            Surface(
                                onClick = { selectedWalletId = wallet.id },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) PaisaTealPrimary else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = wallet.name,
                                    fontSize = 11.sp,
                                    color = if (isSel) Color.White else Color.Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (transactionType == "EXPENSE") {
                            viewModel.addExpense(
                                walletId = selectedWalletId,
                                amount = convertedBdtAmount,
                                category = category,
                                fee = 0.0,
                                note = note
                            )
                            Toast.makeText(context, "খরচ সফলভাবে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addIncome(
                                walletId = selectedWalletId,
                                amount = convertedBdtAmount,
                                category = category,
                                note = note
                            )
                            Toast.makeText(context, "রেমিট্যান্স আয় সফলভাবে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                        showQuickTransactionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (transactionType == "EXPENSE") PaisaExpenseRed else PaisaIncomeGreen
                    )
                ) {
                    Text("নিশ্চিত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickTransactionDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun CurrencySelectBox(
    label: String,
    currency: SupportedCurrency,
    amount: String,
    onAmountChange: (String) -> Unit,
    isEditable: Boolean,
    onClick: () -> Unit
) {
    Column {
        Text(label, fontSize = 11.sp, color = PaisaTextSecondary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF8FAFC))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Currency selector button
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(currency.flag, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(currency.code, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Amount Display / Input
            if (isEditable) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.End,
                        color = PaisaTextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = "${currency.symbol} $amount",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.End,
                    color = PaisaTealPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerBottomSheet(
    currencies: List<SupportedCurrency>,
    selectedCurrency: SupportedCurrency,
    onSelect: (SupportedCurrency) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(currencies, searchQuery) {
        currencies.filter {
            it.code.contains(searchQuery, ignoreCase = true) ||
                    it.nameBn.contains(searchQuery, ignoreCase = true) ||
                    it.nameEn.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PaisaSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text("মুদ্রা নির্বাচন করুন (Select Currency)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("মুদ্রার কোড বা নাম খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.code }) { cur ->
                    val isSelected = cur.code == selectedCurrency.code
                    Surface(
                        onClick = { onSelect(cur) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PaisaTealContainer else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, PaisaTealPrimary) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cur.flag, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("${cur.code} - ${cur.nameBn}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(cur.nameEn, fontSize = 11.sp, color = PaisaTextSecondary)
                                }
                            }

                            Text(cur.symbol, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PaisaTealPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
