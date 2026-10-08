package com.paisa.najarine.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        WorkspaceEntity::class,
        WalletEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        GoalVaultEntity::class,
        BillSubscriptionEntity::class,
        DebtEntity::class,
        CustomerLedgerEntity::class,
        MessEntryEntity::class,
        BazarItemEntity::class,
        AssetEntity::class,
        PrayerLogEntity::class,
        QazaPrayerEntity::class,
        ZakatRecordEntity::class,
        HourlyHadithEntity::class,
        HourlyQuranEntity::class,
        SyncOutboxEntity::class,
        TombstoneEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PaisaDatabase : RoomDatabase() {

    abstract fun workspaceDao(): WorkspaceDao
    abstract fun walletDao(): WalletDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalVaultDao(): GoalVaultDao
    abstract fun billDao(): BillDao
    abstract fun debtDao(): DebtDao
    abstract fun customerLedgerDao(): CustomerLedgerDao
    abstract fun messDao(): MessDao
    abstract fun bazarDao(): BazarDao
    abstract fun assetDao(): AssetDao
    abstract fun prayerLogDao(): PrayerLogDao
    abstract fun qazaPrayerDao(): QazaPrayerDao
    abstract fun zakatDao(): ZakatDao
    abstract fun hourlyHadithDao(): HourlyHadithDao
    abstract fun hourlyQuranDao(): HourlyQuranDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun tombstoneDao(): TombstoneDao

    companion object {
        @Volatile
        private var INSTANCE: PaisaDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure all standard tables exist safely with IF NOT EXISTS
                db.execSQL("CREATE TABLE IF NOT EXISTS workspaces (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, currencyCode TEXT NOT NULL, currencySymbol TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS wallets (id TEXT PRIMARY KEY NOT NULL, workspaceId TEXT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, institutionId TEXT NOT NULL, accountNumber TEXT NOT NULL, balance REAL NOT NULL, creditLimit REAL NOT NULL, currencyCode TEXT NOT NULL, colorHex TEXT NOT NULL, isExcludedFromTotal INTEGER NOT NULL, note TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS transactions (id TEXT PRIMARY KEY NOT NULL, workspaceId TEXT NOT NULL, walletId TEXT NOT NULL, toWalletId TEXT, type TEXT NOT NULL, amount REAL NOT NULL, fee REAL NOT NULL, category TEXT NOT NULL, note TEXT NOT NULL, dateMillis INTEGER NOT NULL, tags TEXT NOT NULL, receiptImageUri TEXT, isDraft INTEGER NOT NULL, isAiGenerated INTEGER NOT NULL, confirmedByUser INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, iconName TEXT NOT NULL, colorHex TEXT NOT NULL, isDefault INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS budgets (id TEXT PRIMARY KEY NOT NULL, workspaceId TEXT NOT NULL, categoryName TEXT NOT NULL, amountLimit REAL NOT NULL, period TEXT NOT NULL, month INTEGER NOT NULL, year INTEGER NOT NULL)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add any missing auxiliary tables or columns for v2 to v3 safely
                db.execSQL("CREATE TABLE IF NOT EXISTS goals_vaults (id TEXT PRIMARY KEY NOT NULL, workspaceId TEXT NOT NULL, name TEXT NOT NULL, targetAmount REAL NOT NULL, currentAmount REAL NOT NULL, targetDateMillis INTEGER NOT NULL, colorHex TEXT NOT NULL, isCompleted INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS bills_subscriptions (id TEXT PRIMARY KEY NOT NULL, workspaceId TEXT NOT NULL, walletId TEXT NOT NULL, name TEXT NOT NULL, amount REAL NOT NULL, cycle TEXT NOT NULL, nextDueDateMillis INTEGER NOT NULL, isPaid INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS debts_dena_pona (id TEXT PRIMARY KEY NOT NULL, workspaceId TEXT NOT NULL, walletId TEXT NOT NULL, personName TEXT NOT NULL, phoneNumber TEXT NOT NULL, amount REAL NOT NULL, type TEXT NOT NULL, dueDateMillis INTEGER NOT NULL, isSettled INTEGER NOT NULL, note TEXT NOT NULL)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add updatedAt to transactions if not existing
                try {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                // Backfill existing rows with dateMillis
                try {
                    db.execSQL("UPDATE transactions SET updatedAt = dateMillis WHERE updatedAt = 0")
                } catch (_: Exception) {}
                // Create sync outbox table for durable offline mutations
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_outbox (
                        id TEXT PRIMARY KEY NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        action TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                // Create tombstones table to protect deleted records from cloud resurrection
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_tombstones (
                        entityId TEXT PRIMARY KEY NOT NULL,
                        entityType TEXT NOT NULL,
                        deletedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): PaisaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PaisaDatabase::class.java,
                    "paisa_financial_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialData(database)
                    }
                }
            }
        }

        private suspend fun seedInitialData(database: PaisaDatabase) {
            val workspaceId = "personal_default"
            database.workspaceDao().insertWorkspace(
                WorkspaceEntity(
                    id = workspaceId,
                    name = "Personal Finances",
                    type = "PERSONAL",
                    currencyCode = "BDT",
                    currencySymbol = "৳"
                )
            )

            // Seed default Categories (Categories define spending classification without financial balances)
            val defaultCategories = listOf(
                CategoryEntity("cat_salary", "Salary & Income", "INCOME", "payments", "#10B981", true),
                CategoryEntity("cat_freelance", "Freelance / Gig", "INCOME", "work", "#059669", true),
                CategoryEntity("cat_business", "Business Profit", "INCOME", "storefront", "#0D9488", true),
                CategoryEntity("cat_investment", "Dividends & Returns", "INCOME", "trending_up", "#2563EB", true),
                CategoryEntity("cat_food", "Food & Dining", "EXPENSE", "restaurant", "#EF4444", true),
                CategoryEntity("cat_groceries", "Groceries / Bazar", "EXPENSE", "shopping_cart", "#F97316", true),
                CategoryEntity("cat_transport", "Transport & Fuel", "EXPENSE", "directions_car", "#6366F1", true),
                CategoryEntity("cat_bills", "Utility & Bills", "EXPENSE", "receipt_long", "#8B5CF6", true),
                CategoryEntity("cat_health", "Health & Medical", "EXPENSE", "medical_services", "#EC4899", true),
                CategoryEntity("cat_shopping", "Shopping & Clothes", "EXPENSE", "checkroom", "#F59E0B", true),
                CategoryEntity("cat_charity", "Sadaqah & Charity", "EXPENSE", "volunteer_activism", "#065F46", true),
                CategoryEntity("cat_education", "Education & Books", "EXPENSE", "menu_book", "#0284C7", true),
                CategoryEntity("cat_entertainment", "Entertainment", "EXPENSE", "movie", "#D946EF", true)
            )
            database.categoryDao().insertCategories(defaultCategories)

            // Strict zero-state initial record: NO sample wallets, transactions, or fake balances
            // Qaza initial zero record
            database.qazaPrayerDao().updateQaza(
                QazaPrayerEntity(
                    id = "singleton_qaza",
                    fajrCount = 0,
                    dhuhrCount = 0,
                    asrCount = 0,
                    maghribCount = 0,
                    ishaCount = 0,
                    witrCount = 0
                )
            )
        }
    }
}
