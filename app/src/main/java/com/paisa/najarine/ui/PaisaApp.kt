package com.paisa.najarine.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.paisa.najarine.auth.AuthState
import com.paisa.najarine.ui.components.PaisaBrandHeader
import com.paisa.najarine.ui.components.SecurityLockOverlay
import com.paisa.najarine.ui.screens.accounting.*
import com.paisa.najarine.ui.screens.analytics.DetailedFinancialAnalyticsScreen
import com.paisa.najarine.ui.screens.analytics.MoreAndAnalyticsScreen
import com.paisa.najarine.ui.screens.auth.LoginScreen
import com.paisa.najarine.ui.screens.home.HomeScreen
import com.paisa.najarine.ui.screens.islamic.*
import com.paisa.najarine.ui.screens.profile.UserProfileScreen
import com.paisa.najarine.ui.screens.settings.DeveloperDiagnosticsScreen
import com.paisa.najarine.ui.screens.settings.MasterSettingsScreen
import com.paisa.najarine.ui.screens.support.SupportDeveloperScreen
import com.paisa.najarine.ui.screens.sync.SyncCenterScreen
import com.paisa.najarine.ui.screens.transactions.AddExpenseDialog
import com.paisa.najarine.ui.screens.transactions.AddIncomeDialog
import com.paisa.najarine.ui.screens.transactions.AddTransferDialog
import com.paisa.najarine.ui.screens.transactions.TransactionsScreen
import com.paisa.najarine.ui.screens.wallets.AddWalletDialog
import com.paisa.najarine.ui.screens.wallets.WalletsScreen
import com.paisa.najarine.ui.theme.*

enum class AppScreen {
    HOME,
    WALLETS,
    TRANSACTIONS,
    ISLAMIC,
    TOOLS,
    FINANCIAL_ANALYTICS,
    MASTER_SETTINGS,
    DIAGNOSTICS,
    SYNC_CENTER,
    USER_PROFILE,
    SUPPORT_DEVELOPER,
    ADHAN_PRAYER,
    QIBLA,
    QURAN,
    HADITH,
    ZAKAT,
    BUDGET_PLANNING,
    SAVINGS_GOALS,
    BILLS_SUBSCRIPTIONS,
    DENA_PAONA,
    CREDIT_CARD_EMI,
    FDR_DPS_SHONCHOY,
    GOLD_SILVER,
    STOCK_MARKET,
    CRYPTO_ASSETS,
    CUSTOMER_LEDGER,
    PROJECT_INVOICE,
    TASK_HABIT,
    MESS_MANAGER,
    BAZAR_SHODAI,
    CURRENCY_CONVERTER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaisaApp(
    viewModel: PaisaViewModel,
    activity: FragmentActivity,
    targetScreenName: String? = null,
    onTargetScreenHandled: () -> Unit = {}
) {
    val authState by viewModel.authState.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val bills by viewModel.bills.collectAsState()

    // Proper LIFO Navigation BackStack (fixes back button jumping straight to home)
    val backStack = remember { mutableStateListOf(AppScreen.HOME) }
    val currentScreen = backStack.lastOrNull() ?: AppScreen.HOME

    fun navigateTo(screen: AppScreen) {
        if (screen in listOf(AppScreen.HOME, AppScreen.WALLETS, AppScreen.TRANSACTIONS, AppScreen.ISLAMIC, AppScreen.TOOLS)) {
            backStack.clear()
            backStack.add(screen)
        } else {
            if (currentScreen != screen) {
                backStack.add(screen)
            }
        }
    }

    LaunchedEffect(targetScreenName) {
        targetScreenName?.let { screenName ->
            when (screenName) {
                "HADITH" -> navigateTo(AppScreen.HADITH)
                "QURAN" -> navigateTo(AppScreen.QURAN)
                "ADHAN_PRAYER" -> navigateTo(AppScreen.ADHAN_PRAYER)
                else -> {}
            }
            onTargetScreenHandled()
        }
    }

    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    val context = LocalContext.current

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler(enabled = true) {
        if (backStack.size > 1) {
            goBack()
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000L) {
                activity.finish()
            } else {
                lastBackPressTime = currentTime
                android.widget.Toast.makeText(
                    context,
                    "অ্যাপটি বন্ধ করতে আবার ব্যাক বাটনে চাপুন (Press back again to exit)",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    var showExpenseDialog by remember { mutableStateOf(false) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val locationGranted = perms[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            viewModel.detectLocationAndRefreshPrayerTimings(context)
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val permissionsToRequest = mutableListOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
            viewModel.detectLocationAndRefreshPrayerTimings(context)
        }
    }

    LaunchedEffect(Unit) {
        // Auto-schedule exact prayer alarms if Adhan toggle is active
        try {
            val timings = viewModel.prayerTimings.value ?: viewModel.islamicRepo.getOfflinePrayerTimings("Hanafi")
            if (com.paisa.najarine.notification.AdhanPreferences.isAdhanEnabled(context)) {
                com.paisa.najarine.notification.AdhanScheduler.schedulePrayerAlarms(
                    context = context,
                    fajr = timings.fajr,
                    dhuhr = timings.dhuhr,
                    asr = timings.asr,
                    maghrib = timings.maghrib,
                    isha = timings.isha
                )
            }
        } catch (_: Exception) {}
    }

    // 1. If not authenticated -> Login Screen (or Developer Diagnostics if requested from Login)
    if (authState !is AuthState.Success) {
        if (currentScreen == AppScreen.DIAGNOSTICS) {
            DeveloperDiagnosticsScreen(
                viewModel = viewModel,
                onBackClick = { navigateTo(AppScreen.HOME) }
            )
        } else {
            LoginScreen(
                authState = authState,
                onSignInClick = { viewModel.signInWithGoogle(activity) },
                onCancelLoading = { viewModel.resetAuthState() }
            )
        }
        return
    }

    // 2. If authenticated but Biometric/PIN lock is enabled and locked
    if (!isAppUnlocked) {
        SecurityLockOverlay(
            onUnlockWithBiometrics = {
                viewModel.securityManager.promptBiometric(
                    activity = activity,
                    onSuccess = {},
                    onError = {}
                )
            },
            onVerifyPin = { pin ->
                viewModel.securityManager.verifyPin(pin)
            }
        )
        return
    }

    val isMainTab = currentScreen in listOf(
        AppScreen.HOME,
        AppScreen.WALLETS,
        AppScreen.TRANSACTIONS,
        AppScreen.ISLAMIC,
        AppScreen.TOOLS
    )

    // 3. Authenticated App UI
    Scaffold(
        topBar = {
            if (isMainTab) {
                val syncStatus by viewModel.syncManager.syncStatus.collectAsState()
                val currentFirebaseUser = (authState as? AuthState.Success)?.user
                val ownerDisplayName = currentFirebaseUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: currentFirebaseUser?.email?.substringBefore("@")
                    ?: "Paisa User"

                PaisaBrandHeader(
                    accountOwnerName = ownerDisplayName,
                    accountOwnerEmail = currentFirebaseUser?.email ?: "",
                    userPhotoUrl = currentFirebaseUser?.photoUrl?.toString(),
                    syncStatus = syncStatus,
                    onAccountClick = { navigateTo(AppScreen.USER_PROFILE) },
                    onProfileClick = { navigateTo(AppScreen.USER_PROFILE) },
                    onSettingsClick = { navigateTo(AppScreen.MASTER_SETTINGS) },
                    onSearchClick = { showSearchDialog = true },
                    onLockClick = { viewModel.securityManager.lock() },
                    onSyncClick = { navigateTo(AppScreen.SYNC_CENTER) }
                )
            }
        },
        bottomBar = {
            if (isMainTab) {
                NavigationBar(
                    containerColor = PaisaSurface,
                    tonalElevation = 6.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.HOME,
                        onClick = { navigateTo(AppScreen.HOME) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 11.sp, fontWeight = if (currentScreen == AppScreen.HOME) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PaisaTealPrimary,
                            selectedTextColor = PaisaTealPrimary,
                            indicatorColor = PaisaTealContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.WALLETS,
                        onClick = { navigateTo(AppScreen.WALLETS) },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallets") },
                        label = { Text("Wallets", fontSize = 11.sp, fontWeight = if (currentScreen == AppScreen.WALLETS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PaisaTealPrimary,
                            selectedTextColor = PaisaTealPrimary,
                            indicatorColor = PaisaTealContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.TRANSACTIONS,
                        onClick = { navigateTo(AppScreen.TRANSACTIONS) },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                        label = { Text("Ledger", fontSize = 11.sp, fontWeight = if (currentScreen == AppScreen.TRANSACTIONS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PaisaTealPrimary,
                            selectedTextColor = PaisaTealPrimary,
                            indicatorColor = PaisaTealContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.ISLAMIC,
                        onClick = { navigateTo(AppScreen.ISLAMIC) },
                        icon = { Icon(Icons.Default.Mosque, contentDescription = "Islamic") },
                        label = { Text("Islamic", fontSize = 11.sp, fontWeight = if (currentScreen == AppScreen.ISLAMIC) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PaisaTealPrimary,
                            selectedTextColor = PaisaTealPrimary,
                            indicatorColor = PaisaTealContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.TOOLS,
                        onClick = { navigateTo(AppScreen.TOOLS) },
                        icon = { Icon(Icons.Default.Widgets, contentDescription = "Tools") },
                        label = { Text("Tools", fontSize = 11.sp, fontWeight = if (currentScreen == AppScreen.TOOLS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PaisaTealPrimary,
                            selectedTextColor = PaisaTealPrimary,
                            indicatorColor = PaisaTealContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToWallets = { navigateTo(AppScreen.WALLETS) },
                    onNavigateToTransactions = { navigateTo(AppScreen.TRANSACTIONS) },
                    onNavigateToIslamic = { navigateTo(AppScreen.ISLAMIC) },
                    onAddExpenseClick = { showExpenseDialog = true },
                    onAddIncomeClick = { showIncomeDialog = true },
                    onTransferClick = { showTransferDialog = true },
                    onAddWalletClick = { showAddWalletDialog = true },
                    onNavigateToFinancialAnalytics = { navigateTo(AppScreen.FINANCIAL_ANALYTICS) },
                    onNavigateToBudgetPlanning = { navigateTo(AppScreen.BUDGET_PLANNING) },
                    onNavigateToSavingsGoals = { navigateTo(AppScreen.SAVINGS_GOALS) },
                    onNavigateToBillsSubscriptions = { navigateTo(AppScreen.BILLS_SUBSCRIPTIONS) },
                    onNavigateToDenaPaona = { navigateTo(AppScreen.DENA_PAONA) },
                    onNavigateToCreditCardEmi = { navigateTo(AppScreen.CREDIT_CARD_EMI) },
                    onNavigateToFdrDpsShonchoy = { navigateTo(AppScreen.FDR_DPS_SHONCHOY) },
                    onNavigateToGoldSilver = { navigateTo(AppScreen.GOLD_SILVER) },
                    onNavigateToStockMarket = { navigateTo(AppScreen.STOCK_MARKET) },
                    onNavigateToCryptoAssets = { navigateTo(AppScreen.CRYPTO_ASSETS) },
                    onNavigateToCustomerLedger = { navigateTo(AppScreen.CUSTOMER_LEDGER) },
                    onNavigateToProjectInvoice = { navigateTo(AppScreen.PROJECT_INVOICE) },
                    onNavigateToMessManager = { navigateTo(AppScreen.MESS_MANAGER) },
                    onNavigateToBazarShodai = { navigateTo(AppScreen.BAZAR_SHODAI) },
                    onNavigateToCurrencyConverter = { navigateTo(AppScreen.CURRENCY_CONVERTER) },
                    onNavigateToMasterSettings = { navigateTo(AppScreen.MASTER_SETTINGS) },
                    onSearchClick = { showSearchDialog = true }
                )
                AppScreen.WALLETS -> WalletsScreen(
                    viewModel = viewModel,
                    onTransferClick = { showTransferDialog = true }
                )
                AppScreen.TRANSACTIONS -> TransactionsScreen(
                    viewModel = viewModel
                )
                AppScreen.ISLAMIC -> IslamicScreen(
                    viewModel = viewModel,
                    onOpenPrayerAdhan = { navigateTo(AppScreen.ADHAN_PRAYER) },
                    onOpenQibla = { navigateTo(AppScreen.QIBLA) },
                    onOpenQuran = { navigateTo(AppScreen.QURAN) },
                    onOpenHadith = { navigateTo(AppScreen.HADITH) },
                    onOpenZakat = { navigateTo(AppScreen.ZAKAT) },
                    onSearchClick = { showSearchDialog = true }
                )
                AppScreen.TOOLS -> MoreAndAnalyticsScreen(
                    viewModel = viewModel,
                    onNavigateToFinancialAnalytics = { navigateTo(AppScreen.FINANCIAL_ANALYTICS) },
                    onNavigateToBudgetPlanning = { navigateTo(AppScreen.BUDGET_PLANNING) },
                    onNavigateToSavingsGoals = { navigateTo(AppScreen.SAVINGS_GOALS) },
                    onNavigateToBillsSubscriptions = { navigateTo(AppScreen.BILLS_SUBSCRIPTIONS) },
                    onNavigateToDenaPaona = { navigateTo(AppScreen.DENA_PAONA) },
                    onNavigateToCreditCardEmi = { navigateTo(AppScreen.CREDIT_CARD_EMI) },
                    onNavigateToFdrDpsShonchoy = { navigateTo(AppScreen.FDR_DPS_SHONCHOY) },
                    onNavigateToGoldSilver = { navigateTo(AppScreen.GOLD_SILVER) },
                    onNavigateToStockMarket = { navigateTo(AppScreen.STOCK_MARKET) },
                    onNavigateToCryptoAssets = { navigateTo(AppScreen.CRYPTO_ASSETS) },
                    onNavigateToCustomerLedger = { navigateTo(AppScreen.CUSTOMER_LEDGER) },
                    onNavigateToProjectInvoice = { navigateTo(AppScreen.PROJECT_INVOICE) },
                    onNavigateToMessManager = { navigateTo(AppScreen.MESS_MANAGER) },
                    onNavigateToBazarShodai = { navigateTo(AppScreen.BAZAR_SHODAI) },
                    onNavigateToCurrencyConverter = { navigateTo(AppScreen.CURRENCY_CONVERTER) },
                    onNavigateToTaskHabit = { navigateTo(AppScreen.TASK_HABIT) },
                    onNavigateToUserProfile = { navigateTo(AppScreen.USER_PROFILE) },
                    onOpenSyncCenter = { navigateTo(AppScreen.SYNC_CENTER) },
                    onOpenMasterSettings = { navigateTo(AppScreen.MASTER_SETTINGS) },
                    onOpenSupportDeveloper = { navigateTo(AppScreen.SUPPORT_DEVELOPER) },
                    onNavigateToWallets = { navigateTo(AppScreen.WALLETS) },
                    onNavigateToTransactions = { navigateTo(AppScreen.TRANSACTIONS) },
                    onNavigateToIslamic = { navigateTo(AppScreen.ISLAMIC) },
                    onNavigateToPrayerAdhan = { navigateTo(AppScreen.ADHAN_PRAYER) },
                    onNavigateToQibla = { navigateTo(AppScreen.QIBLA) },
                    onNavigateToQuran = { navigateTo(AppScreen.QURAN) },
                    onNavigateToHadith = { navigateTo(AppScreen.HADITH) },
                    onNavigateToZakat = { navigateTo(AppScreen.ZAKAT) },
                    onSearchClick = { showSearchDialog = true }
                )
                AppScreen.FINANCIAL_ANALYTICS -> DetailedFinancialAnalyticsScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.USER_PROFILE -> UserProfileScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.MASTER_SETTINGS -> MasterSettingsScreen(
                    viewModel = viewModel,
                    onOpenProfile = { navigateTo(AppScreen.USER_PROFILE) },
                    onOpenDiagnostics = { navigateTo(AppScreen.DIAGNOSTICS) },
                    onOpenSyncCenter = { navigateTo(AppScreen.SYNC_CENTER) },
                    onOpenSupportDeveloper = { navigateTo(AppScreen.SUPPORT_DEVELOPER) },
                    onSignOutClick = {
                        navigateTo(AppScreen.HOME)
                        viewModel.signOut()
                    },
                    onSwitchAccountClick = {
                        navigateTo(AppScreen.HOME)
                        viewModel.switchAccount()
                    },
                    onBackClick = { goBack() }
                )
                AppScreen.DIAGNOSTICS -> DeveloperDiagnosticsScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.SYNC_CENTER -> SyncCenterScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.SUPPORT_DEVELOPER -> SupportDeveloperScreen(
                    onBackClick = { goBack() }
                )
                AppScreen.ADHAN_PRAYER -> AdhanPrayerTimesScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.QIBLA -> QiblaCompassScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.QURAN -> QuranLibraryScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.HADITH -> DailyHadithScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.ZAKAT -> ZakatCalculatorScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.BUDGET_PLANNING -> BudgetPlanningScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.SAVINGS_GOALS -> SavingsGoalsScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.BILLS_SUBSCRIPTIONS -> BillsSubscriptionsScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.DENA_PAONA -> DenaPaonaScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.CREDIT_CARD_EMI -> CreditCardEmiScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.FDR_DPS_SHONCHOY -> FdrDpsShonchoyScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.GOLD_SILVER -> GoldSilverScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.STOCK_MARKET -> StockMarketScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.CRYPTO_ASSETS -> CryptoAssetsScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.CUSTOMER_LEDGER -> CustomerLedgerScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.PROJECT_INVOICE -> ProjectInvoiceScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.TASK_HABIT -> TaskHabitScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.MESS_MANAGER -> MessManagerScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() },
                    onNavigateToBazarShodai = { navigateTo(AppScreen.BAZAR_SHODAI) }
                )
                AppScreen.BAZAR_SHODAI -> BazarShodaiScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
                AppScreen.CURRENCY_CONVERTER -> CurrencyConverterScreen(
                    viewModel = viewModel,
                    onBackClick = { goBack() }
                )
            }
        }
    }

    // Quick Add Expense Dialog
    if (showExpenseDialog) {
        AddExpenseDialog(
            wallets = wallets,
            categories = categories.filter { it.type == "EXPENSE" },
            bills = bills,
            onDismiss = { showExpenseDialog = false },
            onConfirm = { walletId, amount, cat, fee, note ->
                viewModel.addExpense(walletId, amount, cat, fee, note)
                showExpenseDialog = false
            }
        )
    }

    // Quick Add Income Dialog
    if (showIncomeDialog) {
        AddIncomeDialog(
            wallets = wallets,
            categories = categories.filter { it.type == "INCOME" },
            onDismiss = { showIncomeDialog = false },
            onConfirm = { walletId, amount, cat, note ->
                viewModel.addIncome(walletId, amount, cat, note)
                showIncomeDialog = false
            }
        )
    }

    // Quick Transfer Dialog
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

    // Quick Add Wallet Dialog
    if (showAddWalletDialog) {
        AddWalletDialog(
            onDismiss = { showAddWalletDialog = false },
            onConfirm = { name, type, instId, accNum, balance, creditLimit, isEx ->
                viewModel.createWallet(name, type, instId, accNum, balance, creditLimit, isExcludedFromTotal = isEx)
                showAddWalletDialog = false
            }
        )
    }

    // Global Search Dialog
    if (showSearchDialog) {
        var query by remember { mutableStateOf("") }
        val allTx by viewModel.transactions.collectAsState()
        val matchingTx = allTx.filter {
            query.isNotEmpty() && (it.note.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) || it.amount.toString().contains(query))
        }

        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Global Financial Search", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search any amount, category, or note...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (matchingTx.isNotEmpty()) {
                        Text("Matching Records (${matchingTx.size}):", style = MaterialTheme.typography.labelMedium)
                        matchingTx.take(4).forEach { tx ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(tx.note.ifEmpty { tx.category }, style = MaterialTheme.typography.bodyMedium)
                                Text("৳ ${tx.amount}", fontWeight = FontWeight.Bold, color = if (tx.type == "INCOME") PaisaIncomeGreen else PaisaExpenseRed)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) { Text("Close") }
            }
        )
    }
}
