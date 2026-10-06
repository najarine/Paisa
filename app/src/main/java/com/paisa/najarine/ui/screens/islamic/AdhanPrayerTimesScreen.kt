package com.paisa.najarine.ui.screens.islamic

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.paisa.najarine.notification.AdhanPreferences
import com.paisa.najarine.notification.AdhanScheduler
import com.paisa.najarine.notification.DefaultAdhanAudioProvider
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhanPrayerTimesScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val prayerTimings by viewModel.prayerTimings.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val isDetectingLocation by viewModel.isDetectingLocation.collectAsState()

    var isAdhanEnabled by remember { mutableStateOf(AdhanPreferences.isAdhanEnabled(context)) }
    val isAudioPlaying by DefaultAdhanAudioProvider.isPlayingState.collectAsState()
    val selectedAdhanId by AdhanPreferences.selectedAdhanIdState.collectAsState()
    val currentPlayingUrl by DefaultAdhanAudioProvider.currentPlayingUrlState.collectAsState()

    val selectedMadhab by AdhanPreferences.selectedMadhabState.collectAsState()
    val timings = prayerTimings ?: viewModel.islamicRepo.getOfflinePrayerTimings(selectedMadhab)

    // Location Permission Launcher for worldwide GPS prayer times
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.detectLocationAndRefreshPrayerTimings(context) { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "লোকেশন পারমিশন ছাড়া ডিফল্ট সময় ব্যবহার করা হচ্ছে।", Toast.LENGTH_LONG).show()
        }
    }

    // Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(context, "নোটিফিকেশন পারমিশন সফলভাবে অনুমোদিত", Toast.LENGTH_SHORT).show()
        }
    }

    // Ensure alarms are scheduled if adhan toggle is enabled
    LaunchedEffect(timings, isAdhanEnabled) {
        if (isAdhanEnabled) {
            AdhanScheduler.schedulePrayerAlarms(
                context = context,
                fajr = timings.fajr,
                dhuhr = timings.dhuhr,
                asr = timings.asr,
                maghrib = timings.maghrib,
                isha = timings.isha
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.paisa.najarine.ui.components.PaisaLogoBadge(
                            size = 32.dp,
                            fontSize = 16.sp,
                            cornerRadius = 10.dp,
                            borderWidth = 1.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("নামাজের সময় ও আযান", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(
                                text = if (userLocation != null) "অবস্থান: ${userLocation!!.displayName}" else "বিশ্বব্যাপী অটো-লোকেশন উপলব্ধ",
                                fontSize = 11.sp,
                                color = PaisaTextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "জিপিএস লোকেশন", tint = PaisaTealPrimary)
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
            // 1. WORLDWIDE GPS LOCATION DETECTION CARD
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
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PaisaTealContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = PaisaTealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (userLocation != null) userLocation!!.displayName else "বিশ্বব্যাপী যেকোনো অবস্থান",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = if (userLocation != null) "জিপিএস স্থানাঙ্ক: ${String.format(java.util.Locale.US, "%.2f, %.2f", userLocation!!.latitude, userLocation!!.longitude)}" else "পৃথিবীর যেকোনো প্রান্তে সঠিক ওয়াক্ত পেতে GPS দিন",
                                    fontSize = 11.sp,
                                    color = PaisaTextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            enabled = !isDetectingLocation
                        ) {
                            if (isDetectingLocation) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("সনাক্ত", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 2. PRIMARY ADHAN TOGGLE CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAdhanEnabled) PaisaTealContainer.copy(alpha = 0.5f) else PaisaSurface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isAdhanEnabled) PaisaTealPrimary else PaisaSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isAdhanEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                        contentDescription = null,
                                        tint = if (isAdhanEnabled) Color.White else PaisaTextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "নামাজের সময়ে আযান বাজান",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = PaisaTextPrimary
                                    )
                                    Text(
                                        text = if (isAdhanEnabled) "ওয়াক্ত হলে স্বয়ংক্রিয়ভাবে পূর্ণ আযান অডিও বাজবে" else "আযান অডিও বর্তমানে বন্ধ রয়েছে",
                                        fontSize = 12.sp,
                                        color = if (isAdhanEnabled) PaisaTealDark else PaisaTextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = isAdhanEnabled,
                                onCheckedChange = { enabled ->
                                    isAdhanEnabled = enabled
                                    AdhanPreferences.setAdhanEnabled(context, enabled)
                                    if (enabled) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        AdhanScheduler.schedulePrayerAlarms(
                                            context = context,
                                            fajr = timings.fajr,
                                            dhuhr = timings.dhuhr,
                                            asr = timings.asr,
                                            maghrib = timings.maghrib,
                                            isha = timings.isha
                                        )
                                        Toast.makeText(context, "আযান অডিও সফলভাবে চালু করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    } else {
                                        AdhanScheduler.cancelAllAlarms(context)
                                        DefaultAdhanAudioProvider.stopPlayback()
                                        Toast.makeText(context, "আযান অডিও বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PaisaTealPrimary
                                )
                            )
                        }

                        // Play/Stop Quick Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isAudioPlaying) {
                                Button(
                                    onClick = { DefaultAdhanAudioProvider.stopPlayback() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PaisaExpenseRed),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("অডিও বন্ধ করুন", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 2A. ROZA / FASTING TIMINGS CARD (UMMAH API SEHRI & IFTAR)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NightsStay,
                                        contentDescription = null,
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "রোজা ও সেহরি-ইফতারের সময়সূচি",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = PaisaTextPrimary
                                    )
                                    Text(
                                        text = "UmmahAPI লাইভ সময়সূচি",
                                        fontSize = 11.sp,
                                        color = PaisaTextSecondary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "রোজা / সাওম",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8B5CF6)
                                )
                            }
                        }

                        // Sehri and Iftar Display Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Sehri
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "আজ সেহরি শেষ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF6D28D9)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = timings.sehriEnds,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp,
                                        color = Color(0xFF4C1D95)
                                    )
                                    Text(
                                        text = "ফজর: ${timings.fajr}",
                                        fontSize = 10.sp,
                                        color = PaisaTextSecondary
                                    )
                                }
                            }

                            // Iftar
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "আজ ইফতারের সময়",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFD97706)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = timings.iftarTime,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp,
                                        color = Color(0xFFB45309)
                                    )
                                    Text(
                                        text = "মাগরিব: ${timings.maghrib}",
                                        fontSize = 10.sp,
                                        color = PaisaTextSecondary
                                    )
                                }
                            }
                        }

                        // Fasting Duas expandable
                        var showDuas by remember { mutableStateOf(false) }
                        Surface(
                            onClick = { showDuas = !showDuas },
                            shape = RoundedCornerShape(10.dp),
                            color = PaisaSurfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "রোজার নিয়ত ও ইফতারের দোয়া দেখুন",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PaisaTextPrimary
                                )
                                Icon(
                                    imageVector = if (showDuas) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = PaisaTextSecondary
                                )
                            }
                        }

                        if (showDuas) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = PaisaBackground)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("রোজার নিয়ত (উচ্চারণ):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PaisaTealPrimary)
                                        Text("নাওয়াইতু আন আছুমা গাদাম মিন শাহরি রামাদানাল মুবারাকি ফারদাল্লাকা ইয়া আল্লাহু ফাতাকাব্বাল মিন্নী, ইন্নাকা আনতাস সামীউল আলীম।", fontSize = 11.sp, color = PaisaTextSecondary)
                                    }
                                }
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = PaisaBackground)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("ইফতারের দোয়া (উচ্চারণ):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFD97706))
                                        Text("আল্লাহুম্মা লাকা ছুমতু ওয়া আলা রিযক্বিকা আফত্বারতু।", fontSize = 11.sp, color = PaisaTextSecondary)
                                        Text("অর্থ: হে আল্লাহ! তোমার জন্য রোজা রেখেছি এবং তোমার রিযিক দ্বারা ইফতার করলাম।", fontSize = 10.sp, color = PaisaTextTertiary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2B. MADHAB SELECTION CARD (Hanafi vs Shafi'i / Standard)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PaisaTealContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = PaisaTealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "মাজহাব ও আসর ওয়াক্ত নির্ধারণ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = "আসর ওয়াক্তের ছায়ার পরিমাপ অনুযায়ী সঠিক ওয়াক্ত বের করা হয়",
                                    fontSize = 11.sp,
                                    color = PaisaTextSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isHanafi = selectedMadhab.equals("Hanafi", ignoreCase = true) || selectedMadhab.contains("হানাফী")
                            val isShafi = !isHanafi

                            // Hanafi Option
                            Card(
                                modifier = Modifier
                                    .weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isHanafi) PaisaTealContainer.copy(alpha = 0.4f) else PaisaSurfaceVariant.copy(alpha = 0.35f)
                                ),
                                border = if (isHanafi && MaterialTheme.colorScheme.outline != Color.Transparent) androidx.compose.foundation.BorderStroke(1.5.dp, PaisaTealPrimary) else null,
                                onClick = {
                                    AdhanPreferences.setSelectedMadhab(context, "Hanafi")
                                    viewModel.refreshPrayerTimings("Hanafi")
                                    if (isAdhanEnabled) {
                                        AdhanScheduler.schedulePrayerAlarms(
                                            context = context,
                                            fajr = timings.fajr,
                                            dhuhr = timings.dhuhr,
                                            asr = timings.asr,
                                            maghrib = timings.maghrib,
                                            isha = timings.isha
                                        )
                                    }
                                    Toast.makeText(context, "হানাফী মাজহাব অনুযায়ী আসর ওয়াক্ত ও আযান নির্ধারিত হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("হানাফী (Hanafi)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PaisaTextPrimary)
                                        RadioButton(
                                            selected = isHanafi,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(selectedColor = PaisaTealPrimary)
                                        )
                                    }
                                    Text("বস্তুর ছায়া ২ গুণ (দ্বিগুণ)", fontSize = 10.sp, color = PaisaTextSecondary)
                                }
                            }

                            // Shafi'i / Maliki / Hanbali Option
                            Card(
                                modifier = Modifier
                                    .weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isShafi) PaisaTealContainer.copy(alpha = 0.4f) else PaisaSurfaceVariant.copy(alpha = 0.35f)
                                ),
                                border = if (isShafi && MaterialTheme.colorScheme.outline != Color.Transparent) androidx.compose.foundation.BorderStroke(1.5.dp, PaisaTealPrimary) else null,
                                onClick = {
                                    AdhanPreferences.setSelectedMadhab(context, "Shafi")
                                    viewModel.refreshPrayerTimings("Shafi")
                                    if (isAdhanEnabled) {
                                        AdhanScheduler.schedulePrayerAlarms(
                                            context = context,
                                            fajr = timings.fajr,
                                            dhuhr = timings.dhuhr,
                                            asr = timings.asr,
                                            maghrib = timings.maghrib,
                                            isha = timings.isha
                                        )
                                    }
                                    Toast.makeText(context, "শাফেয়ী/মালেকী/হাম্বলী অনুযায়ী আসর ওয়াক্ত নির্ধারিত হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("শাফেয়ী / অন্যান্য", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PaisaTextPrimary)
                                        RadioButton(
                                            selected = isShafi,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(selectedColor = PaisaTealPrimary)
                                        )
                                    }
                                    Text("বস্তুর ছায়া ১ গুণ (সমান)", fontSize = 10.sp, color = PaisaTextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // 2C. ADHAN SELECTION & AUDIO PREVIEW CARD
            item {
                val adhanList = remember {
                    listOf(
                        Triple("makkah", "মক্কা শরিফ (Makkah Al-Mukarramah)", "শেখ আলী আহমেদ মুল্লা — মসজিদুল হারাম" to "https://cdn.aladhan.com/audio/adhans/a1.mp3"),
                        Triple("madinah", "মদিনা শরিফ (Madinah Al-Munawwarah)", "শেখ এশাম বুখারি — মসজিদে নববী" to "https://cdn.aladhan.com/audio/adhans/a2.mp3"),
                        Triple("alaqsa", "মসজিদুল আকসা (Al-Aqsa Mosque)", "আল-কুদস জেরুজালেম আজান" to "https://cdn.aladhan.com/audio/adhans/a3.mp3"),
                        Triple("mishary", "কারী মিশারি রশিদ আল-আফাসি", "মিশারি আল-আফাসি (কুয়েত)" to "https://cdn.aladhan.com/audio/adhans/a4.mp3"),
                        Triple("basit", "কারী আব্দুল বাসিত আব্দুস সামাদ", "মিশরীয় বিখ্যাত ক্বারী" to "https://cdn.aladhan.com/audio/adhans/a5.mp3")
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PaisaTealContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = PaisaTealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "আজান নির্বাচন ও প্রিভিউ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = "পছন্দের আজান নির্বাচন করুন। ফজরে স্বয়ংক্রিয়ভাবে 'আস-সালাতু খাইরুম মিনান নাওম' সহ আজান বাজবে।",
                                    fontSize = 11.sp,
                                    color = PaisaTextSecondary
                                )
                            }
                        }

                        // Automatic Fajr Info Banner
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = PaisaTealContainer.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbTwilight,
                                    contentDescription = null,
                                    tint = PaisaTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "স্বয়ংক্রিয় ফজর আজান: উম্মাহ এপিআই থেকে ফজরের ওয়াক্তে 'الصَّلَاةُ خَيْرٌ مِنَ النَّوْম' যুক্ত প্রামাণ্য ফজর আজান স্বয়ংক্রিয়ভাবে চলবে।",
                                    fontSize = 11.sp,
                                    color = PaisaTealDark,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            adhanList.forEach { (id, name, pair) ->
                                val (subtitle, url) = pair
                                val isSelected = selectedAdhanId == id
                                val isThisPlaying = isAudioPlaying && currentPlayingUrl == url

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) PaisaTealContainer.copy(alpha = 0.35f) else PaisaSurfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    border = if (isSelected && MaterialTheme.colorScheme.outline != Color.Transparent) androidx.compose.foundation.BorderStroke(1.5.dp, PaisaTealPrimary) else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) PaisaTealPrimary else PaisaTealContainer.copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else PaisaTealPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = PaisaTextPrimary,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = subtitle,
                                                fontSize = 11.sp,
                                                color = PaisaTextSecondary,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Aligned Action Controls with fixed widths
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Play / Stop preview button
                                            FilledIconButton(
                                                onClick = {
                                                    if (isThisPlaying) {
                                                        DefaultAdhanAudioProvider.stopPlayback()
                                                    } else {
                                                        DefaultAdhanAudioProvider.previewAdhan(context, url, id)
                                                    }
                                                },
                                                modifier = Modifier.size(34.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = if (isThisPlaying) PaisaExpenseRed else PaisaTealPrimary
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                    contentDescription = "প্রিভিউ শুনুন",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = Color.White
                                                )
                                            }

                                            // Select Pill / Button
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(74.dp)
                                                        .height(34.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(PaisaTealPrimary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("নির্বাচিত", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = {
                                                        AdhanPreferences.setSelectedAdhan(context, id, name, url)
                                                        Toast.makeText(context, "$name আজান হিসেবে নির্বাচিত হয়েছে", Toast.LENGTH_SHORT).show()
                                                    },
                                                    contentPadding = PaddingValues(0.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .width(74.dp)
                                                        .height(34.dp)
                                                ) {
                                                    Text("সিলেক্ট", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. DAILY PRAYER SCHEDULE TABLE (Testing panel removed as requested)
            item {
                Text(
                    text = "আজকের নামাজের সময়সূচি",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PaisaTextPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                val isHanafi = selectedMadhab.equals("Hanafi", ignoreCase = true) || selectedMadhab.contains("হানাফী")
                val asrLabel = if (isHanafi) "আসর (Asr - হানাফী)" else "আসর (Asr - শাফেয়ী)"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PrayerTimeRowItem(prayerNameBn = "ফজর (Fajr)", timeStr = timings.fajr, isNext = timings.nextPrayerName.equals("fajr", ignoreCase = true))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PaisaSurfaceVariant)
                        PrayerTimeRowItem(prayerNameBn = "সূর্যোদয় (Sunrise)", timeStr = timings.sunrise, isNext = false)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PaisaSurfaceVariant)
                        PrayerTimeRowItem(prayerNameBn = "যোহর (Dhuhr)", timeStr = timings.dhuhr, isNext = timings.nextPrayerName.equals("dhuhr", ignoreCase = true))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PaisaSurfaceVariant)
                        PrayerTimeRowItem(prayerNameBn = asrLabel, timeStr = timings.asr, isNext = timings.nextPrayerName.equals("asr", ignoreCase = true))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PaisaSurfaceVariant)
                        PrayerTimeRowItem(prayerNameBn = "মাগরিব / ইফতার (Maghrib)", timeStr = timings.maghrib, isNext = timings.nextPrayerName.equals("maghrib", ignoreCase = true))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PaisaSurfaceVariant)
                        PrayerTimeRowItem(prayerNameBn = "ইশা (Isha)", timeStr = timings.isha, isNext = timings.nextPrayerName.equals("isha", ignoreCase = true))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerTimeRowItem(
    prayerNameBn: String,
    timeStr: String,
    isNext: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isNext) PaisaTealPrimary.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isNext) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PaisaTealPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = prayerNameBn,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isNext) PaisaTealPrimary else PaisaTextPrimary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timeStr,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isNext) PaisaTealPrimary else PaisaTextPrimary
            )
            if (isNext) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PaisaTealPrimary)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "পরবর্তী",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
