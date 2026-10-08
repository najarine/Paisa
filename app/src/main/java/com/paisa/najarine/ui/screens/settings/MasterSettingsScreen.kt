package com.paisa.najarine.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

data class BanglaSettingItem(
    val title: String,
    val subtitle: String,
    val category: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterSettingsScreen(
    viewModel: PaisaViewModel,
    onOpenProfile: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSyncCenter: () -> Unit,
    onOpenSupportDeveloper: () -> Unit = {},
    onSignOutClick: () -> Unit,
    onSwitchAccountClick: () -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val authUser = remember { FirebaseAuth.getInstance().currentUser }
    val sec = viewModel.securityManager

    var searchQuery by remember { mutableStateOf("") }
    var isBioEnabled by remember { mutableStateOf(sec.isSecurityEnabled()) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Settings Search Items in Bangla
    var selectedMadhab by remember { mutableStateOf(com.paisa.najarine.notification.AdhanPreferences.getSelectedMadhab(context)) }
    var showMadhabDialog by remember { mutableStateOf(false) }

    val allSettings = remember(selectedMadhab) {
        listOf(
            BanglaSettingItem("ব্যবহারকারী প্রোফাইল", "গুগল আইডি ও ব্যক্তিগত তথ্য দেখুন বা যোগ করুন", "অ্যাকাউন্ট", Icons.Default.Person, onOpenProfile),
            BanglaSettingItem("গুগল অ্যাকাউন্ট", "লগইনকৃত: ${authUser?.email ?: "ব্যবহারকারী"}", "অ্যাকাউন্ট", Icons.Default.AccountCircle, onSwitchAccountClick),
            BanglaSettingItem("বায়োমেট্রিক অ্যাপ লক", "আঙুলের ছাপ বা ফেস আনলক দ্বারা অ্যাপ সুরক্ষিত করুন", "নিরাপত্তা", Icons.Default.Fingerprint) {
                isBioEnabled = !isBioEnabled
                sec.setSecurityEnabled(isBioEnabled)
            },
            BanglaSettingItem("পিন (PIN) সুরক্ষা", "৪-সংখ্যার ব্যক্তিগত গোপন পিন পরিবর্তন করুন", "নিরাপত্তা", Icons.Default.Pin) {
                Toast.makeText(context, "পিন সেটিংস সক্রিয়", Toast.LENGTH_SHORT).show()
            },
            BanglaSettingItem("সিঙ্ক ও ব্যাকআপ কেন্দ্র", "রুম এসকিউলাইট অফলাইন ডাটাবেস ও ক্লাউড অটো-সিঙ্ক", "সিঙ্ক", Icons.Default.Sync, onOpenSyncCenter),
            BanglaSettingItem("ডিফল্ট মুদ্রা", "বাংলাদেশী টাকা: BDT (৳)", "অর্থব্যবস্থা", Icons.Default.Payments) {
                Toast.makeText(context, "ডিফল্ট মুদ্রা: BDT (৳)", Toast.LENGTH_SHORT).show()
            },
            BanglaSettingItem("নামাজ গণনা পদ্ধতি", "University of Islamic Sciences, Karachi (করাচি)", "ইসলামিক", Icons.Default.Mosque) {},
            BanglaSettingItem("মাজহাব নির্বাচন", if (selectedMadhab == "Hanafi") "হানাফী (Hanafi) — আসর ২য় ছায়া" else "শাফেয়ী/মালেকি/হাম্বলি — আসর ১ম ছায়া", "ইসলামিক", Icons.Default.Mosque) {
                showMadhabDialog = true
            },
            BanglaSettingItem("আযান অডিও নির্বাচন", "যোহর, আসর, মাগরিব, ইশা ও ফজর আযান অডিও", "ইসলামিক", Icons.Default.VolumeUp) {},
            BanglaSettingItem("ডেভেলপারকে সহায়তা করুন", "যোগাযোগ: najarine@gmail.com", "সহায়তা", Icons.Default.VolunteerActivism, onOpenSupportDeveloper)
        )
    }

    val filteredSettings = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allSettings.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.subtitle.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "সেটিংস ও নিয়ন্ত্রণ",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "ফিরে যান")
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
            // Bangla Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("সেটিংস খুঁজুন (যেমন: প্রোফাইল, আযান, সিঙ্ক, পিন, মাজহাব)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PaisaTextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Search Results (if user is searching)
            if (searchQuery.isNotEmpty()) {
                item {
                    Text(
                        text = "অনুসন্ধানের ফলাফল (${filteredSettings.size}টি)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PaisaTealPrimary
                    )
                }

                items(filteredSettings) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { item.onClick() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(item.icon, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(item.subtitle, fontSize = 11.sp, color = PaisaTextSecondary)
                            }
                        }
                    }
                }
            } else {
                // SECTION 1: অ্যাকাউন্ট ও প্রোফাইল
                item {
                    SettingsSectionTitle(title = "অ্যাকাউন্ট ও প্রোফাইল", icon = Icons.Default.AccountCircle)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenProfile() },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    com.paisa.najarine.ui.components.PaisaLogoBadge(
                                        size = 46.dp,
                                        fontSize = 24.sp,
                                        cornerRadius = 14.dp,
                                        borderWidth = 1.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(authUser?.displayName ?: "সম্মানিত ব্যবহারকারী", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(authUser?.email ?: "গুগল অ্যাকাউন্ট সংযুক্ত", fontSize = 12.sp, color = PaisaTextSecondary)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PaisaTextTertiary)
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = onOpenProfile) {
                                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("প্রোফাইল তথ্য")
                                }
                                TextButton(
                                    onClick = { showLogoutConfirmDialog = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = PaisaExpenseRed)
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("সাইন আউট")
                                }
                            }
                        }
                    }
                }

                // SECTION 2: নিরাপত্তা ও গোপনীয়তা
                item {
                    SettingsSectionTitle(title = "নিরাপত্তা ও গোপনীয়তা", icon = Icons.Default.Security)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("বায়োমেট্রিক ও ফিঙ্গারপ্রিন্ট লক", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("অ্যাপ খোলার সময় ফিঙ্গারপ্রিন্ট বা ফেসলক আবশ্যক", fontSize = 11.sp, color = PaisaTextSecondary)
                                }
                                Switch(
                                    checked = isBioEnabled,
                                    onCheckedChange = {
                                        isBioEnabled = it
                                        sec.setSecurityEnabled(it)
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = PaisaTealPrimary)
                                )
                            }
                        }
                    }
                }

                // SECTION 3: ক্লাউড অটো-সিঙ্ক কেন্দ্র
                item {
                    SettingsSectionTitle(title = "ক্লাউড অটো-সিঙ্ক ও ব্যাকআপ", icon = Icons.Default.CloudSync)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SettingsNavRow(
                                title = "ক্লাউড সিঙ্ক কেন্দ্র (Firebase)",
                                subtitle = "অটোমেটিক রিয়েলটাইম সিঙ্ক ও লোকাল ডাটাবেস",
                                icon = Icons.Default.CloudDone,
                                onClick = onOpenSyncCenter
                            )
                        }
                    }
                }

                // SECTION 4: ইসলামিক সেটিংস
                item {
                    SettingsSectionTitle(title = "নামাজ ও ইসলামিক সেটিংস", icon = Icons.Default.Mosque)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SettingsNavRow(
                                title = "গণনা পদ্ধতি ও মাজহাব নির্বাচন",
                                subtitle = if (selectedMadhab.equals("Hanafi", ignoreCase = true) || selectedMadhab.contains("হানাফী"))
                                    "হানাফী (Hanafi) — আসর ২য় ছায়া"
                                else
                                    "শাফেয়ী / মালেকী / হাম্বলী — আসর ১ম ছায়া",
                                icon = Icons.Default.Schedule,
                                onClick = { showMadhabDialog = true }
                            )
                        }
                    }
                }

                // SECTION 5: সহায়তা
                item {
                    SettingsSectionTitle(title = "সহায়তা ও অবদান", icon = Icons.Default.VolunteerActivism)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSupportDeveloper() },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PaisaTealContainer),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("ডেভেলপারকে সহায়তা করুন", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PaisaTealDark)
                                    Text("ইমেইল: najarine@gmail.com • অনুদান দিন", fontSize = 11.sp, color = PaisaTextSecondary)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PaisaTealPrimary)
                        }
                    }
                }
            }
        }
    }

    if (showMadhabDialog) {
        val isHanafi = selectedMadhab.equals("Hanafi", ignoreCase = true) || selectedMadhab.contains("হানাফী")
        AlertDialog(
            onDismissRequest = { showMadhabDialog = false },
            icon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = PaisaTealPrimary) },
            title = { Text("মাজহাব ও আসর ওয়াক্ত নির্বাচন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "আসর ওয়াক্তের ছায়ার পরিমাপ অনুযায়ী সঠিক ওয়াক্ত নির্ধারিত হয়। আপনার মাজহাব নির্বাচন করুন:",
                        fontSize = 13.sp,
                        color = PaisaTextSecondary
                    )

                    // Hanafi Option
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isHanafi) PaisaTealContainer.copy(alpha = 0.5f) else PaisaSurfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (isHanafi && MaterialTheme.colorScheme.outline != Color.Transparent) androidx.compose.foundation.BorderStroke(1.5.dp, PaisaTealPrimary) else null,
                        onClick = {
                            selectedMadhab = "Hanafi"
                            com.paisa.najarine.notification.AdhanPreferences.setSelectedMadhab(context, "Hanafi")
                            viewModel.refreshPrayerTimings("Hanafi")
                            com.paisa.najarine.notification.PrayerNotificationWorker.triggerImmediateCalculation(context)
                            showMadhabDialog = false
                            Toast.makeText(context, "হানাফী মাজহাব নির্ধারিত হয়েছে (আসর ২য় ছায়া)", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("হানাফী (Hanafi)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PaisaTextPrimary)
                                Text("বস্তুর ছায়া ২ গুণ (দ্বিগুণ)", fontSize = 11.sp, color = PaisaTextSecondary)
                            }
                            RadioButton(
                                selected = isHanafi,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = PaisaTealPrimary)
                            )
                        }
                    }

                    // Shafi'i / Others Option
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isHanafi) PaisaTealContainer.copy(alpha = 0.5f) else PaisaSurfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (!isHanafi && MaterialTheme.colorScheme.outline != Color.Transparent) androidx.compose.foundation.BorderStroke(1.5.dp, PaisaTealPrimary) else null,
                        onClick = {
                            selectedMadhab = "Shafi"
                            com.paisa.najarine.notification.AdhanPreferences.setSelectedMadhab(context, "Shafi")
                            viewModel.refreshPrayerTimings("Shafi")
                            com.paisa.najarine.notification.PrayerNotificationWorker.triggerImmediateCalculation(context)
                            showMadhabDialog = false
                            Toast.makeText(context, "শাফেয়ী/মালেকী/হাম্বলী নির্ধারিত হয়েছে (আসর ১ম ছায়া)", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("শাফেয়ী / মালেকী / হাম্বলী", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PaisaTextPrimary)
                                Text("বস্তুর ছায়া ১ গুণ (সমান)", fontSize = 11.sp, color = PaisaTextSecondary)
                            }
                            RadioButton(
                                selected = !isHanafi,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = PaisaTealPrimary)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMadhabDialog = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = PaisaExpenseRed) },
            title = { Text("সাইন আউট নিশ্চিতকরণ", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "আপনার ক্লাউড ডাটা নিরাপদে সংরক্ষিত থাকবে। সাইন আউট করলে শুধুমাত্র এই ডিভাইস থেকে সেশনটি বন্ধ হবে।",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onSignOutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaExpenseRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("সাইন আউট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = PaisaTealPrimary
        )
    }
}

@Composable
fun SettingsNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PaisaTextPrimary)
                Text(subtitle, fontSize = 11.sp, color = PaisaTextSecondary)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PaisaTextTertiary, modifier = Modifier.size(18.dp))
    }
}
