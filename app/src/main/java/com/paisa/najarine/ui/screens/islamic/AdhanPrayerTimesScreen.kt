package com.paisa.najarine.ui.screens.islamic

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
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
import com.paisa.najarine.notification.PrayerNotificationWorker
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import java.util.Locale

fun formatTo12Hour(time24: String): String {
    return try {
        val clean = time24.split(" ")[0].trim()
        val parts = clean.split(":")
        if (parts.size >= 2) {
            var hour = parts[0].toIntOrNull() ?: 0
            val minute = parts[1]
            val ampm = if (hour >= 12) "PM" else "AM"
            if (hour > 12) hour -= 12
            if (hour == 0) hour = 12
            String.format(Locale.US, "%02d:%s %s", hour, minute, ampm)
        } else {
            time24
        }
    } catch (_: Exception) {
        time24
    }
}

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
    var isPrayerNotificationsEnabled by remember { mutableStateOf(AdhanPreferences.isPrayerNotificationsEnabled(context)) }
    val isAudioPlaying by DefaultAdhanAudioProvider.isPlayingState.collectAsState()
    val selectedAdhanId by AdhanPreferences.selectedAdhanIdState.collectAsState()
    val currentPlayingUrl by DefaultAdhanAudioProvider.currentPlayingUrlState.collectAsState()

    val selectedMadhab by AdhanPreferences.selectedMadhabState.collectAsState()
    val timings = prayerTimings ?: viewModel.islamicRepo.getOfflinePrayerTimings(selectedMadhab)

    val is12Hour by AdhanPreferences.is12HourFormatState.collectAsState()
    fun formatTime(time24: String): String {
        return if (is12Hour) formatTo12Hour(time24) else time24
    }

    val nextPrayerEn = timings.nextPrayerName
    val nextPrayerBn = when(nextPrayerEn.lowercase()) {
        "fajr" -> "ফজর"
        "dhuhr" -> "যোহর"
        "asr" -> "আসর"
        "maghrib" -> "মাগরিব"
        "isha" -> "ইশা"
        else -> nextPrayerEn
    }
    val nextPrayerTime = when(nextPrayerEn.lowercase()) {
        "fajr" -> timings.fajr
        "dhuhr" -> timings.dhuhr
        "asr" -> timings.asr
        "maghrib" -> timings.maghrib
        "isha" -> timings.isha
        else -> ""
    }

    // Independent Location Permission Launcher
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

    // Independent Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(context, "নোটিফিকেশন পারমিশন সফলভাবে অনুমোদিত", Toast.LENGTH_SHORT).show()
            PrayerNotificationWorker.schedulePrayerAlertWorkers(
                context = context,
                timings = timings,
                locationName = userLocation?.displayName
            )
        } else {
            Toast.makeText(context, "নোটিফিকেশন পারমিশন দেওয়া হয়নি", Toast.LENGTH_SHORT).show()
        }
    }

    // Ensure WorkManager alerts and alarms are scheduled if toggles are enabled
    LaunchedEffect(timings, isAdhanEnabled, isPrayerNotificationsEnabled) {
        if (isPrayerNotificationsEnabled) {
            PrayerNotificationWorker.schedulePrayerAlertWorkers(
                context = context,
                timings = timings,
                locationName = userLocation?.displayName
            )
        } else {
            PrayerNotificationWorker.cancelAllPrayerWork(context)
        }

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
                    // 1. Independent Notification Permission Button (Bell Icon)
                    IconButton(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                Toast.makeText(context, "এই ডিভাইসে পুশ নোটিফিকেশন সচল আছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isPrayerNotificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = "নোটিফিকেশন পারমিশন",
                            tint = PaisaTealPrimary
                        )
                    }

                    // 2. Independent GPS Location Button (Location Pin Icon)
                    IconButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "জিপিএস লোকেশন সনাক্ত",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =========================================================
            // 1. GPS SHONAKTO CARD (GPS Detection Card)
            // =========================================================
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
                                    text = if (userLocation != null) userLocation!!.displayName else "বিশ্বব্যাপী জিপিএস অবস্থান",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = if (userLocation != null) "স্থানাঙ্ক: ${String.format(Locale.US, "%.2f, %.2f", userLocation!!.latitude, userLocation!!.longitude)}" else "ভ্রমণকালে অটো-আপডেটের জন্য জিপিএস দিন",
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

            // =========================================================
            // NEXT PRAYER HIGHLIGHT BANNER
            // =========================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaTealContainer.copy(alpha = 0.65f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PaisaTealPrimary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(PaisaTealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PaisaTealPrimary)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("পরবর্তী ওয়াক্ত", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = nextPrayerBn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = PaisaTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "নির্ধারিত সময়: ${formatTime(nextPrayerTime)} — আযান ও অ্যালার্ট সক্রিয়",
                                    fontSize = 11.sp,
                                    color = PaisaTealDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================
            // 2. NAMAJER SOMOYSHUCHI (Daily Prayer Schedule Table)
            // =========================================================
            item {
                Text(
                    text = "আজকের নামাজের সময়সূচি (${if (is12Hour) "১২-ঘণ্টা" else "২৪-ঘণ্টা"})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PaisaTextPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                val isHanafi = selectedMadhab.equals("Hanafi", ignoreCase = true) || selectedMadhab.contains("হানাফী")
                val asrLabel = if (isHanafi) "আসর (Asr - হানাফী)" else "আসর (Asr - শাফেয়ী)"
                val sunsetDisplay = if (timings.sunset.isNotBlank()) timings.sunset else timings.maghrib

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PrayerTimeRowItem(prayerNameBn = "ফজর (Fajr)", timeStr = formatTime(timings.fajr), isNext = timings.nextPrayerName.equals("fajr", ignoreCase = true))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PaisaSurfaceVariant)
                        PrayerTimeRowItem(prayerNameBn = "সূর্যোদয় (Sunrise)", timeStr = formatTime(timings.sunrise), isNext = false)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PaisaSurfaceVariant)
                        PrayerTimeRowItem(prayerNameBn = "যোহর (Dhuhr)", timeStr = formatTime(timings.dhuhr), isNext = timings.nextPrayerName.equals("dhuhr", ignoreCase = true))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PaisaSurfaceVariant)
                        PrayerTimeRowItem(prayerNameBn = asrLabel, timeStr = formatTime(timings.asr), isNext = timings.nextPrayerName.equals("asr", ignoreCase = true))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PaisaSurfaceVariant)
                        PrayerTimeRowItem(prayerNameBn = "সূর্যাস্ত (Sunset)", timeStr = formatTime(sunsetDisplay), isNext = false)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PaisaSurfaceVariant)
                        PrayerTimeRowItem(prayerNameBn = "মাগরিব (Maghrib)", timeStr = formatTime(timings.maghrib), isNext = timings.nextPrayerName.equals("maghrib", ignoreCase = true))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PaisaSurfaceVariant)
                        PrayerTimeRowItem(prayerNameBn = "ইশা (Isha)", timeStr = formatTime(timings.isha), isNext = timings.nextPrayerName.equals("isha", ignoreCase = true))
                    }
                }
            }

            // =========================================================
            // 3. SEHERI O IFTAR ER GPS SOMOY WITH ARABIC CALENDAR (Ummah API)
            // =========================================================
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
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                        text = "সেহরি ও ইফতার",
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

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = timings.hijriDateFormatted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8B5CF6)
                                    )
                                }
                            }
                        }

                        // Sehri and Iftar Display Cards (UI friendly, no overlapping)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
                                        text = "সেহরি শেষ সময়",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF6D28D9)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatTime(timings.sehriEnds),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = Color(0xFF4C1D95)
                                    )
                                    Text(
                                        text = "ফজর: ${formatTime(timings.fajr)}",
                                        fontSize = 10.sp,
                                        color = PaisaTextSecondary
                                    )
                                }
                            }

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
                                        text = "ইফতারের সময়",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFD97706)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatTime(timings.iftarTime),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = Color(0xFFB45309)
                                    )
                                    Text(
                                        text = "মাগরিব: ${formatTime(timings.maghrib)}",
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
                                    text = "রোজার নিয়ত ও ইফতারের দোয়া",
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
                                        Text("রোজার নিয়ত:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PaisaTealPrimary)
                                        Text("নাওয়াইতু আন আছুমা গাদাম মিন শাহরি রামাদানাল মুবারাকি ফারদাল্লাকা ইয়া আল্লাহু ফাতাকাব্বাল মিন্নী...", fontSize = 11.sp, color = PaisaTextSecondary)
                                    }
                                }
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = PaisaBackground)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("ইফতারের দোয়া:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFD97706))
                                        Text("আল্লাহুম্মা লাকা ছুমতু ওয়া আলা রিযক্বিকা আফত্বারতু।", fontSize = 11.sp, color = PaisaTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================
            // 4. MADHAB SELECTION CARD (Hanafi vs Shafi'i)
            // =========================================================
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
                                    text = "মাজহাব নির্বাচন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = "আসর ওয়াক্তের ছায়ার পরিমাপ অনুযায়ী নির্ধারণ",
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

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isHanafi) PaisaTealContainer.copy(alpha = 0.4f) else PaisaSurfaceVariant.copy(alpha = 0.35f)
                                ),
                                border = if (isHanafi) androidx.compose.foundation.BorderStroke(1.5.dp, PaisaTealPrimary) else null,
                                onClick = {
                                    AdhanPreferences.setSelectedMadhab(context, "Hanafi")
                                    viewModel.refreshPrayerTimings("Hanafi")
                                    PrayerNotificationWorker.triggerImmediateCalculation(context)
                                    Toast.makeText(context, "হানাফী মাজহাব নির্বাচিত হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("হানাফী", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PaisaTextPrimary)
                                        RadioButton(selected = isHanafi, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = PaisaTealPrimary))
                                    }
                                    Text("ছায়া ২ গুণ", fontSize = 10.sp, color = PaisaTextSecondary)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isShafi) PaisaTealContainer.copy(alpha = 0.4f) else PaisaSurfaceVariant.copy(alpha = 0.35f)
                                ),
                                border = if (isShafi) androidx.compose.foundation.BorderStroke(1.5.dp, PaisaTealPrimary) else null,
                                onClick = {
                                    AdhanPreferences.setSelectedMadhab(context, "Shafi")
                                    viewModel.refreshPrayerTimings("Shafi")
                                    PrayerNotificationWorker.triggerImmediateCalculation(context)
                                    Toast.makeText(context, "শাফেয়ী/অন্যান্য মাজহাব নির্বাচিত হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("শাফেয়ী / অন্যান্য", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PaisaTextPrimary)
                                        RadioButton(selected = isShafi, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = PaisaTealPrimary))
                                    }
                                    Text("ছায়া ১ গুণ", fontSize = 10.sp, color = PaisaTextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================
            // CLOCK FORMAT SETTING CARD (12h vs 24h)
            // =========================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
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
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = PaisaTealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ঘড়ি ফরম্যাট (Clock Format)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = if (is12Hour) "বর্তমানে ১২-ঘণ্টা (12-Hour AM/PM) ফরম্যাট চালিত" else "বর্তমানে ২৪-ঘণ্টা (24-Hour) ফরম্যাট চালিত",
                                    fontSize = 11.sp,
                                    color = PaisaTextSecondary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = is12Hour,
                                onClick = {
                                    AdhanPreferences.set12HourFormat(context, true)
                                    Toast.makeText(context, "১২-ঘণ্টা ঘড়ি ফরম্যাট চালু করা হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("১২ ঘণ্টা", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = !is12Hour,
                                onClick = {
                                    AdhanPreferences.set12HourFormat(context, false)
                                    Toast.makeText(context, "২৪-ঘণ্টা ঘড়ি ফরম্যাট চালু করা হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("২৪ ঘণ্টা", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
            // =========================================================
            item {
                val adhanList = remember {
                    listOf(
                        Triple("makkah", "মক্কা শরিফ (Makkah Al-Mukarramah)", "শেখ আলী আহমেদ মুল্লা — মসজিদুল হারাম" to "https://cdn.aladhan.com/audio/adhans/a1.mp3"),
                        Triple("madinah", "মদিনা শরিফ (Madinah Al-Munawwarah)", "শেখ এশাম বুখারি — মসজিদে নববী" to "https://cdn.aladhan.com/audio/adhans/a2.mp3"),
                        Triple("alaqsa", "মসজিদুল আকসা (Al-Aqsa Mosque)", "আল-কুদস জেরুজালেম আজান" to "https://cdn.aladhan.com/audio/adhans/a3.mp3"),
                        Triple("mishary", "কারী মিশারি রশিদ আল-আফাসি", "মিশারি আল-আফাসি (কুয়েত)" to "https://cdn.aladhan.com/audio/adhans/a4.mp3"),
                        Triple("basit", "কারী আব্দুল বাসিত আব্দুস সামাদ", "মিশরীয় বিখ্যাত ক্বারী" to "https://cdn.aladhan.com/audio/adhans/a5.mp3"),
                        Triple("sudais", "শায়খ আব্দুর রহমান আস-সুদাইস", "ইমাম, মসজিদুল হারাম" to "https://cdn.aladhan.com/audio/adhans/a1.mp3"),
                        Triple("yasser", "শায়খ ইয়াসির আল-দোসারি", "ইমাম, মসজিদুল হারাম" to "https://cdn.aladhan.com/audio/adhans/a2.mp3")
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
                                    text = "মুয়াজ্জিন নির্বাচন ও আজান প্রিভিউ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = "জনপ্রিয় মুয়াজ্জিনদের আজান নির্বাচন করুন",
                                    fontSize = 11.sp,
                                    color = PaisaTextSecondary
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
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PaisaTealPrimary) else null
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
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
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
                                                maxLines = 1
                                            )
                                            Text(
                                                text = subtitle,
                                                fontSize = 11.sp,
                                                color = PaisaTextSecondary,
                                                maxLines = 1
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
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
                                                    contentDescription = "প্রিভিউ",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = Color.White
                                                )
                                            }

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

            // =========================================================
            // 6. WORKMANAGER PUSH NOTIFICATION & ADHAN TOGGLE (Test Notification removed)
            // =========================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPrayerNotificationsEnabled || isAdhanEnabled) PaisaTealContainer.copy(alpha = 0.45f) else PaisaSurface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // WorkManager Push Notification Toggle Row
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
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isPrayerNotificationsEnabled) PaisaTealPrimary else PaisaSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPrayerNotificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = if (isPrayerNotificationsEnabled) Color.White else PaisaTextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "পুশ নোটিফিকেশন অ্যালার্ট (WorkManager)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = PaisaTextPrimary
                                    )
                                    Text(
                                        text = if (isPrayerNotificationsEnabled) "৫ ওয়াক্ত নামাজের পুশ অ্যালার্ট সক্রিয়" else "নামাজের পুশ নোটিফিকেশন বন্ধ",
                                        fontSize = 11.sp,
                                        color = if (isPrayerNotificationsEnabled) PaisaTealDark else PaisaTextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = isPrayerNotificationsEnabled,
                                onCheckedChange = { enabled ->
                                    isPrayerNotificationsEnabled = enabled
                                    AdhanPreferences.setPrayerNotificationsEnabled(context, enabled)
                                    if (enabled) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        PrayerNotificationWorker.schedulePrayerAlertWorkers(
                                            context = context,
                                            timings = timings,
                                            locationName = userLocation?.displayName
                                        )
                                        Toast.makeText(context, "WorkManager পুশ নোটিফিকেশন অ্যালার্ট চালু হয়েছে", Toast.LENGTH_SHORT).show()
                                    } else {
                                        PrayerNotificationWorker.cancelAllPrayerWork(context)
                                        Toast.makeText(context, "নামাজের পুশ নোটিফিকেশন বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PaisaTealPrimary
                                )
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        // Adhan Audio Playback Toggle Row
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
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isAdhanEnabled) PaisaTealPrimary else PaisaSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isAdhanEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                        contentDescription = null,
                                        tint = if (isAdhanEnabled) Color.White else PaisaTextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "নামাজের সময়ে আযান বাজান",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = PaisaTextPrimary
                                    )
                                    Text(
                                        text = if (isAdhanEnabled) "ওয়াক্ত হলে স্বয়ংক্রিয় আযান অডিও বাজবে" else "আযান অডিও বর্তমানে বন্ধ",
                                        fontSize = 11.sp,
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
                                        AdhanScheduler.schedulePrayerAlarms(
                                            context = context,
                                            fajr = timings.fajr,
                                            dhuhr = timings.dhuhr,
                                            asr = timings.asr,
                                            maghrib = timings.maghrib,
                                            isha = timings.isha
                                        )
                                        Toast.makeText(context, "আযান অডিও চালু করা হয়েছে", Toast.LENGTH_SHORT).show()
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

                        if (isAudioPlaying) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { DefaultAdhanAudioProvider.stopPlayback() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PaisaExpenseRed),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("অডিও বন্ধ করুন", fontSize = 11.sp)
                                }
                            }
                        }
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
