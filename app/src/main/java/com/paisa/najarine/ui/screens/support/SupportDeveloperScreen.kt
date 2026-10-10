package com.paisa.najarine.ui.screens.support

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.paisa.najarine.ads.AdMobManager
import com.paisa.najarine.ui.theme.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportDeveloperScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val activity = context as? Activity
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val isLoadingAd by AdMobManager.isLoadingAd.collectAsState()
    val adStatusMessage by AdMobManager.adStatusMessage.collectAsState()
    val canWatchAd by AdMobManager.canWatchAd.collectAsState()
    val cooldownRemainingSeconds by AdMobManager.cooldownRemainingSeconds.collectAsState()
    val todayAdCount by AdMobManager.todayAdCount.collectAsState()

    LaunchedEffect(Unit) {
        AdMobManager.initialize(context)
    }

    fun copyText(text: String, label: String) {
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label কপি করা হয়েছে: $text", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ডেভেলপারকে সহায়তা করুন",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
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
            // Developer Info Card with PaisaLogoBadge
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.paisa.najarine.ui.components.PaisaLogoBadge(
                            size = 54.dp,
                            fontSize = 28.sp,
                            cornerRadius = 16.dp,
                            borderWidth = 1.5.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shah Mostafa Tawsif",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = PaisaTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lead Developer & Maintainer",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = PaisaTealPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ইমেইল: najarine@gmail.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = PaisaTextSecondary
                            )
                        }
                    }
                }
            }

            // Method 1: Google AdMob Rewarded Support (Strictly Rewarded Ads Only - No Interstitial)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PaisaIncomeGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "বিনামূল্যে সহায়তা (Google AdMob)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaisaIncomeGreen
                                )
                            }

                            // Daily Limit Pill Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (todayAdCount >= AdMobManager.MAX_DAILY_ADS) PaisaExpenseLight else PaisaTealContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "আজকের কোটা: $todayAdCount/${AdMobManager.MAX_DAILY_ADS}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (todayAdCount >= AdMobManager.MAX_DAILY_ADS) PaisaExpenseRed else PaisaTealDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PaisaIncomeGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = PaisaIncomeGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "বিজ্ঞাপন দেখে বিনামূল্যে সহায়তা করুন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = "নিয়ন্ত্রিত ও নিরাপদ AdMob স্পনসরড ভিডিও ভিউ",
                                    fontSize = 12.sp,
                                    color = PaisaTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Account Safety & Cooldown Info Banner
                        if (todayAdCount >= AdMobManager.MAX_DAILY_ADS) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PaisaIncomeGreen.copy(alpha = 0.12f))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = PaisaIncomeGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "আজকের জন্য আপনার সহায়তা সীমা সম্পন্ন হয়েছে। ডেভেলপারকে সহায়তা করার জন্য আন্তরিক ধন্যবাদ! আগামীকাল পুনরায় সহায়তা করতে পারবেন।",
                                        fontSize = 12.sp,
                                        color = PaisaTextPrimary,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        } else if (cooldownRemainingSeconds > 0L) {
                            val mins = cooldownRemainingSeconds / 60
                            val secs = cooldownRemainingSeconds % 60
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PaisaSurfaceVariant)
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = PaisaTealPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = String.format(java.util.Locale.US, "পরবর্তী বিজ্ঞাপন দেখার বিরতি বাকি: %02d:%02d", mins, secs),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = PaisaTextPrimary
                                        )
                                        Text(
                                            text = "AdMob অ্যাকাউন্ট ও ইনভ্যালিড ট্রাফিক সুরক্ষায় ৩ মিনিটের বিরতি বজায় রাখা হয়।",
                                            fontSize = 11.sp,
                                            color = PaisaTextSecondary
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Ad Status feedback
                        adStatusMessage?.let { status ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PaisaSurfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = status,
                                    fontSize = 12.sp,
                                    color = PaisaTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        val isButtonActive = canWatchAd && !isLoadingAd

                        Button(
                            onClick = {
                                if (activity != null) {
                                    AdMobManager.showSupportAd(
                                        activity = activity,
                                        onAdStarted = {
                                            Toast.makeText(context, "বিজ্ঞাপন লোড হচ্ছে...", Toast.LENGTH_SHORT).show()
                                        },
                                        onAdCompleted = { message ->
                                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "অ্যাড লোড করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = isButtonActive,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PaisaTealPrimary,
                                disabledContainerColor = PaisaBorder
                            )
                        ) {
                            if (isLoadingAd) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("বিজ্ঞাপন লোড হচ্ছে...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            } else if (todayAdCount >= AdMobManager.MAX_DAILY_ADS) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("আজকের কোটা সম্পন্ন (${AdMobManager.MAX_DAILY_ADS}/${AdMobManager.MAX_DAILY_ADS})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            } else if (cooldownRemainingSeconds > 0L) {
                                val mins = cooldownRemainingSeconds / 60
                                val secs = cooldownRemainingSeconds % 60
                                Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "অপেক্ষা করুন (%02d:%02d)", mins, secs),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            } else {
                                Icon(Icons.Default.SmartDisplay, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("বিজ্ঞাপন দেখুন ও সাপোর্ট করুন (বাকি ${AdMobManager.MAX_DAILY_ADS - todayAdCount} বার)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Google AdMob Banner Ad Space (Ad Unit: ca-app-pub-5571621417978572/1238461366)
            item {
                AdMobBannerCard(adUnitId = AdMobManager.BANNER_AD_UNIT_ID)
            }

            // Google AdMob Sponsor Note
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "বিজ্ঞাপন সহায়তা নীতিমালা",
                            style = MaterialTheme.typography.labelSmall,
                            color = PaisaTextSecondary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Paisa সম্পূর্ণ হালাল ও স্বচ্ছ আর্থিক টুল। কোনো জোরপূর্বক বা অনুপযুক্ত বিজ্ঞাপন প্রদর্শিত হয় না। আপনি স্বেচ্ছায় উপরের বাটন চেপে বিজ্ঞাপন দেখে ডেভেলপমেন্ট ফান্ডে অবদান রাখতে পারেন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = PaisaTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Method 2: Detailed Bank Information (Line-by-Line with Copy Buttons)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF00843D).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "ব্যাংক ট্রান্সফার / ডিপোজিট",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00843D)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00843D).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF00843D), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Eastern Bank PLC (EBL)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PaisaTextPrimary)
                                Text("সরাসরি ব্যাংক অ্যাকাউন্ট তথ্য", fontSize = 12.sp, color = PaisaTextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = PaisaSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Line 1: Developer Account Name
                        BankDetailRowItem(
                            label = "Account Name",
                            value = "Shah Mostafa Tawsif",
                            onCopy = { copyText("Shah Mostafa Tawsif", "Account Name") }
                        )

                        HorizontalDivider(color = PaisaSurfaceVariant.copy(alpha = 0.5f))

                        // Line 2: Account Number
                        BankDetailRowItem(
                            label = "Account Number",
                            value = "1141510306652",
                            onCopy = { copyText("1141510306652", "Account Number") }
                        )

                        HorizontalDivider(color = PaisaSurfaceVariant.copy(alpha = 0.5f))

                        // Line 3: Branch / Shakha
                        BankDetailRowItem(
                            label = "Shakha / Branch",
                            value = "Shantinagar Branch, Dhaka",
                            onCopy = { copyText("Shantinagar Branch, Dhaka", "Branch") }
                        )

                        HorizontalDivider(color = PaisaSurfaceVariant.copy(alpha = 0.5f))

                        // Line 4: Routing Number
                        BankDetailRowItem(
                            label = "Routing Number",
                            value = "095276344",
                            onCopy = { copyText("095276344", "Routing Number") }
                        )
                    }
                }
            }

            // Method 3: Mobile Financial Services (bKash, Rocket, Nagad) - All: 01671044633
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "MFS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PaisaTextPrimary
                        )
                        Text(
                            text = "যেকোনো MFS থেকে সরাসরি সেন্ড মানি করুন:",
                            fontSize = 12.sp,
                            color = PaisaTextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // bKash Row
                        MfsRowItem(
                            title = "বিকাশ (bKash Personal)",
                            number = "01671044633",
                            badgeColor = Color(0xFFE2136E),
                            icon = Icons.Default.Payments,
                            onCopy = { copyText("01671044633", "বিকাশ নম্বর") }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = PaisaSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Nagad Row
                        MfsRowItem(
                            title = "নগদ (Nagad Personal)",
                            number = "01671044633",
                            badgeColor = Color(0xFFF7941D),
                            icon = Icons.Default.AccountBalanceWallet,
                            onCopy = { copyText("01671044633", "নগদ নম্বর") }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = PaisaSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Rocket Row
                        MfsRowItem(
                            title = "রকেট (Rocket Personal)",
                            number = "01671044633",
                            badgeColor = Color(0xFF8C3494),
                            icon = Icons.Default.PhoneAndroid,
                            onCopy = { copyText("01671044633", "রকেট নম্বর") }
                        )
                    }
                }
            }

            // Method 4: Buy Me a Coffee
            item {
                SupportOptionCard(
                    title = "Buy Me a Coffee",
                    subtitle = "buymeacoffee.com/najarine",
                    tag = "আন্তর্জাতিক",
                    tagColor = Color(0xFFFF813F),
                    icon = Icons.Default.LocalCafe,
                    actionLabel = "লিংক খুলুন",
                    onAction = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, "https://buymeacoffee.com/najarine".toUri())
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            copyText("https://buymeacoffee.com/najarine", "Buy Me a Coffee লিংক")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BankDetailRowItem(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = PaisaTextSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PaisaTextPrimary)
        }

        IconButton(
            onClick = onCopy,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PaisaTealContainer)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "কপি করুন",
                tint = PaisaTealPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MfsRowItem(
    title: String,
    number: String,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PaisaTextPrimary)
                Text(number, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = badgeColor)
            }
        }

        FilledTonalButton(
            onClick = onCopy,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("কপি", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SupportOptionCard(
    title: String,
    subtitle: String,
    tag: String,
    tagColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tagColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tagColor)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(tagColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tagColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTextPrimary)
                    Text(subtitle, fontSize = 12.sp, color = PaisaTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun AdMobBannerCard(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    var isAdLoaded by remember { mutableStateOf(false) }
    var adLoadError by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                            .background(PaisaTealPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = PaisaTealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Google AdMob ব্যানার স্পনসর",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PaisaTextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PaisaSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Banner Ad",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PaisaSurfaceVariant)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.runtime.key(retryKey) {
                    AndroidView(
                        modifier = Modifier.wrapContentSize(),
                        factory = { ctx ->
                            AdView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                setAdSize(AdSize.BANNER)
                                setAdUnitId(adUnitId)
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        isAdLoaded = true
                                        adLoadError = null
                                    }

                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        isAdLoaded = false
                                        adLoadError = "স্ট্যাটাস: ${error.message} (কোড ${error.code})"
                                    }
                                }
                                try {
                                    loadAd(AdRequest.Builder().build())
                                } catch (_: Throwable) {}
                            }
                        }
                    )
                }

                if (!isAdLoaded && adLoadError == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp),
                            color = PaisaTealPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AdMob স্পনসর ব্যানার লোড হচ্ছে...",
                            fontSize = 12.sp,
                            color = PaisaTextSecondary
                        )
                    }
                } else if (adLoadError != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = adLoadError ?: "ব্যানার সাময়িকভাবে উপলব্ধ নয়",
                            fontSize = 11.sp,
                            color = PaisaTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                adLoadError = null
                                retryKey++
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text("পুনরায় চেষ্টা করুন", fontSize = 11.sp, color = PaisaTealPrimary)
                        }
                    }
                }
            }
        }
    }
}
