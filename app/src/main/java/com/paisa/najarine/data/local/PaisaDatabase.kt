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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add updatedAt to transactions if not existing
                db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                // Backfill existing rows with dateMillis
                db.execSQL("UPDATE transactions SET updatedAt = dateMillis WHERE updatedAt = 0")
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
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
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
