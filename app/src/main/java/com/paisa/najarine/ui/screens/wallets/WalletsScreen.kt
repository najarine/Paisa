package com.paisa.najarine.ui.screens.wallets

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.data.local.WalletEntity
import com.paisa.najarine.data.model.BankMfsCatalog
import com.paisa.najarine.data.model.FinancialInstitutionType
import com.paisa.najarine.data.model.InstitutionItem
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.components.BankMfsLogo
import com.paisa.najarine.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(
    viewModel: PaisaViewModel,
    onTransferClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wallets by viewModel.wallets.collectAsState()
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var selectedWalletForDetail by remember { mutableStateOf<WalletEntity?>(null) }

    val totalAssets = wallets.filter { it.balance > 0 && !it.isExcludedFromTotal }.sumOf { it.balance }
    val totalLiabilities = wallets.filter { it.balance < 0 }.sumOf { kotlin.math.abs(it.balance) }
    val mfsTotal = wallets.filter { it.type == "MFS" }.sumOf { it.balance }
    val bankTotal = wallets.filter { it.type == "BANK" }.sumOf { it.balance }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddWalletDialog = true },
                containerColor = PaisaTealPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Wallet") },
                text = { Text("Add Wallet / Bank", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(PaisaBackground)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header stats
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Wealth Hub & Wallets",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTextPrimary
                    )
                    Text(
                        text = "Deterministic operational source of truth for where your money is held.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaisaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Summary row cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WalletStatChip(
                            title = "Liquid Assets",
                            amount = "৳ ${String.format(Locale.US, "%,.0f", totalAssets)}",
                            tint = PaisaIncomeGreen,
                            modifier = Modifier.weight(1f)
                        )
                        WalletStatChip(
                            title = "Liabilities",
                            amount = "৳ ${String.format(Locale.US, "%,.0f", totalLiabilities)}",
                            tint = PaisaExpenseRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WalletStatChip(
                            title = "Banks Balance",
                            amount = "৳ ${String.format(Locale.US, "%,.0f", bankTotal)}",
                            tint = PaisaTransferBlue,
                            modifier = Modifier.weight(1f)
                        )
                        WalletStatChip(
                            title = "MFS Total",
                            amount = "৳ ${String.format(Locale.US, "%,.0f", mfsTotal)}",
                            tint = Color(0xFFE2136E), // bKash magenta accent
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Wallet items section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Wallets (${wallets.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTextPrimary
                    )

                    OutlinedButton(
                        onClick = onTransferClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.SyncAlt, contentDescription = "Transfer", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Transfer", fontSize = 12.sp)
                    }
                }
            }

            if (wallets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No wallets found. Tap '+ Add Wallet / Bank' below to configure bKash, Nagad, Islami Bank, or Cash.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PaisaTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(wallets) { wallet ->
                    WalletDetailRow(
                        wallet = wallet,
                        onClick = { selectedWalletForDetail = wallet },
                        onDelete = { viewModel.deleteWallet(wallet.id) }
                    )
                }
            }
        }
    }

    if (showAddWalletDialog) {
        AddWalletDialog(
            onDismiss = { showAddWalletDialog = false },
            onConfirm = { name, type, instId, accNum, balance, creditLimit, isEx ->
                viewModel.createWallet(
                    name = name,
                    type = type,
                    institutionId = instId,
                    accountNumber = accNum,
                    balance = balance,
                    creditLimit = creditLimit,
                    isExcludedFromTotal = isEx
                )
                showAddWalletDialog = false
            }
        )
    }

    selectedWalletForDetail?.let { wallet ->
        WalletDetailDialog(
            wallet = wallet,
            onDismiss = { selectedWalletForDetail = null },
            onDelete = {
                viewModel.deleteWallet(wallet.id)
                selectedWalletForDetail = null
            }
        )
    }
}

@Composable
fun WalletStatChip(
    title: String,
    amount: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaSurface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = PaisaTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}

@Composable
fun WalletDetailRow(
    wallet: WalletEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
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
                // Bank / MFS Logo
                BankMfsLogo(institutionId = wallet.institutionId, size = 46.dp)

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = wallet.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PaisaTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PaisaSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = wallet.type.replace("_", " "),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PaisaTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (wallet.accountNumber.isNotEmpty()) wallet.accountNumber else BankMfsCatalog.findById(wallet.institutionId).subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaisaTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (wallet.type == "CREDIT_CARD" && wallet.creditLimit > 0) {
                        Text(
                            text = "Limit: ৳ ${String.format(Locale.US, "%,.0f", wallet.creditLimit)} • Available: ৳ ${String.format(Locale.US, "%,.0f", wallet.creditLimit + wallet.balance)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PaisaTextTertiary
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "৳ ${String.format(Locale.US, "%,.0f", wallet.balance)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (wallet.balance < 0) PaisaExpenseRed else PaisaTealPrimary
                )
                Text(
                    text = "Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = PaisaIncomeGreen
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        type: String,
        institutionId: String,
        accountNumber: String,
        balance: Double,
        creditLimit: Double,
        isExcludedFromTotal: Boolean
    ) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(FinancialInstitutionType.MFS) }
    var selectedInstitution by remember { mutableStateOf(BankMfsCatalog.MFS_LIST.first()) }
    var walletName by remember { mutableStateOf(selectedInstitution.name) }
    var accountNumber by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var creditLimitText by remember { mutableStateOf("") }
    var isExcludedFromTotal by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val institutionOptions = when (selectedCategory) {
        FinancialInstitutionType.MFS -> BankMfsCatalog.MFS_LIST
        FinancialInstitutionType.BANK -> BankMfsCatalog.BANK_LIST
        else -> BankMfsCatalog.OTHER_LIST
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Wallet / Bank",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category Selector Tabs
                item {
                    Text(
                        text = "Account Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = PaisaTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            FinancialInstitutionType.MFS to "MFS",
                            FinancialInstitutionType.BANK to "Bank",
                            FinancialInstitutionType.CARD to "Card",
                            FinancialInstitutionType.CASH to "Cash"
                        ).forEach { (cat, label) ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = cat
                                    val newOptions = when (cat) {
                                        FinancialInstitutionType.MFS -> BankMfsCatalog.MFS_LIST
                                        FinancialInstitutionType.BANK -> BankMfsCatalog.BANK_LIST
                                        else -> BankMfsCatalog.OTHER_LIST
                                    }
                                    selectedInstitution = newOptions.first()
                                    walletName = selectedInstitution.name
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Dropdown of Banks / MFS with Logos
                item {
                    Text(
                        text = "Select Provider (with Logo)",
                        style = MaterialTheme.typography.labelMedium,
                        color = PaisaTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedInstitution.name,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                BankMfsLogo(
                                    institutionId = selectedInstitution.id,
                                    size = 28.dp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            institutionOptions.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            BankMfsLogo(institutionId = item.id, size = 32.dp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text(item.subtitle, fontSize = 11.sp, color = PaisaTextSecondary)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedInstitution = item
                                        walletName = item.name
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Custom Wallet Name
                item {
                    OutlinedTextField(
                        value = walletName,
                        onValueChange = { walletName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Account Number / Note
                item {
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Account No / Phone (optional)") },
                        placeholder = { Text("e.g. 017xx-xxxxxx or 2050-xxx") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Initial Balance
                item {
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Starting Balance (৳)") },
                        placeholder = { Text("0.00") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Credit Limit if Card
                if (selectedCategory == FinancialInstitutionType.CARD) {
                    item {
                        OutlinedTextField(
                            value = creditLimitText,
                            onValueChange = { creditLimitText = it },
                            label = { Text("Credit Card Limit (৳)") },
                            placeholder = { Text("e.g. 100000") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                // Exclude from net worth toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExcludedFromTotal = !isExcludedFromTotal },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Exclude from Total Net Worth", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("For non-liquid, escrow, or collateral accounts", style = MaterialTheme.typography.labelSmall, color = PaisaTextSecondary)
                        }
                        Switch(
                            checked = isExcludedFromTotal,
                            onCheckedChange = { isExcludedFromTotal = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.toDoubleOrNull() ?: 0.0
                    val limit = creditLimitText.toDoubleOrNull() ?: 0.0
                    val typeStr = when (selectedCategory) {
                        FinancialInstitutionType.MFS -> "MFS"
                        FinancialInstitutionType.BANK -> "BANK"
                        FinancialInstitutionType.CARD -> "CREDIT_CARD"
                        FinancialInstitutionType.CASH -> "CASH"
                        FinancialInstitutionType.CRYPTO -> "CRYPTO"
                        else -> "INVESTMENT"
                    }
                    onConfirm(
                        walletName.ifEmpty { selectedInstitution.name },
                        typeStr,
                        selectedInstitution.id,
                        accountNumber,
                        bal,
                        limit,
                        isExcludedFromTotal
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Wallet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WalletDetailDialog(
    wallet: WalletEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BankMfsLogo(institutionId = wallet.institutionId, size = 36.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(wallet.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(wallet.type.replace("_", " "), fontSize = 12.sp, color = PaisaTextSecondary)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(label = "Current Balance", value = "৳ ${String.format(Locale.US, "%,.2f", wallet.balance)}")
                if (wallet.accountNumber.isNotEmpty()) {
                    DetailRow(label = "Account / Number", value = wallet.accountNumber)
                }
                if (wallet.creditLimit > 0) {
                    DetailRow(label = "Credit Limit", value = "৳ ${String.format(Locale.US, "%,.0f", wallet.creditLimit)}")
                    DetailRow(label = "Available Credit", value = "৳ ${String.format(Locale.US, "%,.0f", wallet.creditLimit + wallet.balance)}")
                }
                DetailRow(label = "Currency", value = wallet.currencyCode)
                DetailRow(label = "Net Worth Inclusion", value = if (wallet.isExcludedFromTotal) "Excluded" else "Included")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = PaisaExpenseRed)
            ) {
                Text("Delete Wallet")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = PaisaTextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PaisaTextPrimary)
    }
}
