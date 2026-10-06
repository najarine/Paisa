package com.paisa.najarine.accounting

import com.paisa.najarine.data.local.*
import com.paisa.najarine.data.remote.CryptoRate
import com.paisa.najarine.ui.screens.accounting.InvoiceItemUi
import kotlin.math.abs
import kotlin.math.max

/**
 * Main Personal Finance Accounting Engine
 * Deterministically computes Net Worth, Asset Classes, Liabilities, Receivables,
 * and Cash Flows across all financial modules of the Paisa ecosystem.
 */
data class AccountingSummary(
    // 1. Liquid Assets (নগদ ও ব্যাংক ব্যালেন্স)
    val liquidCashBank: Double = 0.0,
    val walletBalances: Map<String, Double> = emptyMap(),

    // 2. Receivables & Debt Assets (পাওনা ও বকেয়া সম্পদ)
    val customerReceivables: Double = 0.0,
    val loanReceivables: Double = 0.0,
    val invoiceReceivables: Double = 0.0,
    val totalReceivables: Double = 0.0,

    // 3. Savings & Investments (সঞ্চয়, বিনিয়োগ ও সম্পদ)
    val goalSavings: Double = 0.0,
    val fdrDpsSavings: Double = 0.0,
    val preciousMetals: Double = 0.0,
    val realEstateProperty: Double = 0.0,
    val stockInvestments: Double = 0.0,
    val cryptoDigital: Double = 0.0,
    val totalInvestments: Double = 0.0,

    // 4. Gross Assets (সর্বমোট সম্পদ)
    val grossTotalAssets: Double = 0.0,

    // 5. Liabilities & Obligations (দেনা ও দায়সমূহ)
    val cardLiabilities: Double = 0.0,
    val loanLiabilities: Double = 0.0,
    val customerAdvanceLiabilities: Double = 0.0,
    val totalLiabilities: Double = 0.0,

    // 6. Net Worth (খাঁটি নিট সম্পদ = মোট সম্পদ - মোট দায়)
    val netTotalAsset: Double = 0.0,

    // 7. Cash Flow & Period Performance (নগদ আয়-ব্যয় প্রবাহ)
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val savingsRatePercentage: Double = 0.0
) {
    // Backward compatibility getters
    val receivables: Double get() = totalReceivables
    val liabilities: Double get() = totalLiabilities
    val fdrDpsShonchoy: Double get() = fdrDpsSavings
}

object AccountingEngine {

    /**
     * Executes single-pass comprehensive accounting calculation.
     */
    fun calculate(
        wallets: List<WalletEntity>,
        transactions: List<TransactionEntity>,
        assets: List<AssetEntity>,
        debts: List<DebtEntity>,
        customerLedger: List<CustomerLedgerEntity>,
        goals: List<GoalVaultEntity> = emptyList(),
        invoices: List<InvoiceItemUi> = emptyList(),
        cryptoRates: Map<String, CryptoRate> = emptyMap()
    ): AccountingSummary {

        // -------------------------------------------------------------
        // 1. LIQUID ASSETS & WALLET BALANCES
        // -------------------------------------------------------------
        val activeWallets = wallets.filter { !it.isExcludedFromTotal }
        val nonCardWallets = activeWallets.filter { it.type != "CREDIT_CARD" && it.type != "CARD" }
        val cardWallets = activeWallets.filter { it.type == "CREDIT_CARD" || it.type == "CARD" }

        val walletCashTotal = nonCardWallets.sumOf { it.balance }

        // Any transactions not associated with an existing wallet are dynamically resolved
        val unallocatedTxNet = transactions.filter { tx -> wallets.none { it.id == tx.walletId } }
            .sumOf { tx ->
                when (tx.type) {
                    "INCOME" -> tx.amount
                    "EXPENSE" -> -(tx.amount + tx.fee)
                    else -> 0.0
                }
            }

        val liquidCashBank = walletCashTotal + unallocatedTxNet

        // -------------------------------------------------------------
        // 2. LIABILITIES (Credit Cards + Personal Dena + Advance Customer Deposits)
        // -------------------------------------------------------------
        val cardLiabilities = cardWallets.sumOf {
            if (it.balance < 0) abs(it.balance) else 0.0
        }

        val unsettledDena = debts.filter { it.type == "DENA" && !it.isSettled }.sumOf { it.amount }

        // -------------------------------------------------------------
        // 3. RECEIVABLES (Customer Ledger + Personal Paona + Invoices)
        // -------------------------------------------------------------
        // Customer Ledger: Group by customer to calculate exact per-customer net receivable or advance liability
        var customerReceivables = 0.0
        var customerAdvanceLiabilities = 0.0

        val customerGroups = customerLedger.groupBy { it.customerName.trim().lowercase() }
        for ((_, entries) in customerGroups) {
            val bakiSum = entries.filter { it.type == "BAKI" }.sumOf { it.amount }
            val jomaSum = entries.filter { it.type == "JOMA" }.sumOf { it.amount }
            val netDue = bakiSum - jomaSum
            if (netDue > 0) {
                customerReceivables += netDue
            } else if (netDue < 0) {
                customerAdvanceLiabilities += abs(netDue)
            }
        }

        val unsettledPaona = debts.filter { it.type == "PONA" && !it.isSettled }.sumOf { it.amount }

        val unpaidInvoices = invoices.filter { it.status.uppercase() != "PAID" }.sumOf { it.totalAmount }

        val totalReceivables = customerReceivables + unsettledPaona + unpaidInvoices
        val totalLiabilities = cardLiabilities + unsettledDena + customerAdvanceLiabilities

        // -------------------------------------------------------------
        // 4. SAVINGS & INVESTMENTS
        // -------------------------------------------------------------
        val goalSavings = goals.sumOf { it.currentAmount }

        val fdrDpsSavings = assets.filter { it.category in listOf("FDR", "DPS", "SHONCHOYPOTRO", "SAVINGS") }
            .sumOf { it.quantity * it.currentPrice }

        val preciousMetals = assets.filter { it.category in listOf("GOLD", "SILVER", "METALS") }
            .sumOf { it.quantity * it.currentPrice }

        val realEstateProperty = assets.filter { it.category in listOf("REAL_ESTATE", "PROPERTY", "LAND") }
            .sumOf { it.quantity * it.currentPrice }

        val stockInvestments = assets.filter { it.category in listOf("STOCK", "SHARE", "MUTUAL_FUND") }
            .sumOf { it.quantity * it.currentPrice }

        val cryptoDigital = assets.filter { it.category in listOf("CRYPTO", "DIGITAL") }
            .sumOf { asset ->
                val rate = cryptoRates[asset.name.uppercase()]?.priceBdt ?: asset.currentPrice
                asset.quantity * rate
            }

        val totalInvestments = fdrDpsSavings + preciousMetals + realEstateProperty + stockInvestments + cryptoDigital + goalSavings

        // -------------------------------------------------------------
        // 5. GROSS ASSETS & NET WORTH
        // -------------------------------------------------------------
        val grossTotalAssets = liquidCashBank + totalReceivables + totalInvestments
        val netTotalAsset = grossTotalAssets - totalLiabilities

        // -------------------------------------------------------------
        // 6. CASH FLOW PERFORMANCE
        // -------------------------------------------------------------
        val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount + it.fee }
        val netCashFlow = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) ((netCashFlow / totalIncome) * 100).coerceIn(-100.0, 100.0) else 0.0

        return AccountingSummary(
            liquidCashBank = liquidCashBank,
            walletBalances = wallets.associate { it.id to it.balance },
            customerReceivables = customerReceivables,
            loanReceivables = unsettledPaona,
            invoiceReceivables = unpaidInvoices,
            totalReceivables = totalReceivables,
            goalSavings = goalSavings,
            fdrDpsSavings = fdrDpsSavings,
            preciousMetals = preciousMetals,
            realEstateProperty = realEstateProperty,
            stockInvestments = stockInvestments,
            cryptoDigital = cryptoDigital,
            totalInvestments = totalInvestments,
            grossTotalAssets = grossTotalAssets,
            cardLiabilities = cardLiabilities,
            loanLiabilities = unsettledDena,
            customerAdvanceLiabilities = customerAdvanceLiabilities,
            totalLiabilities = totalLiabilities,
            netTotalAsset = netTotalAsset,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netCashFlow = netCashFlow,
            savingsRatePercentage = savingsRate
        )
    }
}
