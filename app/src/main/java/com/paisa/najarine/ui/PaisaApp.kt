package com.paisa.najarine.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import com.paisa.najarine.auth.AuthState
import com.paisa.najarine.ui.components.AdhanCallAlarmDialog
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
    DUA,
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
    var showBackgroundPermissionDialog by remember { mutableStateOf(false) }
    var isAdhanCallModalMinimized by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val locationGranted = perms[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            viewModel.detectLocationAndRefreshPrayerTimings(context)
        }
    }

    // Request permissions and check background execution ONLY after successful Google Sign-In
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

            // Background execution permission check for Adhan & Alarms (only after sign-in)
            if (!com.paisa.najarine.notification.AdhanPreferences.isIgnoringBatteryOptimizations(context)) {
                showBackgroundPermissionDialog = true
            }

            viewModel.detectLocationAndRefreshPrayerTimings(context)
        }
    }

    LaunchedEffect(Unit) {
        // Load last known location immediately
        viewModel.loadSavedLocationOnStart(context)
        viewModel.detectLocationAndRefreshPrayerTimings(context)

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
                onCancelLoading = { viewModel.resetAuthState() },
                onNavigateToDiagnostics = { navigateTo(AppScreen.DIAGNOSTICS) }
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
    Box(modifier = Modifier.fillMaxSize()) {
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
                        icon = { Icon(Icons.Default.Home, contentDescription = "হোম") },
                        label = { Text("হোম", fontSize = 11.sp, fontWeight = if (currentScreen == AppScreen.HOME) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PaisaTealPrimary,
                            selectedTextColor = PaisaTealPrimary,
                            indicatorColor = PaisaTealContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.WALLETS,
                        onClick = { navigateTo(AppScreen.WALLETS) },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "ওয়ালেট") },
                        label = { Text("ওয়ালেট", fontSize = 11.sp, fontWeight = if (currentScreen == AppScreen.WALLETS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PaisaTealPrimary,
                            selectedTextColor = PaisaTealPrimary,
                            indicatorColor = PaisaTealContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.TRANSACTIONS,
                        onClick = { navigateTo(AppScreen.TRANSACTIONS) },
                        icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "লেনদেন") },
                        label = { Text("লেনদেন", fontSize = 11.sp, fontWeight = if (currentScreen == AppScreen.TRANSACTIONS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PaisaTealPrimary,
                            selectedTextColor = PaisaTealPrimary,
                            indicatorColor = PaisaTealContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.ISLAMIC,
                        onClick = { navigateTo(AppScreen.ISLAMIC) },
                        icon = { Icon(Icons.Default.Mosque, contentDescription = "ইসলামিক") },
                        label = { Text("ইসলামিক", fontSize = 11.sp, fontWeight = if (currentScreen == AppScreen.ISLAMIC) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PaisaTealPrimary,
                            selectedTextColor = PaisaTealPrimary,
                            indicatorColor = PaisaTealContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.TOOLS,
                        onClick = { navigateTo(AppScreen.TOOLS) },
                        icon = { Icon(Icons.Default.Widgets, contentDescription = "টুলস") },
                        label = { Text("টুলস", fontSize = 11.sp, fontWeight = if (currentScreen == AppScreen.TOOLS) FontWeight.Bold else FontWeight.Normal) },
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
                    onOpenDua = { navigateTo(AppScreen.DUA) },
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
                AppScreen.DUA -> DuaScreen(
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

    // In-App Adhan Incoming Call / Alarm Popup Alert
    val isAdhanAudioPlaying by com.paisa.najarine.notification.DefaultAdhanAudioProvider.isPlayingState.collectAsState()
    val activeAdhanName by com.paisa.najarine.notification.DefaultAdhanAudioProvider.activePrayerNameState.collectAsState()

    LaunchedEffect(isAdhanAudioPlaying) {
        if (isAdhanAudioPlaying) {
            isAdhanCallModalMinimized = false
        }
    }

    // 1. Full-Screen Incoming Call / Alarm Popup (In-tree Compose overlay with zero white flash)
    AnimatedVisibility(
        visible = isAdhanAudioPlaying && !isAdhanCallModalMinimized,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = Modifier
            .fillMaxSize()
            .zIndex(99f)
    ) {
        AdhanCallAlarmDialog(
            activePrayerName = activeAdhanName,
            onDeclineClick = {
                com.paisa.najarine.notification.AdhanPlaybackService.stop(context)
                com.paisa.najarine.notification.DefaultAdhanAudioProvider.stopPlayback()
                isAdhanCallModalMinimized = false
            },
            onMinimizeClick = {
                isAdhanCallModalMinimized = true
            },
            onDismissRequest = {
                isAdhanCallModalMinimized = true
            }
        )
    }

    // 2. Minimized Top Floating Banner (Available when user minimizes the call modal)
    AnimatedVisibility(
        visible = isAdhanAudioPlaying && isAdhanCallModalMinimized,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .zIndex(98f)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isAdhanCallModalMinimized = false },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PaisaTealPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "🔊 ${activeAdhanName ?: "আজান"} চলছে...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "ট্যাপ করে ফুলস্ক্রিন দেখুন • সালাতের সময়",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            com.paisa.najarine.notification.AdhanPlaybackService.stop(context)
                            com.paisa.najarine.notification.DefaultAdhanAudioProvider.stopPlayback()
                            isAdhanCallModalMinimized = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "আজান বন্ধ",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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

    // Startup Background Execution & Notification Permission Dialog for Adhan
    if (showBackgroundPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundPermissionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = PaisaTealPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "সময়মতো আযান ও অ্যালার্টের পারমিশন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "অ্যাপ বন্ধ থাকলেও ব্যাকগ্রাউন্ডে যাতে ওয়াক্ত অনুযায়ী সময়মতো আযান এবং পুশ অ্যালার্ট পাওয়া যায়, সেজন্য ব্যাকগ্রাউন্ড পারমিশন নিশ্চিত করুন।",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = PaisaTextPrimary
                    )
                    Text(
                        text = "• ফোন লক থাকলেও সময়মতো আযান বাজবে\n• কোনো ওয়াক্ত মিস হবে না\n• সার্বক্ষণিক সঠিক ওয়াক্ত অ্যালার্ট",
                        fontSize = 12.sp,
                        color = PaisaTealDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackgroundPermissionDialog = false
                        try {
                            context.startActivity(com.paisa.najarine.notification.AdhanPreferences.getBatteryOptimizationSettingsIntent(context))
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTealPrimary)
                ) {
                    Text("অনুমতি দিন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundPermissionDialog = false }) {
                    Text("পরে করব", color = PaisaTextSecondary)
                }
            }
        )
    }
}
