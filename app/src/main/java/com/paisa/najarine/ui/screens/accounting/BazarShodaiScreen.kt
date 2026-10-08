package com.paisa.najarine.ui.screens.accounting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.local.BazarItemEntity
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.*
import java.text.NumberFormat
import java.util.*

val BAZAR_CATEGORIES = listOf(
    "সকল সদাই",
    "চাল ও ডাল",
    "মাছ ও মাংস",
    "শাকসবজি",
    "তেল ও মসলা",
    "ডিম ও ডেয়ারি",
    "ফলমূল",
    "টয়লেট্রিজ ও ক্লিনিং",
    "অন্যান্য"
)

val BAZAR_UNITS = listOf("কেজি", "গ্রাম", "লিটার", "পিস", "ডজন", "প্যাকেট", "আঁটি", "বোতল")

data class BazarPresetItem(
    val name: String,
    val category: String,
    val defaultUnit: String,
    val defaultQty: Double,
    val estPrice: Double
)

val DEFAULT_BAZAR_PRESETS = listOf(
    BazarPresetItem("মিনিকেট চাল", "চাল ও ডাল", "কেজি", 5.0, 360.0),
    BazarPresetItem("মসুর ডাল", "চাল ও ডাল", "কেজি", 1.0, 135.0),
    BazarPresetItem("সয়াবিন তেল", "তেল ও মসলা", "লিটার", 2.0, 380.0),
    BazarPresetItem("ব্রয়লার মুরগি", "মাছ ও মাংস", "কেজি", 1.5, 300.0),
    BazarPresetItem("রুই মাছ", "মাছ ও মাংস", "কেজি", 1.0, 380.0),
    BazarPresetItem("ফার্মের লাল ডিম", "ডিম ও ডেয়ারি", "ডজন", 1.0, 150.0),
    BazarPresetItem("নতুন আলু", "শাকসবজি", "কেজি", 2.0, 80.0),
    BazarPresetItem("দেশি পেঁয়াজ", "শাকসবজি", "কেজি", 1.0, 90.0),
    BazarPresetItem("রসুন ও আদা", "তেল ও মসলা", "গ্রাম", 250.0, 70.0),
    BazarPresetItem("কাঁচা মরিচ", "শাকসবজি", "গ্রাম", 250.0, 40.0),
    BazarPresetItem("হলুদ ও মরিচ গুঁড়া", "তেল ও মসলা", "প্যাকেট", 1.0, 95.0),
    BazarPresetItem("আইয়োডিনযুক্ত লবণ", "তেল ও মসলা", "কেজি", 1.0, 42.0),
    BazarPresetItem("তরল দুধ", "ডিম ও ডেয়ারি", "লিটার", 1.0, 90.0),
    BazarPresetItem("ডিটারজেন্ট ও সাবান", "টয়লেট্রিজ ও ক্লিনিং", "প্যাকেট", 1.0, 140.0)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BazarShodaiScreen(
    viewModel: PaisaViewModel,
    onBackClick: () -> Unit,
    initialFilterMessOnly: Boolean = false
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val bazarItems by viewModel.bazarItems.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val messEntries by viewModel.messEntries.collectAsState()

    val messMembers = remember(messEntries) {
        messEntries.map { it.memberName }.filter { it.isNotBlank() }.distinct()
    }

    var selectedCategory by remember { mutableStateOf("সকল সদাই") }
    var showOnlyPending by remember { mutableStateOf(false) }
    var filterMessOnly by remember { mutableStateOf(initialFilterMessOnly) }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<BazarItemEntity?>(null) }
    var showConvertToExpenseDialog by remember { mutableStateOf(false) }

    val currencyFormat = remember {
        NumberFormat.getNumberInstance(Locale("bn", "BD")).apply {
            maximumFractionDigits = 0
        }
    }

    val filteredItems = remember(bazarItems, selectedCategory, showOnlyPending, filterMessOnly) {
        bazarItems.filter { item ->
            val catMatch = (selectedCategory == "সকল সদাই" || item.category == selectedCategory)
            val pendingMatch = if (showOnlyPending) !item.isChecked else true
            val messMatch = if (filterMessOnly) item.isMessItem else true
            catMatch && pendingMatch && messMatch
        }
    }

    val totalEstPrice = remember(filteredItems) {
        filteredItems.sumOf { if (it.actualPrice > 0) it.actualPrice else it.estimatedPrice }
    }

    val totalPurchasedPrice = remember(filteredItems) {
        filteredItems.filter { it.isChecked }.sumOf { if (it.actualPrice > 0) it.actualPrice else it.estimatedPrice }
    }

    val pendingCount = remember(filteredItems) { filteredItems.count { !it.isChecked } }
    val checkedCount = remember(filteredItems) { filteredItems.count { it.isChecked } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (filterMessOnly) "মেস বাজার সদাই তালিকা" else "বাজার সদাই ও গ্রোসারি তালিকা",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${filteredItems.size} টি সদাই • $pendingCount বাকি • $checkedCount কেনা শেষ",
                            fontSize = 11.sp,
                            color = PaisaTextSecondary
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
                        // Share Bazar list to clipboard
                        val text = buildString {
                            appendLine("🛒 বাজার সদাই তালিকা (${if (filterMessOnly) "মেস বাজার" else "পয়সা"})")
                            appendLine("--------------------------------")
                            filteredItems.forEachIndexed { i, it ->
                                val status = if (it.isChecked) "✅" else "⬜"
                                val price = if (it.actualPrice > 0) "৳${it.actualPrice.toInt()}" else if (it.estimatedPrice > 0) "~৳${it.estimatedPrice.toInt()}" else ""
                                appendLine("${i + 1}. $status ${it.name} - ${it.quantity} ${it.unit} $price ${if (it.note.isNotBlank()) "(${it.note})" else ""}")
                            }
                            appendLine("--------------------------------")
                            appendLine("মোট বাজেট: ৳${currencyFormat.format(totalEstPrice)}")
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Bazar List", text))
                        Toast.makeText(context, "বাজার তালিকা কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "শেয়ার করুন", tint = PaisaTealPrimary)
                    }

                    IconButton(onClick = {
                        if (checkedCount > 0) {
                            showConvertToExpenseDialog = true
                        } else {
                            Toast.makeText(context, "খরচে রূপান্তর করার মতো কেনা সদাই নেই", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "খরচে রূপান্তর করুন", tint = Color(0xFF047857))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaisaSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingItem = null
                    showAddItemDialog = true
                },
                containerColor = PaisaTealPrimary,
                modifier = Modifier.testTag("add_bazar_item_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "আইটেম যোগ করুন", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PaisaBackground)
                .padding(padding)
        ) {
            // 1. Budget Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(PaisaTealContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = PaisaTealPrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("বাজার খরচের হিসাব", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(if (filterMessOnly) "মেস বাজারের অংশ" else "গৃহস্থালি ও ব্যক্তিগত", fontSize = 10.sp, color = PaisaTextSecondary)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = filterMessOnly,
                                onClick = { filterMessOnly = !filterMessOnly },
                                label = { Text("মেস বাজার", fontSize = 11.sp) },
                                leadingIcon = if (filterMessOnly) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) } } else null
                            )

                            FilterChip(
                                selected = showOnlyPending,
                                onClick = { showOnlyPending = !showOnlyPending },
                                label = { Text("বাকি আছে", fontSize = 11.sp) },
                                leadingIcon = if (showOnlyPending) { { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(12.dp)) } } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("মোট বাজেট (Est.)", fontSize = 11.sp, color = PaisaTextSecondary)
                            Text("৳ ${currencyFormat.format(totalEstPrice)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PaisaTextPrimary)
                        }

                        Column {
                            Text("কেনা সম্পন্ন (Spent)", fontSize = 11.sp, color = PaisaTextSecondary)
                            Text("৳ ${currencyFormat.format(totalPurchasedPrice)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PaisaIncomeGreen)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("বাকি খরচ", fontSize = 11.sp, color = PaisaTextSecondary)
                            val remaining = (totalEstPrice - totalPurchasedPrice).coerceAtLeast(0.0)
                            Text("৳ ${currencyFormat.format(remaining)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFD97706))
                        }
                    }

                    if (checkedCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { viewModel.clearCheckedBazarItems() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("কেনা সদাই মুছে ফেলুন", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // 2. Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BAZAR_CATEGORIES) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        onClick = { selectedCategory = cat },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) PaisaTealPrimary else PaisaSurfaceVariant.copy(alpha = 0.6f),
                        border = if (!isSelected) BorderStroke(1.dp, PaisaBorderSubtle) else null
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else PaisaTextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 3. Quick Bangladeshi Item Presets Section
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text(
                        text = "+ দ্রুত যোগ:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTextSecondary,
                        modifier = Modifier.padding(top = 6.dp, end = 4.dp)
                    )
                }
                items(DEFAULT_BAZAR_PRESETS) { preset ->
                    Surface(
                        onClick = {
                            val newItem = BazarItemEntity(
                                id = UUID.randomUUID().toString(),
                                workspaceId = viewModel.activeWorkspaceId.value,
                                name = preset.name,
                                category = preset.category,
                                quantity = preset.defaultQty,
                                unit = preset.defaultUnit,
                                estimatedPrice = preset.estPrice,
                                actualPrice = 0.0,
                                isChecked = false,
                                isMessItem = filterMessOnly,
                                assignedMemberName = ""
                            )
                            viewModel.saveBazarItem(newItem)
                            Toast.makeText(context, "${preset.name} যোগ করা হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = "+ ${preset.name}",
                            fontSize = 11.sp,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4. Bazar Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ProductionQuantityLimits,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color.Gray.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "কোনো বাজার সদাই তালিকা নেই",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PaisaTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "উপরের দ্রুত যোগ বোতামে চাপুন অথবা নিচে নতুন সদাই আইটেম তৈরি করুন।",
                            fontSize = 12.sp,
                            color = PaisaTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                editingItem = null
                                showAddItemDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("নতুন সদাই যোগ করুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        BazarItemCard(
                            item = item,
                            currencyFormat = currencyFormat,
                            onToggleChecked = { isChecked ->
                                viewModel.toggleBazarItemChecked(item.id, isChecked)
                            },
                            onEditClick = {
                                editingItem = item
                                showAddItemDialog = true
                            },
                            onDeleteClick = {
                                viewModel.deleteBazarItem(item.id)
                                Toast.makeText(context, "${item.name} মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Item Dialog
    if (showAddItemDialog) {
        AddEditBazarItemDialog(
            item = editingItem,
            isMessContext = filterMessOnly,
            messMembers = messMembers,
            onDismiss = { showAddItemDialog = false },
            onSave = { savedItem ->
                if (editingItem != null) {
                    viewModel.updateBazarItem(savedItem)
                } else {
                    viewModel.saveBazarItem(savedItem)
                }
                showAddItemDialog = false
                Toast.makeText(context, "${savedItem.name} সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Convert Checked Items to Wallet Expense Dialog
    if (showConvertToExpenseDialog) {
        val checkedItems = remember(filteredItems) { filteredItems.filter { it.isChecked } }
        val checkedSum = remember(checkedItems) {
            checkedItems.sumOf { if (it.actualPrice > 0) it.actualPrice else it.estimatedPrice }
        }
        var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
        var expenseNote by remember {
            mutableStateOf(checkedItems.joinToString(", ") { "${it.name} (${it.quantity}${it.unit})" })
        }
        var isForMess by remember { mutableStateOf(filterMessOnly) }
        var buyerName by remember { mutableStateOf(messMembers.firstOrNull() ?: "") }

        AlertDialog(
            onDismissRequest = { showConvertToExpenseDialog = false },
            title = {
                Text("বাজার খরচ সংরক্ষণ করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "কেনা সম্পন্ন ${checkedItems.size} টি পণ্যের মোট খরচ ৳${currencyFormat.format(checkedSum)} আপনার ওয়ালেটে খরচ হিসেবে সংরক্ষণ করবেন?",
                        fontSize = 13.sp,
                        color = PaisaTextSecondary
                    )

                    OutlinedTextField(
                        value = expenseNote,
                        onValueChange = { expenseNote = it },
                        label = { Text("খরচের বিবরণ / নোট") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("টাকা পরিশোধের ওয়ালেট:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(wallets) { wallet ->
                            val isSel = selectedWalletId == wallet.id
                            Surface(
                                onClick = { selectedWalletId = wallet.id },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) PaisaTealPrimary else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = wallet.name,
                                    fontSize = 11.sp,
                                    color = if (isSel) Color.White else Color.Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isForMess, onCheckedChange = { isForMess = it })
                        Text("মেস ম্যানেজার বাজার খরচেও যুক্ত করুন", fontSize = 12.sp)
                    }

                    if (isForMess) {
                        OutlinedTextField(
                            value = buyerName,
                            onValueChange = { buyerName = it },
                            label = { Text("বাজারকারী মেস সদস্যের নাম") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createExpenseFromBazar(
                            amount = checkedSum,
                            walletId = selectedWalletId,
                            note = expenseNote,
                            isMessBazar = isForMess,
                            buyerMemberName = buyerName
                        )
                        showConvertToExpenseDialog = false
                        Toast.makeText(context, "বাজার খরচ ওয়ালেটে সফলভাবে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                ) {
                    Text("নিশ্চিত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConvertToExpenseDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun BazarItemCard(
    item: BazarItemEntity,
    currencyFormat: NumberFormat,
    onToggleChecked: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bazar_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) Color(0xFFF8FAFC) else PaisaSurface
        ),
        border = BorderStroke(
            1.dp,
            if (item.isChecked) Color(0xFFE2E8F0) else PaisaBorderSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = onToggleChecked,
                    colors = CheckboxDefaults.colors(
                        checkedColor = PaisaIncomeGreen,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (item.isChecked) Color.Gray else PaisaTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.isMessItem) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE0E7FF)
                            ) {
                                Text(
                                    text = "মেস",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3730A3),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity} ${item.unit}",
                            fontSize = 11.sp,
                            color = PaisaTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("•", fontSize = 10.sp, color = Color.LightGray)
                        Text(
                            text = item.category,
                            fontSize = 11.sp,
                            color = PaisaTextSecondary
                        )
                        if (item.assignedMemberName.isNotBlank()) {
                            Text("•", fontSize = 10.sp, color = Color.LightGray)
                            Text(
                                text = "দায়িত্ব: ${item.assignedMemberName}",
                                fontSize = 10.sp,
                                color = Color(0xFF0369A1)
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    val displayPrice = if (item.actualPrice > 0) item.actualPrice else item.estimatedPrice
                    if (displayPrice > 0) {
                        Text(
                            text = "৳ ${currencyFormat.format(displayPrice)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (item.isChecked) PaisaIncomeGreen else PaisaExpenseRed
                        )
                        Text(
                            text = if (item.actualPrice > 0) "প্রকৃত মূল্য" else "আনুমানিক",
                            fontSize = 9.sp,
                            color = PaisaTextSecondary
                        )
                    }
                }

                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = Color.Gray)
                }

                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444).copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun AddEditBazarItemDialog(
    item: BazarItemEntity?,
    isMessContext: Boolean,
    messMembers: List<String>,
    onDismiss: () -> Unit,
    onSave: (BazarItemEntity) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "শাকসবজি") }
    var quantityStr by remember { mutableStateOf(item?.quantity?.toString() ?: "1") }
    var unit by remember { mutableStateOf(item?.unit ?: "কেজি") }
    var estPriceStr by remember { mutableStateOf(if ((item?.estimatedPrice ?: 0.0) > 0) item?.estimatedPrice.toString() else "") }
    var actualPriceStr by remember { mutableStateOf(if ((item?.actualPrice ?: 0.0) > 0) item?.actualPrice.toString() else "") }
    var isMessItem by remember { mutableStateOf(item?.isMessItem ?: isMessContext) }
    var assignedMember by remember { mutableStateOf(item?.assignedMemberName ?: "") }
    var note by remember { mutableStateOf(item?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (item != null) "সদাই আইটেম সম্পাদনা" else "নতুন বাজার সদাই যুক্ত করুন",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("পণ্যের নাম (চাল, তেল, মুরগি, ইত্যাদি)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selector
                Text("ক্যাটাগরি:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(BAZAR_CATEGORIES.filter { it != "সকল সদাই" }) { cat ->
                        val isSel = category == cat
                        Surface(
                            onClick = { category = cat },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) PaisaTealPrimary else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 10.sp,
                                color = if (isSel) Color.White else Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Quantity & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("পরিমাণ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("একক (Unit):", fontSize = 10.sp, color = PaisaTextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(BAZAR_UNITS) { u ->
                                val isU = unit == u
                                Surface(
                                    onClick = { unit = u },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isU) PaisaTealPrimary else Color(0xFFE2E8F0)
                                ) {
                                    Text(
                                        text = u,
                                        fontSize = 10.sp,
                                        color = if (isU) Color.White else Color.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Prices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = estPriceStr,
                        onValueChange = { estPriceStr = it },
                        label = { Text("আনুমানিক মূল্য (৳)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = actualPriceStr,
                        onValueChange = { actualPriceStr = it },
                        label = { Text("প্রকৃত মূল্য (৳)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isMessItem, onCheckedChange = { isMessItem = it })
                    Text("মেস বাজার সদাই হিসেবে চিহ্নিত করুন", fontSize = 12.sp)
                }

                if (isMessItem && messMembers.isNotEmpty()) {
                    Text("বাজারের দায়িত্বে সদস্য:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(messMembers) { mem ->
                            val isSel = assignedMember == mem
                            Surface(
                                onClick = { assignedMember = if (isSel) "" else mem },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF0284C7) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = mem,
                                    fontSize = 11.sp,
                                    color = if (isSel) Color.White else Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("নোট / বিশেষ নির্দেশনা") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val qty = quantityStr.toDoubleOrNull() ?: 1.0
                    val estP = estPriceStr.toDoubleOrNull() ?: 0.0
                    val actP = actualPriceStr.toDoubleOrNull() ?: 0.0

                    val result = item?.copy(
                        name = name.trim(),
                        category = category,
                        quantity = qty,
                        unit = unit,
                        estimatedPrice = estP,
                        actualPrice = actP,
                        isMessItem = isMessItem,
                        assignedMemberName = assignedMember,
                        note = note.trim()
                    ) ?: BazarItemEntity(
                        id = UUID.randomUUID().toString(),
                        workspaceId = "personal_default",
                        name = name.trim(),
                        category = category,
                        quantity = qty,
                        unit = unit,
                        estimatedPrice = estP,
                        actualPrice = actP,
                        isChecked = false,
                        isMessItem = isMessItem,
                        assignedMemberName = assignedMember,
                        note = note.trim()
                    )
                    onSave(result)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
            ) {
                Text(if (item != null) "আপডেট করুন" else "যোগ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
