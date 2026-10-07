package com.paisa.najarine.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces ORDER BY createdAt ASC")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE id = :id LIMIT 1")
    suspend fun getWorkspaceById(id: String): WorkspaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    @Update
    suspend fun updateWorkspace(workspace: WorkspaceEntity)

    @Query("DELETE FROM workspaces WHERE id = :id AND id != 'personal_default'")
    suspend fun deleteWorkspace(id: String)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE workspaceId = :workspaceId ORDER BY updatedAt DESC")
    fun getWalletsByWorkspace(workspaceId: String): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :id LIMIT 1")
    suspend fun getWalletById(id: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity)

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Query("UPDATE wallets SET balance = balance + :amount, updatedAt = :timestamp WHERE id = :walletId")
    suspend fun adjustBalance(walletId: String, amount: Double, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun deleteWallet(id: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE workspaceId = :workspaceId ORDER BY dateMillis DESC")
    fun getTransactionsByWorkspace(workspaceId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE walletId = :walletId OR toWalletId = :walletId ORDER BY dateMillis DESC")
    fun getTransactionsByWallet(walletId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE workspaceId = :workspaceId")
    fun getBudgets(workspaceId: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: String)
}

@Dao
interface GoalVaultDao {
    @Query("SELECT * FROM goals_vaults WHERE workspaceId = :workspaceId ORDER BY targetDateMillis ASC")
    fun getGoals(workspaceId: String): Flow<List<GoalVaultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalVaultEntity)

    @Query("UPDATE goals_vaults SET currentAmount = currentAmount + :amount WHERE id = :id")
    suspend fun contributeToGoal(id: String, amount: Double)

    @Query("DELETE FROM goals_vaults WHERE id = :id")
    suspend fun deleteGoal(id: String)
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills_subscriptions WHERE workspaceId = :workspaceId ORDER BY nextDueDateMillis ASC")
    fun getBills(workspaceId: String): Flow<List<BillSubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillSubscriptionEntity)

    @Query("UPDATE bills_subscriptions SET isPaid = :isPaid WHERE id = :id")
    suspend fun togglePaid(id: String, isPaid: Boolean)

    @Query("DELETE FROM bills_subscriptions WHERE id = :id")
    suspend fun deleteBill(id: String)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts_dena_pona WHERE workspaceId = :workspaceId ORDER BY dueDateMillis ASC")
    fun getDebts(workspaceId: String): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Query("UPDATE debts_dena_pona SET isSettled = :isSettled WHERE id = :id")
    suspend fun toggleSettled(id: String, isSettled: Boolean)

    @Query("DELETE FROM debts_dena_pona WHERE id = :id")
    suspend fun deleteDebt(id: String)
}

@Dao
interface CustomerLedgerDao {
    @Query("SELECT * FROM customer_ledger WHERE workspaceId = :workspaceId ORDER BY dateMillis DESC")
    fun getLedgerEntries(workspaceId: String): Flow<List<CustomerLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CustomerLedgerEntity)

    @Query("DELETE FROM customer_ledger WHERE id = :id")
    suspend fun deleteEntry(id: String)
}

@Dao
interface MessDao {
    @Query("SELECT * FROM mess_entries WHERE workspaceId = :workspaceId ORDER BY dateMillis DESC")
    fun getMessEntries(workspaceId: String): Flow<List<MessEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: MessEntryEntity)

    @Query("DELETE FROM mess_entries WHERE id = :id")
    suspend fun deleteEntry(id: String)
}

@Dao
interface BazarDao {
    @Query("SELECT * FROM bazar_shodai_items WHERE workspaceId = :workspaceId ORDER BY isChecked ASC, createdAtMillis DESC")
    fun getBazarItems(workspaceId: String): Flow<List<BazarItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBazarItem(item: BazarItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBazarItems(items: List<BazarItemEntity>)

    @Update
    suspend fun updateBazarItem(item: BazarItemEntity)

    @Query("UPDATE bazar_shodai_items SET isChecked = :isChecked WHERE id = :id")
    suspend fun toggleChecked(id: String, isChecked: Boolean)

    @Query("DELETE FROM bazar_shodai_items WHERE id = :id")
    suspend fun deleteBazarItem(id: String)

    @Query("DELETE FROM bazar_shodai_items WHERE workspaceId = :workspaceId AND isChecked = 1")
    suspend fun clearCheckedItems(workspaceId: String)
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets WHERE workspaceId = :workspaceId ORDER BY dateMillis DESC")
    fun getAssets(workspaceId: String): Flow<List<AssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteAsset(id: String)
}

@Dao
interface PrayerLogDao {
    @Query("SELECT * FROM prayer_logs WHERE dateKey = :dateKey LIMIT 1")
    fun getLogForDate(dateKey: String): Flow<PrayerLogEntity?>

    @Query("SELECT * FROM prayer_logs ORDER BY dateKey DESC LIMIT 30")
    fun getRecentLogs(): Flow<List<PrayerLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(log: PrayerLogEntity)
}

@Dao
interface QazaPrayerDao {
    @Query("SELECT * FROM qaza_prayers WHERE id = 'singleton_qaza' LIMIT 1")
    fun getQazaCount(): Flow<QazaPrayerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateQaza(qaza: QazaPrayerEntity)
}

@Dao
interface ZakatDao {
    @Query("SELECT * FROM zakat_records ORDER BY calculatedDateMillis DESC")
    fun getAllZakatRecords(): Flow<List<ZakatRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZakatRecord(record: ZakatRecordEntity)
}

@Dao
interface HourlyHadithDao {
    @Query("SELECT * FROM hourly_hadiths WHERE id = 'latest_hourly_hadith' LIMIT 1")
    fun getLatestHadith(): Flow<HourlyHadithEntity?>

    @Query("SELECT * FROM hourly_hadiths WHERE id = 'latest_hourly_hadith' LIMIT 1")
    suspend fun getLatestHadithDirect(): HourlyHadithEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadith(hadith: HourlyHadithEntity)
}

@Dao
interface HourlyQuranDao {
    @Query("SELECT * FROM hourly_quran_ayahs WHERE id = 'latest_hourly_ayah' LIMIT 1")
    fun getLatestAyah(): Flow<HourlyQuranEntity?>

    @Query("SELECT * FROM hourly_quran_ayahs WHERE id = 'latest_hourly_ayah' LIMIT 1")
    suspend fun getLatestAyahDirect(): HourlyQuranEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyah(ayah: HourlyQuranEntity)
}
