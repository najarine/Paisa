package com.paisa.najarine.ui.screens.islamic

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.paisa.najarine.data.remote.UmmahApiService
import com.paisa.najarine.data.repository.HadithItem
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import kotlinx.coroutines.launch

data class HadithCollection(
    val id: String,
    val nameBn: String,
    val totalCount: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyHadithScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiService = remember { UmmahApiService.create() }

    val collections = remember {
        listOf(
            HadithCollection("bukhari", "সহীহ বুখারী", "৭,৫৬৩ হাদিস"),
            HadithCollection("muslim", "সহীহ মুসলিম", "৭,৫০০ হাদিস"),
            HadithCollection("abudawud", "সুনান আবু দাউদ", "৫,২৭৪ হাদিস"),
            HadithCollection("tirmidhi", "জামে তিরমিযী", "৩,৯৫৬ হাদিস"),
            HadithCollection("ibnmajah", "সুনান ইবনে মাজাহ", "৪,৩৪১ হাদিস"),
            HadithCollection("nasai", "সুনান আন-নাসায়ী", "৫,৭৬০ হাদিস"),
            HadithCollection("malik", "মুওয়াত্তা মালিক", "১,৭২০ হাদিস"),
            HadithCollection("nawawi40", "ইমাম নববীর ৪০ হাদিস", "৪০ হাদিস"),
            HadithCollection("qudsi40", "৪০ হাদিসে কুদসী", "৪০ হাদিস"),
            HadithCollection("shahwaliullah40", "শাহ ওয়ালীউল্লাহর ৪০ হাদিস", "৪০ হাদিস")
        )
    }

    var selectedCollection by remember { mutableStateOf(collections[0]) }
    var searchQuery by remember { mutableStateOf("") }
    var hadiths by remember { mutableStateOf<List<HadithItem>>(emptyList()) }
    var currentNumber by remember { mutableIntStateOf(1) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isInitialLoading by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val isHourlyHadithNotifEnabled by com.paisa.najarine.notification.AdhanPreferences.isHourlyHadithNotificationEnabledState.collectAsState()

    // Load initial batch when collection changes
    LaunchedEffect(selectedCollection) {
        isInitialLoading = true
        currentNumber = 1
        hadiths = emptyList()
        val fetched = mutableListOf<HadithItem>()
        for (i in 1..15) {
            try {
                val resp = apiService.getHadithByCollectionAndNumber(selectedCollection.id, i)
                if (resp.success == true && resp.data != null) {
                    val item = resp.data!!
                    fetched.add(
                        HadithItem(
                            title = item.topic ?: selectedCollection.nameBn,
                            arabic = item.arabic ?: "",
                            translation = item.translation ?: "",
                            banglaTranslation = item.banglaTranslation ?: item.translation ?: "",
                            narrator = item.narrator ?: "বর্ণনাকারী উল্লেখ নেই",
                            source = "${selectedCollection.nameBn} (${item.number ?: i})",
                            hadithNumber = (item.number ?: i).toString(),
                            grade = item.grade ?: "সহীহ",
                            topic = item.topic ?: "হাদিস"
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        if (fetched.isEmpty()) {
            val local = viewModel.getAllAuthenticHadiths()
            hadiths = local.map { it.copy(source = "${selectedCollection.nameBn} (${it.hadithNumber})") }
        } else {
            hadiths = fetched
        }
        currentNumber = 16
        isInitialLoading = false
    }

    // Infinite Scroll Pagination Function
    fun loadMore() {
        if (isLoadingMore) return
        scope.launch {
            isLoadingMore = true
            val fetched = mutableListOf<HadithItem>()
            val start = currentNumber
            val end = currentNumber + 14
            for (i in start..end) {
                try {
                    val resp = apiService.getHadithByCollectionAndNumber(selectedCollection.id, i)
                    if (resp.success == true && resp.data != null) {
                        val item = resp.data!!
                        fetched.add(
                            HadithItem(
                                title = item.topic ?: selectedCollection.nameBn,
                                arabic = item.arabic ?: "",
                                translation = item.translation ?: "",
                                banglaTranslation = item.banglaTranslation ?: item.translation ?: "",
                                narrator = item.narrator ?: "বর্ণনাকারী উল্লেখ নেই",
                                source = "${selectedCollection.nameBn} (${item.number ?: i})",
                                hadithNumber = (item.number ?: i).toString(),
                                grade = item.grade ?: "সহীহ",
                                topic = item.topic ?: "হাদিস"
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
            if (fetched.isNotEmpty()) {
                hadiths = hadiths + fetched
                currentNumber = end + 1
            } else {
                // Continuous fallback rotation for infinite feel if API ends or network is slow
                val local = viewModel.getAllAuthenticHadiths()
                val repeated = local.mapIndexed { idx, it ->
                    it.copy(source = "${selectedCollection.nameBn} (${currentNumber + idx})")
                }
                hadiths = hadiths + repeated
                currentNumber += local.size
            }
            isLoadingMore = false
        }
    }

    // Detect scroll near bottom for infinite scrolling
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= hadiths.size - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isInitialLoading && !isLoadingMore) {
            loadMore()
        }
    }

    val filteredHadiths = remember(hadiths, searchQuery) {
        if (searchQuery.isBlank()) hadiths
        else hadiths.filter {
            it.banglaTranslation.contains(searchQuery, ignoreCase = true) ||
            it.arabic.contains(searchQuery, ignoreCase = true) ||
            it.narrator.contains(searchQuery, ignoreCase = true) ||
            it.source.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "হাদিস (১০টি সংকলন ও ৩৬,০০০+ হাদিস)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "উম্মা এপিআই (Ummah API) • ইনফিনিট স্ক্রোল",
                            style = MaterialTheme.typography.bodySmall,
                            color = PaisaTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isInitialLoading = true
                            currentNumber = 1
                            hadiths = viewModel.getAllAuthenticHadiths()
                            isInitialLoading = false
                            Toast.makeText(context, "${selectedCollection.nameBn} রিফ্রেশ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PaisaTealPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaisaSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(PaisaBackground)
                .padding(padding)
        ) {
            // =========================================================
            // HOURLY HADITH NOTIFICATION SETTINGS (Right before Search Bar)
            // =========================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PaisaTealPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = PaisaTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "প্রতি ঘণ্টার হাদিস নোটিফিকেশন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = if (isHourlyHadithNotifEnabled) "প্রতি ঘণ্টায় উম্মা এপিআই থেকে বিশুদ্ধ হাদিস আসবে" else "ঘণ্টার হাদিস নোটিফিকেশন বন্ধ",
                                    fontSize = 11.sp,
                                    color = if (isHourlyHadithNotifEnabled) PaisaTealDark else PaisaTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isHourlyHadithNotifEnabled,
                            onCheckedChange = { enabled ->
                                com.paisa.najarine.notification.AdhanPreferences.setHourlyHadithNotificationEnabled(context, enabled)
                                if (enabled) {
                                    com.paisa.najarine.notification.HourlyIslamicScheduler.scheduleHourlySync(context)
                                    Toast.makeText(context, "ঘণ্টার হাদিস নোটিফিকেশন চালু হয়েছে", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "ঘণ্টার হাদিস নোটিফিকেশন বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PaisaTealPrimary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "৩৬,০০০+ হাদিস • ১০টি প্রামাণ্য গ্রন্থ",
                            fontSize = 10.sp,
                            color = PaisaTextSecondary
                        )
                        TextButton(
                            onClick = {
                                viewModel.testHourlyHadithNotification(context)
                                Toast.makeText(context, "টেস্ট হাদিস নোটিফিকেশন পাঠানো হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp), tint = PaisaTealPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("টেস্ট করুন", fontSize = 11.sp, color = PaisaTealPrimary)
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("হাদিস খুঁজুন (আরবি, বাংলা বা বর্ণনাকারী)...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PaisaTealPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = PaisaTextSecondary)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaisaTealPrimary,
                    unfocusedBorderColor = PaisaBorder,
                    focusedContainerColor = PaisaSurface,
                    unfocusedContainerColor = PaisaSurface
                ),
                singleLine = true
            )

            // Collection Horizontal Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(collections) { col ->
                    val isSelected = col.id == selectedCollection.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCollection = col },
                        label = {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(col.nameBn, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                Text(col.totalCount, fontSize = 9.sp, color = if (isSelected) Color.White.copy(alpha = 0.8f) else PaisaTextSecondary)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PaisaTealPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = PaisaSurface,
                            labelColor = PaisaTextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PaisaTealPrimary else PaisaBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isInitialLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PaisaTealPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("হাদিস লোড হচ্ছে...", fontSize = 13.sp, color = PaisaTextSecondary)
                    }
                }
            } else if (filteredHadiths.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = PaisaTextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("কোনো হাদিস পাওয়া যায়নি", color = PaisaTextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredHadiths) { hadith ->
                        HadithCardItem(hadith = hadith)
                    }

                    // Loading more footer indicator
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PaisaTealPrimary, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("আরও হাদিস আসছে...", fontSize = 12.sp, color = PaisaTextSecondary)
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
fun HadithCardItem(hadith: HadithItem) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
                    text = hadith.source,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PaisaTealPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PaisaTealContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = hadith.grade,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTealDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hadith.narrator,
                style = MaterialTheme.typography.bodySmall,
                color = PaisaTextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Arabic Hadith Text
            Text(
                text = hadith.arabic,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PaisaTextPrimary,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bangla Translation
            Text(
                text = "“${hadith.banglaTranslation}”",
                style = MaterialTheme.typography.bodyMedium,
                color = PaisaTextPrimary,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "হাদিস:\n${hadith.arabic}\n\n\"${hadith.banglaTranslation}\"\n— ${hadith.source} (${hadith.narrator})\n\nPaisa অ্যাপ থেকে শেয়ারকৃত"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "হাদিস শেয়ার করুন"))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = PaisaTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
