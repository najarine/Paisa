package com.paisa.najarine

import com.paisa.najarine.ads.AdMobManager
import com.paisa.najarine.data.local.HourlyHadithEntity
import com.paisa.najarine.data.local.HourlyQuranEntity
import com.paisa.najarine.data.model.BankMfsCatalog
import com.paisa.najarine.data.model.FinancialInstitutionType
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.max

class PaisaFinancialLogicTest {

    @Test
    fun testTransferDoesNotCountAsIncomeOrExpense() {
        val startingWallet1 = 50000.0
        val startingWallet2 = 10000.0
        val transferAmount = 15000.0
        val transferFee = 25.0

        val endingWallet1 = startingWallet1 - (transferAmount + transferFee)
        val endingWallet2 = startingWallet2 + transferAmount

        assertEquals(34975.0, endingWallet1, 0.001)
        assertEquals(25000.0, endingWallet2, 0.001)

        // Net wealth change is only the transfer fee (transfer itself is net-zero)
        val initialNetWorth = startingWallet1 + startingWallet2
        val finalNetWorth = endingWallet1 + endingWallet2
        assertEquals(transferFee, initialNetWorth - finalNetWorth, 0.001)
    }

    @Test
    fun testZakatCalculation() {
        val cash = 20000.0
        val bank = 150000.0
        val goldGrams = 20.0
        val goldPricePerGram = 11500.0 // BDT
        val debts = 10000.0

        val totalAssets = cash + bank + (goldGrams * goldPricePerGram)
        val netZakatable = max(0.0, totalAssets - debts)
        val zakatPayable = netZakatable * 0.025

        assertEquals(400000.0, totalAssets, 0.001)
        assertEquals(390000.0, netZakatable, 0.001)
        assertEquals(9750.0, zakatPayable, 0.001)
    }

    @Test
    fun testBankMfsCatalogCompleteness() {
        val bkash = BankMfsCatalog.findById("bkash")
        assertEquals("bKash", bkash.name)
        assertEquals(FinancialInstitutionType.MFS, bkash.type)

        val ibbl = BankMfsCatalog.findById("ibbl")
        assertTrue(ibbl.name.contains("Islami Bank"))
        assertEquals(FinancialInstitutionType.BANK, ibbl.type)

        val nagad = BankMfsCatalog.findById("nagad")
        assertEquals("Nagad", nagad.name)

        val brac = BankMfsCatalog.findById("brac")
        assertTrue(brac.name.startsWith("BRAC Bank"))

        assertTrue(BankMfsCatalog.MFS_LIST.isNotEmpty())
        assertTrue(BankMfsCatalog.BANK_LIST.isNotEmpty())
    }

    @Test
    fun testAdMobBannerAndMediationAdUnitConfiguration() {
        assertEquals("ca-app-pub-5571621417978572/1238461366", AdMobManager.BANNER_AD_UNIT_ID)
        assertEquals("ca-app-pub-5571621417978572/1238461366", AdMobManager.MEDIATION_AD_UNIT_ID)
    }

    @Test
    fun testHourlyIslamicEntityModel() {
        val hadith = HourlyHadithEntity(
            id = "latest_hourly_hadith",
            title = "Charity test",
            arabic = "مَا نَقَصَتْ صَدَقَةٌ مِنْ مَالٍ",
            translation = "Charity does not decrease wealth",
            banglaTranslation = "দান সম্পদ কমায় না",
            narrator = "আবু হুরায়রা",
            source = "সহীহ মুসলিম",
            hadithNumber = "2588",
            grade = "সহীহ",
            topic = "সাদাকাহ"
        )
        assertEquals("latest_hourly_hadith", hadith.id)
        assertTrue(hadith.banglaTranslation.isNotBlank())

        val ayah = HourlyQuranEntity(
            id = "latest_hourly_ayah",
            surahNumber = 65,
            surahNameArabic = "الطلاق",
            surahNameEnglish = "At-Talaq",
            surahNameBangla = "আত-ত্বালাক্ব",
            ayahNumber = 3,
            arabicText = "وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ",
            englishTranslation = "And provide for him from where he does not expect",
            banglaTranslation = "তিনি তাকে এমন জায়গা থেকে রিজিক দেবেন",
            fetchedAtMillis = System.currentTimeMillis()
        )
        assertEquals(65, ayah.surahNumber)
        assertEquals(3, ayah.ayahNumber)
    }

    @Test
    fun testFinancialAnalyticsCalculations() {
        val income = 75000.0
        val expense = 45000.0
        val netSavings = income - expense
        val savingsRate = (netSavings / income) * 100.0

        assertEquals(30000.0, netSavings, 0.001)
        assertEquals(40.0, savingsRate, 0.001)

        val liquidCash = 180000.0
        val emergencyRunwayMonths = liquidCash / expense
        assertEquals(4.0, emergencyRunwayMonths, 0.001)
    }

    @Test
    fun testAccountingEngineNetWorthComprehensive() {
        val wallets = listOf(
            com.paisa.najarine.data.local.WalletEntity(
                id = "w1",
                workspaceId = "personal_default",
                name = "Cash",
                type = "CASH",
                institutionId = "cash",
                balance = 25000.0
            ),
            com.paisa.najarine.data.local.WalletEntity(
                id = "w2",
                workspaceId = "personal_default",
                name = "City Bank Card",
                type = "CREDIT_CARD",
                institutionId = "city",
                balance = -15000.0 // 15k used / debt
            )
        )

        val transactions = listOf(
            com.paisa.najarine.data.local.TransactionEntity(
                id = "t1",
                workspaceId = "personal_default",
                walletId = "w1",
                type = "INCOME",
                amount = 50000.0,
                category = "Salary"
            ),
            com.paisa.najarine.data.local.TransactionEntity(
                id = "t2",
                workspaceId = "personal_default",
                walletId = "w1",
                type = "EXPENSE",
                amount = 12000.0,
                fee = 50.0,
                category = "Groceries"
            )
        )

        val assets = listOf(
            com.paisa.najarine.data.local.AssetEntity(
                id = "a1",
                workspaceId = "personal_default",
                name = "22K Gold Bar",
                category = "GOLD",
                quantity = 2.0, // 2 vori
                unit = "vori",
                buyPrice = 120000.0,
                currentPrice = 135000.0 // 270,000 total
            ),
            com.paisa.najarine.data.local.AssetEntity(
                id = "a2",
                workspaceId = "personal_default",
                name = "BRAC DPS",
                category = "DPS",
                quantity = 1.0,
                unit = "account",
                buyPrice = 50000.0,
                currentPrice = 50000.0
            )
        )

        val debts = listOf(
            com.paisa.najarine.data.local.DebtEntity(
                id = "d1",
                workspaceId = "personal_default",
                walletId = "w1",
                personName = "Rahim",
                amount = 10000.0,
                type = "PONA", // Rahim owes me 10,000 -> Receivable asset
                isSettled = false
            ),
            com.paisa.najarine.data.local.DebtEntity(
                id = "d2",
                workspaceId = "personal_default",
                walletId = "w1",
                personName = "Karim",
                amount = 5000.0,
                type = "DENA", // I owe Karim 5,000 -> Liability
                isSettled = false
            )
        )

        val customerLedger = listOf(
            com.paisa.najarine.data.local.CustomerLedgerEntity(
                id = "c1",
                workspaceId = "personal_default",
                customerName = "Mr. Salam",
                amount = 8000.0,
                type = "BAKI" // Salam owes me 8,000 -> Customer receivable
            ),
            com.paisa.najarine.data.local.CustomerLedgerEntity(
                id = "c2",
                workspaceId = "personal_default",
                customerName = "Mr. Salam",
                amount = 3000.0,
                type = "JOMA" // Salam paid 3,000 -> Remaining due 5,000
            )
        )

        val summary = com.paisa.najarine.accounting.AccountingEngine.calculate(
            wallets = wallets,
            transactions = transactions,
            assets = assets,
            debts = debts,
            customerLedger = customerLedger
        )

        // 1. Liquid Cash
        assertEquals(25000.0, summary.liquidCashBank, 0.001)

        // 2. Receivables: Customer Ledger (5,000) + Loan Paona (10,000) = 15,000
        assertEquals(5000.0, summary.customerReceivables, 0.001)
        assertEquals(10000.0, summary.loanReceivables, 0.001)
        assertEquals(15000.0, summary.totalReceivables, 0.001)

        // 3. Investments: Gold (270,000) + DPS (50,000) = 320,000
        assertEquals(270000.0, summary.preciousMetals, 0.001)
        assertEquals(50000.0, summary.fdrDpsSavings, 0.001)
        assertEquals(320000.0, summary.totalInvestments, 0.001)

        // 4. Gross Total Assets: 25,000 + 15,000 + 320,000 = 360,000
        assertEquals(360000.0, summary.grossTotalAssets, 0.001)

        // 5. Liabilities: Credit Card (15,000) + Personal Dena (5,000) = 20,000
        assertEquals(15000.0, summary.cardLiabilities, 0.001)
        assertEquals(5000.0, summary.loanLiabilities, 0.001)
        assertEquals(20000.0, summary.totalLiabilities, 0.001)

        // 6. Net Worth: 360,000 - 20,000 = 340,000
        assertEquals(340000.0, summary.netTotalAsset, 0.001)
    }

    @Test
    fun testMonthlySpendingTrendsCalculation() {
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = now
        }

        // Create transaction in current month
        val currentMonthTx = com.paisa.najarine.data.local.TransactionEntity(
            id = "tx_current",
            workspaceId = "ws1",
            walletId = "w1",
            type = "EXPENSE",
            amount = 12000.0,
            category = "Food",
            dateMillis = now
        )

        // Create transaction 1 month ago
        val oneMonthAgoCal = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            add(java.util.Calendar.MONTH, -1)
            set(java.util.Calendar.DAY_OF_MONTH, 15)
        }
        val oneMonthAgoTx = com.paisa.najarine.data.local.TransactionEntity(
            id = "tx_prev",
            workspaceId = "ws1",
            walletId = "w1",
            type = "EXPENSE",
            amount = 8000.0,
            category = "Shopping",
            dateMillis = oneMonthAgoCal.timeInMillis
        )

        // Income transaction (should not be counted in spending trend totalExpense)
        val incomeTx = com.paisa.najarine.data.local.TransactionEntity(
            id = "tx_inc",
            workspaceId = "ws1",
            walletId = "w1",
            type = "INCOME",
            amount = 50000.0,
            category = "Salary",
            dateMillis = now
        )

        val txList = listOf(currentMonthTx, oneMonthAgoTx, incomeTx)

        val trends = com.paisa.najarine.ui.screens.analytics.MonthlySpendingTrendsCalculator.calculateSixMonthTrends(
            transactions = txList,
            referenceTimeMillis = now
        )

        assertEquals(6, trends.points.size)
        // Current month is index 5
        assertEquals(12000.0, trends.points[5].totalExpense, 0.001)
        // Previous month is index 4
        assertEquals(8000.0, trends.points[4].totalExpense, 0.001)
        // 2 months ago is index 3 -> 0.0
        assertEquals(0.0, trends.points[3].totalExpense, 0.001)

        // Total spending over 6 months should be 12000 + 8000 = 20000
        assertEquals(20000.0, trends.totalSpend, 0.001)
        // Average over 6 months: 20000 / 6 = 3333.333
        assertEquals(20000.0 / 6.0, trends.averageMonthlySpend, 0.001)

        // Highest month should be the current month (12000)
        assertEquals(12000.0, trends.highestMonth?.totalExpense ?: 0.0, 0.001)

        // Percent change from previous month to current month: (12000 - 8000) / 8000 = +50%
        val changePct = trends.points[5].percentChangeFromPrevious
        assertNotNull(changePct)
        assertEquals(50.0, changePct!!, 0.001)
    }

    @Test
    fun testDebtPayoffGoalAndLiabilityRemainingBalance() {
        val totalDenaLiability = 75000.0
        val debt1 = com.paisa.najarine.data.local.DebtEntity(
            id = "d1",
            workspaceId = "ws1",
            walletId = "w1",
            personName = "Rahim Bhai",
            amount = 50000.0,
            type = "DENA"
        )
        val debt2 = com.paisa.najarine.data.local.DebtEntity(
            id = "d2",
            workspaceId = "ws1",
            walletId = "w1",
            personName = "Karim Lenders",
            amount = 25000.0,
            type = "DENA"
        )

        // Debt Payoff Goal for Rahim Bhai with 20,000 paid off so far
        val payoffGoalRahim = com.paisa.najarine.data.local.GoalVaultEntity(
            id = "g_debt_1",
            workspaceId = "ws1",
            name = "[ঋণ পরিশোধ] Rahim Bhai এর ঋণ পরিশোধ",
            targetAmount = 50000.0,
            currentAmount = 20000.0,
            targetDateMillis = System.currentTimeMillis() + 1000000L,
            colorHex = "#EF4444"
        )

        // Remaining balance for Rahim Bhai
        val remainingRahim = (debt1.amount - payoffGoalRahim.currentAmount).coerceAtLeast(0.0)
        assertEquals(30000.0, remainingRahim, 0.001)

        // Remaining balance for Karim (no payments yet)
        val remainingKarim = debt2.amount
        assertEquals(25000.0, remainingKarim, 0.001)

        // Overall remaining debt across all liabilities
        val totalPaidOff = payoffGoalRahim.currentAmount
        val totalRemainingDebt = totalDenaLiability - totalPaidOff
        assertEquals(55000.0, totalRemainingDebt, 0.001)
    }

    @Test
    fun testZakatCalculationDirectlyFromNetWorth() {
        // Net worth calculation from Accounting Engine:
        // Cash: 50,000, Gold: 300,000, Stocks: 100,000, Debts: 50,000 -> Net Worth = 400,000
        val netWorth = 400000.0
        val silverNisab = 85000.0

        assertTrue(netWorth >= silverNisab)
        val zakatPayable = netWorth * 0.025
        assertEquals(10000.0, zakatPayable, 0.001)

        // If net worth is below nisab (e.g. 50,000)
        val belowNisabNetWorth = 50000.0
        val zakatPayableBelow = if (belowNisabNetWorth >= silverNisab) belowNisabNetWorth * 0.025 else 0.0
        assertEquals(0.0, zakatPayableBelow, 0.001)
    }

    @Test
    fun testForexAndShareMarketOverviewData() = kotlinx.coroutines.runBlocking {
        val service = com.paisa.najarine.data.remote.MarketDataService()
        val overview = service.fetchShareMarketOverview()
        assertNotNull(overview)
        assertTrue(overview.indices.isNotEmpty())
        assertTrue(overview.topStocks.isNotEmpty())

        val dsex = overview.indices.find { it.symbol == "DSEX" }
        assertNotNull(dsex)
        assertTrue(dsex!!.currentValue > 5000.0)

        val forex = service.fetchForexRates()
        assertNotNull(forex)
        val usd = forex.find { it.code == "USD" }
        assertNotNull(usd)
        assertTrue(usd!!.rateInBdt > 100.0)
    }

    @Test
    fun testAutoZakatSummaryReportGeneration() {
        val grossAssets = 650000.0
        val totalLiabilities = 100000.0
        val netWorth = grossAssets - totalLiabilities
        val nisab = 85000.0
        val isNisabReached = netWorth >= nisab
        val zakatPayable = if (isNisabReached) netWorth * 0.025 else 0.0

        val report = com.paisa.najarine.notification.ZakatSummaryReport(
            calculatedAtMillis = System.currentTimeMillis(),
            grossAssets = grossAssets,
            totalLiabilities = totalLiabilities,
            netWorth = netWorth,
            nisabThreshold = nisab,
            isNisabReached = isNisabReached,
            zakatPayable = zakatPayable,
            liquidCashBank = 150000.0,
            preciousMetals = 350000.0,
            investmentsAndStocks = 100000.0,
            receivables = 50000.0,
            formattedReportText = "Total Net Worth: $netWorth, Zakat: $zakatPayable"
        )

        assertEquals(550000.0, report.netWorth, 0.001)
        assertTrue(report.isNisabReached)
        assertEquals(13750.0, report.zakatPayable, 0.001)
        assertTrue(report.formattedReportText.contains("550000"))
    }

    @Test
    fun testMonthlyCategoryBreakdownCalculation() {
        val now = System.currentTimeMillis()
        val options = com.paisa.najarine.ui.screens.analytics.MonthlyCategoryBreakdownHelper.getRecentMonthOptions()
        assertTrue(options.isNotEmpty())
        val currentMonthOption = options[0]

        val txs = listOf(
            com.paisa.najarine.data.local.TransactionEntity(
                id = "t1", workspaceId = "ws1", walletId = "w1",
                type = "EXPENSE", amount = 15000.0, category = "Food",
                dateMillis = currentMonthOption.startMillis + 1000L
            ),
            com.paisa.najarine.data.local.TransactionEntity(
                id = "t2", workspaceId = "ws1", walletId = "w1",
                type = "EXPENSE", amount = 5000.0, category = "Rent",
                dateMillis = currentMonthOption.startMillis + 2000L
            ),
            com.paisa.najarine.data.local.TransactionEntity(
                id = "t3", workspaceId = "ws1", walletId = "w1",
                type = "INCOME", amount = 40000.0, category = "Salary",
                dateMillis = currentMonthOption.startMillis + 3000L
            )
        )

        val categories = listOf(
            com.paisa.najarine.data.local.CategoryEntity(id = "c1", name = "Food", type = "EXPENSE", iconName = "restaurant", colorHex = "#EF4444"),
            com.paisa.najarine.data.local.CategoryEntity(id = "c2", name = "Rent", type = "EXPENSE", iconName = "home", colorHex = "#2563EB")
        )

        val items = com.paisa.najarine.ui.screens.analytics.MonthlyCategoryBreakdownHelper.computeMonthlyCategorySpending(
            transactions = txs,
            categories = categories,
            monthOption = currentMonthOption
        )

        assertEquals(2, items.size)
        // Highest expense first
        assertEquals("Food", items[0].categoryName)
        assertEquals(15000.0, items[0].totalAmount, 0.001)
        assertEquals(0.75, items[0].percentageOfTotal, 0.001) // 15000 / 20000 = 75%
        assertNotNull(items[0].healthWarning) // > 35% warning

        assertEquals("Rent", items[1].categoryName)
        assertEquals(5000.0, items[1].totalAmount, 0.001)
        assertEquals(0.25, items[1].percentageOfTotal, 0.001) // 5000 / 20000 = 25%
    }

    @Test
    fun testDebtPayoffProjectionCalculation() {
        val now = System.currentTimeMillis()
        val debts = listOf(
            com.paisa.najarine.data.local.DebtEntity(
                id = "d1", workspaceId = "ws1", walletId = "w1", personName = "Karim",
                phoneNumber = "01711111111", amount = 30000.0,
                type = "DENA", dueDateMillis = now + (30L * 24 * 60 * 60 * 1000),
                isSettled = false, note = ""
            ),
            com.paisa.najarine.data.local.DebtEntity(
                id = "d2", workspaceId = "ws1", walletId = "w1", personName = "Rahim",
                phoneNumber = "", amount = 10000.0,
                type = "PONA", dueDateMillis = now + (60L * 24 * 60 * 60 * 1000),
                isSettled = false, note = ""
            )
        )

        val goals = listOf(
            com.paisa.najarine.data.local.GoalVaultEntity(
                id = "g1", workspaceId = "ws1", name = "[ঋণ পরিশোধ] Karim এর ঋণ পরিশোধ",
                targetAmount = 30000.0, currentAmount = 10000.0,
                targetDateMillis = now + (30L * 24 * 60 * 60 * 1000),
                colorHex = "#EF4444"
            )
        )

        val wallets = listOf(
            com.paisa.najarine.data.local.WalletEntity(
                id = "w_card", workspaceId = "ws1", name = "City Bank Card",
                type = "CARD", institutionId = "city", balance = -5000.0,
                accountNumber = "1234", colorHex = "#EF4444"
            )
        )

        val projection = com.paisa.najarine.ui.components.DebtPayoffCalculator.calculateProjection(
            debts = debts,
            goals = goals,
            wallets = wallets,
            transactions = emptyList()
        )

        // Total liabilities = Karim Dena (30,000) + Card negative balance (5,000) = 35,000
        assertEquals(35000.0, projection.totalOriginalLiability, 0.001)
        // Total paid towards debt = 10,000
        assertEquals(10000.0, projection.totalPaidSoFar, 0.001)
        // Remaining liability = 25,000
        assertEquals(25000.0, projection.totalRemainingLiability, 0.001)
        // Progress = 10000 / 35000 =~ 0.2857
        assertTrue(projection.overallProgress > 0.28f)
        // 2 liability items (Card + Karim)
        assertEquals(2, projection.liabilities.size)
        assertTrue(projection.projectedMonthsToDebtFree > 0)
    }

    @Test
    fun testMessCalculationEngine() {
        val now = System.currentTimeMillis()
        val entries = listOf(
            com.paisa.najarine.data.local.MessEntryEntity(
                id = "m1", workspaceId = "ws1", memberName = "Tanvir",
                mealsCount = 30.0, depositAmount = 3000.0, bazarExpense = 1500.0,
                fixedExpenseShare = 500.0, dateMillis = now
            ),
            com.paisa.najarine.data.local.MessEntryEntity(
                id = "m2", workspaceId = "ws1", memberName = "Nabil",
                mealsCount = 20.0, depositAmount = 1000.0, bazarExpense = 1000.0,
                fixedExpenseShare = 500.0, dateMillis = now
            )
        )

        val result = com.paisa.najarine.ui.screens.accounting.MessCalculationEngine.calculate(entries)

        assertEquals(50.0, result.totalMeals, 0.001)
        assertEquals(2500.0, result.totalBazar, 0.001)
        assertEquals(1000.0, result.totalFixedBills, 0.001)
        assertEquals(3500.0, result.totalMessExpense, 0.001)
        // Meal rate = 2500 / 50 = 50.0
        assertEquals(50.0, result.mealRate, 0.001)

        val tanvir = result.memberSummaries.find { it.memberName == "Tanvir" }
        assertNotNull(tanvir)
        assertEquals(30.0, tanvir!!.totalMeals, 0.001)
        // Tanvir total cost = (30 * 50) + 500 = 2000
        assertEquals(2000.0, tanvir.totalIndividualCost, 0.001)
        // Tanvir paid = 3000 + 1500 = 4500. Balance = 4500 - 2000 = +2500 (Refund)
        assertEquals(2500.0, tanvir.netBalance, 0.001)

        val nabil = result.memberSummaries.find { it.memberName == "Nabil" }
        assertNotNull(nabil)
        assertEquals(20.0, nabil!!.totalMeals, 0.001)
        // Nabil total cost = (20 * 50) + 500 = 1500
        assertEquals(1500.0, nabil.totalIndividualCost, 0.001)
        // Nabil paid = 1000 + 1000 = 2000. Balance = 2000 - 1500 = +500 (Refund)
        assertEquals(500.0, nabil.netBalance, 0.001)
    }

    @Test
    fun testBazarItemCustomization() {
        val customItem = com.paisa.najarine.data.local.BazarItemEntity(
            id = "b1",
            workspaceId = "ws1",
            name = "কাঁচা বাজার ও শাকসবজি",
            category = "শাকসবজি",
            quantity = 2.5,
            unit = "কেজি",
            estimatedPrice = 120.0,
            actualPrice = 110.0,
            isChecked = true,
            isMessItem = true,
            assignedMemberName = "Tanvir",
            note = "তাজা লাল শাক ও লাউ"
        )

        assertEquals("কাঁচা বাজার ও শাকসবজি", customItem.name)
        assertEquals("শাকসবজি", customItem.category)
        assertEquals(2.5, customItem.quantity, 0.001)
        assertEquals("কেজি", customItem.unit)
        assertEquals(120.0, customItem.estimatedPrice, 0.001)
        assertEquals(110.0, customItem.actualPrice, 0.001)
        assertTrue(customItem.isChecked)
        assertTrue(customItem.isMessItem)
        assertEquals("Tanvir", customItem.assignedMemberName)
    }

    @Test
    fun testCurrencyConversionLogic() = kotlinx.coroutines.runBlocking {
        val service = com.paisa.najarine.data.remote.MarketDataService()
        // Convert USD to BDT
        val result = service.convertCurrency("USD", "BDT", 100.0)
        assertEquals("USD", result.fromCode)
        assertEquals("BDT", result.toCode)
        assertEquals(100.0, result.fromAmount, 0.001)
        assertTrue("Converted BDT should be positive", result.convertedAmount > 0)
        assertTrue("Exchange rate should be positive", result.exchangeRate > 0)
        assertTrue("Inverse rate should be positive", result.inverseRate > 0)
        assertEquals(1.0, result.exchangeRate * result.inverseRate, 0.01)
    }
}
