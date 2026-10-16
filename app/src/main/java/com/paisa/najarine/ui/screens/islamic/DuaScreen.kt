package com.paisa.najarine.ui.screens.islamic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.repository.DuaItem
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var duas by remember { mutableStateOf(viewModel.islamicRepo.loadDuasFromDiskCache(context)) }
    var rawCategories by remember { mutableStateOf(viewModel.islamicRepo.canonicalDuaCategories) }
    var selectedCategoryObj by remember { mutableStateOf<com.paisa.najarine.data.remote.UmmahCategoryItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSyncingSequentially by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // 1. Initial startup: load categories and start sequential background sync
    LaunchedEffect(Unit) {
        val cached = viewModel.islamicRepo.loadDuasFromDiskCache(context)
        if (cached.isNotEmpty()) {
            duas = cached
        }
        val fetchedCategories = viewModel.islamicRepo.fetchDuaCategoriesFromApi()
        if (fetchedCategories.isNotEmpty()) {
            rawCategories = fetchedCategories
        }

        // Sequential category-wise API fetch across all 27 categories
        isSyncingSequentially = true
        scope.launch {
            try {
                viewModel.islamicRepo.fetchAllDuasSequentially(
                    context = context,
                    categories = rawCategories
                ) { catName, count, totalSoFar ->
                    syncStatusText = "সিঙ্ক হচ্ছে: $catName ($totalSoFar টি দোয়া)"
                    val fresh = viewModel.islamicRepo.loadDuasFromDiskCache(context)
                    if (fresh.size > duas.size) {
                        duas = fresh
                    }
                }
                val finalFresh = viewModel.islamicRepo.loadDuasFromDiskCache(context)
                if (finalFresh.isNotEmpty()) {
                    duas = finalFresh
                }
            } catch (_: Exception) {
            } finally {
                isSyncingSequentially = false
                syncStatusText = null
            }
        }
    }

    // 2. Immediate fetch when a user explicitly taps a category chip
    fun onSelectCategory(cat: com.paisa.najarine.data.remote.UmmahCategoryItem?) {
        selectedCategoryObj = cat
        if (cat != null) {
            scope.launch {
                val catId = cat.id ?: ""
                val catDuas = viewModel.islamicRepo.fetchDuasByCategoryFromApi(catId)
                if (catDuas.isNotEmpty()) {
                    val combined = (duas + catDuas).distinctBy { (it.arabic.take(15) + it.title).trim() }
                    duas = combined
                    viewModel.islamicRepo.saveDuasToDiskCache(context, combined)
                }
            }
        }
    }

    val filteredDuas = remember(duas, selectedCategoryObj, searchQuery) {
        duas.filter { dua ->
            val matchCategory = if (selectedCategoryObj == null) {
                true
            } else {
                val catId = selectedCategoryObj?.id ?: ""
                val catName = selectedCategoryObj?.name ?: ""
                dua.category.equals(catName, ignoreCase = true) ||
                        dua.category.equals(catId, ignoreCase = true) ||
                        dua.category.contains(catName, ignoreCase = true) ||
                        dua.category.contains(catId, ignoreCase = true)
            }
            val matchSearch = searchQuery.isBlank() ||
                    dua.title.contains(searchQuery, ignoreCase = true) ||
                    dua.translation.contains(searchQuery, ignoreCase = true) ||
                    dua.arabic.contains(searchQuery, ignoreCase = true) ||
                    dua.transliteration.contains(searchQuery, ignoreCase = true) ||
                    dua.reference.contains(searchQuery, ignoreCase = true) ||
                    dua.category.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "দোয়া ও যিকির সমাহার",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isSyncingSequentially && syncStatusText != null) {
                                syncStatusText ?: "উম্মা এপিআই থেকে লোড হচ্ছে..."
                            } else {
                                "${duas.size}টি বিশুদ্ধ দোয়া • ২৭টি বিষয়ভিত্তিক অধ্যায়"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSyncingSequentially) PaisaTealPrimary else PaisaTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("dua_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                isSyncingSequentially = true
                                try {
                                    val cat = selectedCategoryObj
                                    if (cat != null) {
                                        val catId = cat.id ?: ""
                                        val catDuas = viewModel.islamicRepo.fetchDuasByCategoryFromApi(catId)
                                        if (catDuas.isNotEmpty()) {
                                            duas = (duas + catDuas).distinctBy { (it.arabic.take(15) + it.title).trim() }
                                        }
                                    } else {
                                        val fresh = viewModel.islamicRepo.fetchAllDuasSequentially(
                                            context = context,
                                            categories = rawCategories
                                        ) { catName, _, total ->
                                            syncStatusText = "সিঙ্ক হচ্ছে: $catName ($total টি)"
                                        }
                                        duas = fresh
                                    }
                                    Toast.makeText(context, "দোয়া সফলভাবে সিঙ্ক করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {
                                } finally {
                                    isLoading = false
                                    isSyncingSequentially = false
                                    syncStatusText = null
                                }
                            }
                        },
                        modifier = Modifier.testTag("dua_refresh_button")
                    ) {
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("দোয়া খুঁজুন (আরবি, বাংলা, উচ্চারণ বা বিষয়)...", fontSize = 13.sp) },
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
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("dua_search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaisaTealPrimary,
                    unfocusedBorderColor = PaisaBorder,
                    focusedContainerColor = PaisaSurface,
                    unfocusedContainerColor = PaisaSurface
                ),
                singleLine = true
            )

            // Sync progress banner if sequentially fetching in background
            AnimatedVisibility(
                visible = isSyncingSequentially && syncStatusText != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = PaisaTealContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = PaisaTealDark,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = syncStatusText ?: "ক্যাটাগরিভিত্তিক দোয়া ক্রমানুসারে সিঙ্ক হচ্ছে...",
                            fontSize = 11.sp,
                            color = PaisaTealDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Category Chips Row (All 27 categories from Ummah API + "সকল")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // First chip: "সকল" (All)
                item {
                    val isSelected = selectedCategoryObj == null
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(null) },
                        label = {
                            Text(
                                text = "সকল (${duas.size})",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
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
                        ),
                        modifier = Modifier.testTag("dua_category_all")
                    )
                }

                items(rawCategories) { cat ->
                    val isSelected = selectedCategoryObj?.id == cat.id
                    val displayLabel = cat.name ?: cat.id ?: "ক্যাটাগরি"
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(cat) },
                        label = {
                            Text(
                                text = displayLabel,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
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
                        ),
                        modifier = Modifier.testTag("dua_category_${cat.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PaisaTealPrimary)
                }
            } else if (filteredDuas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = PaisaTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "এই ক্যাটাগরিতে কোনো দোয়া পাওয়া যায়নি",
                            color = PaisaTextSecondary,
                            fontSize = 14.sp
                        )
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
                    items(filteredDuas, key = { it.id + it.arabic.take(15) }) { dua ->
                        DuaCardItem(dua = dua)
                    }
                }
            }
        }
    }
}

@Composable
fun DuaCardItem(dua: DuaItem) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dua_card_${dua.id}"),
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
                    text = dua.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PaisaTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PaisaTealContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = dua.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTealDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Arabic Text
            Text(
                text = dua.arabic,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PaisaTextPrimary,
                lineHeight = 34.sp
            )

            if (dua.transliteration.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "উচ্চারণ: ${dua.transliteration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PaisaTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Translation
            Text(
                text = "অর্থ: “${dua.translation}”",
                style = MaterialTheme.typography.bodyMedium,
                color = PaisaTextPrimary,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (dua.reference.isNotBlank()) "সূত্র: ${dua.reference}" else "সূত্র: সহীহ হাদিস ও সুন্নাহ",
                    style = MaterialTheme.typography.labelSmall,
                    color = PaisaTealPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Copy to clipboard action
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = "${dua.title}\n\n${dua.arabic}\n\nউচ্চারণ: ${dua.transliteration}\nঅর্থ: ${dua.translation}\nসূত্র: ${dua.reference}"
                            val clip = ClipData.newPlainText("দোয়া", clipText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "দোয়া ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("dua_copy_button_${dua.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Dua",
                            tint = PaisaTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Share action
                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "${dua.title}\n\n${dua.arabic}\n\nউচ্চারণ: ${dua.transliteration}\nঅর্থ: ${dua.translation}\nসূত্র: ${dua.reference}\n\n— পয়সা অ্যাপ (Paisa) থেকে প্রেরিত"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "দোয়া শেয়ার করুন"))
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("dua_share_button_${dua.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Dua",
                            tint = PaisaTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
