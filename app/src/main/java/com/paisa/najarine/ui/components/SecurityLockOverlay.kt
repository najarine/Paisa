package com.paisa.najarine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.ui.theme.*

@Composable
fun SecurityLockOverlay(
    onUnlockWithBiometrics: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        onUnlockWithBiometrics()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PaisaBackground)
            .padding(24.dp)
            .testTag("biometric_security_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Emblem with 'P' Logo Badge
            PaisaLogoBadge(
                size = 76.dp,
                fontSize = 40.sp,
                cornerRadius = 24.dp,
                borderWidth = 2.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = PaisaTealPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Paisa বায়োমেট্রিক নিরাপত্তা",
                    style = MaterialTheme.typography.titleLarge,
                    color = PaisaTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "আর্থিক ড্যাশবোর্ডে প্রবেশের জন্য ফিঙ্গারপ্রিন্ট স্ক্যান করুন অথবা ৪ সংখ্যার পিন প্রদান করুন",
                style = MaterialTheme.typography.bodySmall,
                color = PaisaTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) PaisaTealPrimary else PaisaBorder)
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    color = PaisaExpenseRed,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Biometric Quick Button
            Button(
                onClick = onUnlockWithBiometrics,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(48.dp)
                    .testTag("scan_biometric_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Fingerprint",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("ফিঙ্গারপ্রিন্ট স্ক্যান করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Numeric PIN Keypad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keys.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(PaisaSurface)
                                    .border(1.dp, PaisaBorder, CircleShape)
                                    .clickable {
                                        when (key) {
                                            "C" -> {
                                                enteredPin = ""
                                                errorMessage = null
                                            }
                                            "DEL" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    errorMessage = null
                                                }
                                            }
                                            else -> {
                                                if (enteredPin.length < 4) {
                                                    enteredPin += key
                                                    if (enteredPin.length == 4) {
                                                        val ok = onVerifyPin(enteredPin)
                                                        if (!ok) {
                                                            errorMessage = "ভুল পিন (PIN)! ডিফল্ট পিন: 1234"
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Delete",
                                        tint = PaisaTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PaisaTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ডিফল্ট পিন: 1234 (Master Settings থেকে পরিবর্তনযোগ্য)",
                fontSize = 11.sp,
                color = PaisaTextTertiary
            )
        }
    }
}
