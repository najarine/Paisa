package com.paisa.najarine.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.local.CategoryEntity
import com.paisa.najarine.data.local.TransactionEntity
import com.paisa.najarine.data.local.WalletEntity
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.components.BankMfsLogo
import com.paisa.najarine.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: PaisaViewModel,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.transactions.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, INCOME, EXPENSE, TRANSFER
    var searchQuery by remember { mutableStateOf("") }
    var selectedWalletId by remember { mutableStateOf<String?>(null) }

    var showTypeChooserDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showVoiceDraftDialog by remember { mutableStateOf(false) }
    var showScanDraftDialog by remember { mutableStateOf(false) }
    var selectedTxForDetail by remember { mutableStateOf<TransactionEntity?>(null) }

    val filteredTransactions = allTransactions.filter { tx ->
        val matchesType = when (selectedFilter) {
            "INCOME" -> tx.type == "INCOME"
            "EXPENSE" -> tx.type == "EXPENSE"
            "TRANSFER" -> tx.type == "TRANSFER"
            else -> true
        }
        val matchesWallet = selectedWalletId == null || tx.walletId == selectedWalletId || tx.toWalletId == selectedWalletId
        val matchesSearch = searchQuery.isEmpty() ||
                tx.note.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.amount.toString().contains(searchQuery)
        matchesType && matchesWallet && matchesSearch
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Secondary quick tools
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallFloatingActionButton(
                        onClick = { showScanDraftDialog = true },
                        containerColor = PaisaSurfaceVariant,
                        contentColor = PaisaTextPrimary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "Receipt OCR Draft", modifier = Modifier.size(18.dp))
                    }
                    SmallFloatingActionButton(
                        onClick = { showVoiceDraftDialog = true },
                        containerColor = PaisaSurfaceVariant,
                        contentColor = PaisaTextPrimary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Draft", modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                // Primary Add Transaction (Prompts choice between Income & Expense)
                ExtendedFloatingActionButton(
                    onClick = { showTypeChooserDialog = true },
                    containerColor = PaisaTealPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Transaction") },
                    text = { Text("+ Transaction", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(PaisaBackground)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Transactions & Ledger",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTextPrimary
                    )
                    Text(
                        text = "Real-time records. All writes work offline and sync locally to Room.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaisaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search transactions, notes, categories...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PaisaTextSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PaisaSurface,
                            unfocusedContainerColor = PaisaSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Type Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "All",
                            "EXPENSE" to "Expenses",
                            "INCOME" to "Income",
                            "TRANSFER" to "Transfers"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedFilter == key,
                                onClick = { selectedFilter = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Wallet Filter Row
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedWalletId == null,
                                onClick = { selectedWalletId = null },
                                label = { Text("All Accounts", fontSize = 11.sp) }
                            )
                        }
                        items(wallets) { w ->
                            FilterChip(
                                selected = selectedWalletId == w.id,
                                onClick = { selectedWalletId = if (selectedWalletId == w.id) null else w.id },
                                leadingIcon = { BankMfsLogo(institutionId = w.institutionId, size = 18.dp) },
                                label = { Text(w.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Quick Type Buttons Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showExpenseDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PaisaExpenseRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Expense", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showIncomeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PaisaIncomeGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Income", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showTransferDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PaisaTransferBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Transfer", fontSize = 12.sp)
                    }
                }
            }

            // Results count
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "History (${filteredTransactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTextSecondary
                    )
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching transactions found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PaisaTextSecondary
                        )
                    }
                }
            } else {
                items(filteredTransactions) { tx ->
                    val sourceWallet = wallets.find { it.id == tx.walletId }
                    val destWallet = wallets.find { it.id == tx.toWalletId }
                    TransactionCard(
                        tx = tx,
                        sourceWallet = sourceWallet,
                        destWallet = destWallet,
                        onClick = { selectedTxForDetail = tx }
                    )
                }
            }
        }
    }

    // Transaction Type Chooser Dialog (User Request: 2 options for Income vs Expense)
    if (showTypeChooserDialog) {
        TransactionTypeChooserDialog(
            onDismiss = { showTypeChooserDialog = false },
            onSelectIncome = {
                showTypeChooserDialog = false
                showIncomeDialog = true
            },
            onSelectExpense = {
                showTypeChooserDialog = false
                showExpenseDialog = true
            },
            onSelectTransfer = {
                showTypeChooserDialog = false
                showTransferDialog = true
            }
        )
    }

    // Add Expense Dialog
    if (showExpenseDialog) {
        AddExpenseDialog(
            wallets = wallets,
            categories = categories.filter { it.type == "EXPENSE" },
            bills = viewModel.bills.collectAsState().value,
            onDismiss = { showExpenseDialog = false },
            onSwitchToIncome = {
                showExpenseDialog = false
                showIncomeDialog = true
            },
            onConfirm = { walletId, amount, cat, fee, note ->
                viewModel.addExpense(walletId, amount, cat, fee, note)
                showExpenseDialog = false
            }
        )
    }

    // Add Income Dialog
    if (showIncomeDialog) {
        AddIncomeDialog(
            wallets = wallets,
            categories = categories.filter { it.type == "INCOME" },
            onDismiss = { showIncomeDialog = false },
            onSwitchToExpense = {
                showIncomeDialog = false
                showExpenseDialog = true
            },
            onConfirm = { walletId, amount, cat, note ->
                viewModel.addIncome(walletId, amount, cat, note)
                showIncomeDialog = false
            }
        )
    }

    // Transfer Dialog
    if (showTransferDialog) {
        AddTransferDialog(
            wallets = wallets,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromId, toId, amount, fee, note ->
                viewModel.addTransfer(fromId, toId, amount, fee, note)
                showTransferDialog = false
            }
        )
    }

    // Voice Entry Structured Draft Dialog (Strict: Draft -> Review -> Confirm -> Room)
    if (showVoiceDraftDialog) {
        VoiceDraftDialog(
            wallets = wallets,
            onDismiss = { showVoiceDraftDialog = false },
            onConfirmDraft = { walletId, amount, type, category, note ->
                if (type == "INCOME") {
                    viewModel.addIncome(walletId, amount, category, note)
                } else {
                    viewModel.addExpense(walletId, amount, category, 0.0, note)
                }
                showVoiceDraftDialog = false
            }
        )
    }

    // Receipt Scanner Structured Draft Dialog (Strict: Draft -> Review -> Confirm -> Room)
    if (showScanDraftDialog) {
        ReceiptScanDraftDialog(
            wallets = wallets,
            onDismiss = { showScanDraftDialog = false },
            onConfirmDraft = { walletId, amount, category, note ->
                viewModel.addExpense(walletId, amount, category, 0.0, note)
                showScanDraftDialog = false
            }
        )
    }

    // Transaction Detail & Delete Dialog
    selectedTxForDetail?.let { tx ->
        TransactionDetailDialog(
            tx = tx,
            sourceWallet = wallets.find { it.id == tx.walletId },
            destWallet = wallets.find { it.id == tx.toWalletId },
            onDismiss = { selectedTxForDetail = null },
            onDelete = {
                viewModel.deleteTransaction(tx)
                selectedTxForDetail = null
            }
        )
    }
}

@Composable
fun TransactionCard(
    tx: TransactionEntity,
    sourceWallet: WalletEntity?,
    destWallet: WalletEntity?,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val isIncome = tx.type == "INCOME"
    val isTransfer = tx.type == "TRANSFER"

    val color = when {
        isIncome -> PaisaIncomeGreen
        isTransfer -> PaisaTransferBlue
        else -> PaisaExpenseRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
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
                // Wallet logo of primary institution
                BankMfsLogo(
                    institutionId = sourceWallet?.institutionId ?: "cash",
                    size = 42.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (tx.note.isNotEmpty()) tx.note else tx.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val walletText = if (isTransfer) {
                        "${sourceWallet?.name ?: "Wallet"} ➔ ${destWallet?.name ?: "Wallet"}"
                    } else {
                        "${sourceWallet?.name ?: "Wallet"} • ${tx.category}"
                    }
                    Text(
                        text = walletText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaisaTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateFormat.format(Date(tx.dateMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = PaisaTextTertiary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val prefix = when {
                    isIncome -> "+৳ "
                    isTransfer -> "৳ "
                    else -> "-৳ "
                }
                Text(
                    text = "$prefix${String.format(Locale.US, "%,.0f", tx.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (tx.fee > 0) {
                    Text(
                        text = "Fee: ৳${String.format(Locale.US, "%.0f", tx.fee)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaisaTextTertiary
                    )
                }
            }
        }
    }
}

// ----------------- DIALOGS ------------------

@Composable
fun TransactionTypeChooserDialog(
    onDismiss: () -> Unit,
    onSelectIncome: () -> Unit,
    onSelectExpense: () -> Unit,
    onSelectTransfer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "নতুন লেনদেন যোগ করুন",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = PaisaTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "লেনদেনের ধরন নির্বাচন করুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = PaisaTextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option 1: Expense
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectExpense() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaExpenseRed.copy(alpha = 0.08f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PaisaExpenseRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "ব্যয় যোগ করুন (Expense)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PaisaExpenseRed
                            )
                            Text(
                                text = "বাজার, খাওয়া, বিল, কেনাকাটা ও অন্যান্য খরচ",
                                fontSize = 11.sp,
                                color = PaisaTextSecondary
                            )
                        }
                    }
                }

                // Option 2: Income
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectIncome() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaIncomeGreen.copy(alpha = 0.08f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PaisaIncomeGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SouthWest, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "আয় যোগ করুন (Income)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PaisaIncomeGreen
                            )
                            Text(
                                text = "বেতন, ফ্রিল্যান্সিং, ব্যবসা বা লভ্যাংশ জমা",
                                fontSize = 11.sp,
                                color = PaisaTextSecondary
                            )
                        }
                    }
                }

                // Option 3: Transfer
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTransfer() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PaisaSurface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PaisaTransferBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = PaisaTransferBlue)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "ওয়ালেট স্থানান্তর (Transfer)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = PaisaTextPrimary
                            )
                            Text(
                                text = "এক ওয়ালেট বা ব্যাংক থেকে অন্যটিতে টাকা পাঠানো",
                                fontSize = 11.sp,
                                color = PaisaTextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    wallets: List<WalletEntity>,
    categories: List<CategoryEntity>,
    bills: List<com.paisa.najarine.data.local.BillSubscriptionEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSwitchToIncome: () -> Unit = {},
    onConfirm: (walletId: String, amount: Double, category: String, fee: Double, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf(wallets.firstOrNull()) }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Food & Dining") }
    var walletDropdownOpen by remember { mutableStateOf(false) }
    var billDropdownOpen by remember { mutableStateOf(false) }
    var selectedBill by remember { mutableStateOf<com.paisa.najarine.data.local.BillSubscriptionEntity?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text("ব্যয় রেকর্ড করুন (Record Expense)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Quick Toggle Row between Expense and Income
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PaisaBackground)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PaisaExpenseRed)
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔴 ব্যয় (Expense)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isSubmitting) { onSwitchToIncome() }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("আয় (Income) ➔", fontWeight = FontWeight.SemiBold, color = PaisaTextSecondary, fontSize = 12.sp)
                    }
                }
                // Bills & Subscriptions Dropdown (Optional Quick Autofill)
                if (bills.isNotEmpty()) {
                    Text("রিকারিং বিল / সাবস্ক্রিপশন (ঐচ্ছিক)", style = MaterialTheme.typography.labelSmall, color = PaisaTealPrimary, fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(
                        expanded = billDropdownOpen,
                        onExpandedChange = { if (!isSubmitting) billDropdownOpen = !billDropdownOpen },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedBill?.let { "${it.name} (৳${it.amount})" } ?: "সংরক্ষিত বিল থেকে সিলেক্ট করুন...",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = PaisaTealPrimary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = billDropdownOpen) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = billDropdownOpen,
                            onDismissRequest = { billDropdownOpen = false }
                        ) {
                            bills.forEach { b ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(b.name, fontWeight = FontWeight.SemiBold)
                                            Text("৳ ${b.amount}", color = PaisaExpenseRed, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        selectedBill = b
                                        amountText = b.amount.toString()
                                        selectedCategory = "Utility & Bills"
                                        note = "বিল পরিশোধ: ${b.name}"
                                        billDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Expense Amount (৳)") },
                    placeholder = { Text("e.g. 450") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Select Wallet with Logo Dropdown
                Text("Deduct From Wallet / Bank", style = MaterialTheme.typography.labelMedium, color = PaisaTextSecondary)
                ExposedDropdownMenuBox(
                    expanded = walletDropdownOpen,
                    onExpandedChange = { if (!isSubmitting) walletDropdownOpen = !walletDropdownOpen },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedWallet?.name ?: "Select Wallet",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            selectedWallet?.let {
                                BankMfsLogo(institutionId = it.institutionId, size = 26.dp, modifier = Modifier.padding(start = 6.dp))
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletDropdownOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = walletDropdownOpen,
                        onDismissRequest = { walletDropdownOpen = false }
                    ) {
                        wallets.forEach { w ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BankMfsLogo(institutionId = w.institutionId, size = 28.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(w.name, fontWeight = FontWeight.SemiBold)
                                            Text("৳ ${String.format(Locale.US, "%,.0f", w.balance)}", fontSize = 11.sp, color = PaisaTextSecondary)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedWallet = w
                                    walletDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Category Selector Chips
                Text("Category", style = MaterialTheme.typography.labelMedium, color = PaisaTextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val defaultCats = listOf("Food & Dining", "Groceries / Bazar", "Transport & Fuel", "Utility & Bills", "Health", "Shopping", "Charity")
                    items(defaultCats) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Description") },
                    placeholder = { Text("e.g. Bazar groceries") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Optional Fee
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it },
                    label = { Text("MFS / Gateway Fee (optional)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (isSubmitting) return@Button
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    val fee = feeText.toDoubleOrNull() ?: 0.0
                    val targetWalletId = selectedWallet?.id ?: wallets.firstOrNull()?.id ?: "wallet_default_cash"
                    if (amt > 0) {
                        isSubmitting = true
                        onConfirm(targetWalletId, amt, selectedCategory, fee, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PaisaExpenseRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isSubmitting) "সংরক্ষণ হচ্ছে..." else "ব্যয় সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = onDismiss
            ) { Text("বাতিল") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeDialog(
    wallets: List<WalletEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSwitchToExpense: () -> Unit = {},
    onConfirm: (walletId: String, amount: Double, category: String, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf(wallets.firstOrNull()) }
    var selectedCategory by remember { mutableStateOf("Salary & Income") }
    var walletDropdownOpen by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("আয় রেকর্ড করুন (Record Income)", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Quick Toggle Row between Income and Expense
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PaisaBackground)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isSubmitting) { onSwitchToExpense() }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("← ব্যয় (Expense)", fontWeight = FontWeight.SemiBold, color = PaisaTextSecondary, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PaisaIncomeGreen)
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🟢 আয় (Income)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Income Amount (৳)") },
                    placeholder = { Text("e.g. 50000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Deposit To Wallet / Bank", style = MaterialTheme.typography.labelMedium, color = PaisaTextSecondary)
                ExposedDropdownMenuBox(
                    expanded = walletDropdownOpen,
                    onExpandedChange = { if (!isSubmitting) walletDropdownOpen = !walletDropdownOpen },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedWallet?.name ?: "Select Wallet",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            selectedWallet?.let {
                                BankMfsLogo(institutionId = it.institutionId, size = 26.dp, modifier = Modifier.padding(start = 6.dp))
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletDropdownOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = walletDropdownOpen,
                        onDismissRequest = { walletDropdownOpen = false }
                    ) {
                        wallets.forEach { w ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BankMfsLogo(institutionId = w.institutionId, size = 28.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(w.name, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = {
                                    selectedWallet = w
                                    walletDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Text("Income Category", style = MaterialTheme.typography.labelMedium, color = PaisaTextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val incomeCats = listOf("Salary & Income", "Freelance / Gig", "Business Profit", "Investment Returns", "Gift / Other")
                    items(incomeCats) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Description") },
                    placeholder = { Text("e.g. October monthly salary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (isSubmitting) return@Button
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    val targetWalletId = selectedWallet?.id ?: wallets.firstOrNull()?.id ?: "wallet_default_cash"
                    if (amt > 0) {
                        isSubmitting = true
                        onConfirm(targetWalletId, amt, selectedCategory, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PaisaIncomeGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isSubmitting) "সংরক্ষণ হচ্ছে..." else "আয় সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = onDismiss
            ) { Text("বাতিল") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransferDialog(
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirm: (fromId: String, toId: String, amount: Double, fee: Double, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var fromWallet by remember { mutableStateOf(wallets.firstOrNull()) }
    var toWallet by remember { mutableStateOf(wallets.getOrNull(1) ?: wallets.firstOrNull()) }
    var fromOpen by remember { mutableStateOf(false) }
    var toOpen by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text("Transfer Between Wallets", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Transfers update wallet balances and never count as income or expense.",
                    style = MaterialTheme.typography.labelSmall,
                    color = PaisaTransferBlue
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Transfer Amount (৳)") },
                    placeholder = { Text("e.g. 5000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // From Wallet
                Text("From Source Wallet", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = fromOpen,
                    onExpandedChange = { if (!isSubmitting) fromOpen = !fromOpen }
                ) {
                    OutlinedTextField(
                        value = fromWallet?.name ?: "Select Source",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            fromWallet?.let { BankMfsLogo(institutionId = it.institutionId, size = 26.dp) }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = fromOpen,
                        onDismissRequest = { fromOpen = false }
                    ) {
                        wallets.forEach { w ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BankMfsLogo(institutionId = w.institutionId, size = 28.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(w.name)
                                    }
                                },
                                onClick = {
                                    fromWallet = w
                                    fromOpen = false
                                }
                            )
                        }
                    }
                }

                // To Wallet
                Text("To Destination Wallet", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = toOpen,
                    onExpandedChange = { if (!isSubmitting) toOpen = !toOpen }
                ) {
                    OutlinedTextField(
                        value = toWallet?.name ?: "Select Destination",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            toWallet?.let { BankMfsLogo(institutionId = it.institutionId, size = 26.dp) }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = toOpen,
                        onDismissRequest = { toOpen = false }
                    ) {
                        wallets.filter { it.id != fromWallet?.id }.forEach { w ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BankMfsLogo(institutionId = w.institutionId, size = 28.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(w.name)
                                    }
                                },
                                onClick = {
                                    toWallet = w
                                    toOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it },
                    label = { Text("Transfer Fee (e.g. bKash cashout / ATM fee)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Memo") },
                    placeholder = { Text("e.g. Bank to bKash topup") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (isSubmitting) return@Button
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    val fee = feeText.toDoubleOrNull() ?: 0.0
                    val from = fromWallet?.id
                    val to = toWallet?.id
                    if (amt > 0 && from != null && to != null && from != to) {
                        isSubmitting = true
                        onConfirm(from, to, amt, fee, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PaisaTransferBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isSubmitting) "Processing..." else "Confirm Transfer")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = onDismiss
            ) { Text("Cancel") }
        }
    )
}

// Voice Draft Review Dialog adhering to:
// "AI/OCR/Voice -> Structured draft -> Editable review -> Explicit confirmation -> deterministic UseCase -> Room"
@Composable
fun VoiceDraftDialog(
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirmDraft: (walletId: String, amount: Double, type: String, category: String, note: String) -> Unit
) {
    var rawSpeech by remember { mutableStateOf("Spent 350 taka for lunch with team from pocket cash") }
    var parsedAmount by remember { mutableStateOf("350") }
    var parsedCategory by remember { mutableStateOf("Food & Dining") }
    var parsedNote by remember { mutableStateOf("Lunch with team") }
    var parsedType by remember { mutableStateOf("EXPENSE") }
    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Entry", tint = PaisaTealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voice Entry Draft Review", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Raw audio transcription draft:",
                    style = MaterialTheme.typography.labelSmall,
                    color = PaisaTextSecondary
                )
                Text(
                    text = "\"$rawSpeech\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = PaisaTealDark
                )

                HorizontalDivider()

                Text("Parsed Structured Draft (Editable):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = parsedAmount,
                    onValueChange = { parsedAmount = it },
                    label = { Text("Amount (৳)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = parsedCategory,
                    onValueChange = { parsedCategory = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = parsedNote,
                    onValueChange = { parsedNote = it },
                    label = { Text("Note / Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (isSubmitting) return@Button
                    val amt = parsedAmount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        isSubmitting = true
                        onConfirmDraft(selectedWalletId, amt, parsedType, parsedCategory, parsedNote)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
            ) {
                Text(if (isSubmitting) "Saving..." else "Confirm & Save to Room")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = onDismiss
            ) { Text("Cancel") }
        }
    )
}

// Receipt Scanner Draft Dialog adhering to:
// "AI/OCR/Voice -> Structured draft -> Editable review -> Explicit confirmation -> deterministic UseCase -> Room"
@Composable
fun ReceiptScanDraftDialog(
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirmDraft: (walletId: String, amount: Double, category: String, note: String) -> Unit
) {
    var parsedMerchant by remember { mutableStateOf("Shwapno Superstore") }
    var parsedAmount by remember { mutableStateOf("2840") }
    var parsedCategory by remember { mutableStateOf("Groceries / Bazar") }
    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, contentDescription = "Receipt OCR", tint = PaisaTealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Receipt Draft Review", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Review extracted fields before confirming to local database:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PaisaTextSecondary
                )

                OutlinedTextField(
                    value = parsedMerchant,
                    onValueChange = { parsedMerchant = it },
                    label = { Text("Merchant / Store") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = parsedAmount,
                    onValueChange = { parsedAmount = it },
                    label = { Text("Total Bill (৳)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = parsedCategory,
                    onValueChange = { parsedCategory = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (isSubmitting) return@Button
                    val amt = parsedAmount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        isSubmitting = true
                        onConfirmDraft(selectedWalletId, amt, parsedCategory, parsedMerchant)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
            ) {
                Text(if (isSubmitting) "Recording..." else "Confirm & Record")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = onDismiss
            ) { Text("Cancel") }
        }
    )
}

@Composable
fun TransactionDetailDialog(
    tx: TransactionEntity,
    sourceWallet: WalletEntity?,
    destWallet: WalletEntity?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()) }
    var isDeleting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = {
            Text(
                text = "${tx.type} Details",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Amount", color = PaisaTextSecondary)
                    Text("৳ ${String.format(Locale.US, "%,.2f", tx.amount)}", fontWeight = FontWeight.Bold, color = PaisaTextPrimary)
                }
                if (tx.fee > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Fee", color = PaisaTextSecondary)
                        Text("৳ ${String.format(Locale.US, "%,.2f", tx.fee)}", fontWeight = FontWeight.Medium)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Wallet", color = PaisaTextSecondary)
                    Text(sourceWallet?.name ?: "Unknown", fontWeight = FontWeight.SemiBold)
                }
                if (tx.toWalletId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Destination", color = PaisaTextSecondary)
                        Text(destWallet?.name ?: "Unknown", fontWeight = FontWeight.SemiBold)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Category", color = PaisaTextSecondary)
                    Text(tx.category, fontWeight = FontWeight.Medium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date & Time", color = PaisaTextSecondary)
                    Text(dateFormat.format(Date(tx.dateMillis)), fontSize = 12.sp)
                }
                if (tx.note.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Note", color = PaisaTextSecondary)
                        Text(tx.note, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isDeleting,
                onClick = onDismiss
            ) { Text("Close") }
        },
        dismissButton = {
            TextButton(
                enabled = !isDeleting,
                onClick = {
                    if (isDeleting) return@TextButton
                    isDeleting = true
                    onDelete()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = PaisaExpenseRed)
            ) {
                Text(if (isDeleting) "Deleting..." else "Delete Transaction")
            }
        }
    )
}
