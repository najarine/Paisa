package com.paisa.najarine.ui.screens.islamic

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.paisa.najarine.data.local.ZakatRecordEntity
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalculatorScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val totalAssetSummary by viewModel.totalAssetSummary.collectAsState()

    var cashInHand by remember { mutableStateOf("") }
    var bankBalance by remember { mutableStateOf("") }
    var goldSilverValue by remember { mutableStateOf("") }
    var businessInventory by remember { mutableStateOf("") }
    var loansGiven by remember { mutableStateOf("") }
    var debtsPayable by remember { mutableStateOf("") }
    var familyMembersForFitrah by remember { mutableStateOf("1") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Auto-populate from live Accounting Engine if user hasn't overridden
    LaunchedEffect(totalAssetSummary) {
        if (cashInHand.isEmpty() && totalAssetSummary.liquidCashBank > 0) {
            cashInHand = (totalAssetSummary.liquidCashBank * 0.3).toInt().toString()
            bankBalance = (totalAssetSummary.liquidCashBank * 0.7).toInt().toString()
        }
        if (goldSilverValue.isEmpty() && totalAssetSummary.preciousMetals > 0) {
            goldSilverValue = totalAssetSummary.preciousMetals.toInt().toString()
        }
        if (loansGiven.isEmpty() && (totalAssetSummary.loanReceivables + totalAssetSummary.customerReceivables) > 0) {
            loansGiven = (totalAssetSummary.loanReceivables + totalAssetSummary.customerReceivables).toInt().toString()
        }
        if (debtsPayable.isEmpty() && totalAssetSummary.totalLiabilities > 0) {
            debtsPayable = totalAssetSummary.totalLiabilities.toInt().toString()
        }
    }

    val cCash = cashInHand.toDoubleOrNull() ?: (totalAssetSummary.liquidCashBank * 0.3)
    val cBank = bankBalance.toDoubleOrNull() ?: (totalAssetSummary.liquidCashBank * 0.7)
    val cGold = goldSilverValue.toDoubleOrNull() ?: totalAssetSummary.preciousMetals
    val cInventory = businessInventory.toDoubleOrNull() ?: 0.0
    val cLoans = loansGiven.toDoubleOrNull() ?: (totalAssetSummary.loanReceivables + totalAssetSummary.customerReceivables)
    val cDebts = debtsPayable.toDoubleOrNull() ?: totalAssetSummary.totalLiabilities

    val grossAssets = cCash + cBank + cGold + cInventory + cLoans
    val netZakatPool = (grossAssets - cDebts).coerceAtLeast(0.0)

    // Current standard Silver Nisab in Bangladesh (approx 52.5 tola silver = ~৳85,000)
    val nisabThreshold = 85000.0
    val isNisabReached = netZakatPool >= nisabThreshold
    val zakatPayable = if (isNisabReached) netZakatPool * 0.025 else 0.0

    // Standard Fitrah rate per person in Bangladesh (approx ৳115 - ৳2,970 based on Islamic Foundation)
    val members = familyMembersForFitrah.toIntOrNull() ?: 1
    val fitrahRate = 115.0
    val totalFitrah = members * fitrahRate

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "যাকাত ও ফিতরা ক্যালকুলেটর",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "ফিরে যান")
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
            // Net Zakat Payable Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNisabReached) PaisaIslamicGreen else PaisaSurfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (isNisabReached) "আপনার প্রদেয় যাকাত (২.৫%)" else "নেসাব পূরণ হয়নি (যাকাত আবশ্যক নয়)",
                            fontWeight = FontWeight.Bold,
                            color = if (isNisabReached) Color(0xFFD1FAE5) else PaisaTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "৳ ${String.format("%,.0f", zakatPayable)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = if (isNisabReached) Color.White else PaisaTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "মোট যাকাতযোগ্য সম্পদ: ৳ ${String.format("%,.0f", netZakatPool)} • বর্তমান রৌপ্য নেসাব: ৳ ৮৫,০০০",
                            fontSize = 11.sp,
                            color = if (isNisabReached) Color.White.copy(alpha = 0.85f) else PaisaTextSecondary
                        )
                    }
                }
            }

            // Input Fields
            item {
                Text("যাকাতযোগ্য সম্পদের বিবরণ (টাকা)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
            }

            item {
                OutlinedTextField(
                    value = cashInHand,
                    onValueChange = { cashInHand = it },
                    label = { Text("হাতের নগদ টাকা (Cash in Hand)") },
                    placeholder = { Text("৳ ০") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = bankBalance,
                    onValueChange = { bankBalance = it },
                    label = { Text("ব্যাংক ও ওয়ালেট ব্যালেন্স (Bank / MFS)") },
                    placeholder = { Text("৳ ০") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = goldSilverValue,
                    onValueChange = { goldSilverValue = it },
                    label = { Text("স্বর্ণ ও রৌপ্যের বর্তমান বাজারমূল্য") },
                    placeholder = { Text("৳ ০") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = businessInventory,
                    onValueChange = { businessInventory = it },
                    label = { Text("ব্যবসায়িক পণ্যের বিক্রয়যোগ্য মজুদ (Inventory)") },
                    placeholder = { Text("৳ ০") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = debtsPayable,
                    onValueChange = { debtsPayable = it },
                    label = { Text("তাত্ক্ষণিক প্রদেয় ঋণ বা দায় (বাদ যাবে)") },
                    placeholder = { Text("৳ ০") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Fitrah Section
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("সাদাকাতুল ফিতর (ফিতরা হিসাব)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("মোট প্রদেয় ফিতরা:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("৳ ${String.format("%,.0f", totalFitrah)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PaisaTealPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ইসলামিক ফাউন্ডেশন নির্ধারিত গম/আটার ন্যূনতম হার অনুযায়ী জনপ্রতি ১১৫ টাকা।",
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

            // Save Record Action
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
                            Toast.makeText(context, "যাকাত হিসাব নিরাপদে সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                            isSubmitting = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
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
}
