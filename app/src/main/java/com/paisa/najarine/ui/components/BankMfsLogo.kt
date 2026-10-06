package com.paisa.najarine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.model.BankMfsCatalog
import com.paisa.najarine.data.model.FinancialInstitutionType
import com.paisa.najarine.data.model.InstitutionItem

@Composable
fun BankMfsLogo(
    institutionId: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val item = BankMfsCatalog.findById(institutionId)
    val cornerRadius = size * 0.28f

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        item.brandColor,
                        item.brandColor.copy(alpha = 0.85f)
                    )
                )
            )
            .border(1.dp, if (MaterialTheme.colorScheme.outline == Color.Transparent) Color.Transparent else Color.White.copy(alpha = 0.25f), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        when (item.id.lowercase()) {
            "bkash" -> {
                // bKash origami bird motif
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "bK",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = (size.value * 0.38f).sp,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
            "nagad" -> {
                // Nagad flash symbol
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Nagad",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }
            "rocket" -> {
                // Rocket symbol
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = "Rocket",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
            "upay" -> {
                Text(
                    text = "upay",
                    color = Color(0xFFFFD200),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.32f).sp
                )
            }
            "ibbl" -> {
                // Islami Bank green dome / crescent star emblem
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Mosque,
                        contentDescription = "IBBL",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(size * 0.45f)
                    )
                    Text(
                        text = "IBBL",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.22f).sp
                    )
                }
            }
            "brac" -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "BRAC",
                        tint = Color(0xFFFFCC00),
                        modifier = Modifier.size(size * 0.42f)
                    )
                    Text(
                        text = "BRAC",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.22f).sp
                    )
                }
            }
            "dbbl" -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "DBBL",
                        tint = Color(0xFFFF9900),
                        modifier = Modifier.size(size * 0.42f)
                    )
                    Text(
                        text = "DBBL",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.22f).sp
                    )
                }
            }
            "city" -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocationCity,
                        contentDescription = "City Bank",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.45f)
                    )
                    Text(
                        text = "CITY",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.20f).sp
                    )
                }
            }
            "ebl" -> {
                Text(
                    text = "EBL",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.36f).sp
                )
            }
            "sonali" -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Sonali",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.45f)
                    )
                    Text(
                        text = "সোনালী",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.20f).sp
                    )
                }
            }
            "scb" -> {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "Standard Chartered",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
            "hsbc" -> {
                Icon(
                    imageVector = Icons.Default.ChangeHistory,
                    contentDescription = "HSBC",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
            "cash" -> {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = "Cash",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
            "creditcard", "visa", "mastercard" -> {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = "Card",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
            "binance" -> {
                Icon(
                    imageVector = Icons.Default.CurrencyBitcoin,
                    contentDescription = "Crypto",
                    tint = Color.Black,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
            "paypal" -> {
                Text(
                    text = "P",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.48f).sp
                )
            }
            else -> {
                // Bank or default institution
                if (item.type == FinancialInstitutionType.BANK) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = item.name,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                } else {
                    Text(
                        text = item.shortName.take(3).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.28f).sp
                    )
                }
            }
        }
    }
}
