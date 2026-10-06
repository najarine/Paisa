package com.paisa.najarine.sync

import android.util.Log
import com.paisa.najarine.data.local.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

enum class SyncState {
    SYNCED,
    SYNCING,
    ERROR,
    OFFLINE
}

data class SyncStatus(
    val state: SyncState = SyncState.SYNCED,
    val lastSyncTimeMillis: Long = System.currentTimeMillis(),
    val pendingOperations: Int = 0
)

/**
 * Single-Account Cloud Auto-Sync Engine for Paisa Personal Finance
 * Synchronizes user data directly with private cloud storage at /users/{uid}/
 */
class FirestoreSyncManager(
    private val context: android.content.Context,
    private val database: PaisaDatabase,
    private val db: FirebaseFirestore? = null
) {
    private val firestore: FirebaseFirestore by lazy {
        if (db != null) return@lazy db
        val dbId = runCatching { context.getString(com.paisa.najarine.R.string.firestore_database_id) }.getOrNull()
        if (!dbId.isNullOrBlank() && dbId != "(default)") {
            FirebaseFirestore.getInstance(dbId)
        } else {
            FirebaseFirestore.getInstance()
        }
    }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val activeListeners = mutableListOf<ListenerRegistration>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun stopAllListeners() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
    }

    suspend fun initializeUserAccount(uid: String, userEmail: String?, displayName: String?) {
        val userRef = firestore.collection("users").document(uid)
        try {
            _syncStatus.value = SyncStatus(state = SyncState.SYNCING)
            val cleanEmail = userEmail?.lowercase()?.trim() ?: ""

            val userData = hashMapOf(
                "uid" to uid,
                "email" to cleanEmail,
                "displayName" to (displayName ?: ""),
                "lastActive" to System.currentTimeMillis()
            )
            userRef.set(userData, SetOptions.merge()).await()
            _syncStatus.value = SyncStatus(state = SyncState.SYNCED)
            startRealtimeSync(uid)
            performFullCloudSync()
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Init user notice: ${e.localizedMessage}")
            _syncStatus.value = SyncStatus(state = SyncState.SYNCED)
            startRealtimeSync(uid)
            performFullCloudSync()
        }
    }

    fun startRealtimeSync(uid: String) {
        stopAllListeners()

        // 1. Wallets listener
        val walletReg = firestore.collection("users").document(uid).collection("wallets")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    coroutineScope.launch {
                        for (doc in snapshot.documents) {
                            val data = doc.data ?: continue
                            val wallet = WalletEntity(
                                id = doc.id,
                                workspaceId = "personal_default",
                                name = data["name"] as? String ?: "Wallet",
                                type = data["type"] as? String ?: "CASH",
                                institutionId = data["institutionId"] as? String ?: "cash",
                                accountNumber = data["accountNumber"] as? String ?: "",
                                balance = (data["balance"] as? Number)?.toDouble() ?: 0.0,
                                creditLimit = (data["creditLimit"] as? Number)?.toDouble() ?: 0.0,
                                currencyCode = data["currencyCode"] as? String ?: "BDT",
                                colorHex = data["colorHex"] as? String ?: "#0D9488",
                                note = data["note"] as? String ?: "",
                                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            )
                            database.walletDao().insertWallet(wallet)
                        }
                    }
                }
            }
        walletReg?.let { activeListeners.add(it) }

        // 2. Transactions listener
        val txReg = firestore.collection("users").document(uid).collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    coroutineScope.launch {
                        for (doc in snapshot.documents) {
                            val data = doc.data ?: continue
                            val tx = TransactionEntity(
                                id = doc.id,
                                workspaceId = "personal_default",
                                walletId = data["walletId"] as? String ?: "default",
                                toWalletId = data["toWalletId"] as? String,
                                type = data["type"] as? String ?: "EXPENSE",
                                amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                                fee = (data["fee"] as? Number)?.toDouble() ?: 0.0,
                                category = data["category"] as? String ?: "General",
                                note = data["note"] as? String ?: "",
                                dateMillis = (data["dateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            )
                            database.transactionDao().insertTransaction(tx)
                        }
                    }
                }
            }
        txReg?.let { activeListeners.add(it) }

        // 3. Assets listener
        val assetReg = firestore.collection("users").document(uid).collection("assets")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    coroutineScope.launch {
                        for (doc in snapshot.documents) {
                            val data = doc.data ?: continue
                            val asset = AssetEntity(
                                id = doc.id,
                                workspaceId = "personal_default",
                                name = data["name"] as? String ?: "",
                                category = data["category"] as? String ?: "GOLD",
                                quantity = (data["quantity"] as? Number)?.toDouble() ?: 0.0,
                                unit = data["unit"] as? String ?: "vori",
                                buyPrice = (data["buyPrice"] as? Number)?.toDouble() ?: 0.0,
                                currentPrice = (data["currentPrice"] as? Number)?.toDouble() ?: 0.0,
                                dateMillis = (data["dateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            )
                            database.assetDao().insertAsset(asset)
                        }
                    }
                }
            }
        assetReg?.let { activeListeners.add(it) }

        // 4. Debts listener
        val debtReg = firestore.collection("users").document(uid).collection("debts")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    coroutineScope.launch {
                        for (doc in snapshot.documents) {
                            val data = doc.data ?: continue
                            val debt = DebtEntity(
                                id = doc.id,
                                workspaceId = "personal_default",
                                walletId = data["walletId"] as? String ?: "",
                                personName = data["personName"] as? String ?: "",
                                phoneNumber = data["phoneNumber"] as? String ?: "",
                                amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                                type = data["type"] as? String ?: "DENA",
                                dueDateMillis = (data["dueDateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                isSettled = data["isSettled"] as? Boolean ?: false,
                                note = data["note"] as? String ?: ""
                            )
                            database.debtDao().insertDebt(debt)
                        }
                    }
                }
            }
        debtReg?.let { activeListeners.add(it) }

        // 5. Budgets listener
        val budgetReg = firestore.collection("users").document(uid).collection("budgets")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    coroutineScope.launch {
                        for (doc in snapshot.documents) {
                            val data = doc.data ?: continue
                            val budget = BudgetEntity(
                                id = doc.id,
                                workspaceId = "personal_default",
                                categoryName = data["categoryName"] as? String ?: "",
                                amountLimit = (data["amountLimit"] as? Number)?.toDouble() ?: 0.0,
                                period = data["period"] as? String ?: "MONTHLY",
                                month = (data["month"] as? Number)?.toInt() ?: 1,
                                year = (data["year"] as? Number)?.toInt() ?: 2026
                            )
                            database.budgetDao().insertBudget(budget)
                        }
                    }
                }
            }
        budgetReg?.let { activeListeners.add(it) }

        // 6. Goals listener
        val goalReg = firestore.collection("users").document(uid).collection("goals")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    coroutineScope.launch {
                        for (doc in snapshot.documents) {
                            val data = doc.data ?: continue
                            val goal = GoalVaultEntity(
                                id = doc.id,
                                workspaceId = "personal_default",
                                name = data["name"] as? String ?: "",
                                targetAmount = (data["targetAmount"] as? Number)?.toDouble() ?: 0.0,
                                currentAmount = (data["currentAmount"] as? Number)?.toDouble() ?: 0.0,
                                targetDateMillis = (data["targetDateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                colorHex = data["colorHex"] as? String ?: "#10B981",
                                isCompleted = data["isCompleted"] as? Boolean ?: false
                            )
                            database.goalVaultDao().insertGoal(goal)
                        }
                    }
                }
            }
        goalReg?.let { activeListeners.add(it) }

        // 7. Bills listener
        val billReg = firestore.collection("users").document(uid).collection("bills")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    coroutineScope.launch {
                        for (doc in snapshot.documents) {
                            val data = doc.data ?: continue
                            val bill = BillSubscriptionEntity(
                                id = doc.id,
                                workspaceId = "personal_default",
                                walletId = data["walletId"] as? String ?: "",
                                name = data["name"] as? String ?: "",
                                amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                                cycle = data["cycle"] as? String ?: "MONTHLY",
                                nextDueDateMillis = (data["nextDueDateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                isPaid = data["isPaid"] as? Boolean ?: false
                            )
                            database.billDao().insertBill(bill)
                        }
                    }
                }
            }
        billReg?.let { activeListeners.add(it) }

        // 8. Customer Ledger listener
        val ledgerReg = firestore.collection("users").document(uid).collection("ledger")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    coroutineScope.launch {
                        for (doc in snapshot.documents) {
                            val data = doc.data ?: continue
                            val entry = CustomerLedgerEntity(
                                id = doc.id,
                                workspaceId = "personal_default",
                                customerName = data["customerName"] as? String ?: "",
                                phone = data["phone"] as? String ?: "",
                                amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                                type = data["type"] as? String ?: "BAKI",
                                dateMillis = (data["dateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                note = data["note"] as? String ?: ""
                            )
                            database.customerLedgerDao().insertEntry(entry)
                        }
                    }
                }
            }
        ledgerReg?.let { activeListeners.add(it) }

        // 9. Qaza prayers listener
        val qazaReg = firestore.collection("users").document(uid).collection("islamic").document("qaza_summary")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    coroutineScope.launch {
                        val data = snapshot.data ?: return@launch
                        val qaza = QazaPrayerEntity(
                            id = "singleton_qaza",
                            fajrCount = (data["fajrCount"] as? Number)?.toInt() ?: 0,
                            dhuhrCount = (data["dhuhrCount"] as? Number)?.toInt() ?: 0,
                            asrCount = (data["asrCount"] as? Number)?.toInt() ?: 0,
                            maghribCount = (data["maghribCount"] as? Number)?.toInt() ?: 0,
                            ishaCount = (data["ishaCount"] as? Number)?.toInt() ?: 0,
                            witrCount = (data["witrCount"] as? Number)?.toInt() ?: 0
                        )
                        database.qazaPrayerDao().updateQaza(qaza)
                    }
                }
            }
        qazaReg?.let { activeListeners.add(it) }
    }

    suspend fun performFullCloudSync() = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        try {
            _syncStatus.value = SyncStatus(state = SyncState.SYNCING)
            val uid = currentUser.uid
            val userDoc = firestore.collection("users").document(uid)

            // 1. Pull Wallets from Cloud
            try {
                val walletDocs = userDoc.collection("wallets").get().await()
                for (doc in walletDocs.documents) {
                    val data = doc.data ?: continue
                    val wallet = WalletEntity(
                        id = doc.id,
                        workspaceId = "personal_default",
                        name = data["name"] as? String ?: "Wallet",
                        type = data["type"] as? String ?: "CASH",
                        institutionId = data["institutionId"] as? String ?: "cash",
                        accountNumber = data["accountNumber"] as? String ?: "",
                        balance = (data["balance"] as? Number)?.toDouble() ?: 0.0,
                        creditLimit = (data["creditLimit"] as? Number)?.toDouble() ?: 0.0,
                        currencyCode = data["currencyCode"] as? String ?: "BDT",
                        colorHex = data["colorHex"] as? String ?: "#0D9488",
                        note = data["note"] as? String ?: "",
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                    database.walletDao().insertWallet(wallet)
                }
            } catch (e: Exception) {
                Log.w("SyncManager", "Wallets cloud pull notice: ${e.message}")
            }

            // 2. Pull Transactions from Cloud
            try {
                val txDocs = userDoc.collection("transactions").get().await()
                for (doc in txDocs.documents) {
                    val data = doc.data ?: continue
                    val tx = TransactionEntity(
                        id = doc.id,
                        workspaceId = "personal_default",
                        walletId = data["walletId"] as? String ?: "default",
                        toWalletId = data["toWalletId"] as? String,
                        type = data["type"] as? String ?: "EXPENSE",
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        fee = (data["fee"] as? Number)?.toDouble() ?: 0.0,
                        category = data["category"] as? String ?: "General",
                        note = data["note"] as? String ?: "",
                        dateMillis = (data["dateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                    database.transactionDao().insertTransaction(tx)
                }
            } catch (e: Exception) {
                Log.w("SyncManager", "Tx cloud pull notice: ${e.message}")
            }

            // 3. Pull Assets (Gold, Silver, Stocks, FDR, Crypto) from Cloud
            try {
                val assetDocs = userDoc.collection("assets").get().await()
                for (doc in assetDocs.documents) {
                    val data = doc.data ?: continue
                    val asset = AssetEntity(
                        id = doc.id,
                        workspaceId = "personal_default",
                        name = data["name"] as? String ?: "",
                        category = data["category"] as? String ?: "GOLD",
                        quantity = (data["quantity"] as? Number)?.toDouble() ?: 0.0,
                        unit = data["unit"] as? String ?: "vori",
                        buyPrice = (data["buyPrice"] as? Number)?.toDouble() ?: 0.0,
                        currentPrice = (data["currentPrice"] as? Number)?.toDouble() ?: 0.0,
                        dateMillis = (data["dateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                    database.assetDao().insertAsset(asset)
                }
            } catch (e: Exception) {
                Log.w("SyncManager", "Assets cloud pull notice: ${e.message}")
            }

            // 4. Pull Debts (Dena / Paona) from Cloud
            try {
                val debtDocs = userDoc.collection("debts").get().await()
                for (doc in debtDocs.documents) {
                    val data = doc.data ?: continue
                    val debt = DebtEntity(
                        id = doc.id,
                        workspaceId = "personal_default",
                        walletId = data["walletId"] as? String ?: "",
                        personName = data["personName"] as? String ?: "",
                        phoneNumber = data["phoneNumber"] as? String ?: "",
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        type = data["type"] as? String ?: "DENA",
                        dueDateMillis = (data["dueDateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        isSettled = data["isSettled"] as? Boolean ?: false,
                        note = data["note"] as? String ?: ""
                    )
                    database.debtDao().insertDebt(debt)
                }
            } catch (e: Exception) {
                Log.w("SyncManager", "Debts cloud pull notice: ${e.message}")
            }

            // 5. Pull Budgets from Cloud
            try {
                val budgetDocs = userDoc.collection("budgets").get().await()
                for (doc in budgetDocs.documents) {
                    val data = doc.data ?: continue
                    val budget = BudgetEntity(
                        id = doc.id,
                        workspaceId = "personal_default",
                        categoryName = data["categoryName"] as? String ?: "",
                        amountLimit = (data["amountLimit"] as? Number)?.toDouble() ?: 0.0,
                        period = data["period"] as? String ?: "MONTHLY",
                        month = (data["month"] as? Number)?.toInt() ?: 1,
                        year = (data["year"] as? Number)?.toInt() ?: 2026
                    )
                    database.budgetDao().insertBudget(budget)
                }
            } catch (e: Exception) {
                Log.w("SyncManager", "Budgets cloud pull notice: ${e.message}")
            }

            // 6. Pull Goals from Cloud
            try {
                val goalDocs = userDoc.collection("goals").get().await()
                for (doc in goalDocs.documents) {
                    val data = doc.data ?: continue
                    val goal = GoalVaultEntity(
                        id = doc.id,
                        workspaceId = "personal_default",
                        name = data["name"] as? String ?: "",
                        targetAmount = (data["targetAmount"] as? Number)?.toDouble() ?: 0.0,
                        currentAmount = (data["currentAmount"] as? Number)?.toDouble() ?: 0.0,
                        targetDateMillis = (data["targetDateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        colorHex = data["colorHex"] as? String ?: "#10B981",
                        isCompleted = data["isCompleted"] as? Boolean ?: false
                    )
                    database.goalVaultDao().insertGoal(goal)
                }
            } catch (e: Exception) {
                Log.w("SyncManager", "Goals cloud pull notice: ${e.message}")
            }

            // 7. Pull Bills from Cloud
            try {
                val billDocs = userDoc.collection("bills").get().await()
                for (doc in billDocs.documents) {
                    val data = doc.data ?: continue
                    val bill = BillSubscriptionEntity(
                        id = doc.id,
                        workspaceId = "personal_default",
                        walletId = data["walletId"] as? String ?: "",
                        name = data["name"] as? String ?: "",
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        cycle = data["cycle"] as? String ?: "MONTHLY",
                        nextDueDateMillis = (data["nextDueDateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        isPaid = data["isPaid"] as? Boolean ?: false
                    )
                    database.billDao().insertBill(bill)
                }
            } catch (e: Exception) {
                Log.w("SyncManager", "Bills cloud pull notice: ${e.message}")
            }

            // 8. Pull Customer Ledger from Cloud
            try {
                val ledgerDocs = userDoc.collection("ledger").get().await()
                for (doc in ledgerDocs.documents) {
                    val data = doc.data ?: continue
                    val entry = CustomerLedgerEntity(
                        id = doc.id,
                        workspaceId = "personal_default",
                        customerName = data["customerName"] as? String ?: "",
                        phone = data["phone"] as? String ?: "",
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        type = data["type"] as? String ?: "BAKI",
                        dateMillis = (data["dateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        note = data["note"] as? String ?: ""
                    )
                    database.customerLedgerDao().insertEntry(entry)
                }
            } catch (e: Exception) {
                Log.w("SyncManager", "Ledger cloud pull notice: ${e.message}")
            }

            // 9. Pull Qaza from Cloud
            try {
                val qDoc = userDoc.collection("islamic").document("qaza_summary").get().await()
                if (qDoc.exists()) {
                    val data = qDoc.data
                    if (data != null) {
                        val qaza = QazaPrayerEntity(
                            id = "singleton_qaza",
                            fajrCount = (data["fajrCount"] as? Number)?.toInt() ?: 0,
                            dhuhrCount = (data["dhuhrCount"] as? Number)?.toInt() ?: 0,
                            asrCount = (data["asrCount"] as? Number)?.toInt() ?: 0,
                            maghribCount = (data["maghribCount"] as? Number)?.toInt() ?: 0,
                            ishaCount = (data["ishaCount"] as? Number)?.toInt() ?: 0,
                            witrCount = (data["witrCount"] as? Number)?.toInt() ?: 0
                        )
                        database.qazaPrayerDao().updateQaza(qaza)
                    }
                }
            } catch (e: Exception) {
                Log.w("SyncManager", "Qaza cloud pull notice: ${e.message}")
            }

            _syncStatus.value = SyncStatus(state = SyncState.SYNCED)
            Log.d("SyncManager", "Full cloud sync completed successfully.")
        } catch (e: Exception) {
            Log.w("SyncManager", "Full cloud sync notice: ${e.localizedMessage}")
            _syncStatus.value = SyncStatus(state = SyncState.SYNCED)
        }
    }

    // Direct background sync methods called on data changes
    fun autoSyncWallet(wallet: WalletEntity) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "id" to wallet.id,
                    "workspaceId" to wallet.workspaceId,
                    "name" to wallet.name,
                    "type" to wallet.type,
                    "institutionId" to wallet.institutionId,
                    "accountNumber" to wallet.accountNumber,
                    "balance" to wallet.balance,
                    "creditLimit" to wallet.creditLimit,
                    "currencyCode" to wallet.currencyCode,
                    "colorHex" to wallet.colorHex,
                    "isExcludedFromTotal" to wallet.isExcludedFromTotal,
                    "note" to wallet.note,
                    "updatedAt" to wallet.updatedAt
                )
                firestore.collection("users").document(uid)
                    .collection("wallets").document(wallet.id)
                    .set(data, SetOptions.merge())
            } catch (e: Exception) {
                Log.w("SyncManager", "Wallet sync notice: ${e.localizedMessage}")
            }
        }
    }

    fun autoDeleteWallet(walletId: String) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .collection("wallets").document(walletId)
                    .delete()
            } catch (_: Exception) {}
        }
    }

    fun autoSyncTransaction(tx: TransactionEntity) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "id" to tx.id,
                    "workspaceId" to tx.workspaceId,
                    "walletId" to tx.walletId,
                    "toWalletId" to tx.toWalletId,
                    "type" to tx.type,
                    "amount" to tx.amount,
                    "fee" to tx.fee,
                    "category" to tx.category,
                    "note" to tx.note,
                    "dateMillis" to tx.dateMillis,
                    "tags" to tx.tags,
                    "receiptImageUri" to tx.receiptImageUri,
                    "isDraft" to tx.isDraft,
                    "isAiGenerated" to tx.isAiGenerated,
                    "confirmedByUser" to tx.confirmedByUser
                )
                firestore.collection("users").document(uid)
                    .collection("transactions").document(tx.id)
                    .set(data, SetOptions.merge())
            } catch (e: Exception) {
                Log.w("SyncManager", "Tx sync notice: ${e.localizedMessage}")
            }
        }
    }

    fun autoDeleteTransaction(txId: String) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .collection("transactions").document(txId)
                    .delete()
            } catch (_: Exception) {}
        }
    }

    fun autoSyncAsset(asset: AssetEntity) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "id" to asset.id,
                    "workspaceId" to asset.workspaceId,
                    "name" to asset.name,
                    "category" to asset.category,
                    "quantity" to asset.quantity,
                    "unit" to asset.unit,
                    "buyPrice" to asset.buyPrice,
                    "currentPrice" to asset.currentPrice,
                    "dateMillis" to asset.dateMillis
                )
                firestore.collection("users").document(uid)
                    .collection("assets").document(asset.id)
                    .set(data, SetOptions.merge())
            } catch (e: Exception) {
                Log.w("SyncManager", "Asset sync notice: ${e.localizedMessage}")
            }
        }
    }

    fun autoDeleteAsset(assetId: String) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .collection("assets").document(assetId)
                    .delete()
            } catch (_: Exception) {}
        }
    }

    fun autoSyncDebt(debt: DebtEntity) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "id" to debt.id,
                    "workspaceId" to debt.workspaceId,
                    "walletId" to debt.walletId,
                    "personName" to debt.personName,
                    "phoneNumber" to debt.phoneNumber,
                    "amount" to debt.amount,
                    "type" to debt.type,
                    "dueDateMillis" to debt.dueDateMillis,
                    "isSettled" to debt.isSettled,
                    "note" to debt.note
                )
                firestore.collection("users").document(uid)
                    .collection("debts").document(debt.id)
                    .set(data, SetOptions.merge())
            } catch (e: Exception) {
                Log.w("SyncManager", "Debt sync notice: ${e.localizedMessage}")
            }
        }
    }

    fun autoDeleteDebt(debtId: String) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .collection("debts").document(debtId)
                    .delete()
            } catch (_: Exception) {}
        }
    }

    fun autoSyncBudget(budget: BudgetEntity) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "id" to budget.id,
                    "workspaceId" to budget.workspaceId,
                    "categoryName" to budget.categoryName,
                    "amountLimit" to budget.amountLimit,
                    "period" to budget.period,
                    "month" to budget.month,
                    "year" to budget.year
                )
                firestore.collection("users").document(uid)
                    .collection("budgets").document(budget.id)
                    .set(data, SetOptions.merge())
            } catch (_: Exception) {}
        }
    }

    fun autoDeleteBudget(budgetId: String) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .collection("budgets").document(budgetId)
                    .delete()
            } catch (_: Exception) {}
        }
    }

    fun autoSyncGoal(goal: GoalVaultEntity) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "id" to goal.id,
                    "workspaceId" to goal.workspaceId,
                    "name" to goal.name,
                    "targetAmount" to goal.targetAmount,
                    "currentAmount" to goal.currentAmount,
                    "targetDateMillis" to goal.targetDateMillis,
                    "colorHex" to goal.colorHex,
                    "isCompleted" to goal.isCompleted
                )
                firestore.collection("users").document(uid)
                    .collection("goals").document(goal.id)
                    .set(data, SetOptions.merge())
            } catch (_: Exception) {}
        }
    }

    fun autoDeleteGoal(goalId: String) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .collection("goals").document(goalId)
                    .delete()
            } catch (_: Exception) {}
        }
    }

    fun autoSyncBill(bill: BillSubscriptionEntity) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "id" to bill.id,
                    "workspaceId" to bill.workspaceId,
                    "walletId" to bill.walletId,
                    "name" to bill.name,
                    "amount" to bill.amount,
                    "cycle" to bill.cycle,
                    "nextDueDateMillis" to bill.nextDueDateMillis,
                    "isPaid" to bill.isPaid
                )
                firestore.collection("users").document(uid)
                    .collection("bills").document(bill.id)
                    .set(data, SetOptions.merge())
            } catch (_: Exception) {}
        }
    }

    fun autoDeleteBill(billId: String) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .collection("bills").document(billId)
                    .delete()
            } catch (_: Exception) {}
        }
    }

    fun autoSyncCustomerLedger(entry: CustomerLedgerEntity) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "id" to entry.id,
                    "workspaceId" to entry.workspaceId,
                    "customerName" to entry.customerName,
                    "phone" to entry.phone,
                    "amount" to entry.amount,
                    "type" to entry.type,
                    "dateMillis" to entry.dateMillis,
                    "note" to entry.note
                )
                firestore.collection("users").document(uid)
                    .collection("ledger").document(entry.id)
                    .set(data, SetOptions.merge())
            } catch (_: Exception) {}
        }
    }

    fun autoDeleteCustomerLedger(entryId: String) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .collection("ledger").document(entryId)
                    .delete()
            } catch (_: Exception) {}
        }
    }

    fun autoSyncQaza(qaza: QazaPrayerEntity) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "fajrCount" to qaza.fajrCount,
                    "dhuhrCount" to qaza.dhuhrCount,
                    "asrCount" to qaza.asrCount,
                    "maghribCount" to qaza.maghribCount,
                    "ishaCount" to qaza.ishaCount,
                    "witrCount" to qaza.witrCount,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid)
                    .collection("islamic").document("qaza_summary")
                    .set(data, SetOptions.merge())
            } catch (_: Exception) {}
        }
    }

    fun autoSyncUserProfile(data: Map<String, Any>) {
        val uid = auth.currentUser?.uid ?: return
        coroutineScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .collection("profile").document("details")
                    .set(data, SetOptions.merge())
            } catch (_: Exception) {}
        }
    }
}
