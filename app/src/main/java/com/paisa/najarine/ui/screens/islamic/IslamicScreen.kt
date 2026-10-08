package com.paisa.najarine.ui.screens.islamic

import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicScreen(
    viewModel: PaisaViewModel,
    onOpenPrayerAdhan: () -> Unit = {},
    onOpenQibla: () -> Unit = {},
    onOpenQuran: () -> Unit = {},
    onOpenHadith: () -> Unit = {},
    onOpenZakat: () -> Unit = {},
    onOpenDua: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prayerTimings by viewModel.prayerTimings.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val isDetectingLocation by viewModel.isDetectingLocation.collectAsState()

    val repo = viewModel.islamicRepo
    val timings = prayerTimings ?: repo.getOfflinePrayerTimings()

    val is12Hour by com.paisa.najarine.notification.AdhanPreferences.is12HourFormatState.collectAsState()
    fun ft(time: String): String = com.paisa.najarine.notification.AdhanPreferences.formatTo12Hour(time, is12Hour)

    // Hourly Room Table Observers (Dynamic updates from hourly worker & tests)
    val latestHourlyHadith by viewModel.latestHourlyHadith.collectAsState(initial = null)
    val latestHourlyQuran by viewModel.latestHourlyQuran.collectAsState(initial = null)
    val canonicalDailyHadithState by viewModel.currentHadith.collectAsState()

    // Runtime Permission Launchers for GPS & Notifications
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
            Toast.makeText(context, "লোকেশন পারমিশন ছাড়া ডিফল্ট সময় ব্যবহার করা হচ্ছে।", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(context, "নোটিফিকেশন পারমিশন অনুমোদিত হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaisaBackground),
        contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp)
    ) {
        // Hero Card: ISLAMIC TODAY (Works Worldwide with GPS / Auto-Coordinates)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onOpenPrayerAdhan() },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "আজকের ইসলামিক সময়সূচী",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706), // Gold/Amber
                            letterSpacing = 1.sp
                        )

                        // GPS Auto-detect Trigger Chip
                        Surface(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = PaisaTealContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = null,
                                    tint = PaisaTealPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isDetectingLocation) "সনাক্ত হচ্ছে..." else if (userLocation != null) "জিপিএস সক্রিয়" else "GPS লোকেশন দিন",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PaisaTealPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "পরবর্তী ওয়াক্ত: ${timings.nextPrayerNameBn} (${ft(timings.nextPrayerTime)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = PaisaTextPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${userLocation?.displayName ?: "ঢাকা, বাংলাদেশ (বিশ্বব্যাপী অটো-সময়)"} • ${timings.hijriDateFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PaisaTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Horizontal Row of 5 Prayer Time Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PrayerPillItem(name = "ফজর", time = ft(timings.fajr), isHighlighted = timings.nextPrayerName.contains("Fajr", ignoreCase = true))
                        PrayerPillItem(name = "যোহর", time = ft(timings.dhuhr), isHighlighted = timings.nextPrayerName.contains("Dhuhr", ignoreCase = true))
                        PrayerPillItem(name = "আসর", time = ft(timings.asr), isHighlighted = timings.nextPrayerName.contains("Asr", ignoreCase = true))
                        PrayerPillItem(name = "মাগরিব", time = ft(timings.maghrib), isHighlighted = timings.nextPrayerName.contains("Maghrib", ignoreCase = true))
                        PrayerPillItem(name = "ইশা", time = ft(timings.isha), isHighlighted = timings.nextPrayerName.contains("Isha", ignoreCase = true))
                    }
                }
            }
        }

        // Fasting / Roja (সেহরি ও ইফতার) Card based on GPS Location
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🌙",
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "রোজা, সেহরি ও ইফতারের সময়সূচী",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PaisaTextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "GPS অটো-আপডেট",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "অবস্থান: ${userLocation?.displayName ?: "ঢাকা, বাংলাদেশ"} • ${timings.hijriDateFormatted}",
                        fontSize = 12.sp,
                        color = PaisaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sehri Time Box
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🌅 সেহরির শেষ সময়", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(ft(timings.sehriEnds), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF065F46))
                                Text("ফজরের আগে শেষ", fontSize = 10.sp, color = Color(0xFF059669))
                            }
                        }

                        // Iftar Time Box
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🌇 ইফতারের সময়", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(ft(timings.iftarTime), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF9A3412))
                                Text("মাগরিবের আযানে", fontSize = 10.sp, color = Color(0xFFEA580C))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fasting Niyyat Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PaisaBackground)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("🤲 রোজার নিয়ত (নালিশ):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PaisaTextPrimary)
                            Text("“নাওয়াইতু আন আসুমা গাদাম মিন শাহরি রামাদান...” (হে আল্লাহ! আমি আগামীকাল রোজা রাখার নিয়ত করলাম)", fontSize = 11.sp, color = PaisaTextSecondary)
                        }
                    }
                }
            }
        }

        // Section Title: Islamic Modules
        item {
            Text(
                text = "ইসলামিক মডিউলসমূহ",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = PaisaTextPrimary,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 10.dp)
            )
        }

        // Row 1 of Modules: Prayer & Adhan, Qibla Compass
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Prayer & Adhan Module (Yellow Bell)
                ModuleLargeCard(
                    title = "নামাজ ও আযান",
                    banglaSub = "বিজ্ঞপ্তি ও অডিও",
                    icon = Icons.Default.NotificationsActive,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenPrayerAdhan
                )

                // Qibla Compass Module (Green Compass)
                ModuleLargeCard(
                    title = "কিবলা কম্পাস",
                    banglaSub = "দিক নির্ণয়",
                    icon = Icons.Default.Explore,
                    iconBg = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF047857),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenQibla
                )
            }
        }

        // Row 2 of Modules: Al-Quran, Daily Hadith, Zakat & Fitrah, Dua
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Al-Quran
                    ModuleSmallCard(
                        title = "আল-কুরআন",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        iconBg = Color(0xFFD1FAE5),
                        iconTint = Color(0xFF059669),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenQuran
                    )

                    // Hadith
                    ModuleSmallCard(
                        title = "হাদিস",
                        icon = Icons.Default.AutoStories,
                        iconBg = Color(0xFFE0F2FE),
                        iconTint = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenHadith
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Zakat & Fitrah
                    ModuleSmallCard(
                        title = "যাকাত ও ফিতরা",
                        icon = Icons.Default.Calculate,
                        iconBg = Color(0xFFFFEDD5),
                        iconTint = Color(0xFFEA580C),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenZakat
                    )

                    // Dua & Adhkar
                    ModuleSmallCard(
                        title = "দোয়া ও যিকির",
                        icon = Icons.Default.MenuBook,
                        iconBg = Color(0xFFFCE7F3),
                        iconTint = Color(0xFFDB2777),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenDua
                    )
                }
            }
        }



        // HOURLY QURAN AYAH & HADITH (Same Design, Random/Hourly from Ummah API)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            val quranEntity = latestHourlyQuran
            val ayahRef = quranEntity?.let { "সূরা ${it.surahNameBangla} [${it.surahNumber}:${it.ayahNumber}]" } ?: repo.canonicalDailyAyah.reference
            val quranArabic = quranEntity?.arabicText ?: repo.canonicalDailyAyah.arabicText
            val quranTranslation = quranEntity?.banglaTranslation ?: repo.canonicalDailyAyah.banglaText

            IslamicFeaturedCard(
                title = "প্রতি ঘণ্টার কুরআনী আয়াত — $ayahRef",
                badgeText = "উম্মা এপিআই সিঙ্কড",
                badgeColor = Color(0xFFD97706),
                badgeBg = Color(0xFFFEF3C7),
                subtitle = "",
                arabicText = quranArabic,
                translationText = quranTranslation,
                onClick = onOpenQuran
            )
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
            val hadithEntity = latestHourlyHadith
            val hadithSource = hadithEntity?.source ?: canonicalDailyHadithState.source
            val hadithNarrator = hadithEntity?.narrator ?: canonicalDailyHadithState.narrator
            val hadithArabic = hadithEntity?.arabic ?: canonicalDailyHadithState.arabic
            val hadithBangla = hadithEntity?.banglaTranslation ?: canonicalDailyHadithState.banglaTranslation

            IslamicFeaturedCard(
                title = "প্রতি ঘণ্টার হাদিস — $hadithSource",
                badgeText = "উম্মা এপিআই সিঙ্কড",
                badgeColor = Color(0xFF047857),
                badgeBg = Color(0xFFD1FAE5),
                subtitle = hadithNarrator,
                arabicText = hadithArabic,
                translationText = hadithBangla,
                onClick = onOpenHadith
            )
        }
    }
}

@Composable
fun IslamicFeaturedCard(
    title: String,
    badgeText: String,
    badgeColor: Color,
    badgeBg: Color,
    subtitle: String,
    arabicText: String,
    translationText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    letterSpacing = 0.5.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PaisaTextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Arabic Text
            Text(
                text = arabicText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = PaisaTextPrimary,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Translation
            Text(
                text = "“$translationText”",
                style = MaterialTheme.typography.bodyMedium,
                color = PaisaTextSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PrayerPillItem(name: String, time: String, isHighlighted: Boolean) {
    if (isHighlighted) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFEF3C7))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(name, fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                Text(time, fontSize = 12.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Text(name, fontSize = 11.sp, color = PaisaTextSecondary, fontWeight = FontWeight.Medium)
            Text(time, fontSize = 12.sp, color = PaisaTextPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ModuleLargeCard(
    title: String,
    banglaSub: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = PaisaTextPrimary
            )
            Text(
                text = banglaSub,
                fontSize = 12.sp,
                color = PaisaTextSecondary
            )
        }
    }
}

@Composable
fun ModuleSmallCard(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = PaisaTextPrimary,
                maxLines = 1
            )
        }
    }
}
