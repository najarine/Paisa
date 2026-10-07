package com.paisa.najarine.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.paisa.najarine.accounting.AccountingEngine
import com.paisa.najarine.data.local.PaisaDatabase
import com.paisa.najarine.data.local.ZakatRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Data structure holding the comprehensive Zakat calculation summary report.
 */
data class ZakatSummaryReport(
    val calculatedAtMillis: Long,
    val grossAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double,
    val nisabThreshold: Double,
    val isNisabReached: Boolean,
    val zakatPayable: Double,
    val liquidCashBank: Double,
    val preciousMetals: Double,
    val investmentsAndStocks: Double,
    val receivables: Double,
    val formattedReportText: String
)

/**
 * Background Service / Worker that automatically calculates Zakat based on
 * the user's current net worth and holdings tracked in the app, storing
 * a summary report and sending a notification.
 */
class AutoZakatCalculatorWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = PaisaDatabase.getDatabase(applicationContext)
            val workspaceId = "personal_default"

            // 1. Fetch current tracked financial records across all modules
            val wallets = database.walletDao().getWalletsByWorkspace(workspaceId).first()
            val assets = database.assetDao().getAssets(workspaceId).first()
            val debts = database.debtDao().getDebts(workspaceId).first()
            val transactions = database.transactionDao().getTransactionsByWorkspace(workspaceId).first()
            val customerLedger = database.customerLedgerDao().getLedgerEntries(workspaceId).first()
            val goals = database.goalVaultDao().getGoals(workspaceId).first()

            // 2. Deterministically calculate live accounting summary and Net Worth
            val summary = AccountingEngine.calculate(
                wallets = wallets,
                transactions = transactions,
                assets = assets,
                debts = debts,
                customerLedger = customerLedger,
                goals = goals
            )

            // 3. Shariah Zakat Calculation
            // Standard Silver Nisab in Bangladesh (approx 52.5 tola silver = ~৳ 85,000)
            val nisabThreshold = 85000.0
            val netWorth = summary.netTotalAsset
            val netZakatable = netWorth.coerceAtLeast(0.0)
            val isNisabReached = netZakatable >= nisabThreshold
            val zakatPayable = if (isNisabReached) netZakatable * 0.025 else 0.0
            val now = System.currentTimeMillis()

            // 4. Record calculation in Room DB
            val zakatRecord = ZakatRecordEntity(
                id = UUID.randomUUID().toString(),
                calculatedDateMillis = now,
                cashAmount = summary.liquidCashBank * 0.3,
                bankAmount = summary.liquidCashBank * 0.7,
                goldValue = summary.preciousMetals,
                silverValue = 0.0,
                businessStockValue = summary.stockInvestments + summary.cryptoDigital,
                receivables = summary.totalReceivables,
                liabilitiesDue = summary.totalLiabilities,
                totalZakatable = netZakatable,
                zakatPayable = zakatPayable,
                isPaid = false
            )
            database.zakatDao().insertZakatRecord(zakatRecord)

            // 5. Generate human-readable Zakat Summary Report
            val dateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("bn", "BD")).format(Date(now))
            val reportText = buildString {
                appendLine("========================================")
                appendLine("   পয়সা (PAISA) স্বয়ংক্রিয় যাকাত রিপোর্ট")
                appendLine("========================================")
                appendLine("হিসাবের তারিখ: $dateStr")
                appendLine("----------------------------------------")
                appendLine("১. মোট সম্পদ (Gross Assets): ৳ ${String.format(Locale.US, "%,.0f", summary.grossTotalAssets)}")
                appendLine("   • নগদ ও ব্যাংক ওয়ালেট: ৳ ${String.format(Locale.US, "%,.0f", summary.liquidCashBank)}")
                appendLine("   • স্বর্ণ ও মূল্যবান ধাতু: ৳ ${String.format(Locale.US, "%,.0f", summary.preciousMetals)}")
                appendLine("   • শেয়ার ও ডিজিটাল সম্পদ: ৳ ${String.format(Locale.US, "%,.0f", summary.stockInvestments + summary.cryptoDigital)}")
                appendLine("   • ডিপিএস ও সঞ্চয় স্কিম: ৳ ${String.format(Locale.US, "%,.0f", summary.fdrDpsSavings)}")
                appendLine("   • প্রাপ্য ঋণ ও লেজার পাওনা: ৳ ${String.format(Locale.US, "%,.0f", summary.totalReceivables)}")
                appendLine("----------------------------------------")
                appendLine("২. বাদ: দেনা ও দায় (Liabilities): -৳ ${String.format(Locale.US, "%,.0f", summary.totalLiabilities)}")
                appendLine("----------------------------------------")
                appendLine("৩. খাঁটি নিট সম্পদ (Net Worth): ৳ ${String.format(Locale.US, "%,.0f", netWorth)}")
                appendLine("৪. বর্তমান রৌপ্য নেসাব সীমা: ৳ ${String.format(Locale.US, "%,.0f", nisabThreshold)}")
                appendLine("৫. নেসাব স্ট্যাটাস: ${if (isNisabReached) "নেসাব পূর্ণ হয়েছে (যাকাত আবশ্যক)" else "নেসাব অপূর্ণ (যাকাত আবশ্যক নয়)"}")
                appendLine("----------------------------------------")
                appendLine("★ প্রদেয় যাকাত (২.৫%): ৳ ${String.format(Locale.US, "%,.0f", zakatPayable)}")
                appendLine("========================================")
            }

            // 6. Save Report to SharedPreferences for instant UI access
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonReport = JSONObject().apply {
                put("calculatedAtMillis", now)
                put("grossAssets", summary.grossTotalAssets)
                put("totalLiabilities", summary.totalLiabilities)
                put("netWorth", netWorth)
                put("nisabThreshold", nisabThreshold)
                put("isNisabReached", isNisabReached)
                put("zakatPayable", zakatPayable)
                put("liquidCashBank", summary.liquidCashBank)
                put("preciousMetals", summary.preciousMetals)
                put("investmentsAndStocks", summary.stockInvestments + summary.cryptoDigital)
                put("receivables", summary.totalReceivables)
                put("formattedReportText", reportText)
            }
            prefs.edit()
                .putString(KEY_LATEST_REPORT_JSON, jsonReport.toString())
                .putLong(KEY_LAST_CALCULATED_TIME, now)
                .apply()

            // 7. Send Push Notification Summary
            if (isNisabReached) {
                PaisaNotificationManager.showNotification(
                    context = applicationContext,
                    channelId = PaisaNotificationManager.CHANNEL_FINANCIAL,
                    notificationId = 2001,
                    title = "স্বয়ংক্রিয় যাকাত হিসাব ও রিপোর্ট",
                    body = "আপনার বর্তমান নিট সম্পদের (৳${String.format(Locale.US, "%,.0f", netWorth)}) ভিত্তিতে প্রদেয় যাকাত: ৳${String.format(Locale.US, "%,.0f", zakatPayable)}। সম্পূর্ণ রিপোর্ট দেখুন।",
                    targetTab = 0,
                    targetScreen = "ZAKAT"
                )
            }

            Log.d("AutoZakatWorker", "Auto Zakat calculated successfully. Net Worth: $netWorth, Zakat: $zakatPayable")
            Result.success()
        } catch (e: Exception) {
            Log.e("AutoZakatWorker", "Error calculating automatic Zakat: ${e.localizedMessage}", e)
            Result.retry()
        }
    }

    companion object {
        const val PREFS_NAME = "paisa_auto_zakat_prefs"
        const val KEY_LATEST_REPORT_JSON = "latest_zakat_report_json"
        const val KEY_LAST_CALCULATED_TIME = "last_zakat_calculated_time"

        fun getLatestReport(context: Context): ZakatSummaryReport? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_LATEST_REPORT_JSON, null) ?: return null
            return try {
                val json = JSONObject(jsonStr)
                ZakatSummaryReport(
                    calculatedAtMillis = json.optLong("calculatedAtMillis", System.currentTimeMillis()),
                    grossAssets = json.optDouble("grossAssets", 0.0),
                    totalLiabilities = json.optDouble("totalLiabilities", 0.0),
                    netWorth = json.optDouble("netWorth", 0.0),
                    nisabThreshold = json.optDouble("nisabThreshold", 85000.0),
                    isNisabReached = json.optBoolean("isNisabReached", false),
                    zakatPayable = json.optDouble("zakatPayable", 0.0),
                    liquidCashBank = json.optDouble("liquidCashBank", 0.0),
                    preciousMetals = json.optDouble("preciousMetals", 0.0),
                    investmentsAndStocks = json.optDouble("investmentsAndStocks", 0.0),
                    receivables = json.optDouble("receivables", 0.0),
                    formattedReportText = json.optString("formattedReportText", "")
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
