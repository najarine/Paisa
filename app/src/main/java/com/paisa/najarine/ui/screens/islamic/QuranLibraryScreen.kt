package com.paisa.najarine.ui.screens.islamic

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.audio.QuranAudioPlayer
import com.paisa.najarine.audio.QuranPlaybackState
import com.paisa.najarine.audio.QuranReciter
import com.paisa.najarine.audio.QuranReciters
import com.paisa.najarine.data.repository.FullAyahItem
import com.paisa.najarine.data.repository.SurahItem
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*

private const val PREFS_QURAN_AUDIO = "paisa_quran_audio_prefs"
private const val KEY_SELECTED_RECITER_ID = "selected_reciter_id"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranLibraryScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_QURAN_AUDIO, Context.MODE_PRIVATE) }
    val allSurahs = remember { viewModel.islamicRepo.all114Surahs }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSurahForReading by remember { mutableStateOf<SurahItem?>(null) }
    var lastReadSurahNumber by remember { mutableStateOf(2) }

    var selectedReciter by remember {
        val savedId = prefs.getString(KEY_SELECTED_RECITER_ID, QuranReciters.DEFAULT_RECITER.id)
        mutableStateOf(QuranReciters.getReciterById(savedId ?: QuranReciters.DEFAULT_RECITER.id))
    }
    var showReciterDialog by remember { mutableStateOf(false) }

    fun onSelectReciter(reciter: QuranReciter) {
        selectedReciter = reciter
        prefs.edit().putString(KEY_SELECTED_RECITER_ID, reciter.id).apply()
        showReciterDialog = false
        Toast.makeText(context, "তিলাওয়াতকারী: ${reciter.nameBangla}", Toast.LENGTH_SHORT).show()
    }

    val filteredSurahs = remember(searchQuery) {
        if (searchQuery.isBlank()) allSurahs
        else allSurahs.filter {
            it.nameBangla.contains(searchQuery, ignoreCase = true) ||
            it.nameEnglish.contains(searchQuery, ignoreCase = true) ||
            it.nameArabic.contains(searchQuery, ignoreCase = true) ||
            it.number.toString().contains(searchQuery)
        }
    }

    if (showReciterDialog) {
        ReciterSelectionDialog(
            currentReciter = selectedReciter,
            onSelect = { onSelectReciter(it) },
            onDismiss = { showReciterDialog = false }
        )
    }

    if (selectedSurahForReading != null) {
        SurahReaderView(
            surah = selectedSurahForReading!!,
            viewModel = viewModel,
            currentReciter = selectedReciter,
            onReciterClick = { showReciterDialog = true },
            onBack = { selectedSurahForReading = null },
            onBookmark = {
                lastReadSurahNumber = selectedSurahForReading!!.number
                Toast.makeText(context, "${selectedSurahForReading!!.nameBangla} শেষ পড়া হিসেবে চিহ্নিত", Toast.LENGTH_SHORT).show()
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "পবিত্র আল-কুরআন (১১৪টি সূরা)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(onClick = { showReciterDialog = true }) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "ক্বারী নির্বাচন করুন", tint = PaisaTealPrimary)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Reciter quick banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReciterDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Headphones,
                                contentDescription = null,
                                tint = PaisaTealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "নির্বাচিত ক্বারী (Reciter)",
                                    fontSize = 11.sp,
                                    color = PaisaTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = selectedReciter.nameBangla,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaisaTextPrimary
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PaisaTealPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "পরিবর্তন",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PaisaTealPrimary
                            )
                        }
                    }
                }
            }

            // Search Surah Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("সূরা খুঁজুন (যেমন: বাকারা, ইয়াসীন, রহমান, ১১৪)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PaisaTextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // Last Read Bookmark Card
            item {
                val lastReadSurah = allSurahs.find { it.number == lastReadSurahNumber } ?: allSurahs[1]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSurahForReading = lastReadSurah },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaTealContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PaisaTealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("সর্বশেষ পড়া সূরা", fontSize = 11.sp, color = PaisaTealDark, fontWeight = FontWeight.Bold)
                                Text("${lastReadSurah.number}. ${lastReadSurah.nameBangla}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PaisaTextPrimary)
                                Text("${lastReadSurah.versesCount}টি আয়াত • ${lastReadSurah.type}", fontSize = 12.sp, color = PaisaTextSecondary)
                            }
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = "পড়া চালিয়ে যান", tint = PaisaTealPrimary)
                    }
                }
            }

            // 114 Surahs Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সকল সূরা (${filteredSurahs.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = PaisaTextPrimary
                    )
                    Text(
                        text = "৩০ পারা • ১১৪ সূরা",
                        fontSize = 12.sp,
                        color = PaisaTextSecondary
                    )
                }
            }

            // Scrollable 114 Surahs List
            items(filteredSurahs) { surah ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSurahForReading = surah },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PaisaSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${surah.number}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PaisaTealPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = surah.nameBangla,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = "${surah.versesCount} আয়াত • ${surah.type} • অর্থ: ${surah.meaningBangla}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PaisaTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = surah.nameArabic,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = PaisaTealPrimary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahReaderView(
    surah: SurahItem,
    viewModel: PaisaViewModel,
    currentReciter: QuranReciter,
    onReciterClick: () -> Unit,
    onBack: () -> Unit,
    onBookmark: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val audioPlayer = remember { QuranAudioPlayer(context) }

    var ayahs by remember { mutableStateOf<List<FullAyahItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadTrigger by remember { mutableStateOf(0) }

    val playbackState by audioPlayer.playbackState.collectAsState()
    val playingAyahInSurah by audioPlayer.currentAyahInSurah.collectAsState()
    val playingSurahNumber by audioPlayer.currentSurahNumber.collectAsState()
    val isAutoPlayNext by audioPlayer.autoPlayNext.collectAsState()

    // Setup auto-play next listener
    LaunchedEffect(ayahs, isAutoPlayNext, currentReciter) {
        audioPlayer.onAyahCompletedListener = { sNum, aNum ->
            if (isAutoPlayNext && sNum == surah.number) {
                val nextAyah = ayahs.find { it.numberInSurah == aNum + 1 }
                if (nextAyah != null) {
                    audioPlayer.playAyah(
                        surahNumber = surah.number,
                        ayahInSurah = nextAyah.numberInSurah,
                        globalNumber = nextAyah.globalNumber,
                        reciter = currentReciter
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }

    LaunchedEffect(surah.number, reloadTrigger) {
        isLoading = true
        errorMessage = null
        val result = viewModel.islamicRepo.fetchSurahAyahsFromApi(surah.number)
        result.fold(
            onSuccess = {
                ayahs = it
                isLoading = false
            },
            onFailure = {
                errorMessage = "এপিআই থেকে আয়াত লোড করা সম্ভব হয়নি: ${it.localizedMessage}"
                isLoading = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(surah.nameBangla, fontWeight = FontWeight.Bold)
                        Text("${surah.number}. ${surah.nameArabic} • ${surah.versesCount} আয়াত (${surah.type})", fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(onClick = onReciterClick) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "ক্বারী পরিবর্তন", tint = PaisaTealPrimary)
                    }
                    IconButton(onClick = onBookmark) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "বুকমার্ক")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaisaSurface)
            )
        },
        bottomBar = {
            // Floating Audio Mini Player when an Ayah is active
            AnimatedVisibility(
                visible = playbackState !is QuranPlaybackState.Idle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = PaisaSurface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PaisaTealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                if (playbackState is QuranPlaybackState.Buffering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Audiotrack,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "আয়াত $playingAyahInSurah বাজছে",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = currentReciter.nameBangla,
                                    fontSize = 11.sp,
                                    color = PaisaTealPrimary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Previous Ayah
                            IconButton(
                                onClick = {
                                    val prevAyah = ayahs.find { it.numberInSurah == playingAyahInSurah - 1 }
                                    if (prevAyah != null) {
                                        audioPlayer.playAyah(
                                            surahNumber = surah.number,
                                            ayahInSurah = prevAyah.numberInSurah,
                                            globalNumber = prevAyah.globalNumber,
                                            reciter = currentReciter
                                        )
                                    }
                                },
                                enabled = playingAyahInSurah > 1
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "পূর্ববর্তী আয়াত")
                            }

                            // Play/Pause Button
                            FilledIconButton(
                                onClick = { audioPlayer.togglePlayPause() },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = PaisaTealPrimary)
                            ) {
                                when (playbackState) {
                                    is QuranPlaybackState.Playing -> {
                                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White)
                                    }
                                    is QuranPlaybackState.Buffering -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    }
                                    else -> {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                                    }
                                }
                            }

                            // Next Ayah
                            IconButton(
                                onClick = {
                                    val nextAyah = ayahs.find { it.numberInSurah == playingAyahInSurah + 1 }
                                    if (nextAyah != null) {
                                        audioPlayer.playAyah(
                                            surahNumber = surah.number,
                                            ayahInSurah = nextAyah.numberInSurah,
                                            globalNumber = nextAyah.globalNumber,
                                            reciter = currentReciter
                                        )
                                    }
                                },
                                enabled = playingAyahInSurah < ayahs.size
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "পরবর্তী আয়াত")
                            }

                            // Stop Button
                            IconButton(onClick = { audioPlayer.stop() }) {
                                Icon(Icons.Default.Close, contentDescription = "Stop", tint = PaisaTextSecondary)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PaisaBackground)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Reciter & Auto-play bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(PaisaSurfaceVariant)
                            .clickable { onReciterClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = PaisaTealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentReciter.nameBangla,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PaisaTextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isAutoPlayNext) PaisaTealContainer else PaisaSurfaceVariant)
                            .clickable { audioPlayer.setAutoPlayNext(!isAutoPlayNext) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isAutoPlayNext) Icons.Default.PlaylistPlay else Icons.Default.PauseCircle,
                            contentDescription = null,
                            tint = if (isAutoPlayNext) PaisaTealDark else PaisaTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAutoPlayNext) "অটো-প্লে চালু" else "অটো-প্লে বন্ধ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAutoPlayNext) PaisaTealDark else PaisaTextSecondary
                        )
                    }
                }
            }

            // Bismillah Header (except for Surah At-Tawbah 9)
            if (surah.number != 9) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PaisaTealContainer)
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = PaisaTealDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "পরম করুণাময় অসীম দয়ালু আল্লাহর নামে শুরু করছি",
                                fontSize = 12.sp,
                                color = PaisaTextSecondary
                            )
                        }
                    }
                }
            }

            // Loading state indicator
            if (isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = PaisaTealPrimary, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "আল-কুরআন এপিআই থেকে আয়াতসমূহ লোড হচ্ছে...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PaisaTextSecondary
                            )
                        }
                    }
                }
            }

            // Error state with retry
            if (errorMessage != null && ayahs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PaisaExpenseLight),
                        border = if (MaterialTheme.colorScheme.outline == Color.Transparent) null else BorderStroke(1.dp, PaisaExpenseRed.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ইন্টারনেট সংযোগ চেক করুন",
                                fontWeight = FontWeight.Bold,
                                color = PaisaExpenseRed
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage ?: "লোড করতে ব্যর্থ হয়েছে",
                                style = MaterialTheme.typography.bodySmall,
                                color = PaisaTextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { reloadTrigger++ },
                                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("পুনরায় লোড করুন")
                            }
                        }
                    }
                }
            }

            // Complete list of ayahs with individual Play buttons
            items(ayahs) { ayah ->
                val isThisAyahActive = playingSurahNumber == surah.number && playingAyahInSurah == ayah.numberInSurah
                val isThisAyahPlaying = isThisAyahActive && playbackState is QuranPlaybackState.Playing
                val isThisAyahBuffering = isThisAyahActive && playbackState is QuranPlaybackState.Buffering

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isThisAyahActive) PaisaTealContainer.copy(alpha = 0.4f) else PaisaSurface
                    ),
                    border = if (isThisAyahActive) BorderStroke(1.5.dp, PaisaTealPrimary) else CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isThisAyahActive) PaisaTealDark else PaisaTealPrimary)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "আয়াত ${ayah.numberInSurah}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (isThisAyahPlaying) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "তিলাওয়াত চলছে...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PaisaTealDark
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Dedicated Play Button for this Ayah
                                FilledTonalIconButton(
                                    onClick = {
                                        if (isThisAyahPlaying) {
                                            audioPlayer.pause()
                                        } else if (isThisAyahActive && playbackState is QuranPlaybackState.Paused) {
                                            audioPlayer.resume()
                                        } else {
                                            audioPlayer.playAyah(
                                                surahNumber = surah.number,
                                                ayahInSurah = ayah.numberInSurah,
                                                globalNumber = ayah.globalNumber,
                                                reciter = currentReciter
                                            )
                                        }
                                    },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = if (isThisAyahActive) PaisaTealPrimary else PaisaTealContainer,
                                        contentColor = if (isThisAyahActive) Color.White else PaisaTealDark
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    if (isThisAyahBuffering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = if (isThisAyahActive) Color.White else PaisaTealDark
                                        )
                                    } else if (isThisAyahPlaying) {
                                        Icon(
                                            imageVector = Icons.Default.Pause,
                                            contentDescription = "Pause Ayah",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play Ayah",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "${surah.nameBangla} [${ayah.numberInSurah}]\n${ayah.arabicText}\n\n${ayah.banglaText}\n\n— Paisa Islamic"
                                            )
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "আয়াত শেয়ার করুন"))
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = PaisaTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Arabic Ayah Text
                        Text(
                            text = ayah.arabicText,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = PaisaTextPrimary,
                            lineHeight = 36.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = PaisaBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Bangla Translation
                        Text(
                            text = ayah.banglaText,
                            fontSize = 14.sp,
                            color = PaisaTextSecondary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            if (!isLoading && ayahs.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "সূরা ${surah.nameBangla} সম্পন্ন • ${ayahs.size}টি আয়াত",
                            style = MaterialTheme.typography.labelMedium,
                            color = PaisaTextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReciterSelectionDialog(
    currentReciter: QuranReciter,
    onSelect: (QuranReciter) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = PaisaTealPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "ক্বারী / তিলাওয়াতকারী নির্বাচন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QuranReciters.ALL_RECITERS) { reciter ->
                    val isSelected = reciter.id == currentReciter.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(reciter) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) PaisaTealContainer else PaisaSurface
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, PaisaTealPrimary) else CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reciter.nameBangla,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PaisaTextPrimary
                                )
                                Text(
                                    text = "${reciter.nameArabic} • ${reciter.style}",
                                    fontSize = 11.sp,
                                    color = if (isSelected) PaisaTealDark else PaisaTextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = PaisaTealPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = PaisaTealPrimary)
            }
        }
    )
}
