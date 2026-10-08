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

    var duas by remember { mutableStateOf(viewModel.islamicRepo.categorizedDuas) }
    var categories by remember { mutableStateOf(listOf("সকল", "প্রতিদিনের দোয়া", "আর্থিক মুক্তি", "রিজিক ও বরকত", "রমজান")) }
    var selectedCategory by remember { mutableStateOf("সকল") }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Fetch all categories once on startup (ensure "সকল" is only 1 time)
    LaunchedEffect(Unit) {
        isLoading = true
        val fetchedCategories = viewModel.islamicRepo.fetchDuaCategoriesFromApi()
        if (fetchedCategories.isNotEmpty()) {
            val cleanedCategories = fetchedCategories.filter {
                val lower = it.trim().lowercase()
                lower != "সকল" && lower != "all" && lower != "everything" && lower != "sokol"
            }.distinct()
            categories = listOf("সকল") + cleanedCategories
        }
        val fetchedDuas = viewModel.islamicRepo.fetchDuasFromApi()
        if (fetchedDuas.isNotEmpty()) {
            duas = fetchedDuas
        }
        isLoading = false
    }

    // Lazy load duas when category changes
    LaunchedEffect(selectedCategory) {
        isLoading = true
        if (selectedCategory == "সকল") {
            val fetchedDuas = viewModel.islamicRepo.fetchDuasFromApi()
            if (fetchedDuas.isNotEmpty()) {
                duas = fetchedDuas
            }
        } else {
            val categoryDuas = viewModel.islamicRepo.fetchDuasByCategoryFromApi(selectedCategory)
            if (categoryDuas.isNotEmpty()) {
                duas = categoryDuas
            }
        }
        isLoading = false
    }

    // Infinite scroll pagination for Duas
    fun loadMoreDuas() {
        if (isLoadingMore) return
        scope.launch {
            isLoadingMore = true
            val moreDuas = viewModel.islamicRepo.fetchDuasFromApi()
            if (moreDuas.isNotEmpty()) {
                duas = duas + moreDuas
            }
            isLoadingMore = false
        }
    }

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= duas.size - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoading && !isLoadingMore) {
            loadMoreDuas()
        }
    }

    val filteredDuas = remember(duas, selectedCategory, searchQuery) {
        duas.filter { dua ->
            val matchCategory = selectedCategory == "সকল" || dua.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                    dua.title.contains(searchQuery, ignoreCase = true) ||
                    dua.translation.contains(searchQuery, ignoreCase = true) ||
                    dua.arabic.contains(searchQuery, ignoreCase = true) ||
                    dua.reference.contains(searchQuery, ignoreCase = true)
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
                            isLoading = true
                            if (selectedCategory == "সকল") {
                                duas = viewModel.islamicRepo.fetchDuasFromApi()
                            } else {
                                duas = viewModel.islamicRepo.fetchDuasByCategoryFromApi(selectedCategory)
                            }
                            isLoading = false
                            Toast.makeText(context, "দোয়া সফলভাবে সিঙ্ক করা হয়েছে!", Toast.LENGTH_SHORT).show()
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("দোয়া খুঁজুন (আরবি, বাংলা বা বিষয়)...", fontSize = 13.sp) },
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

            // Category Chips Row (Unique "সকল")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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
                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = PaisaTextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("এই ক্যাটাগরিতে কোনো দোয়া পাওয়া যায়নি", color = PaisaTextSecondary, fontSize = 14.sp)
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
                    items(filteredDuas) { dua ->
                        DuaCardItem(dua = dua)
                    }

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
                                    Text("আরও দোয়া আসছে...", fontSize = 12.sp, color = PaisaTextSecondary)
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
fun DuaCardItem(dua: DuaItem) {
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
                    text = dua.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PaisaTextPrimary,
                    modifier = Modifier.weight(1f)
                )
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

            if (dua.reference.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সূত্র: ${dua.reference}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaisaTealPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "${dua.title}\n\n${dua.arabic}\n\nঅর্থ: ${dua.translation}\nসূত্র: ${dua.reference}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "শেয়ার করুন"))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = PaisaTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
