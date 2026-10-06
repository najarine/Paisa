package com.paisa.najarine.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        AssetEntity::class,
        PrayerLogEntity::class,
        QazaPrayerEntity::class,
        ZakatRecordEntity::class,
        HourlyHadithEntity::class,
        HourlyQuranEntity::class
    ],
    version = 2,
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
    abstract fun assetDao(): AssetDao
    abstract fun prayerLogDao(): PrayerLogDao
    abstract fun qazaPrayerDao(): QazaPrayerDao
    abstract fun zakatDao(): ZakatDao
    abstract fun hourlyHadithDao(): HourlyHadithDao
    abstract fun hourlyQuranDao(): HourlyQuranDao

    companion object {
        @Volatile
        private var INSTANCE: PaisaDatabase? = null

        fun getDatabase(context: Context): PaisaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PaisaDatabase::class.java,
                    "paisa_financial_db"
                )
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
