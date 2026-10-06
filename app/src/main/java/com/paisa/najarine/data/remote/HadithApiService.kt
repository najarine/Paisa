package com.paisa.najarine.data.remote

import com.paisa.najarine.data.repository.HadithItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class HadithApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val refreshCounter = AtomicInteger(0)

    // Comprehensive authentic authenticated Hadith collection for hourly rotation and instant refresh
    private val authenticHourlyHadiths = listOf(
        HadithItem(
            title = "দান-সাদাকাহ ধন-সম্পদ হ্রাস করে না",
            arabic = "مَا نَقَصَتْ صَدَقَةٌ مِنْ مَالٍ",
            translation = "Charity does not decrease wealth.",
            banglaTranslation = "দান-সাদাকাহ ধন-সম্পদ হ্রাস করে না।",
            narrator = "আবু হুরায়রা (রা.) হতে বর্ণিত",
            source = "সহীহ মুসলিম (২৫৮৮)",
            hadithNumber = "২৫৮৮",
            grade = "সহীহ",
            topic = "দান ও সাদাকাহ"
        ),
        HadithItem(
            title = "সত্যবাদী ও বিশ্বস্ত ব্যবসায়ী",
            arabic = "التَّاجِرُ الصَّدُوقُ الأَمِينُ مَعَ النَّبِيِّينَ وَالصِّدِّيقِينَ وَالشُّهَدَاءِ",
            translation = "The truthful, trustworthy merchant is with the Prophets, the truthful ones, and the martyrs.",
            banglaTranslation = "সত্যবাদী ও বিশ্বস্ত ব্যবসায়ী কিয়ামতের দিন নবী, সত্যনিষ্ঠ ও শহীদদের সাথে থাকবেন।",
            narrator = "আবু সাঈদ আল-খুদরী (রা.) হতে বর্ণিত",
            source = "জামে তিরমিযী (১২০৯)",
            hadithNumber = "১২০৯",
            grade = "সহীহ",
            topic = "হালাল ব্যবসা ও সততা"
        ),
        HadithItem(
            title = "অসচ্ছল ঋণগ্রস্ত ব্যক্তির ঋণ সহজ করা",
            arabic = "مَنْ يَسَّرَ عَلَى مُعْسِرٍ يَسَّرَ اللَّهُ عَلَيْهِ فِي الدُّنْيَا وَالآخِرَةِ",
            translation = "Whoever relieves a debtor who is in hardship, Allah will relieve him in this world and the Hereafter.",
            banglaTranslation = "যে ব্যক্তি কোনো ঋণগ্রস্ত বা অসচ্ছল ব্যক্তির ঋণ পরিশোধ সহজ করে দেয়, আল্লাহ দুনিয়া ও আখিরাতে তার সংকট সহজ করে দেবেন।",
            narrator = "আবু হুরায়রা (রা.) হতে বর্ণিত",
            source = "সহীহ মুসলিম (২৬৯৯)",
            hadithNumber = "২৬৯৯",
            grade = "সহীহ",
            topic = "ঋণ ও সহমর্মিতা"
        ),
        HadithItem(
            title = "সুদ (রিবা) এর সুস্পষ্ট নিষেধাজ্ঞা",
            arabic = "لَعَنَ رَسُولُ اللَّهِ صلى الله عليه وسلم آكِلَ الرِّبَا وَمُؤْكِلَهُ وَكَاتِبَهُ وَشَاهِدَيْهِ",
            translation = "The Messenger of Allah cursed the one who accepts usury, the one who pays it, the one who records it, and the two witnesses.",
            banglaTranslation = "রাসূলুল্লাহ (সা.) সুদ গ্রহণকারী, সুদ প্রদানকারী, সুদের হিসাব লেখক এবং সুদের উভয় সাক্ষীর ওপর অভিসম্পাত করেছেন।",
            narrator = "জাবির ইবনে আব্দুল্লাহ (রা.) হতে বর্ণিত",
            source = "সহীহ মুসলিম (১৫৯৮)",
            hadithNumber = "১৫৯৮",
            grade = "সহীহ",
            topic = "সুদমুক্ত অর্থনীতি"
        ),
        HadithItem(
            title = "শ্রমিকের ঘাম শুকানোর পূর্বেই পারিশ্রমিক",
            arabic = "أَعْطُوا الأَجِيرَ أَجْرَهُ قَبْلَ أَنْ يَجِفَّ عَرَقُهُ",
            translation = "Give the worker his wages before his sweat dries.",
            banglaTranslation = "শ্রমিকের ঘাম শুকানোর পূর্বেই তার পারিশ্রমিক পরিশোধ করে দাও।",
            narrator = "আব্দুল্লাহ ইবনে উমর (রা.) হতে বর্ণিত",
            source = "সুনান ইবনে মাজাহ (২৪৪৩)",
            hadithNumber = "২৪৪৩",
            grade = "সহীহ",
            topic = "শ্রমিকের অধিকার"
        ),
        HadithItem(
            title = "উত্তম উপার্জন হচ্ছে হাতের কাজ",
            arabic = "مَا أَكَلَ أَحَدٌ طَعَامًا قَطُّ خَيْرًا مِنْ أَنْ يَأْكُلَ مِنْ عَمَلِ يَدِهِ",
            translation = "Nobody has ever eaten a better meal than that which one has earned by working with one's own hands.",
            banglaTranslation = "নিজের হাতের উপার্জিত খাদ্যের চেয়ে উত্তম খাদ্য কেউ কখনো খায়নি।",
            narrator = "মিকদাম ইবনে মাদিকারিব (রা.) হতে বর্ণিত",
            source = "সহীহ বুখারী (২০৭২)",
            hadithNumber = "২০৭২",
            grade = "সহীহ",
            topic = "হালাল রিজিক ও পরিশ্রম"
        ),
        HadithItem(
            title = "মানুষের প্রতি কৃতজ্ঞতা",
            arabic = "مَنْ لاَ يَشْكُرُ النَّاسَ لاَ يَشْكُرُ اللَّهَ",
            translation = "Whoever does not thank people does not thank Allah.",
            banglaTranslation = "যে ব্যক্তি মানুষের কৃতজ্ঞতা প্রকাশ করে না, সে আল্লাহরও কৃতজ্ঞতা আদায় করে না।",
            narrator = "আবু হুরায়রা (রা.) হতে বর্ণিত",
            source = "সুনান আবু দাউদ (৪৮১১)",
            hadithNumber = "৪৮১১",
            grade = "সহীহ",
            topic = "সততা ও কৃতজ্ঞতা"
        ),
        HadithItem(
            title = "অনর্থক বিষয় ত্যাগ করা উত্তম ইসলামের লক্ষণ",
            arabic = "مِنْ حُسْنِ إِسْلاَمِ الْمَرْءِ تَرْكُهُ مَا لاَ يَعْنِيهِ",
            translation = "Part of the perfection of one's Islam is his leaving that which is of no concern to him.",
            banglaTranslation = "একজন ব্যক্তির ইসলামের অন্যতম সৌন্দর্য হলো এমন বিষয় বর্জন করা যা তার কোনো কল্যাণে আসে না।",
            narrator = "আলী ইবনে হুসাইন (রা.) হতে বর্ণিত",
            source = "জামে তিরমিযী (২৩১৭)",
            hadithNumber = "২৩১৭",
            grade = "সহীহ",
            topic = "আদর্শ চরিত্র"
        ),
        HadithItem(
            title = "লেনদেনে উদারতা ও ক্ষমার ফজিলত",
            arabic = "رَحِمَ اللَّهُ رَجُلاً سَمْحًا إِذَا بَاعَ، وَإِذَا اشْتَرَى، وَإِذَا اقْتَضَى",
            translation = "May Allah have mercy on a person who is lenient when selling, buying, and demanding what is due.",
            banglaTranslation = "আল্লাহ সেই ব্যক্তির প্রতি রহম করেন, যে বিক্রয়কালে, ক্রয়কালে এবং পাওনা তলবকালে নম্রতা ও উদারতা প্রদর্শন করে।",
            narrator = "জাবির ইবনে আব্দুল্লাহ (রা.) হতে বর্ণিত",
            source = "সহীহ বুখারী (২০৭৬)",
            hadithNumber = "২০৭৬",
            grade = "সহীহ",
            topic = "লেনদেনে শিষ্টাচার"
        ),
        HadithItem(
            title = "সম্পদ জমানোর চেয়ে আন্তরিক তৃপ্তিই প্রকৃত প্রাচুর্য",
            arabic = "لَيْسَ الْغِنَى عَنْ كَثْرَةِ الْعَرَضِ، وَلَكِنَّ الْغِنَى غِنَى النَّفْسِ",
            translation = "Richness is not in the abundance of worldly possessions, but richness is the richness of the soul.",
            banglaTranslation = "প্রচুর ধন-সম্পদ থাকলেই ধনী হওয়া যায় না; বরং অন্তরের আত্মতৃপ্তিই হলো প্রকৃত ধন ও প্রাচুর্য।",
            narrator = "আবু হুরায়রা (রা.) হতে বর্ণিত",
            source = "সহীহ বুখারী (৬৪৪৬)",
            hadithNumber = "৬৪৪৬",
            grade = "সহীহ",
            topic = "অন্তর তৃপ্তি ও যুহদ"
        ),
        HadithItem(
            title = "যে ধোঁকা দেয় সে আমাদের দলভুক্ত নয়",
            arabic = "مَنْ غَشَّنَا فَلَيْسَ مِنَّا",
            translation = "Whoever deceives us is not of us.",
            banglaTranslation = "যে ব্যক্তি আমাদের সাথে প্রতারণা বা ধোঁকাবাজি করে, সে আমাদের দলভুক্ত নয়।",
            narrator = "আবু হুরায়রা (রা.) হতে বর্ণিত",
            source = "সহীহ মুসলিম (১০২)",
            hadithNumber = "১০২",
            grade = "সহীহ",
            topic = "ব্যবসায় সততা"
        ),
        HadithItem(
            title = "উত্তম ব্যক্তি সে যার হাত ও মুখ থেকে অন্য মুসলিম নিরাপদ",
            arabic = "الْمُسْلِمُ مَنْ سَلِمَ الْمُسْلِمُونَ مِنْ لِسَانِهِ وَيَدِهِ",
            translation = "A Muslim is the one from whose tongue and hands the Muslims are safe.",
            banglaTranslation = "প্রকৃত মুসলিম সেই ব্যক্তি যার মুখ ও হাত থেকে অন্য মুসলিম নিরাপদ থাকে।",
            narrator = "আব্দুল্লাহ ইবনে আমর (রা.) হতে বর্ণিত",
            source = "সহীহ বুখারী (১০)",
            hadithNumber = "১০",
            grade = "সহীহ",
            topic = "উত্তম চরিত্র ও নিরাপত্তা"
        )
    )

    suspend fun fetchHourlyHadith(forceNext: Boolean = false): HadithItem = withContext(Dispatchers.IO) {
        val currentHour = (System.currentTimeMillis() / (1000 * 60 * 60)).toInt()
        val index = if (forceNext) {
            val step = refreshCounter.incrementAndGet()
            Math.floorMod(currentHour + step, authenticHourlyHadiths.size)
        } else {
            Math.floorMod(currentHour, authenticHourlyHadiths.size)
        }
        val fallback = authenticHourlyHadiths[index]

        // 1. Attempt dynamic live fetch from Ummah API / Hadith endpoint
        try {
            val request = Request.Builder()
                .url("https://ummahapi.com/api/hadith/hourly")
                .header("Accept", "application/json")
                .header("User-Agent", "Paisa-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val root = JSONObject(body)
                    val data = root.optJSONObject("data") ?: root
                    val ar = data.optString("arabic").ifBlank { data.optString("text_ar") }
                    val bn = data.optString("bangla").ifBlank { data.optString("translation_bn") }
                    val en = data.optString("english").ifBlank { data.optString("translation_en") }
                    val book = data.optString("source").ifBlank { data.optString("book") }
                    val num = data.optString("hadith_number").ifBlank { data.optString("number") }

                    if (ar.isNotBlank() || bn.isNotBlank() || en.isNotBlank()) {
                        return@withContext HadithItem(
                            title = data.optString("title", "উম্মা এপিআই দৈনিক হাদিস"),
                            arabic = if (ar.isNotBlank()) ar else fallback.arabic,
                            translation = if (en.isNotBlank()) en else fallback.translation,
                            banglaTranslation = if (bn.isNotBlank()) bn else fallback.banglaTranslation,
                            narrator = data.optString("narrator", fallback.narrator),
                            source = if (book.isNotBlank()) book else fallback.source,
                            hadithNumber = if (num.isNotBlank()) num else fallback.hadithNumber,
                            grade = data.optString("grade", "সহীহ"),
                            topic = data.optString("topic", "দৈনিক নসিহত ও অর্থনীতি")
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Network fallback to authentic rotation
        }

        fallback
    }

    fun getAllAuthenticHadiths(): List<HadithItem> = authenticHourlyHadiths
}
