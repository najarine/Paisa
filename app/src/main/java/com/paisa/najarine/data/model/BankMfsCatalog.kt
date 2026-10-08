package com.paisa.najarine.data.model

import androidx.compose.ui.graphics.Color

enum class FinancialInstitutionType {
    MFS,
    BANK,
    CARD,
    CASH,
    INVESTMENT,
    CRYPTO
}

enum class BankClassification(val banglaLabel: String) {
    STATE_OWNED("রাষ্ট্রায়ত্ত বাণিজ্যিক ব্যাংক"),
    SPECIALIZED("বিশেষায়িত ব্যাংক"),
    ISLAMIC_SHARIAH("ইসলামী শরীয়াহ ব্যাংক"),
    PRIVATE_CONVENTIONAL("বেসরকারি বাণিজ্যিক ব্যাংক"),
    FOREIGN("বিদেশি বাণিজ্যিক ব্যাংক"),
    MFS("MFS"),
    OTHER("অন্যান্য আর্থিক মাধ্যম")
}

data class InstitutionItem(
    val id: String,
    val name: String,
    val banglaName: String,
    val shortName: String,
    val type: FinancialInstitutionType,
    val classification: BankClassification,
    val isIslamicShariah: Boolean,
    val brandColor: Color,
    val tag: String,
    val subtitle: String,
    val website: String = ""
)

object BankMfsCatalog {

    val MFS_LIST = listOf(
        InstitutionItem("bkash", "bKash", "বিকাশ", "bKash", FinancialInstitutionType.MFS, BankClassification.MFS, false, Color(0xFFE2136E), "MFS", "ব্র্যাক ব্যাংক MFS (০১...)", "https://www.bkash.com"),
        InstitutionItem("nagad", "Nagad", "নগদ", "Nagad", FinancialInstitutionType.MFS, BankClassification.MFS, true, Color(0xFFF7941D), "MFS", "ডাক বিভাগ MFS ও ইসলামিক সেবা", "https://nagad.com.bd"),
        InstitutionItem("rocket", "Rocket (DBBL)", "রকেট", "Rocket", FinancialInstitutionType.MFS, BankClassification.MFS, false, Color(0xFF8C3494), "MFS", "ডাচ-বাংলা ব্যাংক MFS", "https://www.dutchbanglabank.com/rocket"),
        InstitutionItem("upay", "Upay (UCB)", "উপায়", "Upay", FinancialInstitutionType.MFS, BankClassification.MFS, false, Color(0xFF002B49), "MFS", "ইউসিবি ফিনটেক MFS", "https://www.upaybd.com"),
        InstitutionItem("cellfin", "Cellfin (IBBL)", "সেলফিন", "Cellfin", FinancialInstitutionType.MFS, BankClassification.MFS, true, Color(0xFF00843D), "Digital MFS", "ইসলামী ব্যাংক ডিজিটাল ওয়ালেট", "https://www.islamibankbd.com"),
        InstitutionItem("tap", "Tap", "ট্যাপ", "Tap", FinancialInstitutionType.MFS, BankClassification.MFS, false, Color(0xFF0056B3), "MFS", "ট্রাস্ট আজিয়াটা পে", "https://www.tapnpay.com.bd"),
        InstitutionItem("surecash", "SureCash", "শিওরক্যাশ", "SureCash", FinancialInstitutionType.MFS, BankClassification.MFS, false, Color(0xFF00AEEF), "MFS", "পেমেন্ট সার্ভিস", "https://surecash.net"),
        InstitutionItem("mcash", "mCash (IBBL)", "এমক্যাশ", "mCash", FinancialInstitutionType.MFS, BankClassification.MFS, true, Color(0xFF00843D), "MFS", "ইসলামী ব্যাংক মোবাইল ক্যাশ", "https://www.islamibankbd.com"),
        InstitutionItem("okwallet", "OK Wallet", "ওকে ওয়ালেট", "OK Wallet", FinancialInstitutionType.MFS, BankClassification.MFS, false, Color(0xFFC41230), "MFS", "ওয়ান ব্যাংক MFS", "https://www.onebank.com.bd")
    )

    // Complete Bangladesh Scheduled Bank Directory
    val BANK_LIST = listOf(
        // 1. Islamic / Shariah Banks
        InstitutionItem("ibbl", "Islami Bank Bangladesh PLC", "ইসলামী ব্যাংক বাংলাদেশ পিএলসি", "IBBL", FinancialInstitutionType.BANK, BankClassification.ISLAMIC_SHARIAH, true, Color(0xFF00843D), "ইসলামী ব্যাংক", "পূর্ণাঙ্গ শরীয়াহ ভিত্তিক ব্যাংকিং", "https://www.islamibankbd.com"),
        InstitutionItem("alarafah", "Al-Arafah Islami Bank PLC", "আল-আরাফাহ্ ইসলামী ব্যাংক পিএলসি", "AIBL", FinancialInstitutionType.BANK, BankClassification.ISLAMIC_SHARIAH, true, Color(0xFF006837), "ইসলামী ব্যাংক", "শরীয়াহ ব্যাংক ও হজ ডিপোজিট", "https://www.aibl.com.bd"),
        InstitutionItem("shahjalal", "Shahjalal Islami Bank PLC", "শাহ্জালাল ইসলামী ব্যাংক পিএলসি", "SJIBL", FinancialInstitutionType.BANK, BankClassification.ISLAMIC_SHARIAH, true, Color(0xFF008751), "ইসলামী ব্যাংক", "ইসলামিক বাণিজ্যিক সেবা", "https://sjiblbd.com"),
        InstitutionItem("exim", "EXIM Bank of Bangladesh PLC", "এক্সিম ব্যাংক অব বাংলাদেশ পিএলসি", "EXIM", FinancialInstitutionType.BANK, BankClassification.ISLAMIC_SHARIAH, true, Color(0xFF006F45), "ইসলামী ব্যাংক", "রপ্তানি-আমদানি ও শরীয়াহ ব্যাংক", "https://www.eximbankbd.com"),
        InstitutionItem("sibl", "Social Islami Bank PLC", "সোশ্যাল ইসলামী ব্যাংক পিএলসি", "SIBL", FinancialInstitutionType.BANK, BankClassification.ISLAMIC_SHARIAH, true, Color(0xFF004D25), "ইসলামী ব্যাংক", "মূল্যবোধ ভিত্তিক ব্যাংকিং", "https://www.siblbd.com"),
        InstitutionItem("fsibl", "First Security Islami Bank PLC", "ফার্স্ট সিকিউরিটি ইসলামী ব্যাংক পিএলসি", "FSIBL", FinancialInstitutionType.BANK, BankClassification.ISLAMIC_SHARIAH, true, Color(0xFF107C41), "ইসলামী ব্যাংক", "তাকওয়া ভিত্তিক আধুনিক ব্যাংকিং", "https://www.fsiblbd.com"),
        InstitutionItem("union", "Union Bank PLC", "ইউনিয়ন ব্যাংক পিএলসি", "Union", FinancialInstitutionType.BANK, BankClassification.ISLAMIC_SHARIAH, true, Color(0xFF008080), "ইসলামী ব্যাংক", "শরীয়াহ সম্মত ব্যাংকিং সেবা", "https://www.unionbank.com.bd"),
        InstitutionItem("globalislami", "Global Islami Bank PLC", "গ্লোবাল ইসলামী ব্যাংক পিএলসি", "GIB", FinancialInstitutionType.BANK, BankClassification.ISLAMIC_SHARIAH, true, Color(0xFF1B5E20), "ইসলামী ব্যাংক", "আধুনিক ইসলামিক অর্থনীতি", "https://www.globalislamibankbd.com"),
        InstitutionItem("standardislami", "Standard Bank PLC (Islamic)", "স্ট্যান্ডার্ড ব্যাংক পিএলসি (ইসলামিক)", "SBL", FinancialInstitutionType.BANK, BankClassification.ISLAMIC_SHARIAH, true, Color(0xFF1A3A68), "ইসলামী ব্যাংক", "রূপান্তরিত ইসলামিক ব্যাংকিং", "https://www.standardbankbd.com"),
        InstitutionItem("icbislamic", "ICB Islamic Bank PLC", "আইসিবি ইসলামিক ব্যাংক পিএলসি", "ICB", FinancialInstitutionType.BANK, BankClassification.ISLAMIC_SHARIAH, true, Color(0xFF2E7D32), "ইসলামী ব্যাংক", "শরীয়াহ অর্থায়ন", "https://www.icbislamic-bank.com"),

        // 2. State-Owned Commercial Banks
        InstitutionItem("sonali", "Sonali Bank PLC", "সোনালী ব্যাংক পিএলসি", "Sonali", FinancialInstitutionType.BANK, BankClassification.STATE_OWNED, false, Color(0xFFF39C12), "রাষ্ট্রায়ত্ত", "জাতীয় ট্রেজারি ও বৃহত্তম ব্যাংক", "https://www.sonalibank.com.bd"),
        InstitutionItem("janata", "Janata Bank PLC", "জনতা ব্যাংক পিএলসি", "Janata", FinancialInstitutionType.BANK, BankClassification.STATE_OWNED, false, Color(0xFF2980B9), "রাষ্ট্রায়ত্ত", "রাষ্ট্রায়ত্ত বাণিজ্যিক ব্যাংক", "https://jb.com.bd"),
        InstitutionItem("agrani", "Agrani Bank PLC", "অগ্রণী ব্যাংক পিএলসি", "Agrani", FinancialInstitutionType.BANK, BankClassification.STATE_OWNED, false, Color(0xFF27AE60), "রাষ্ট্রায়ত্ত", "সারাদেশে বিস্তৃত নেটওয়ার্ক", "https://www.agranibank.org"),
        InstitutionItem("rupali", "Rupali Bank PLC", "রূপালী ব্যাংক পিএলসি", "Rupali", FinancialInstitutionType.BANK, BankClassification.STATE_OWNED, false, Color(0xFF8E44AD), "রাষ্ট্রায়ত্ত", "জাতীয় অর্থনৈতিক সেবা", "https://www.rupalibank.com.bd"),
        InstitutionItem("basic", "BASIC Bank Limited", "বেসিক ব্যাংক লিমিটেড", "BASIC", FinancialInstitutionType.BANK, BankClassification.STATE_OWNED, false, Color(0xFFC0392B), "রাষ্ট্রায়ত্ত", "ক্ষুদ্র ও মাঝারি শিল্প অর্থায়ন", "https://www.basicbank.com.bd"),
        InstitutionItem("bdbl", "Bangladesh Development Bank (BDBL)", "বাংলাদেশ ডেভেলপমেন্ট ব্যাংক পিএলসি", "BDBL", FinancialInstitutionType.BANK, BankClassification.STATE_OWNED, false, Color(0xFF34495E), "রাষ্ট্রায়ত্ত", "উন্নয়ন ও বাণিজ্যিক ব্যাংকিং", "https://www.bdbl.com.bd"),

        // 3. Specialized Banks
        InstitutionItem("bkb", "Bangladesh Krishi Bank", "বাংলাদেশ কৃষি ব্যাংক", "BKB", FinancialInstitutionType.BANK, BankClassification.SPECIALIZED, false, Color(0xFF1E824C), "বিশেষায়িত", "কৃষি ও গ্রামীণ অর্থনীতি", "https://www.krishibank.org.bd"),
        InstitutionItem("rakub", "Rajshahi Krishi Unnayan Bank", "রাজশাহী কৃষি উন্নয়ন ব্যাংক", "RAKUB", FinancialInstitutionType.BANK, BankClassification.SPECIALIZED, false, Color(0xFF229954), "বিশেষায়িত", "উত্তরবঙ্গ কৃষি ও কুটির শিল্প", "https://www.rakub.org.bd"),
        InstitutionItem("pkb", "Probashi Kallyan Bank", "প্রবাসী কল্যাণ ব্যাংক", "PKB", FinancialInstitutionType.BANK, BankClassification.SPECIALIZED, false, Color(0xFF16A085), "বিশেষায়িত", "প্রবাসী ঋণ ও রেমিট্যান্স", "https://www.pkb.gov.bd"),
        InstitutionItem("karmasangsthan", "Karmasangsthan Bank", "কর্মসংস্থান ব্যাংক", "KB", FinancialInstitutionType.BANK, BankClassification.SPECIALIZED, false, Color(0xFF45B39D), "বিশেষায়িত", "যুব আত্মকর্মসংস্থান অর্থায়ন", "https://www.karmasangsthanbank.gov.bd"),

        // 4. Private Commercial / Conventional Banks
        InstitutionItem("brac", "BRAC Bank PLC", "ব্র্যাক ব্যাংক পিএলসি", "BRAC", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF0054A6), "বেসরকারি", "রিটেইল, এসএমই ও আস্থা অ্যাপ", "https://www.bracbank.com"),
        InstitutionItem("dbbl", "Dutch-Bangla Bank PLC", "ডাচ-বাংলা ব্যাংক পিএলসি", "DBBL", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF008000), "বেসরকারি", "নেক্সাসপে ও কোর ব্যাংকিং", "https://www.dutchbanglabank.com"),
        InstitutionItem("city", "The City Bank PLC", "দি সিটি ব্যাংক পিএলসি", "City Bank", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFFED1C24), "বেসরকারি", "সিটিটাচ ও অ্যামেক্স কার্ড", "https://www.thecitybank.com"),
        InstitutionItem("ebl", "Eastern Bank PLC", "ইস্টার্ন ব্যাংক পিএলসি", "EBL", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF1D2553), "বেসরকারি", "স্কাইব্যাংকিং ও কার্ডস", "https://www.ebl.com.bd"),
        InstitutionItem("prime", "Prime Bank PLC", "প্রাইম ব্যাংক পিএলসি", "Prime", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF0072CE), "বেসরকারি", "অল্টিটিউড ও কনজ্যুমার ব্যাংকিং", "https://www.primebank.com.bd"),
        InstitutionItem("pubali", "Pubali Bank PLC", "পূবালী ব্যাংক পিএলসি", "Pubali", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF004B87), "বেসরকারি", "ঐতিহ্যবাহী বাণিজ্যিক ব্যাংক", "https://www.pubalibangla.com"),
        InstitutionItem("mtb", "Mutual Trust Bank PLC", "মিউচুয়াল ট্রাস্ট ব্যাংক পিএলসি", "MTB", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFFE30613), "বেসরকারি", "স্মার্ট ব্যাংকিং ও করপোরেট", "https://www.mutualtrustbank.com"),
        InstitutionItem("ucb", "United Commercial Bank PLC", "ইউনাইটেড কমার্শিয়াল ব্যাংক পিএলসি", "UCB", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF002B49), "বেসরকারি", "উপায় ও সার্বিক ব্যাংকিং", "https://www.ucb.com.bd"),
        InstitutionItem("dhakabank", "Dhaka Bank PLC", "ঢাকা ব্যাংক পিএলসি", "Dhaka Bank", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF8B0000), "বেসরকারি", "কর্পোরেট ও রিটেইল ব্যাংকিং", "https://dhakabank.com.bd"),
        InstitutionItem("bankasia", "Bank Asia PLC", "ব্যাংক এশিয়া পিএলসি", "Bank Asia", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF800020), "বেসরকারি", "এজেন্ট ব্যাংকিং ও অনলাইন", "https://www.bankasia-bd.com"),
        InstitutionItem("southeast", "Southeast Bank PLC", "সাউথইস্ট ব্যাংক পিএলসি", "Southeast", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF1B365D), "বেসরকারি", "বাণিজ্যিক ও এসএমই ব্যাংকিং", "https://www.southeastbank.com.bd"),
        InstitutionItem("trust", "Trust Bank PLC", "ট্রাস্ট ব্যাংক পিএলসি", "Trust Bank", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF003865), "বেসরকারি", "সেনা কল্যাণ ও সাধারণ ব্যাংকিং", "https://www.trustbank.com.bd"),
        InstitutionItem("premier", "The Premier Bank PLC", "দি প্রিমিয়ার ব্যাংক পিএলসি", "Premier", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF005691), "বেসরকারি", "সার্ভিস ফার্স্ট ব্যাংকিং", "https://www.premierbankltd.com"),
        InstitutionItem("ncc", "NCC Bank PLC", "এনসিসি ব্যাংক পিএলসি", "NCC", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF004080), "বেসরকারি", "শিল্প ঋণ ও বাণিজ্য সেবা", "https://www.nccbank.com.bd"),
        InstitutionItem("jamuna", "Jamuna Bank PLC", "যমুনা ব্যাংক পিএলসি", "Jamuna", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF800080), "বেসরকারি", "বাণিজ্যিক অর্থনীতি", "https://www.jamunabank.com.bd"),
        InstitutionItem("mercantile", "Mercantile Bank PLC", "মার্কেন্টাইল ব্যাংক পিএলসি", "Mercantile", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF4B0082), "বেসরকারি", "দক্ষ ব্যাংকিং সেবা", "https://www.mblbd.com"),
        InstitutionItem("onebank", "ONE Bank PLC", "ওয়ান ব্যাংক পিএলসি", "ONE Bank", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFFB22222), "বেসরকারি", "স্মার্ট রিটেইল সেবা", "https://www.onebank.com.bd"),
        InstitutionItem("ific", "IFIC Bank PLC", "আইএফআইসি ব্যাংক পিএলসি", "IFIC", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFFDAA520), "বেসরকারি", "উপশাখা ও গ্রামীণ সংযোগ", "https://www.ificbank.com.bd"),
        InstitutionItem("ab", "AB Bank PLC", "এবি ব্যাংক পিএলসি", "AB Bank", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF2F4F4F), "বেসরকারি", "প্রথম বেসরকারি ব্যাংক", "https://abbl.com"),
        InstitutionItem("nrb", "NRB Bank PLC", "এনআরবি ব্যাংক পিএলসি", "NRB Bank", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF2E8B57), "বেসরকারি", "প্রবাসী বিনিয়োগ ও ব্যাংকিং", "https://www.nrbbankbd.com"),
        InstitutionItem("nrbc", "NRBC Bank PLC", "এনআরবিসি ব্যাংক পিএলসি", "NRBC", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF008080), "বেসরকারি", "ডিজিটাল ও উপশাখা ব্যাংকিং", "https://www.nrbcommercialbank.com"),
        InstitutionItem("sbac", "SBAC Bank PLC", "এসবিএসি ব্যাংক পিএলসি", "SBAC", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF4682B4), "বেসরকারি", "কৃষি ও বাণিজ্য অর্থায়ন", "https://www.sbacbank.com"),
        InstitutionItem("community", "Community Bank Bangladesh PLC", "কমিউনিটি ব্যাংক বাংলাদেশ পিএলসি", "Community", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF0B5345), "বেসরকারি", "পুলিশ কল্যাণ ও আধুনিক সেবা", "https://communitybankbd.com"),
        InstitutionItem("bengal", "Bengal Commercial Bank PLC", "বেঙ্গল কমার্শিয়াল ব্যাংক পিএলসি", "Bengal", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF7D6608), "বেসরকারি", "ব্যবসায়ী ও উদ্যোক্তা অর্থায়ন", "https://bgcb.com.bd"),
        InstitutionItem("citizens", "Citizens Bank PLC", "সিটিজেন্স ব্যাংক পিএলসি", "Citizens", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF512E5F), "বেসরকারি", "নতুন প্রজন্মের স্মার্ট ব্যাংকিং", "https://citizensbankbd.com"),
        InstitutionItem("midland", "Midland Bank PLC", "মিডল্যান্ড ব্যাংক পিএলসি", "Midland", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF1A5276), "বেসরকারি", "মিডল্যান্ড অনলাইন", "https://www.midlandbankbd.net"),
        InstitutionItem("modhumoti", "Modhumoti Bank PLC", "মধুমতি ব্যাংক পিএলসি", "Modhumoti", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF641E16), "বেসরকারি", "ডিজিটাল সেবা ও এসএমই", "https://www.modhumotibank.net"),
        InstitutionItem("meghna", "Meghna Bank PLC", "মেঘনা ব্যাংক পিএলসি", "Meghna", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF154360), "বেসরকারি", "পার্সোনাল ও বিজনেস ব্যাংকিং", "https://www.meghnabank.com.bd"),
        InstitutionItem("padma", "Padma Bank PLC", "পদ্মা ব্যাংক পিএলসি", "Padma", FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF78281F), "বেসরকারি", "বাণিজ্যিক ও এসএমই", "https://www.padmabankbd.com"),

        // 5. Foreign Commercial Banks in Bangladesh
        InstitutionItem("scb", "Standard Chartered Bangladesh", "স্ট্যান্ডার্ড চার্টার্ড বাংলাদেশ", "SCB", FinancialInstitutionType.BANK, BankClassification.FOREIGN, false, Color(0xFF009900), "বিদেশি ব্যাংক", "আন্তর্জাতিক ব্যাংকিং ও ক্রেডিট কার্ড", "https://www.sc.com/bd"),
        InstitutionItem("hsbc", "HSBC Bangladesh", "এইচএসবিসি বাংলাদেশ", "HSBC", FinancialInstitutionType.BANK, BankClassification.FOREIGN, false, Color(0xFFDB0011), "বিদেশি ব্যাংক", "গ্লোবাল ব্যাংকিং ও ট্রেড", "https://www.hsbc.com.bd"),
        InstitutionItem("ceylon", "Commercial Bank of Ceylon", "কমার্শিয়াল ব্যাংক অব সিলন", "CBC", FinancialInstitutionType.BANK, BankClassification.FOREIGN, false, Color(0xFF004380), "বিদেশি ব্যাংক", "আন্তর্জাতিক সেবা নেটওয়ার্ক", "https://www.combank.net/bd"),
        InstitutionItem("sbi", "State Bank of India (Bangladesh)", "স্টেট ব্যাংক অব ইন্ডিয়া", "SBI", FinancialInstitutionType.BANK, BankClassification.FOREIGN, false, Color(0xFF20B2AA), "বিদেশি ব্যাংক", "দ্বিপাক্ষিক বাণিজ্য ব্যাংকিং", "https://bd.statebank"),
        InstitutionItem("woori", "Woori Bank Bangladesh", "উরি ব্যাংক বাংলাদেশ", "Woori", FinancialInstitutionType.BANK, BankClassification.FOREIGN, false, Color(0xFF1E90FF), "বিদেশি ব্যাংক", "কোরিয়ান বৈশ্বিক ব্যাংকিং", "https://spot.wooribank.com/pot/Dream?withyou=en"),
        InstitutionItem("alfalah", "Bank Alfalah Limited", "ব্যাংক আলফালাহ", "Alfalah", FinancialInstitutionType.BANK, BankClassification.FOREIGN, true, Color(0xFF8B0000), "বিদেশি ব্যাংক", "ইসলামিক ও বাণিজ্যিক উইং", "https://www.bankalfalah.com/bd"),
        InstitutionItem("citi", "Citibank N.A. Bangladesh", "সিটিব্যাংক এন.এ.", "Citi", FinancialInstitutionType.BANK, BankClassification.FOREIGN, false, Color(0xFF003B70), "বিদেশি ব্যাংক", "আন্তর্জাতিক বিনিয়োগ ও করপোরেট", "https://www.citigroup.com")
    )

    val OTHER_LIST = listOf(
        InstitutionItem("cash", "Physical Cash Wallet", "নগদ টাকা (ক্যাশ)", "ক্যাশ", FinancialInstitutionType.CASH, BankClassification.OTHER, true, Color(0xFF0D9488), "নগদ", "হাতের নগদ বা পকেট ক্যাশ"),
        InstitutionItem("creditcard", "Credit Card (Liability)", "ক্রেডিট কার্ড (দায়)", "কার্ড", FinancialInstitutionType.CARD, BankClassification.OTHER, false, Color(0xFF6366F1), "দায় কার্ড", "ক্রেডিট লিমিট ও বকেয়া হিসাব"),
        InstitutionItem("visa", "Visa Card / Account", "ভিসা কার্ড অ্যাকাউন্ট", "Visa", FinancialInstitutionType.CARD, BankClassification.OTHER, false, Color(0xFF1A1F71), "কার্ড", "ডেবিট বা ক্রেডিট কার্ড"),
        InstitutionItem("mastercard", "MasterCard Account", "মাস্টারকার্ড অ্যাকাউন্ট", "MasterCard", FinancialInstitutionType.CARD, BankClassification.OTHER, false, Color(0xFFEB001B), "কার্ড", "আন্তর্জাতিক কার্ড সেবা"),
        InstitutionItem("paypal", "PayPal Balance", "পেপ্যাল ব্যালেন্স", "PayPal", FinancialInstitutionType.MFS, BankClassification.OTHER, false, Color(0xFF003087), "আন্তর্জাতিক ওয়ালেট", "ইউএসডি ও ফ্রিল্যান্স আয়"),
        InstitutionItem("binance", "Binance / Crypto Wallet", "ক্রিপ্টো ওয়ালেট", "Crypto", FinancialInstitutionType.CRYPTO, BankClassification.OTHER, false, Color(0xFFF3BA2F), "ডিজিটাল সম্পদ", "বিটকয়েন, ইথেরিয়াম, ইত্যাদি")
    )

    val ALL_INSTITUTIONS: List<InstitutionItem> = MFS_LIST + BANK_LIST + OTHER_LIST

    fun findById(id: String): InstitutionItem {
        return ALL_INSTITUTIONS.find { it.id.equals(id, ignoreCase = true) }
            ?: InstitutionItem("custom", id, id, id, FinancialInstitutionType.BANK, BankClassification.PRIVATE_CONVENTIONAL, false, Color(0xFF0D9488), "কাস্টম", "সাধারণ অ্যাকাউন্ট")
    }

    fun getBanksByClassification(classification: BankClassification): List<InstitutionItem> {
        return BANK_LIST.filter { it.classification == classification }
    }
}
