package com.paisa.najarine.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.remote.GoldApiService
import com.paisa.najarine.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GoldAssetDialog(
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val goldApi = remember { GoldApiService.create() }

    var bhoriInput by remember { mutableStateOf("2.0") }
    var karatType by remember { mutableStateOf("22") } // 22, 21, 18
    var liveGoldPriceUsd by remember { mutableStateOf(4140.0) }
    var usdToBdt by remember { mutableStateOf(118.0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val fetchLivePrice: () -> Unit = {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val resp = withContext(Dispatchers.IO) { goldApi.getGoldPrice() }
                liveGoldPriceUsd = resp.price
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "লাইভ রেট লোড করতে ব্যর্থ (ক্যাশড রেট ব্যবহৃত হচ্ছে): ${e.localizedMessage}"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchLivePrice()
    }

    // Calculations
    // 1 troy oz = 31.1035 grams. 1 bhori = 11.664 grams.
    val pricePerGramUsd = liveGoldPriceUsd / 31.1035
    val pricePerGramBdt = pricePerGramUsd * usdToBdt
    val purityMultiplier = when (karatType) {
        "21" -> 21.0 / 22.0
        "18" -> 18.0 / 22.0
        else -> 1.0
    }
    val adjustedPricePerGramBdt = pricePerGramBdt * purityMultiplier
    val pricePerBhoriBdt = adjustedPricePerGramBdt * 11.664

    val parsedBhori = bhoriInput.toDoubleOrNull() ?: 0.0
    val totalAssetValueBdt = parsedBhori * pricePerBhoriBdt

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("স্বর্ণ ও রৌপ্য সম্পদ (Gold & Silver)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = fetchLivePrice) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Rate", tint = PaisaTealPrimary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Rate Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaTealContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("লাইভ স্পট গোল্ড রেট ($)", fontSize = 12.sp, color = PaisaTextSecondary)
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text("$${String.format("%.2f", liveGoldPriceUsd)} / oz", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PaisaTealDark)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("বর্তমান প্রতি ভরি ($karatType ক্যারেট):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PaisaTextPrimary)
                            Text("৳${String.format("%,.0f", pricePerBhoriBdt)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = PaisaTealDark)
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, fontSize = 11.sp, color = PaisaExpenseRed)
                }

                // Karat Selection
                Text("স্বর্ণের মান (Karat)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PaisaTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("22", "21", "18").forEach { k ->
                        FilterChip(
                            selected = karatType == k,
                            onClick = { karatType = k },
                            label = { Text("$k ক্যারেট", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Input Bhori quantity
                OutlinedTextField(
                    value = bhoriInput,
                    onValueChange = { bhoriInput = it },
                    label = { Text("পরিমাণ (ভরি / Bhori)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Total Value Display
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("আপনার স্বর্ণের মোট আনুমানিক বাজারমূল্য", fontSize = 12.sp, color = PaisaTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "৳${String.format("%,.2f", totalAssetValueBdt)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = PaisaTealPrimary
                        )
                        Text("($bhoriInput ভরি • ${String.format("%.2f", parsedBhori * 11.664)} গ্রাম)", fontSize = 11.sp, color = PaisaTextTertiary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("সম্পন্ন")
            }
        }
    )
}
