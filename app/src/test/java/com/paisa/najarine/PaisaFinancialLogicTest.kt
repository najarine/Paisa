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
}
