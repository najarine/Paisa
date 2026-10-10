package com.paisa.najarine.notification

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.paisa.najarine.data.local.HourlyHadithEntity
import com.paisa.najarine.data.local.HourlyQuranEntity
import com.paisa.najarine.data.local.PaisaDatabase
import com.paisa.najarine.data.remote.HadithApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HourlyAyahItem(
    val surahNumber: Int,
    val surahNameArabic: String,
    val surahNameEnglish: String,
    val surahNameBangla: String,
    val ayahNumber: Int,
    val arabicText: String,
    val englishTranslation: String,
    val banglaTranslation: String
)

class HourlyIslamicSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Canonical rotating hourly Quran Ayahs with authentic translations
    private val canonicalHourlyAyahs = listOf(
        HourlyAyahItem(
            surahNumber = 2,
            surahNameArabic = "البقرة",
            surahNameEnglish = "Al-Baqarah",
            surahNameBangla = "আল-বাক্বারাহ",
            ayahNumber = 261,
            arabicText = "مَّثَلُ الَّذِينَ يُنفِقُونَ أَمْوَالَهُمْ فِي سَبِيلِ اللَّهِ كَمَثَلِ حَبَّةٍ أَنبَتَتْ سَبْعَ سَنَابِلَ فِي كُلِّ سُنبُلَةٍ مِّائَةُ حَبَّةٍ",
            englishTranslation = "The example of those who spend their wealth in the way of Allah is like a seed of grain which grows seven spikes; in each spike is a hundred grains.",
            banglaTranslation = "যারা আল্লাহর পথে নিজেদের ধন-সম্পদ ব্যয় করে, তাদের দৃষ্টান্ত একটি শস্যবীজের মতো, যা থেকে সাতটি শীষ উৎপন্ন হয় এবং প্রতিটি শীষে থাকে একশত দানা।"
        ),
        HourlyAyahItem(
            surahNumber = 65,
            surahNameArabic = "الطلاق",
            surahNameEnglish = "At-Talaq",
            surahNameBangla = "আত-ত্বালাক্ব",
            ayahNumber = 3,
            arabicText = "وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ ۚ وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ",
            englishTranslation = "And will provide for him from where he does not expect. And whoever relies upon Allah - then He is sufficient for him.",
            banglaTranslation = "এবং তিনি তাকে এমন উৎস থেকে রিজিক দান করবেন যা সে কল্পনাও করেনি। আর যে ব্যক্তি আল্লাহর ওপর তাওয়াক্কুল (ভরসা) করে, তার জন্য তিনিই যথেষ্ট।"
        ),
        HourlyAyahItem(
            surahNumber = 94,
            surahNameArabic = "الشرح",
            surahNameEnglish = "Ash-Sharh",
            surahNameBangla = "আল-ইনশিরাহ",
            ayahNumber = 6,
            arabicText = "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            englishTranslation = "Indeed, with hardship [will be] ease.",
            banglaTranslation = "নিশ্চয়ই কষ্টের সাথেই রয়েছে স্বস্তি ও সহজতা।"
        ),
        HourlyAyahItem(
            surahNumber = 14,
            surahNameArabic = "إبراهيم",
            surahNameEnglish = "Ibrahim",
            surahNameBangla = "ইব্রাহীম",
            ayahNumber = 7,
            arabicText = "لَئِن شَكَرْتُمْ لَأَزِيدَنَّكُمْ ۖ وَلَئِن كَفَرْتُمْ إِنَّ عَذَابِي لَشَدِيدٌ",
            englishTranslation = "If you are grateful, I will surely increase your favor; but if you deny, indeed, My punishment is severe.",
            banglaTranslation = "যদি তোমরা শুকরিয়া আদায় করো, তবে আমি অবশ্যই তোমাদের নেয়ামত বাড়িয়ে দেব; আর যদি অকৃতজ্ঞ হও, তবে নিশ্চয়ই আমার শাস্তি অত্যন্ত কঠোর।"
        ),
        HourlyAyahItem(
            surahNumber = 62,
            surahNameArabic = "الجمعة",
            surahNameEnglish = "Al-Jumu'ah",
            surahNameBangla = "আল-জুমুআহ",
            ayahNumber = 10,
            arabicText = "فَإِذَا قُضِيَتِ الصَّلَاةُ فَانتَشِرُوا فِي الْأَرْضِ وَابْتَغُوا مِن فَضْلِ اللَّهِ وَاذْكُرُوا اللَّهَ كَثِيرًا لَّعَلَّكُمْ تُفْلِحُونَ",
            englishTranslation = "And when the prayer has been concluded, disperse within the land and seek from the bounty of Allah, and remember Allah often that you may succeed.",
            banglaTranslation = "অতঃপর নামায সমাপ্ত হলে তোমরা জমিনে ছড়িয়ে পড়ো এবং আল্লাহর অনুগ্রহ (হালাল রিযিক) অনুসন্ধান করো এবং আল্লাহকে অধিক স্মরণ করো, যাতে তোমরা সফলকাম হও।"
        ),
        HourlyAyahItem(
            surahNumber = 3,
            surahNameArabic = "آل عمران",
            surahNameEnglish = "Ali 'Imran",
            surahNameBangla = "আলে ইমরান",
            ayahNumber = 134,
            arabicText = "الَّذِينَ يُنفِقُونَ فِي السَّرَّاءِ وَالضَّرَّاءِ وَالْكَاظِمِينَ الْغَيْظَ وَالْعَافِينَ عَنِ النَّاسِ ۗ وَاللَّهُ يُحِبُّ الْمُحْسِنِينَ",
            englishTranslation = "Who spend [in the cause of Allah] during ease and hardship and who restrain anger and who pardon the people - and Allah loves the doers of good.",
            banglaTranslation = "যারা সচ্ছল ও অসচ্ছল উভয় অবস্থাতেই ব্যয় করে এবং যারা রাগ দমন করে ও মানুষের ভুল ক্ষমা করে—আল্লাহ এমন সৎকর্মশীলদের ভালোবাসেন।"
        )
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = PaisaDatabase.getDatabase(applicationContext)
            val sharedPrefs = applicationContext.getSharedPreferences("paisa_notification_freq", Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()

            // User screen time & active diagnostics
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val isScreenActive = powerManager?.isInteractive ?: true
            val hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val isDaytimeHours = hourOfDay in 6..23

            // 1. Fetch & Store Hourly Hadith from UmmahAPI (Frequency: 1 hadith per 1 hour)
            val hadithService = HadithApiService()
            val hadithItem = hadithService.fetchHourlyHadith()
            val hadithEntity = HourlyHadithEntity(
                id = "latest_hourly_hadith",
                title = hadithItem.title,
                arabic = hadithItem.arabic,
                translation = hadithItem.translation,
                banglaTranslation = hadithItem.banglaTranslation,
                narrator = hadithItem.narrator,
                source = hadithItem.source,
                hadithNumber = hadithItem.hadithNumber,
                grade = hadithItem.grade,
                topic = hadithItem.topic,
                fetchedAtMillis = now
            )
            database.hourlyHadithDao().insertHadith(hadithEntity)

            // Deliver notification when enabled by user toggle
            val isHadithNotifEnabled = AdhanPreferences.isHourlyHadithNotificationEnabled(applicationContext)
            if (isHadithNotifEnabled) {
                PaisaNotificationManager.showNotification(
                    context = applicationContext,
                    channelId = PaisaNotificationManager.CHANNEL_QURAN_HADITH,
                    notificationId = 1001,
                    title = "ঘণ্টার নির্বাচিত হাদিস — ${hadithItem.title}",
                    body = "${hadithItem.banglaTranslation}\n(${hadithItem.source})",
                    targetTab = 3,
                    targetScreen = "HADITH"
                )
            }

            // 2. Quran Ayah (Frequency: 1 Ayah every 1 hour)
            val ayahItem = fetchLiveHourlyAyah()
            val quranEntity = HourlyQuranEntity(
                id = "latest_hourly_ayah",
                surahNumber = ayahItem.surahNumber,
                surahNameArabic = ayahItem.surahNameArabic,
                surahNameEnglish = ayahItem.surahNameEnglish,
                surahNameBangla = ayahItem.surahNameBangla,
                ayahNumber = ayahItem.ayahNumber,
                arabicText = ayahItem.arabicText,
                englishTranslation = ayahItem.englishTranslation,
                banglaTranslation = ayahItem.banglaTranslation,
                fetchedAtMillis = now
            )
            database.hourlyQuranDao().insertAyah(quranEntity)

            val isQuranNotifEnabled = AdhanPreferences.isHourlyQuranNotificationEnabled(applicationContext)
            if (isQuranNotifEnabled) {
                PaisaNotificationManager.showNotification(
                    context = applicationContext,
                    channelId = PaisaNotificationManager.CHANNEL_QURAN_HADITH,
                    notificationId = 1002,
                    title = "ঘণ্টার পবিত্র কুরআন আয়াত — সূরা ${ayahItem.surahNameBangla} (${ayahItem.surahNumber}:${ayahItem.ayahNumber})",
                    body = "${ayahItem.banglaTranslation}\n\"${ayahItem.arabicText}\"",
                    targetTab = 3,
                    targetScreen = "QURAN"
                )
                sharedPrefs.edit { putLong("last_ayah_notification_time", now) }
            }

            Log.d("HourlyIslamicSyncWorker", "Hourly Hadith (1 hr) & Quran Ayah (1 hr) successfully processed and notified.")
            Result.success()
        } catch (e: Exception) {
            Log.e("HourlyIslamicSyncWorker", "Error in HourlyIslamicSyncWorker: ${e.localizedMessage}", e)
            Result.retry()
        }
    }

    private fun fetchLiveHourlyAyah(): HourlyAyahItem {
        val currentHour = (System.currentTimeMillis() / (1000 * 60 * 60)).toInt()
        val fallback = canonicalHourlyAyahs[Math.floorMod(currentHour, canonicalHourlyAyahs.size)]

        return try {
            val request = Request.Builder()
                .url("https://api.alquran.cloud/v1/ayah/${fallback.surahNumber}:${fallback.ayahNumber}/editions/quran-uthmani,bn.bengali")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val root = JSONObject(body)
                    val data = root.optJSONArray("data")
                    if (data != null && data.length() >= 2) {
                        val arObj = data.getJSONObject(0)
                        val bnObj = data.getJSONObject(1)
                        val arText = arObj.optString("text")
                        val bnText = bnObj.optString("text")
                        if (arText.isNotBlank() && bnText.isNotBlank()) {
                            return fallback.copy(
                                arabicText = arText,
                                banglaTranslation = bnText
                            )
                        }
                    }
                }
            }
            fallback
        } catch (_: Exception) {
            fallback
        }
    }
}
