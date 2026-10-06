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
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prayerTimings by viewModel.prayerTimings.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val isDetectingLocation by viewModel.isDetectingLocation.collectAsState()

    val repo = viewModel.islamicRepo
    val timings = prayerTimings ?: repo.getOfflinePrayerTimings()

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
                            text = "ISLAMIC TODAY",
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
                        text = "পরবর্তী ওয়াক্ত: ${timings.nextPrayerNameBn} (${timings.nextPrayerTime})",
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
                        PrayerPillItem(name = "ফজর", time = timings.fajr, isHighlighted = timings.nextPrayerName.contains("Fajr", ignoreCase = true))
                        PrayerPillItem(name = "যোহর", time = timings.dhuhr, isHighlighted = timings.nextPrayerName.contains("Dhuhr", ignoreCase = true))
                        PrayerPillItem(name = "আসর", time = timings.asr, isHighlighted = timings.nextPrayerName.contains("Asr", ignoreCase = true))
                        PrayerPillItem(name = "মাগরিব", time = timings.maghrib, isHighlighted = timings.nextPrayerName.contains("Maghrib", ignoreCase = true))
                        PrayerPillItem(name = "ইশা", time = timings.isha, isHighlighted = timings.nextPrayerName.contains("Isha", ignoreCase = true))
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
                    title = "Prayer & Adhan",
                    banglaSub = "নামাজ ও আযান",
                    icon = Icons.Default.NotificationsActive,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenPrayerAdhan
                )

                // Qibla Compass Module (Green Compass)
                ModuleLargeCard(
                    title = "Qibla Compass",
                    banglaSub = "কিবলা কম্পাস",
                    icon = Icons.Default.Explore,
                    iconBg = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF047857),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenQibla
                )
            }
        }

        // Row 2 of Modules: Al-Quran, Daily Hadith, Zakat & Fitrah
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Al-Quran
                ModuleSmallCard(
                    title = "Al-Quran",
                    icon = Icons.Default.MenuBook,
                    iconBg = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF059669),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenQuran
                )

                // Daily Hadith
                ModuleSmallCard(
                    title = "Daily Hadith",
                    icon = Icons.Default.AutoStories,
                    iconBg = Color(0xFFE0F2FE),
                    iconTint = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenHadith
                )

                // Zakat & Fitrah
                ModuleSmallCard(
                    title = "Zakat & Fitrah",
                    icon = Icons.Default.Calculate,
                    iconBg = Color(0xFFFFEDD5),
                    iconTint = Color(0xFFEA580C),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenZakat
                )
            }
        }



        // HOURLY QURAN AYAH (Backed by Room Table + Auto-changes hourly)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            val quranEntity = latestHourlyQuran
            val ayahRef = if (quranEntity != null) {
                "সূরা ${quranEntity.surahNameBangla} [${quranEntity.surahNumber}:${quranEntity.ayahNumber}]"
            } else {
                repo.canonicalDailyAyah.reference
            }
            val arabicText = quranEntity?.arabicText ?: repo.canonicalDailyAyah.arabicText
            val banglaText = quranEntity?.banglaTranslation ?: repo.canonicalDailyAyah.banglaText

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                            text = "HOURLY QURAN AYAH — $ayahRef",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            letterSpacing = 0.5.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "প্রতি ঘণ্টায় পরিবর্তিত",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Arabic Calligraphy
                    Text(
                        text = arabicText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTextPrimary,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bangla Translation
                    Text(
                        text = banglaText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaisaTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // HOURLY HADITH (Backed by Room Table + Auto-changes hourly from UmmahAPI)
        item {
            Spacer(modifier = Modifier.height(14.dp))
            val hadithEntity = latestHourlyHadith
            val hadithTitle = hadithEntity?.title ?: canonicalDailyHadithState.title
            val hadithSource = hadithEntity?.source ?: canonicalDailyHadithState.source
            val hadithNarrator = hadithEntity?.narrator ?: canonicalDailyHadithState.narrator
            val hadithArabic = hadithEntity?.arabic ?: canonicalDailyHadithState.arabic
            val hadithBangla = hadithEntity?.banglaTranslation ?: canonicalDailyHadithState.banglaTranslation

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                            text = "HOURLY HADITH — $hadithSource",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857),
                            letterSpacing = 0.5.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFD1FAE5))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "উম্মা এপিআই সিঙ্কড",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = hadithNarrator,
                        style = MaterialTheme.typography.bodySmall,
                        color = PaisaTextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = hadithArabic,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTealDark,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "“$hadithBangla”",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = PaisaTextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }
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
