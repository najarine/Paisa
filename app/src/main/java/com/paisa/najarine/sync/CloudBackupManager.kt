package com.paisa.najarine.sync

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.paisa.najarine.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudBackupMetadata(
    val backupId: String,
    val timestamp: Long,
    val formattedDate: String,
    val recordsCount: Int,
    val appVersion: String = "1.0",
    val isEncrypted: Boolean = true
)

sealed class BackupResult {
    data class Success(val message: String, val metadata: CloudBackupMetadata) : BackupResult()
    data class Error(val error: String) : BackupResult()
}

/**
 * End-to-end Encrypted Cloud Backup & Restore Manager
 * Securely exports Room database tables and SharedPreferences to user's private Firestore document.
 */
class CloudBackupManager(
    private val context: Context,
    private val database: PaisaDatabase
) {
    private val firestore: FirebaseFirestore by lazy {
        val dbId = runCatching { context.getString(com.paisa.najarine.R.string.firestore_database_id) }.getOrNull()
        if (!dbId.isNullOrBlank() && dbId != "(default)") {
            FirebaseFirestore.getInstance(dbId)
        } else {
            FirebaseFirestore.getInstance()
        }
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Creates an encrypted backup of Room DB and User Preferences, then stores it in Firestore.
     */
    suspend fun createEncryptedBackup(): BackupResult = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext BackupResult.Error("ব্যবহারকারী লগইন করা নেই। প্রথমে গুগল সাইন ইন করুন।")
        val uid = user.uid

        try {
            // 1. Gather all local Room data
            val workspaces = database.workspaceDao().getAllWorkspaces().first()
            val wallets = database.walletDao().getWalletsByWorkspace("personal_default").first()
            val transactions = database.transactionDao().getTransactionsByWorkspace("personal_default").first()
            val budgets = database.budgetDao().getBudgets("personal_default").first()
            val goals = database.goalVaultDao().getGoals("personal_default").first()
            val bills = database.billDao().getBills("personal_default").first()
            val debts = database.debtDao().getDebts("personal_default").first()
            val customerLedger = database.customerLedgerDao().getLedgerEntries("personal_default").first()
            val assets = database.assetDao().getAssets("personal_default").first()
            val zakatRecords = database.zakatDao().getAllZakatRecords().first()
            val qazaPrayer = database.qazaPrayerDao().getQazaCount().first()

            // 2. Gather user settings
            val securityPrefs = context.getSharedPreferences("paisa_security_prefs", Context.MODE_PRIVATE)
            val adhanPrefs = context.getSharedPreferences("paisa_adhan_prefs", Context.MODE_PRIVATE)
            val audioPrefs = context.getSharedPreferences("paisa_quran_audio_prefs", Context.MODE_PRIVATE)

            val totalRecords = workspaces.size + wallets.size + transactions.size + budgets.size +
                    goals.size + bills.size + debts.size + customerLedger.size + assets.size + zakatRecords.size

            val rootJson = JSONObject().apply {
                put("version", 1)
                put("createdAt", System.currentTimeMillis())
                put("userEmail", user.email ?: "")

                // Workspaces
                val wsArray = JSONArray()
                workspaces.forEach { ws ->
                    wsArray.put(JSONObject().apply {
                        put("id", ws.id)
                        put("name", ws.name)
                        put("type", ws.type)
                        put("currencyCode", ws.currencyCode)
                        put("currencySymbol", ws.currencySymbol)
                        put("createdAt", ws.createdAt)
                    })
                }
                put("workspaces", wsArray)

                // Wallets
                val wArray = JSONArray()
                wallets.forEach { w ->
                    wArray.put(JSONObject().apply {
                        put("id", w.id)
                        put("workspaceId", w.workspaceId)
                        put("name", w.name)
                        put("type", w.type)
                        put("institutionId", w.institutionId)
                        put("accountNumber", w.accountNumber)
                        put("balance", w.balance)
                        put("creditLimit", w.creditLimit)
                        put("currencyCode", w.currencyCode)
                        put("colorHex", w.colorHex)
                        put("isExcludedFromTotal", w.isExcludedFromTotal)
                        put("note", w.note)
                        put("updatedAt", w.updatedAt)
                    })
                }
                put("wallets", wArray)

                // Transactions
                val txArray = JSONArray()
                transactions.forEach { tx ->
                    txArray.put(JSONObject().apply {
                        put("id", tx.id)
                        put("workspaceId", tx.workspaceId)
                        put("walletId", tx.walletId)
                        put("toWalletId", tx.toWalletId ?: JSONObject.NULL)
                        put("type", tx.type)
                        put("amount", tx.amount)
                        put("fee", tx.fee)
                        put("category", tx.category)
                        put("note", tx.note)
                        put("dateMillis", tx.dateMillis)
                        put("tags", tx.tags)
                        put("receiptImageUri", tx.receiptImageUri ?: JSONObject.NULL)
                        put("isDraft", tx.isDraft)
                        put("isAiGenerated", tx.isAiGenerated)
                        put("confirmedByUser", tx.confirmedByUser)
                    })
                }
                put("transactions", txArray)

                // Budgets
                val bArray = JSONArray()
                budgets.forEach { b ->
                    bArray.put(JSONObject().apply {
                        put("id", b.id)
                        put("workspaceId", b.workspaceId)
                        put("categoryName", b.categoryName)
                        put("amountLimit", b.amountLimit)
                        put("period", b.period)
                        put("month", b.month)
                        put("year", b.year)
                    })
                }
                put("budgets", bArray)

                // Goals
                val gArray = JSONArray()
                goals.forEach { g ->
                    gArray.put(JSONObject().apply {
                        put("id", g.id)
                        put("workspaceId", g.workspaceId)
                        put("name", g.name)
                        put("targetAmount", g.targetAmount)
                        put("currentAmount", g.currentAmount)
                        put("targetDateMillis", g.targetDateMillis)
                        put("colorHex", g.colorHex)
                        put("isCompleted", g.isCompleted)
                    })
                }
                put("goals", gArray)

                // Bills
                val billArray = JSONArray()
                bills.forEach { b ->
                    billArray.put(JSONObject().apply {
                        put("id", b.id)
                        put("workspaceId", b.workspaceId)
                        put("walletId", b.walletId)
                        put("name", b.name)
                        put("amount", b.amount)
                        put("cycle", b.cycle)
                        put("nextDueDateMillis", b.nextDueDateMillis)
                        put("isPaid", b.isPaid)
                    })
                }
                put("bills", billArray)

                // Debts
                val dArray = JSONArray()
                debts.forEach { d ->
                    dArray.put(JSONObject().apply {
                        put("id", d.id)
                        put("workspaceId", d.workspaceId)
                        put("walletId", d.walletId)
                        put("personName", d.personName)
                        put("phoneNumber", d.phoneNumber)
                        put("amount", d.amount)
                        put("type", d.type)
                        put("dueDateMillis", d.dueDateMillis)
                        put("isSettled", d.isSettled)
                        put("note", d.note)
                    })
                }
                put("debts", dArray)

                // Customer Ledger
                val clArray = JSONArray()
                customerLedger.forEach { cl ->
                    clArray.put(JSONObject().apply {
                        put("id", cl.id)
                        put("workspaceId", cl.workspaceId)
                        put("customerName", cl.customerName)
                        put("phone", cl.phone)
                        put("amount", cl.amount)
                        put("type", cl.type)
                        put("dateMillis", cl.dateMillis)
                        put("note", cl.note)
                    })
                }
                put("customerLedger", clArray)

                // Assets
                val aArray = JSONArray()
                assets.forEach { a ->
                    aArray.put(JSONObject().apply {
                        put("id", a.id)
                        put("workspaceId", a.workspaceId)
                        put("name", a.name)
                        put("category", a.category)
                        put("quantity", a.quantity)
                        put("unit", a.unit)
                        put("buyPrice", a.buyPrice)
                        put("currentPrice", a.currentPrice)
                        put("dateMillis", a.dateMillis)
                    })
                }
                put("assets", aArray)

                // Zakat
                val zArray = JSONArray()
                zakatRecords.forEach { z ->
                    zArray.put(JSONObject().apply {
                        put("id", z.id)
                        put("calculatedDateMillis", z.calculatedDateMillis)
                        put("cashAmount", z.cashAmount)
                        put("bankAmount", z.bankAmount)
                        put("goldValue", z.goldValue)
                        put("silverValue", z.silverValue)
                        put("businessStockValue", z.businessStockValue)
                        put("receivables", z.receivables)
                        put("liabilitiesDue", z.liabilitiesDue)
                        put("totalZakatable", z.totalZakatable)
                        put("zakatPayable", z.zakatPayable)
                        put("isPaid", z.isPaid)
                    })
                }
                put("zakatRecords", zArray)

                // Qaza
                qazaPrayer?.let { q ->
                    put("qazaPrayer", JSONObject().apply {
                        put("fajrCount", q.fajrCount)
                        put("dhuhrCount", q.dhuhrCount)
                        put("asrCount", q.asrCount)
                        put("maghribCount", q.maghribCount)
                        put("ishaCount", q.ishaCount)
                        put("witrCount", q.witrCount)
                    })
                }

                // Settings
                put("settings", JSONObject().apply {
                    put("securityLockEnabled", securityPrefs.getBoolean("security_lock_enabled", false))
                    put("securityPin", securityPrefs.getString("security_pin", "1234"))
                    put("selectedMadhab", adhanPrefs.getString("selected_madhab", "Hanafi"))
                    put("adhanAudioEnabled", adhanPrefs.getBoolean("adhan_audio_enabled", true))
                    put("selectedReciter", audioPrefs.getString("selected_reciter_id", "ar.alafasy"))
                })
            }

            // 3. Encrypt payload with AES-256-GCM
            val plaintextString = rootJson.toString()
            val encryptedPayload = BackupEncryptionHelper.encrypt(plaintextString, uid)

            val now = System.currentTimeMillis()
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val formattedDate = sdf.format(Date(now))

            val metadata = CloudBackupMetadata(
                backupId = "backup_latest",
                timestamp = now,
                formattedDate = formattedDate,
                recordsCount = totalRecords,
                appVersion = "1.0",
                isEncrypted = true
            )

            // 4. Save to Firestore under user document
            val backupDoc = firestore.collection("users").document(uid)
                .collection("backups").document("backup_latest")

            val backupMap = hashMapOf(
                "backupId" to metadata.backupId,
                "timestamp" to metadata.timestamp,
                "formattedDate" to metadata.formattedDate,
                "recordsCount" to metadata.recordsCount,
                "appVersion" to metadata.appVersion,
                "isEncrypted" to true,
                "encryptedData" to encryptedPayload,
                "userEmail" to (user.email ?: "")
            )

            backupDoc.set(backupMap, SetOptions.merge()).await()

            // Update user document summary
            firestore.collection("users").document(uid).set(
                hashMapOf(
                    "lastBackupTimestamp" to now,
                    "lastBackupDate" to formattedDate,
                    "lastBackupRecords" to totalRecords
                ),
                SetOptions.merge()
            ).await()

            BackupResult.Success(
                message = "রুম ডাটাবেস ও সেটিংস সফলভাবে ক্লাউডে ব্যাকআপ সংরক্ষিত হয়েছে ($totalRecords টি রেকর্ড)।",
                metadata = metadata
            )
        } catch (e: Exception) {
            Log.e("CloudBackupManager", "Error creating backup", e)
            BackupResult.Error("ব্যাকআপ তৈরি ব্যর্থ হয়েছে: ${e.localizedMessage}")
        }
    }

    /**
     * Retrieves the latest backup metadata from Firestore.
     */
    suspend fun getLatestBackupMetadata(): CloudBackupMetadata? = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext null
        val uid = user.uid

        try {
            val doc = firestore.collection("users").document(uid)
                .collection("backups").document("backup_latest").get().await()

            if (!doc.exists()) return@withContext null

            val ts = doc.getLong("timestamp") ?: 0L
            val formattedDate = doc.getString("formattedDate") ?: SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ts))
            val recordsCount = doc.getLong("recordsCount")?.toInt() ?: 0

            CloudBackupMetadata(
                backupId = doc.id,
                timestamp = ts,
                formattedDate = formattedDate,
                recordsCount = recordsCount,
                appVersion = doc.getString("appVersion") ?: "1.0",
                isEncrypted = doc.getBoolean("isEncrypted") ?: true
            )
        } catch (e: Exception) {
            Log.e("CloudBackupManager", "Error fetching backup metadata", e)
            null
        }
    }

    /**
     * Downloads and decrypts the backup from Firestore, restoring it into Room DB and SharedPreferences.
     */
    suspend fun restoreEncryptedBackup(): BackupResult = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext BackupResult.Error("ব্যবহারকারী লগইন করা নেই। প্রথমে গুগল সাইন ইন করুন।")
        val uid = user.uid

        try {
            val doc = firestore.collection("users").document(uid)
                .collection("backups").document("backup_latest").get().await()

            if (!doc.exists()) {
                return@withContext BackupResult.Error("কোনো ক্লাউড ব্যাকআপ পাওয়া যায়নি।")
            }

            val encryptedPayload = doc.getString("encryptedData")
                ?: return@withContext BackupResult.Error("ব্যাকআপ ডাটা ক্ষতিগ্রস্ত বা খালি।")

            // Decrypt with AES-256-GCM
            val decryptedJsonString = BackupEncryptionHelper.decrypt(encryptedPayload, uid)
            val rootJson = JSONObject(decryptedJsonString)

            var restoredRecords = 0

            // 1. Restore Workspaces
            val wsArray = rootJson.optJSONArray("workspaces")
            if (wsArray != null) {
                for (i in 0 until wsArray.length()) {
                    val obj = wsArray.getJSONObject(i)
                    database.workspaceDao().insertWorkspace(
                        WorkspaceEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            type = obj.optString("type", "PERSONAL"),
                            currencyCode = obj.optString("currencyCode", "BDT"),
                            currencySymbol = obj.optString("currencySymbol", "৳"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                    restoredRecords++
                }
            }

            // 2. Restore Wallets
            val wArray = rootJson.optJSONArray("wallets")
            if (wArray != null) {
                for (i in 0 until wArray.length()) {
                    val obj = wArray.getJSONObject(i)
                    database.walletDao().insertWallet(
                        WalletEntity(
                            id = obj.getString("id"),
                            workspaceId = obj.optString("workspaceId", "personal_default"),
                            name = obj.getString("name"),
                            type = obj.getString("type"),
                            institutionId = obj.optString("institutionId", "cash"),
                            accountNumber = obj.optString("accountNumber", ""),
                            balance = obj.optDouble("balance", 0.0),
                            creditLimit = obj.optDouble("creditLimit", 0.0),
                            currencyCode = obj.optString("currencyCode", "BDT"),
                            colorHex = obj.optString("colorHex", "#0D9488"),
                            isExcludedFromTotal = obj.optBoolean("isExcludedFromTotal", false),
                            note = obj.optString("note", ""),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                    restoredRecords++
                }
            }

            // 3. Restore Transactions
            val txArray = rootJson.optJSONArray("transactions")
            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    database.transactionDao().insertTransaction(
                        TransactionEntity(
                            id = obj.getString("id"),
                            workspaceId = obj.optString("workspaceId", "personal_default"),
                            walletId = obj.getString("walletId"),
                            toWalletId = if (obj.isNull("toWalletId")) null else obj.optString("toWalletId"),
                            type = obj.getString("type"),
                            amount = obj.getDouble("amount"),
                            fee = obj.optDouble("fee", 0.0),
                            category = obj.optString("category", "General"),
                            note = obj.optString("note", ""),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                            tags = obj.optString("tags", ""),
                            receiptImageUri = if (obj.isNull("receiptImageUri")) null else obj.optString("receiptImageUri"),
                            isDraft = obj.optBoolean("isDraft", false),
                            isAiGenerated = obj.optBoolean("isAiGenerated", false),
                            confirmedByUser = obj.optBoolean("confirmedByUser", true)
                        )
                    )
                    restoredRecords++
                }
            }

            // 4. Restore Budgets
            val bArray = rootJson.optJSONArray("budgets")
            if (bArray != null) {
                for (i in 0 until bArray.length()) {
                    val obj = bArray.getJSONObject(i)
                    database.budgetDao().insertBudget(
                        BudgetEntity(
                            id = obj.getString("id"),
                            workspaceId = obj.optString("workspaceId", "personal_default"),
                            categoryName = obj.getString("categoryName"),
                            amountLimit = obj.getDouble("amountLimit"),
                            period = obj.optString("period", "MONTHLY"),
                            month = obj.optInt("month", 1),
                            year = obj.optInt("year", 2026)
                        )
                    )
                    restoredRecords++
                }
            }

            // 5. Restore Goals
            val gArray = rootJson.optJSONArray("goals")
            if (gArray != null) {
                for (i in 0 until gArray.length()) {
                    val obj = gArray.getJSONObject(i)
                    database.goalVaultDao().insertGoal(
                        GoalVaultEntity(
                            id = obj.getString("id"),
                            workspaceId = obj.optString("workspaceId", "personal_default"),
                            name = obj.getString("name"),
                            targetAmount = obj.getDouble("targetAmount"),
                            currentAmount = obj.optDouble("currentAmount", 0.0),
                            targetDateMillis = obj.optLong("targetDateMillis", System.currentTimeMillis()),
                            colorHex = obj.optString("colorHex", "#10B981"),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                    restoredRecords++
                }
            }

            // 6. Restore Bills
            val billArray = rootJson.optJSONArray("bills")
            if (billArray != null) {
                for (i in 0 until billArray.length()) {
                    val obj = billArray.getJSONObject(i)
                    database.billDao().insertBill(
                        BillSubscriptionEntity(
                            id = obj.getString("id"),
                            workspaceId = obj.optString("workspaceId", "personal_default"),
                            walletId = obj.getString("walletId"),
                            name = obj.getString("name"),
                            amount = obj.getDouble("amount"),
                            cycle = obj.optString("cycle", "MONTHLY"),
                            nextDueDateMillis = obj.optLong("nextDueDateMillis", System.currentTimeMillis()),
                            isPaid = obj.optBoolean("isPaid", false)
                        )
                    )
                    restoredRecords++
                }
            }

            // 7. Restore Debts
            val dArray = rootJson.optJSONArray("debts")
            if (dArray != null) {
                for (i in 0 until dArray.length()) {
                    val obj = dArray.getJSONObject(i)
                    database.debtDao().insertDebt(
                        DebtEntity(
                            id = obj.getString("id"),
                            workspaceId = obj.optString("workspaceId", "personal_default"),
                            walletId = obj.getString("walletId"),
                            personName = obj.getString("personName"),
                            phoneNumber = obj.optString("phoneNumber", ""),
                            amount = obj.getDouble("amount"),
                            type = obj.getString("type"),
                            dueDateMillis = obj.optLong("dueDateMillis", System.currentTimeMillis()),
                            isSettled = obj.optBoolean("isSettled", false),
                            note = obj.optString("note", "")
                        )
                    )
                    restoredRecords++
                }
            }

            // 8. Restore Customer Ledger
            val clArray = rootJson.optJSONArray("customerLedger")
            if (clArray != null) {
                for (i in 0 until clArray.length()) {
                    val obj = clArray.getJSONObject(i)
                    database.customerLedgerDao().insertEntry(
                        CustomerLedgerEntity(
                            id = obj.getString("id"),
                            workspaceId = obj.optString("workspaceId", "personal_default"),
                            customerName = obj.getString("customerName"),
                            phone = obj.optString("phone", ""),
                            amount = obj.getDouble("amount"),
                            type = obj.getString("type"),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                            note = obj.optString("note", "")
                        )
                    )
                    restoredRecords++
                }
            }

            // 9. Restore Assets
            val aArray = rootJson.optJSONArray("assets")
            if (aArray != null) {
                for (i in 0 until aArray.length()) {
                    val obj = aArray.getJSONObject(i)
                    database.assetDao().insertAsset(
                        AssetEntity(
                            id = obj.getString("id"),
                            workspaceId = obj.optString("workspaceId", "personal_default"),
                            name = obj.getString("name"),
                            category = obj.getString("category"),
                            quantity = obj.getDouble("quantity"),
                            unit = obj.getString("unit"),
                            buyPrice = obj.getDouble("buyPrice"),
                            currentPrice = obj.getDouble("currentPrice"),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis())
                        )
                    )
                    restoredRecords++
                }
            }

            // 10. Restore Zakat
            val zArray = rootJson.optJSONArray("zakatRecords")
            if (zArray != null) {
                for (i in 0 until zArray.length()) {
                    val obj = zArray.getJSONObject(i)
                    database.zakatDao().insertZakatRecord(
                        ZakatRecordEntity(
                            id = obj.getString("id"),
                            calculatedDateMillis = obj.optLong("calculatedDateMillis", System.currentTimeMillis()),
                            cashAmount = obj.getDouble("cashAmount"),
                            bankAmount = obj.getDouble("bankAmount"),
                            goldValue = obj.getDouble("goldValue"),
                            silverValue = obj.getDouble("silverValue"),
                            businessStockValue = obj.getDouble("businessStockValue"),
                            receivables = obj.getDouble("receivables"),
                            liabilitiesDue = obj.getDouble("liabilitiesDue"),
                            totalZakatable = obj.getDouble("totalZakatable"),
                            zakatPayable = obj.getDouble("zakatPayable"),
                            isPaid = obj.optBoolean("isPaid", false)
                        )
                    )
                    restoredRecords++
                }
            }

            // 11. Restore Qaza
            val qazaObj = rootJson.optJSONObject("qazaPrayer")
            if (qazaObj != null) {
                database.qazaPrayerDao().updateQaza(
                    QazaPrayerEntity(
                        id = "singleton_qaza",
                        fajrCount = qazaObj.optInt("fajrCount", 0),
                        dhuhrCount = qazaObj.optInt("dhuhrCount", 0),
                        asrCount = qazaObj.optInt("asrCount", 0),
                        maghribCount = qazaObj.optInt("maghribCount", 0),
                        ishaCount = qazaObj.optInt("ishaCount", 0),
                        witrCount = qazaObj.optInt("witrCount", 0)
                    )
                )
                restoredRecords++
            }

            // 12. Restore Settings
            val settingsObj = rootJson.optJSONObject("settings")
            if (settingsObj != null) {
                context.getSharedPreferences("paisa_security_prefs", Context.MODE_PRIVATE).edit().apply {
                    putBoolean("security_lock_enabled", settingsObj.optBoolean("securityLockEnabled", false))
                    putString("security_pin", settingsObj.optString("securityPin", "1234"))
                    apply()
                }
                context.getSharedPreferences("paisa_adhan_prefs", Context.MODE_PRIVATE).edit().apply {
                    putString("selected_madhab", settingsObj.optString("selectedMadhab", "Hanafi"))
                    putBoolean("adhan_audio_enabled", settingsObj.optBoolean("adhanAudioEnabled", true))
                    apply()
                }
                context.getSharedPreferences("paisa_quran_audio_prefs", Context.MODE_PRIVATE).edit().apply {
                    putString("selected_reciter_id", settingsObj.optString("selectedReciter", "ar.alafasy"))
                    apply()
                }
            }

            val metadata = CloudBackupMetadata(
                backupId = doc.id,
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                formattedDate = doc.getString("formattedDate") ?: "আজকে",
                recordsCount = restoredRecords,
                appVersion = doc.getString("appVersion") ?: "1.0",
                isEncrypted = true
            )

            BackupResult.Success(
                message = "ক্লাউড থেকে এনক্রিপ্টেড ব্যাকআপ সফলভাবে পুনরুদ্ধার করা হয়েছে ($restoredRecords টি রেকর্ড)।",
                metadata = metadata
            )
        } catch (e: Exception) {
            Log.e("CloudBackupManager", "Error restoring backup", e)
            BackupResult.Error("ব্যাকআপ পুনরুদ্ধার ব্যর্থ হয়েছে: ${e.localizedMessage}")
        }
    }
}
