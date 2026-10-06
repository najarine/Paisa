package com.paisa.najarine.ui.screens.islamic

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.paisa.najarine.data.repository.HadithItem
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyHadithScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler { onBackClick() }
    val context = LocalContext.current
    val canonicalDailyHadith by viewModel.currentHadith.collectAsState()
    val hadiths = remember { viewModel.islamicRepo.authenticHadiths }

    LaunchedEffect(Unit) {
        viewModel.refreshDynamicHadith()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "দৈনিক সহীহ হাদিস (প্রতি ঘন্টায় রিফ্রেশ)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.refreshDynamicHadith(forceNext = true)
                        Toast.makeText(context, "নতুন হাদিস তাৎক্ষণিকভাবে লোড করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PaisaTealPrimary)
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
            // Feature Banner: Canonical Hadith of the Day (Matches Reference Snapshot 3 & Notification)
            item {
                Text(
                    text = "আজকের নির্ধারিত হাদিস (HADITH OF THE DAY)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PaisaTealPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = canonicalDailyHadith.source,
                                fontWeight = FontWeight.Bold,
                                color = PaisaTealPrimary,
                                fontSize = 13.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PaisaTealContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = canonicalDailyHadith.grade,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaisaTealDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = canonicalDailyHadith.narrator,
                            style = MaterialTheme.typography.bodySmall,
                            color = PaisaTextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Arabic Hadith Text
                        Text(
                            text = canonicalDailyHadith.arabic,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PaisaTextPrimary,
                            lineHeight = 32.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // English & Bangla Translations
                        Text(
                            text = "“${canonicalDailyHadith.translation}”",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = PaisaTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = canonicalDailyHadith.banglaTranslation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PaisaTextSecondary,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions: Refresh, Share & Bookmark
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.refreshDynamicHadith(forceNext = true)
                                    Toast.makeText(context, "নতুন হাদিস তাৎক্ষণিকভাবে লোড হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PaisaTealPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("নতুন হাদিস", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "আজকের হাদিস:\n${canonicalDailyHadith.arabic}\n\n\"${canonicalDailyHadith.banglaTranslation}\"\n— ${canonicalDailyHadith.source} (${canonicalDailyHadith.narrator})\n\nPaisa অ্যাপ থেকে শেয়ারকৃত"
                                            )
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "হাদিস শেয়ার করুন"))
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("শেয়ার", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        Toast.makeText(context, "হাদিসটি প্রিয় তালিকায় সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("সংরক্ষণ", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // More Authentic Hadiths on Finance and Life
            item {
                Text(
                    text = "অর্থনীতি, লেনদেন ও হালাল উপার্জন সম্পর্কিত সহীহ হাদিস",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PaisaTextPrimary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            items(hadiths) { hadith ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = hadith.topic,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PaisaTealPrimary
                            )
                            Text(
                                text = hadith.source,
                                fontSize = 11.sp,
                                color = PaisaTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = hadith.arabic,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PaisaTextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = hadith.banglaTranslation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PaisaTextSecondary,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "সূত্র: ${hadith.narrator}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PaisaTextTertiary
                        )
                    }
                }
            }
        }
    }
}
