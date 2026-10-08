package com.paisa.najarine.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String = "PERSONAL", // PERSONAL, BUSINESS, FAMILY, MESS
    val currencyCode: String = "BDT",
    val currencySymbol: String = "৳",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val type: String, // CASH, BANK, MFS, CREDIT_CARD, INVESTMENT, CRYPTO
    val institutionId: String, // "bkash", "nagad", "ibbl", "brac", "cash", etc.
    val accountNumber: String = "",
    val balance: Double = 0.0,
    val creditLimit: Double = 0.0, // For credit card liability semantics
    val currencyCode: String = "BDT",
    val colorHex: String = "#0D9488",
    val isExcludedFromTotal: Boolean = false,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val walletId: String,               // Primary source wallet
    val toWalletId: String? = null,     // Required for TRANSFER; null for income/expense
    val type: String,                   // INCOME, EXPENSE, TRANSFER
    val amount: Double,
    val fee: Double = 0.0,
    val category: String,               // Food, Shopping, Salary, Bills, etc.
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val tags: String = "",
    val receiptImageUri: String? = null,
    val isDraft: Boolean = false,
    val isAiGenerated: Boolean = false,
    val confirmedByUser: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    val entityType: String,      // "WALLET", "TRANSACTION", "BUDGET", "GOAL", "DEBT", "ASSET", "BILL", "LEDGER", "QAZA"
    val entityId: String,
    val action: String,          // "UPSERT", "DELETE"
    val payload: String = "",    // JSON serialized representation for offline replays
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

@Entity(tableName = "sync_tombstones")
data class TombstoneEntity(
    @PrimaryKey val entityId: String,
    val entityType: String,
    val deletedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // INCOME, EXPENSE
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val categoryName: String,
    val amountLimit: Double,
    val period: String = "MONTHLY", // MONTHLY, WEEKLY
    val month: Int,
    val year: Int
)

@Entity(tableName = "goals_vaults")
data class GoalVaultEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDateMillis: Long,
    val colorHex: String = "#10B981",
    val isCompleted: Boolean = false
)

@Entity(tableName = "bills_subscriptions")
data class BillSubscriptionEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val walletId: String,
    val name: String,
    val amount: Double,
    val cycle: String = "MONTHLY",
    val nextDueDateMillis: Long,
    val isPaid: Boolean = false
)

@Entity(tableName = "debts_dena_pona")
data class DebtEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val walletId: String,
    val personName: String,
    val phoneNumber: String = "",
    val amount: Double,
    val type: String, // DENA (I Owe / Payable), PONA (They Owe Me / Receivable)
    val dueDateMillis: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val isSettled: Boolean = false,
    val note: String = ""
)

@Entity(tableName = "customer_ledger")
data class CustomerLedgerEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val customerName: String,
    val phone: String = "",
    val amount: Double,
    val type: String, // BAKI (Due), JOMA (Received)
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "mess_entries")
data class MessEntryEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val memberName: String,
    val mealsCount: Double = 0.0,
    val depositAmount: Double = 0.0,
    val bazarExpense: Double = 0.0,
    val fixedExpenseShare: Double = 0.0,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "bazar_shodai_items")
data class BazarItemEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val category: String, // চাল ও ডাল, মাছ ও মাংস, শাকসবজি, তেল ও মসলা, ডিম ও দুধ, ফলমূল, বেকারি ও স্ন্যাকস, টয়লেট্রিজ, অন্যান্য
    val quantity: Double = 1.0,
    val unit: String = "কেজি", // কেজি, গ্রাম, লিটার, পিস, ডজন, প্যাকেট, আঁটি
    val estimatedPrice: Double = 0.0,
    val actualPrice: Double = 0.0,
    val isChecked: Boolean = false,
    val isMessItem: Boolean = false,
    val assignedMemberName: String = "",
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val category: String, // GOLD, SILVER, CRYPTO, REAL_ESTATE, STOCK, VEHICLE
    val quantity: Double,
    val unit: String,     // grams, bhori, coins, sqft
    val buyPrice: Double,
    val currentPrice: Double,
    val dateMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "prayer_logs")
data class PrayerLogEntity(
    @PrimaryKey val dateKey: String, // YYYY-MM-DD
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false
)

@Entity(tableName = "qaza_prayers")
data class QazaPrayerEntity(
    @PrimaryKey val id: String = "singleton_qaza",
    val fajrCount: Int = 0,
    val dhuhrCount: Int = 0,
    val asrCount: Int = 0,
    val maghribCount: Int = 0,
    val ishaCount: Int = 0,
    val witrCount: Int = 0
)

@Entity(tableName = "zakat_records")
data class ZakatRecordEntity(
    @PrimaryKey val id: String,
    val calculatedDateMillis: Long = System.currentTimeMillis(),
    val cashAmount: Double,
    val bankAmount: Double,
    val goldValue: Double,
    val silverValue: Double,
    val businessStockValue: Double,
    val receivables: Double,
    val liabilitiesDue: Double,
    val totalZakatable: Double,
    val zakatPayable: Double,
    val isPaid: Boolean = false
)

@Entity(tableName = "hourly_hadiths")
data class HourlyHadithEntity(
    @PrimaryKey val id: String = "latest_hourly_hadith",
    val title: String,
    val arabic: String,
    val translation: String,
    val banglaTranslation: String,
    val narrator: String,
    val source: String,
    val hadithNumber: String,
    val grade: String,
    val topic: String,
    val fetchedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "hourly_quran_ayahs")
data class HourlyQuranEntity(
    @PrimaryKey val id: String = "latest_hourly_ayah",
    val surahNumber: Int,
    val surahNameArabic: String,
    val surahNameEnglish: String,
    val surahNameBangla: String,
    val ayahNumber: Int,
    val arabicText: String,
    val englishTranslation: String,
    val banglaTranslation: String,
    val fetchedAtMillis: Long = System.currentTimeMillis()
)
