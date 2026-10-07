package com.paisa.najarine.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paisa.najarine.accounting.AccountingEngine
import com.paisa.najarine.accounting.AccountingSummary
import com.paisa.najarine.auth.AuthManager
import com.paisa.najarine.auth.AuthState
import com.paisa.najarine.data.local.*
import com.paisa.najarine.data.remote.CryptoRate
import com.paisa.najarine.data.remote.CryptoRateService
import com.paisa.najarine.data.remote.HadithApiService
import com.paisa.najarine.data.repository.*
import com.paisa.najarine.security.BiometricAuthManager
import com.paisa.najarine.sync.CloudBackupManager
import com.paisa.najarine.sync.CloudBackupMetadata
import com.paisa.najarine.sync.FirestoreSyncManager
import com.paisa.najarine.ui.screens.accounting.HabitUi
import com.paisa.najarine.ui.screens.accounting.InvoiceItemUi
import com.paisa.najarine.ui.screens.accounting.TaskUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max

typealias TotalAssetSummary = AccountingSummary

private data class ExtendedAccountingInputs(
    val customerLedger: List<CustomerLedgerEntity>,
    val goals: List<GoalVaultEntity>,
    val invoices: List<InvoiceItemUi>,
    val cryptoRates: Map<String, CryptoRate>
)

class PaisaViewModel(application: Application) : AndroidViewModel(application) {

    val database = PaisaDatabase.getDatabase(application)
    val walletRepo = WalletRepository(database)
    val transactionRepo = TransactionRepository(database)
    val islamicRepo = IslamicRepository(database)
    val financialToolsRepo = FinancialToolsRepository(database)
    val securityManager = BiometricAuthManager(application)
    val authManager = AuthManager(application)
    val syncManager = FirestoreSyncManager(application, database)
    val backupManager = CloudBackupManager(application, database)

    val latestBackupMetadata = MutableStateFlow<CloudBackupMetadata?>(null)
    val isBackupOperating = MutableStateFlow(false)

    fun refreshBackupMetadata() {
        viewModelScope.launch {
            latestBackupMetadata.value = backupManager.getLatestBackupMetadata()
        }
    }

    private val cryptoService = CryptoRateService()
    private val hadithService = HadithApiService()

    val authState: StateFlow<AuthState> = authManager.authState

    // Single-Account User Workspace
    val activeWorkspaceId = MutableStateFlow("personal_default")

    // Wallets & Transactions
    val wallets: StateFlow<List<WalletEntity>> = activeWorkspaceId.flatMapLatest { id ->
        walletRepo.getWallets(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = activeWorkspaceId.flatMapLatest { id ->
        transactionRepo.getTransactions(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = database.categoryDao().getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Accounting & Asset Management Entities
    val budgets: StateFlow<List<BudgetEntity>> = activeWorkspaceId.flatMapLatest { id ->
        financialToolsRepo.getBudgets(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<GoalVaultEntity>> = activeWorkspaceId.flatMapLatest { id ->
        financialToolsRepo.getGoals(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bills: StateFlow<List<BillSubscriptionEntity>> = activeWorkspaceId.flatMapLatest { id ->
        financialToolsRepo.getBills(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debts: StateFlow<List<DebtEntity>> = activeWorkspaceId.flatMapLatest { id ->
        financialToolsRepo.getDebts(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerLedger: StateFlow<List<CustomerLedgerEntity>> = activeWorkspaceId.flatMapLatest { id ->
        financialToolsRepo.getCustomerLedger(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assets: StateFlow<List<AssetEntity>> = activeWorkspaceId.flatMapLatest { id ->
        financialToolsRepo.getAssets(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messEntries: StateFlow<List<MessEntryEntity>> = activeWorkspaceId.flatMapLatest { id ->
        financialToolsRepo.getMessEntries(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bazarItems: StateFlow<List<BazarItemEntity>> = activeWorkspaceId.flatMapLatest { id ->
        financialToolsRepo.getBazarItems(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Crypto Rates
    val cryptoRates = MutableStateFlow<Map<String, CryptoRate>>(emptyMap())
    val isFetchingCryptoRates = MutableStateFlow(false)

    // Dynamic Hourly Hadith
    val currentHadith = MutableStateFlow<HadithItem>(hadithService.getAllAuthenticHadiths().first())

    // In-Memory/Persistent Invoices, Tasks, and Habits
    val invoices = MutableStateFlow<List<InvoiceItemUi>>(emptyList())
    val tasks = MutableStateFlow<List<TaskUi>>(emptyList())
    val habits = MutableStateFlow<List<HabitUi>>(
        listOf(
            HabitUi(name = "৫ ওয়াক্ত নামাজ জামাতে আদায়", streak = 5, isDoneToday = false),
            HabitUi(name = "সকাল ৬টায় ঘুম থেকে ওঠা", streak = 12, isDoneToday = true),
            HabitUi(name = "দৈনিক আয়-ব্যয় Paisa অ্যাপে লেখা", streak = 8, isDoneToday = true)
        )
    )

    // Islamic State
    val qazaPrayers = islamicRepo.qazaPrayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestHourlyHadith = islamicRepo.latestHourlyHadith
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestHourlyQuran = islamicRepo.latestHourlyQuran
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val prayerTimings = MutableStateFlow<PrayerTimingsUi?>(null)
    val userLocation = MutableStateFlow<com.paisa.najarine.util.UserLocationInfo?>(null)
    val isDetectingLocation = MutableStateFlow(false)

    // Security Lock State
    val isAppUnlocked = securityManager.isUnlocked

    // Tasbeeh Session Count
    val tasbeehCount = MutableStateFlow(0)
    val tasbeehTarget = MutableStateFlow(33)
    val currentDhikr = MutableStateFlow("سُبْحَانَ اللَّهِ (SubhanAllah)")

    // Dynamic Total Asset Summary Computation (Strictly synchronizes Wallets + Transactions + Ledgers + Debts with Net Worth)
    val totalAssetSummary: StateFlow<TotalAssetSummary> = combine(
        wallets,
        transactions,
        assets,
        debts,
        combine(customerLedger, goals, invoices, cryptoRates) { cList, gList, invList, cRates ->
            ExtendedAccountingInputs(cList, gList, invList, cRates)
        }
    ) { wList, tList, aList, dList, ext ->
        AccountingEngine.calculate(
            wallets = wList,
            transactions = tList,
            assets = aList,
            debts = dList,
            customerLedger = ext.customerLedger,
            goals = ext.goals,
            invoices = ext.invoices,
            cryptoRates = ext.cryptoRates
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountingSummary())

    init {
        refreshPrayerTimings()
        refreshCryptoRates()
        refreshDynamicHadith()
        observeAuthenticationChanges()
        authManager.attemptAutoSignIn(viewModelScope)
    }

    private fun observeAuthenticationChanges() {
        viewModelScope.launch {
            authManager.authState.collect { state ->
                when (state) {
                    is AuthState.Success -> {
                        val uid = state.user.uid
                        com.paisa.najarine.analytics.PaisaAnalytics.setUserId(uid)
                        syncManager.initializeUserAccount(uid, state.user.email, state.user.displayName)
                    }
                    is AuthState.Idle, is AuthState.Cancelled, is AuthState.Error, is AuthState.Timeout -> {
                        com.paisa.najarine.analytics.PaisaAnalytics.setUserId(null)
                        syncManager.stopAllListeners()
                    }
                    else -> {}
                }
            }
        }
    }

    fun refreshCryptoRates() {
        viewModelScope.launch {
            isFetchingCryptoRates.value = true
            val rates = cryptoService.fetchLiveCryptoRates()
            cryptoRates.value = rates
            isFetchingCryptoRates.value = false
        }
    }

    fun refreshDynamicHadith(forceNext: Boolean = false) {
        viewModelScope.launch {
            val hadith = hadithService.fetchHourlyHadith(forceNext = forceNext)
            currentHadith.value = hadith
        }
    }

    // Wallets & Transactions Actions
    fun addWallet(wallet: WalletEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.walletDao().insertWallet(wallet)
            syncManager.autoSyncWallet(wallet)
        }
    }

    fun deleteWallet(walletId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.walletDao().deleteWallet(walletId)
            syncManager.autoDeleteWallet(walletId)
        }
    }

    suspend fun ensureDefaultCashWallet(): WalletEntity {
        val existing = database.walletDao().getWalletById("wallet_default_cash")
        if (existing != null) return existing

        val defaultWallet = WalletEntity(
            id = "wallet_default_cash",
            workspaceId = activeWorkspaceId.value,
            name = "ক্যাশ ওয়ালেট (Cash)",
            type = "CASH",
            institutionId = "cash",
            accountNumber = "",
            balance = 0.0,
            currencyCode = "BDT",
            colorHex = "#0D9488",
            note = "প্রাথমিক নগদ টাকা ও খরচের হিসাব",
            updatedAt = System.currentTimeMillis()
        )
        database.walletDao().insertWallet(defaultWallet)
        syncManager.autoSyncWallet(defaultWallet)
        return defaultWallet
    }

    // Thread-safe debounce & idempotency registry to protect against duplicate submissions & rapid multi-taps
    private val recentActionsMap = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private fun isDuplicateSubmission(key: String, windowMs: Long = 1200L): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = recentActionsMap[key]
        if (lastTime != null && (now - lastTime) < windowMs) {
            android.util.Log.d("PaisaViewModel", "Prevented duplicate submission for key: $key")
            return true
        }
        recentActionsMap[key] = now
        // Periodically cleanup older keys
        if (recentActionsMap.size > 200) {
            recentActionsMap.entries.removeIf { (now - it.value) > 10000L }
        }
        return false
    }

    fun addTransaction(
        walletId: String,
        amount: Double,
        category: String,
        note: String,
        type: String,
        toWalletId: String? = null,
        fee: Double = 0.0
    ) {
        if (amount <= 0.0) return
        val dedupKey = "tx_${type}_${walletId}_${toWalletId ?: ""}_${amount}_${category}_${note.trim()}"
        if (isDuplicateSubmission(dedupKey)) return

        viewModelScope.launch(Dispatchers.IO) {
            val targetWalletId = if (walletId.isBlank() || database.walletDao().getWalletById(walletId) == null) {
                ensureDefaultCashWallet().id
            } else {
                walletId
            }

            val txId = UUID.randomUUID().toString()
            val tx = TransactionEntity(
                id = txId,
                workspaceId = activeWorkspaceId.value,
                walletId = targetWalletId,
                toWalletId = toWalletId,
                type = type,
                amount = amount,
                fee = fee,
                category = category,
                note = note,
                dateMillis = System.currentTimeMillis()
            )
            database.transactionDao().insertTransaction(tx)

            // Adjust Wallet Balances
            when (type) {
                "INCOME" -> database.walletDao().adjustBalance(targetWalletId, amount)
                "EXPENSE" -> database.walletDao().adjustBalance(targetWalletId, -(amount + fee))
                "TRANSFER" -> {
                    database.walletDao().adjustBalance(targetWalletId, -(amount + fee))
                    toWalletId?.let { database.walletDao().adjustBalance(it, amount) }
                }
            }

            // Sync updated wallet(s) directly to Firestore
            database.walletDao().getWalletById(targetWalletId)?.let {
                syncManager.autoSyncWallet(it)
            }
            if (type == "TRANSFER" && toWalletId != null) {
                database.walletDao().getWalletById(toWalletId)?.let {
                    syncManager.autoSyncWallet(it)
                }
            }

            syncManager.autoSyncTransaction(tx)
        }
    }

    fun addExpense(walletId: String, amount: Double, category: String, fee: Double = 0.0, note: String = "") {
        addTransaction(walletId, amount, category, note, "EXPENSE", fee = fee)
    }

    fun addIncome(walletId: String, amount: Double, category: String, note: String = "") {
        addTransaction(walletId, amount, category, note, "INCOME")
    }

    fun addTransfer(fromId: String, toId: String, amount: Double, fee: Double = 0.0, note: String = "") {
        addTransaction(fromId, amount, "টাকা স্থানান্তর (Transfer)", note, "TRANSFER", toWalletId = toId, fee = fee)
    }

    fun createWallet(
        name: String,
        type: String,
        institutionId: String,
        accountNumber: String,
        balance: Double = 0.0,
        creditLimit: Double = 0.0,
        isExcludedFromTotal: Boolean = false,
        initialBalance: Double = balance
    ) {
        val finalBalance = if (balance != 0.0) balance else initialBalance
        addWallet(
            WalletEntity(
                id = UUID.randomUUID().toString(),
                workspaceId = activeWorkspaceId.value,
                name = name,
                type = type,
                institutionId = institutionId,
                accountNumber = accountNumber,
                balance = finalBalance,
                creditLimit = creditLimit,
                isExcludedFromTotal = isExcludedFromTotal
            )
        )
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.transactionDao().deleteTransaction(tx.id)
            // Revert Wallet Balances
            when (tx.type) {
                "INCOME" -> database.walletDao().adjustBalance(tx.walletId, -tx.amount)
                "EXPENSE" -> database.walletDao().adjustBalance(tx.walletId, tx.amount + tx.fee)
                "TRANSFER" -> {
                    database.walletDao().adjustBalance(tx.walletId, tx.amount + tx.fee)
                    tx.toWalletId?.let { database.walletDao().adjustBalance(it, -tx.amount) }
                }
            }
            // Sync updated wallet balances to Firestore
            database.walletDao().getWalletById(tx.walletId)?.let {
                syncManager.autoSyncWallet(it)
            }
            if (tx.type == "TRANSFER" && tx.toWalletId != null) {
                database.walletDao().getWalletById(tx.toWalletId)?.let {
                    syncManager.autoSyncWallet(it)
                }
            }
            syncManager.autoDeleteTransaction(tx.id)
        }
    }

    // Budget Planning
    fun addBudget(budget: BudgetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.budgetDao().insertBudget(budget)
            syncManager.autoSyncBudget(budget)
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.budgetDao().deleteBudget(id)
            syncManager.autoDeleteBudget(id)
        }
    }

    // Savings Goals
    fun addGoal(goal: GoalVaultEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.goalVaultDao().insertGoal(goal)
            syncManager.autoSyncGoal(goal)
        }
    }

    fun depositToGoal(goalId: String, walletId: String, amount: Double) {
        if (amount <= 0.0) return
        val dedupKey = "goal_dep_${goalId}_${walletId}_${amount}"
        if (isDuplicateSubmission(dedupKey)) return

        viewModelScope.launch(Dispatchers.IO) {
            database.walletDao().adjustBalance(walletId, -amount)
            database.goalVaultDao().contributeToGoal(goalId, amount)

            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                workspaceId = activeWorkspaceId.value,
                walletId = walletId,
                type = "EXPENSE",
                amount = amount,
                category = "Savings & Investments",
                note = "সঞ্চয় লক্ষ্য তহবিলে জমা",
                dateMillis = System.currentTimeMillis()
            )
            database.transactionDao().insertTransaction(tx)
            syncManager.autoSyncTransaction(tx)
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.goalVaultDao().deleteGoal(id)
            syncManager.autoDeleteGoal(id)
        }
    }

    // Bills & Subscriptions
    fun addBill(bill: BillSubscriptionEntity) {
        val dedupKey = "add_bill_${bill.name}_${bill.amount}"
        if (isDuplicateSubmission(dedupKey)) return
        viewModelScope.launch(Dispatchers.IO) {
            database.billDao().insertBill(bill)
            syncManager.autoSyncBill(bill)
        }
    }

    fun payBill(bill: BillSubscriptionEntity, walletId: String) {
        if (bill.isPaid) return
        val dedupKey = "pay_bill_${bill.id}_${walletId}"
        if (isDuplicateSubmission(dedupKey)) return

        viewModelScope.launch(Dispatchers.IO) {
            database.walletDao().adjustBalance(walletId, -bill.amount)
            val updated = bill.copy(
                isPaid = true
            )
            database.billDao().insertBill(updated)

            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                workspaceId = activeWorkspaceId.value,
                walletId = walletId,
                type = "EXPENSE",
                amount = bill.amount,
                category = "Utility & Bills",
                note = "বিল পরিশোধ: ${bill.name}",
                dateMillis = System.currentTimeMillis()
            )
            database.transactionDao().insertTransaction(tx)
            database.walletDao().getWalletById(walletId)?.let {
                syncManager.autoSyncWallet(it)
            }
            syncManager.autoSyncTransaction(tx)
            syncManager.autoSyncBill(updated)
        }
    }

    fun resetBillForNextCycle(bill: BillSubscriptionEntity) {
        val dedupKey = "reset_bill_${bill.id}"
        if (isDuplicateSubmission(dedupKey)) return

        viewModelScope.launch(Dispatchers.IO) {
            val updated = bill.copy(
                isPaid = false,
                nextDueDateMillis = bill.nextDueDateMillis + (30L * 24 * 60 * 60 * 1000)
            )
            database.billDao().insertBill(updated)
            syncManager.autoSyncBill(updated)
        }
    }

    fun deleteBill(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.billDao().deleteBill(id)
            syncManager.autoDeleteBill(id)
        }
    }

    // Dena-Paona
    fun addDebt(debt: DebtEntity, updateWallet: Boolean) {
        val dedupKey = "add_debt_${debt.type}_${debt.personName}_${debt.amount}"
        if (isDuplicateSubmission(dedupKey)) return

        viewModelScope.launch(Dispatchers.IO) {
            if (updateWallet && debt.walletId.isNotBlank()) {
                val delta = if (debt.type == "PONA") -debt.amount else debt.amount
                database.walletDao().adjustBalance(debt.walletId, delta)
            }
            database.debtDao().insertDebt(debt)
            syncManager.autoSyncDebt(debt)
        }
    }

    fun settleDebt(debt: DebtEntity, walletId: String) {
        val dedupKey = "settle_debt_${debt.id}_${walletId}"
        if (isDuplicateSubmission(dedupKey)) return

        viewModelScope.launch(Dispatchers.IO) {
            if (walletId.isNotBlank()) {
                val delta = if (debt.type == "PONA") debt.amount else -debt.amount
                database.walletDao().adjustBalance(walletId, delta)
            }
            database.debtDao().toggleSettled(debt.id, true)
            syncManager.autoSyncDebt(debt.copy(isSettled = true))
        }
    }

    fun deleteDebt(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.debtDao().deleteDebt(id)
            syncManager.autoDeleteDebt(id)
        }
    }

    // Credit Card Bill Payment
    fun payCreditCardBill(cardWalletId: String, fromWalletId: String, amount: Double) {
        if (amount <= 0.0) return
        val dedupKey = "pay_card_${cardWalletId}_${fromWalletId}_${amount}"
        if (isDuplicateSubmission(dedupKey)) return

        viewModelScope.launch(Dispatchers.IO) {
            // Card balance is stored as -debt, so adding amount brings it towards 0
            database.walletDao().adjustBalance(cardWalletId, amount)
            database.walletDao().adjustBalance(fromWalletId, -amount)

            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                workspaceId = activeWorkspaceId.value,
                walletId = fromWalletId,
                toWalletId = cardWalletId,
                type = "TRANSFER",
                amount = amount,
                category = "Card Payment",
                note = "ক্রেডিট কার্ড বিল পরিশোধ",
                dateMillis = System.currentTimeMillis()
            )
            database.transactionDao().insertTransaction(tx)
            syncManager.autoSyncTransaction(tx)
        }
    }

    // Asset Management (FDR, DPS, Gold, Silver, Stocks, Crypto)
    fun addAsset(asset: AssetEntity, deductFromWallet: Boolean = false, walletId: String = "") {
        val dedupKey = "add_asset_${asset.category}_${asset.name}_${asset.quantity}_${asset.buyPrice}"
        if (isDuplicateSubmission(dedupKey)) return

        viewModelScope.launch(Dispatchers.IO) {
            if (deductFromWallet && walletId.isNotBlank()) {
                database.walletDao().adjustBalance(walletId, -asset.buyPrice)
            }
            database.assetDao().insertAsset(asset)
            syncManager.autoSyncAsset(asset)
        }
    }

    fun deleteAsset(assetId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.assetDao().deleteAsset(assetId)
            syncManager.autoDeleteAsset(assetId)
        }
    }

    // Customer / Client Ledger
    fun addCustomerLedger(entry: CustomerLedgerEntity, depositToWalletId: String? = null) {
        val dedupKey = "add_ledger_${entry.customerName}_${entry.type}_${entry.amount}"
        if (isDuplicateSubmission(dedupKey)) return

        viewModelScope.launch(Dispatchers.IO) {
            database.customerLedgerDao().insertEntry(entry)
            
            // If user selected a wallet to deposit collected cash/bank amount
            if (entry.type == "JOMA" && !depositToWalletId.isNullOrBlank()) {
                database.walletDao().adjustBalance(depositToWalletId, entry.amount)
                database.walletDao().getWalletById(depositToWalletId)?.let {
                    syncManager.autoSyncWallet(it)
                }
                val tx = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    workspaceId = activeWorkspaceId.value,
                    walletId = depositToWalletId,
                    type = "INCOME",
                    amount = entry.amount,
                    category = "খদ্দের বাকি আদায় (Customer Collection)",
                    note = "খদ্দের: ${entry.customerName} - ${entry.note.ifBlank { "বাকি পরিশোধ" }}",
                    dateMillis = entry.dateMillis
                )
                database.transactionDao().insertTransaction(tx)
                syncManager.autoSyncTransaction(tx)
            }

            syncManager.autoSyncCustomerLedger(entry)
        }
    }

    fun deleteCustomerLedger(entryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.customerLedgerDao().deleteEntry(entryId)
            syncManager.autoDeleteCustomerLedger(entryId)
        }
    }

    // Invoices Management
    fun addInvoice(invoice: InvoiceItemUi) {
        invoices.value = listOf(invoice) + invoices.value
    }

    fun markInvoicePaid(id: String) {
        invoices.value = invoices.value.map {
            if (it.id == id) it.copy(status = "PAID") else it
        }
    }

    fun deleteInvoice(id: String) {
        invoices.value = invoices.value.filter { it.id != id }
    }

    // Tasks & Habits Management
    fun addTask(task: TaskUi) {
        tasks.value = listOf(task) + tasks.value
    }

    fun toggleTask(id: String) {
        tasks.value = tasks.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
    }

    fun deleteTask(id: String) {
        tasks.value = tasks.value.filter { it.id != id }
    }

    // Mess Management
    fun saveMessEntry(entry: MessEntryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            financialToolsRepo.saveMessEntry(entry)
        }
    }

    fun deleteMessEntry(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            financialToolsRepo.deleteMessEntry(id)
        }
    }

    // Bazar & Grocery Shopping Management
    fun saveBazarItem(item: BazarItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            financialToolsRepo.saveBazarItem(item)
        }
    }

    fun saveBazarItems(items: List<BazarItemEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            financialToolsRepo.saveBazarItems(items)
        }
    }

    fun updateBazarItem(item: BazarItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            financialToolsRepo.updateBazarItem(item)
        }
    }

    fun toggleBazarItemChecked(id: String, isChecked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            financialToolsRepo.toggleBazarItemChecked(id, isChecked)
        }
    }

    fun deleteBazarItem(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            financialToolsRepo.deleteBazarItem(id)
        }
    }

    fun clearCheckedBazarItems() {
        viewModelScope.launch(Dispatchers.IO) {
            financialToolsRepo.clearCheckedBazarItems(activeWorkspaceId.value)
        }
    }

    fun createExpenseFromBazar(
        amount: Double,
        walletId: String,
        note: String,
        isMessBazar: Boolean = false,
        buyerMemberName: String = ""
    ) {
        if (amount <= 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            if (walletId.isNotBlank()) {
                database.walletDao().adjustBalance(walletId, -amount)
            }
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                workspaceId = activeWorkspaceId.value,
                walletId = walletId,
                type = "EXPENSE",
                amount = amount,
                category = "Groceries / Bazar",
                note = note.ifBlank { if (isMessBazar) "মেস বাজার খরচ ($buyerMemberName)" else "বাজার সদাই খরচ" },
                dateMillis = System.currentTimeMillis()
            )
            database.transactionDao().insertTransaction(tx)
            syncManager.autoSyncTransaction(tx)

            if (isMessBazar && buyerMemberName.isNotBlank()) {
                val messEntry = MessEntryEntity(
                    id = UUID.randomUUID().toString(),
                    workspaceId = activeWorkspaceId.value,
                    memberName = buyerMemberName,
                    mealsCount = 0.0,
                    depositAmount = 0.0,
                    bazarExpense = amount,
                    fixedExpenseShare = 0.0,
                    dateMillis = System.currentTimeMillis(),
                    note = note.ifBlank { "বাজার সদাই কেনাকাটা" }
                )
                financialToolsRepo.saveMessEntry(messEntry)
            }
        }
    }

    fun addHabit(habit: HabitUi) {
        habits.value = listOf(habit) + habits.value
    }

    fun toggleHabit(id: String) {
        habits.value = habits.value.map {
            if (it.id == id) {
                val newDone = !it.isDoneToday
                val newStreak = if (newDone) it.streak + 1 else max(1, it.streak - 1)
                it.copy(isDoneToday = newDone, streak = newStreak)
            } else it
        }
    }

    fun deleteHabit(id: String) {
        habits.value = habits.value.filter { it.id != id }
    }

    // Islamic & Prayer Times
    fun refreshPrayerTimings(madhab: String? = null) {
        viewModelScope.launch {
            try {
                val loc = userLocation.value
                val currentMadhab = madhab ?: com.paisa.najarine.notification.AdhanPreferences.selectedMadhabState.value
                val timings = if (loc != null) {
                    islamicRepo.getPrayerTimingsByCoordinates(loc.latitude, loc.longitude, madhab = currentMadhab)
                } else {
                    islamicRepo.getPrayerTimings(madhab = currentMadhab)
                }
                prayerTimings.value = timings
            } catch (_: Exception) {}
        }
    }

    fun detectLocationAndRefreshPrayerTimings(context: android.content.Context, madhab: String? = null, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            isDetectingLocation.value = true
            try {
                val currentMadhab = madhab ?: com.paisa.najarine.notification.AdhanPreferences.getSelectedMadhab(context)
                val loc = com.paisa.najarine.util.LocationHelper.getCurrentLocation(context)
                if (loc != null) {
                    userLocation.value = loc
                    val timings = islamicRepo.getPrayerTimingsByCoordinates(loc.latitude, loc.longitude, madhab = currentMadhab)
                    prayerTimings.value = timings
                    onComplete("অবস্থান শনাক্ত হয়েছে: ${loc.displayName}")
                } else {
                    onComplete("অবস্থান শনাক্ত করা যায়নি, ডিফল্ট সময় গণনা ব্যবহার করা হচ্ছে।")
                }
            } catch (e: Exception) {
                onComplete("ত্রুটি: ${e.localizedMessage}")
            } finally {
                isDetectingLocation.value = false
            }
        }
    }

    fun testAdhanAudioAndNotification(context: android.content.Context) {
        viewModelScope.launch {
            // 1. Play Adhan audio
            com.paisa.najarine.notification.DefaultAdhanAudioProvider.playAdhan(context, isFajr = false)

            // 2. Fire full mobile notification
            val timings = prayerTimings.value ?: islamicRepo.getOfflinePrayerTimings()
            com.paisa.najarine.notification.PaisaNotificationManager.showNotification(
                context = context,
                channelId = com.paisa.najarine.notification.PaisaNotificationManager.CHANNEL_PRAYER,
                notificationId = 777,
                title = "নামাজের আযান পরীক্ষা — الصَّلَاةُ خَيْرٌ مِنَ النَّوْمِ",
                body = "আযান সফলভাবে পরীক্ষামূলকভাবে চালু হয়েছে। আল্লাহু আকবার! ওয়াক্ত: ${timings.fajr}",
                targetTab = 3,
                targetScreen = "ADHAN_PRAYER"
            )
        }
    }

    fun testHourlyHadithNotification(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.paisa.najarine.notification.HourlyIslamicScheduler.triggerImmediateSync(context)
            // Also directly update in-memory currentHadith for immediate UI response
            try {
                val hadith = hadithService.fetchHourlyHadith()
                currentHadith.value = hadith
                val entity = com.paisa.najarine.data.local.HourlyHadithEntity(
                    id = "latest_hourly_hadith",
                    title = hadith.title,
                    arabic = hadith.arabic,
                    translation = hadith.translation,
                    banglaTranslation = hadith.banglaTranslation,
                    narrator = hadith.narrator,
                    source = hadith.source,
                    hadithNumber = hadith.hadithNumber,
                    grade = hadith.grade,
                    topic = hadith.topic,
                    fetchedAtMillis = System.currentTimeMillis()
                )
                database.hourlyHadithDao().insertHadith(entity)
                com.paisa.najarine.notification.PaisaNotificationManager.showNotification(
                    context = context,
                    channelId = com.paisa.najarine.notification.PaisaNotificationManager.CHANNEL_QURAN_HADITH,
                    notificationId = 1001,
                    title = "ঘণ্টার নির্বাচিত হাদিস — ${hadith.title}",
                    body = "${hadith.banglaTranslation}\n(${hadith.source})",
                    targetTab = 3,
                    targetScreen = "HADITH"
                )
            } catch (_: Exception) {}
        }
    }

    fun testHourlyQuranNotification(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.paisa.najarine.notification.HourlyIslamicScheduler.triggerImmediateSync(context)
            try {
                val entity = com.paisa.najarine.data.local.HourlyQuranEntity(
                    id = "latest_hourly_ayah",
                    surahNumber = 65,
                    surahNameArabic = "الطلاق",
                    surahNameEnglish = "At-Talaq",
                    surahNameBangla = "আত-ত্বালাক্ব",
                    ayahNumber = 3,
                    arabicText = "وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ ۚ وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ",
                    englishTranslation = "And will provide for him from where he does not expect. And whoever relies upon Allah - then He is sufficient for him.",
                    banglaTranslation = "এবং তিনি তাকে এমন উৎস থেকে রিজিক দান করবেন যা সে কল্পনাও করেনি। আর যে ব্যক্তি আল্লাহর ওপর ভরসা করে, তার জন্য তিনিই যথেষ্ট।",
                    fetchedAtMillis = System.currentTimeMillis()
                )
                database.hourlyQuranDao().insertAyah(entity)
                com.paisa.najarine.notification.PaisaNotificationManager.showNotification(
                    context = context,
                    channelId = com.paisa.najarine.notification.PaisaNotificationManager.CHANNEL_QURAN_HADITH,
                    notificationId = 1002,
                    title = "ঘণ্টার কুরআন আয়াত — সূরা ${entity.surahNameBangla} (${entity.surahNumber}:${entity.ayahNumber})",
                    body = "${entity.banglaTranslation}\n\"${entity.arabicText}\"",
                    targetTab = 3,
                    targetScreen = "QURAN"
                )
            } catch (_: Exception) {}
        }
    }

    fun incrementTasbeeh() {
        tasbeehCount.value += 1
    }

    fun resetTasbeeh() {
        tasbeehCount.value = 0
    }

    fun updateQaza(fajr: Int, dhuhr: Int, asr: Int, maghrib: Int, isha: Int, witr: Int) {
        viewModelScope.launch {
            islamicRepo.updateQaza(fajr, dhuhr, asr, maghrib, isha, witr)
            syncManager.autoSyncQaza(
                QazaPrayerEntity(
                    id = "singleton_qaza",
                    fajrCount = fajr,
                    dhuhrCount = dhuhr,
                    asrCount = asr,
                    maghribCount = maghrib,
                    ishaCount = isha,
                    witrCount = witr
                )
            )
        }
    }

    fun calculateAndSaveZakat(
        cash: Double,
        bank: Double,
        goldGrams: Double,
        goldPricePerGram: Double,
        silverGrams: Double,
        silverPricePerGram: Double,
        businessStock: Double,
        receivables: Double,
        debtsDue: Double
    ): Double {
        val goldVal = goldGrams * goldPricePerGram
        val silverVal = silverGrams * silverPricePerGram
        val totalAssets = cash + bank + goldVal + silverVal + businessStock + receivables
        val netZakatable = max(0.0, totalAssets - debtsDue)
        val zakatPayable = netZakatable * 0.025

        viewModelScope.launch {
            islamicRepo.saveZakatCalculation(
                ZakatRecordEntity(
                    id = UUID.randomUUID().toString(),
                    cashAmount = cash,
                    bankAmount = bank,
                    goldValue = goldVal,
                    silverValue = silverVal,
                    businessStockValue = businessStock,
                    receivables = receivables,
                    liabilitiesDue = debtsDue,
                    totalZakatable = netZakatable,
                    zakatPayable = zakatPayable
                )
            )
        }
        return zakatPayable
    }

    // Auth & Cloud Sync Handlers
    fun performOnlineSync() {
        viewModelScope.launch {
            syncManager.performFullCloudSync()
        }
    }

    fun signInWithGoogle(activity: android.app.Activity) {
        authManager.signInWithGoogle(activity, viewModelScope)
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }

    fun switchAccount() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }

    fun resetAuthState() = authManager.resetState()
}
