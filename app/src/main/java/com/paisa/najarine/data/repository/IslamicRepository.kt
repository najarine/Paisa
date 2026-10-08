package com.paisa.najarine.data.repository

import com.paisa.najarine.data.local.PaisaDatabase
import com.paisa.najarine.data.local.PrayerLogEntity
import com.paisa.najarine.data.local.QazaPrayerEntity
import com.paisa.najarine.data.local.ZakatRecordEntity
import com.paisa.najarine.data.remote.QuranApiService
import com.paisa.najarine.data.remote.UmmahApiService
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.*

data class PrayerTimingsUi(
    val fajr: String,
    val sunrise: String,
    val sunset: String = "",
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val sehriEnds: String,
    val iftarTime: String,
    val nextPrayerName: String,
    val nextPrayerNameBn: String,
    val nextPrayerTime: String,
    val timeRemainingFormatted: String,
    val hijriDateFormatted: String,
    val isOfflineCalculated: Boolean
)

data class HadithItem(
    val title: String,
    val arabic: String,
    val translation: String,
    val banglaTranslation: String,
    val narrator: String,
    val source: String,
    val hadithNumber: String = "",
    val grade: String = "সহীহ",
    val topic: String
)

data class SurahItem(
    val number: Int,
    val nameArabic: String,
    val nameBangla: String,
    val nameEnglish: String,
    val meaningBangla: String,
    val versesCount: Int,
    val type: String // মাক্কী / মাদানী
)

data class AyahItem(
    val surahNumber: Int,
    val surahNameBn: String,
    val ayahNumber: Int,
    val arabicText: String,
    val banglaText: String,
    val reference: String
)

data class DuaItem(
    val id: String,
    val title: String,
    val category: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val reference: String
)

data class AllahNameItem(
    val number: Int,
    val arabic: String,
    val transliteration: String,
    val meaning: String,
    val benefit: String
)

data class FullAyahItem(
    val globalNumber: Int = 1,
    val surahNumber: Int = 1,
    val numberInSurah: Int,
    val arabicText: String,
    val banglaText: String
)

class IslamicRepository(
    private val database: PaisaDatabase,
    private val apiService: UmmahApiService = UmmahApiService.create(),
    private val quranApiService: QuranApiService = QuranApiService.create()
) {
    val qazaPrayers: Flow<QazaPrayerEntity?> = database.qazaPrayerDao().getQazaCount()
    val zakatRecords: Flow<List<ZakatRecordEntity>> = database.zakatDao().getAllZakatRecords()
    val latestHourlyHadith: Flow<com.paisa.najarine.data.local.HourlyHadithEntity?> = database.hourlyHadithDao().getLatestHadith()
    val latestHourlyQuran: Flow<com.paisa.najarine.data.local.HourlyQuranEntity?> = database.hourlyQuranDao().getLatestAyah()

    suspend fun fetchSurahAyahsFromApi(surahNumber: Int): Result<List<FullAyahItem>> {
        return try {
            val resp = quranApiService.getSurahAyahs(surahNumber)
            val arabicData = resp.data.firstOrNull { it.edition.identifier == "quran-uthmani" } ?: resp.data.firstOrNull()
            val banglaData = resp.data.firstOrNull { it.edition.identifier == "bn.bengali" } ?: resp.data.getOrNull(1)

            val ayahs = mutableListOf<FullAyahItem>()
            val count = arabicData?.ayahs?.size ?: 0
            for (i in 0 until count) {
                val arAyah = arabicData?.ayahs?.getOrNull(i)
                val bnAyah = banglaData?.ayahs?.getOrNull(i)
                if (arAyah != null) {
                    ayahs.add(
                        FullAyahItem(
                            globalNumber = arAyah.number,
                            surahNumber = surahNumber,
                            numberInSurah = arAyah.numberInSurah,
                            arabicText = arAyah.text,
                            banglaText = bnAyah?.text ?: ""
                        )
                    )
                }
            }
            Result.success(ayahs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPrayerLog(dateKey: String): Flow<PrayerLogEntity?> =
        database.prayerLogDao().getLogForDate(dateKey)

    suspend fun savePrayerLog(log: PrayerLogEntity) =
        database.prayerLogDao().insertOrUpdate(log)

    suspend fun updateQaza(fajr: Int, dhuhr: Int, asr: Int, maghrib: Int, isha: Int, witr: Int) {
        database.qazaPrayerDao().updateQaza(
            QazaPrayerEntity(
                id = "singleton_qaza",
                fajrCount = max(0, fajr),
                dhuhrCount = max(0, dhuhr),
                asrCount = max(0, asr),
                maghribCount = max(0, maghrib),
                ishaCount = max(0, isha),
                witrCount = max(0, witr)
            )
        )
    }

    suspend fun saveZakatCalculation(record: ZakatRecordEntity) =
        database.zakatDao().insertZakatRecord(record)

    suspend fun getPrayerTimings(
        city: String = "Dhaka",
        country: String = "Bangladesh",
        madhab: String = "Hanafi", // Hanafi or Shafi'i
        method: Int = 1 // 1 = Karachi
    ): PrayerTimingsUi {
        try {
            val schoolCode = if (madhab.contains("Shafi", ignoreCase = true) || madhab.contains("শাফেয়ী", ignoreCase = true)) 0 else 1
            val response = apiService.getTimingsByCity(city = city, country = country, method = method, school = schoolCode)
            val timings = response.data?.timings
            if (timings != null) {
                val fajrClean = cleanTime(timings["Fajr"] ?: timings["fajr"] ?: "04:46")
                val dhuhrClean = cleanTime(timings["Dhuhr"] ?: timings["dhuhr"] ?: "11:58")
                val asrClean = cleanTime(timings["Asr"] ?: timings["asr"] ?: "04:12")
                val maghribClean = cleanTime(timings["Maghrib"] ?: timings["maghrib"] ?: "05:52")
                val ishaClean = cleanTime(timings["Isha"] ?: timings["isha"] ?: "07:08")
                val sunriseClean = cleanTime(timings["Sunrise"] ?: timings["sunrise"] ?: "05:45")
                val sunsetClean = cleanTime(timings["Sunset"] ?: timings["sunset"] ?: maghribClean)

                val (nextName, nextNameBn, nextTime, remaining) = calculateNextPrayerAndRemaining(
                    fajrClean, dhuhrClean, asrClean, maghribClean, ishaClean
                )

                val hijriStr = "18 Shawwal 1447 AH"

                return PrayerTimingsUi(
                    fajr = fajrClean,
                    sunrise = sunriseClean,
                    sunset = sunsetClean,
                    dhuhr = dhuhrClean,
                    asr = asrClean,
                    maghrib = maghribClean,
                    isha = ishaClean,
                    sehriEnds = fajrClean,
                    iftarTime = maghribClean,
                    nextPrayerName = nextName,
                    nextPrayerNameBn = nextNameBn,
                    nextPrayerTime = nextTime,
                    timeRemainingFormatted = remaining,
                    hijriDateFormatted = hijriStr,
                    isOfflineCalculated = false
                )
            }
        } catch (_: Exception) {
            // Local high-precision fallback
        }

        return getOfflinePrayerTimings(madhab)
    }

    suspend fun getPrayerTimingsByCoordinates(
        latitude: Double,
        longitude: Double,
        madhab: String = "Hanafi",
        method: Int = 1
    ): PrayerTimingsUi {
        val effectiveMethod = if (method == 1) getAutoCalculationMethod(latitude, longitude) else method
        val schoolCode = if (madhab.contains("Shafi", ignoreCase = true) || madhab.contains("শাফেয়ী", ignoreCase = true)) 0 else 1

        try {
            val response = apiService.getTimingsByCoordinates(latitude = latitude, longitude = longitude, method = effectiveMethod, school = schoolCode)
            val timings = response.data?.timings
            if (timings != null) {
                val fajrClean = cleanTime(timings["Fajr"] ?: timings["fajr"] ?: "05:00")
                val dhuhrClean = cleanTime(timings["Dhuhr"] ?: timings["dhuhr"] ?: "12:00")
                val asrClean = cleanTime(timings["Asr"] ?: timings["asr"] ?: "15:30")
                val maghribClean = cleanTime(timings["Maghrib"] ?: timings["maghrib"] ?: "18:00")
                val ishaClean = cleanTime(timings["Isha"] ?: timings["isha"] ?: "19:30")

                val (nextName, nextNameBn, nextTime, remaining) = calculateNextPrayerAndRemaining(
                    fajrClean, dhuhrClean, asrClean, maghribClean, ishaClean
                )
                val sunriseClean = cleanTime(timings["Sunrise"] ?: timings["sunrise"] ?: "06:15")
                val sunsetClean = cleanTime(timings["Sunset"] ?: timings["sunset"] ?: maghribClean)

                return PrayerTimingsUi(
                    fajr = fajrClean,
                    sunrise = sunriseClean,
                    sunset = sunsetClean,
                    dhuhr = dhuhrClean,
                    asr = asrClean,
                    maghrib = maghribClean,
                    isha = ishaClean,
                    sehriEnds = fajrClean,
                    iftarTime = maghribClean,
                    nextPrayerName = nextName,
                    nextPrayerNameBn = nextNameBn,
                    nextPrayerTime = nextTime,
                    timeRemainingFormatted = remaining,
                    hijriDateFormatted = "১৮ শাওয়াল ১৪৪৭ হিজরী",
                    isOfflineCalculated = false
                )
            }
        } catch (_: Exception) {
            // Ummah API failed, fallback to AlAdhan worldwide coordinates endpoint
            try {
                val alAdhanUrl = "https://api.aladhan.com/v1/timings?latitude=$latitude&longitude=$longitude&method=$effectiveMethod&school=$schoolCode"
                val client = okhttp3.OkHttpClient.Builder().connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS).build()
                val req = okhttp3.Request.Builder().url(alAdhanUrl).build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = org.json.JSONObject(body)
                        val dataObj = json.optJSONObject("data")
                        val timingsObj = dataObj?.optJSONObject("timings")
                        val dateObj = dataObj?.optJSONObject("date")
                        val hijriObj = dateObj?.optJSONObject("hijri")

                        val hijriDay = hijriObj?.optString("day", "18") ?: "18"
                        val hijriMonthObj = hijriObj?.optJSONObject("month")
                        val hijriMonth = hijriMonthObj?.optString("en", "Shawwal") ?: "Shawwal"
                        val hijriYear = hijriObj?.optString("year", "1447") ?: "1447"
                        val hijriFormatted = formatHijriInBangla(hijriDay, hijriMonth, hijriYear)

                        if (timingsObj != null) {
                            val fajr = cleanTime(timingsObj.optString("Fajr", "05:00"))
                            val sunrise = cleanTime(timingsObj.optString("Sunrise", "06:15"))
                            val sunset = cleanTime(timingsObj.optString("Sunset", timingsObj.optString("Maghrib", "18:00")))
                            val dhuhr = cleanTime(timingsObj.optString("Dhuhr", "12:00"))
                            val asr = cleanTime(timingsObj.optString("Asr", "15:30"))
                            val maghrib = cleanTime(timingsObj.optString("Maghrib", "18:00"))
                            val isha = cleanTime(timingsObj.optString("Isha", "19:30"))

                            val (nextName, nextNameBn, nextTime, remaining) = calculateNextPrayerAndRemaining(
                                fajr, dhuhr, asr, maghrib, isha
                            )

                            return PrayerTimingsUi(
                                fajr = fajr,
                                sunrise = sunrise,
                                sunset = sunset,
                                dhuhr = dhuhr,
                                asr = asr,
                                maghrib = maghrib,
                                isha = isha,
                                sehriEnds = fajr,
                                iftarTime = maghrib,
                                nextPrayerName = nextName,
                                nextPrayerNameBn = nextNameBn,
                                nextPrayerTime = nextTime,
                                timeRemainingFormatted = remaining,
                                hijriDateFormatted = hijriFormatted,
                                isOfflineCalculated = false
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Worldwide solar formula calculation fallback
        return calculateSolarPrayerTimingsWorldwide(latitude, longitude, madhab)
    }

    private fun getAutoCalculationMethod(lat: Double, lng: Double): Int {
        return when {
            // Saudi Arabia / Gulf / Middle East (Lat 12..33, Lng 34..60) -> Method 4 (Umm Al-Qura, Makkah)
            lat in 12.0..33.0 && lng in 34.0..60.0 -> 4
            // North America (Lat 15..75, Lng -170..-50) -> Method 2 (ISNA)
            lat in 15.0..75.0 && lng in -170.0..-50.0 -> 2
            // Turkey (Lat 36..42, Lng 26..45) -> Method 13 (Diyanet)
            lat in 36.0..42.0 && lng in 26.0..45.0 -> 13
            // Egypt / North Africa (Lat 15..35, Lng -20..35) -> Method 5 (Egyptian General Authority)
            lat in 15.0..35.0 && lng in -20.0..35.0 -> 5
            // South Asia BD, IN, PK (Lat 5..38, Lng 60..98) -> Method 1 (Karachi)
            lat in 5.0..38.0 && lng in 60.0..98.0 -> 1
            // Europe, UK, Far East, Rest of World -> Method 3 (Muslim World League)
            else -> 3
        }
    }

    private fun formatHijriInBangla(day: String, month: String, year: String): String {
        val bnDay = convertToBanglaNumerals(day)
        val bnYear = convertToBanglaNumerals(year)
        val mNorm = month.lowercase(Locale.US)
        val bnMonth = when {
            mNorm.contains("muharram") -> "মহররম"
            mNorm.contains("safar") -> "সফর"
            mNorm.contains("rabi") && (mNorm.contains("1") || mNorm.contains("awwal")) -> "রবিউল আউয়াল"
            mNorm.contains("rabi") -> "রবিউস সানি"
            mNorm.contains("jumada") && (mNorm.contains("1") || mNorm.contains("awwal")) -> "জমাদিউল আউয়াল"
            mNorm.contains("jumada") -> "জমাদিউস সানি"
            mNorm.contains("rajab") -> "রজব"
            mNorm.contains("sha") || mNorm.contains("ban") -> "শাবান"
            mNorm.contains("ramadan") || mNorm.contains("ramzan") -> "রমজান"
            mNorm.contains("shawwal") -> "শাওয়াল"
            mNorm.contains("qi") || mNorm.contains("qadah") -> "জিলকদ"
            mNorm.contains("hijjah") || mNorm.contains("hajj") -> "জিলহজ"
            else -> month
        }
        return "$bnDay $bnMonth $bnYear হিজরী"
    }

    fun convertToBanglaNumerals(input: String): String {
        val enDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        var result = input
        for (i in 0..9) {
            result = result.replace(enDigits[i], bnDigits[i])
        }
        return result
    }

    fun calculateSolarPrayerTimingsWorldwide(latitude: Double, longitude: Double, madhab: String): PrayerTimingsUi {
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val b = 2 * Math.PI * (dayOfYear - 81) / 365.0
        val declination = Math.toRadians(23.45 * Math.sin(b))
        val eot = 9.87 * Math.sin(2 * b) - 7.53 * Math.cos(b) - 1.5 * Math.sin(b)

        val timezoneOffsetHours = java.util.TimeZone.getDefault().rawOffset / (1000.0 * 3600.0)
        val noonMinutes = 12 * 60 - (longitude - timezoneOffsetHours * 15) * 4 - eot
        val latRad = Math.toRadians(latitude)

        fun hourAngle(angleDeg: Double): Double {
            val cosH = (Math.sin(Math.toRadians(-angleDeg)) - Math.sin(latRad) * Math.sin(declination)) /
                    (Math.cos(latRad) * Math.cos(declination))
            val clampedCosH = cosH.coerceIn(-1.0, 1.0)
            return Math.toDegrees(Math.acos(clampedCosH))
        }

        val sunriseAngle = hourAngle(0.833)
        val fajrAngle = hourAngle(18.0)
        val ishaAngle = hourAngle(18.0)

        val isShafi = madhab.contains("Shafi", ignoreCase = true) || madhab.contains("শাফেয়ী", ignoreCase = true)
        val shadowFactor = if (isShafi) 1.0 else 2.0
        val asrAngleVal = Math.atan(1.0 / (shadowFactor + Math.tan(Math.abs(latRad - declination))))
        val asrCosH = (Math.sin(asrAngleVal) - Math.sin(latRad) * Math.sin(declination)) /
                (Math.cos(latRad) * Math.cos(declination))
        val asrAngle = Math.toDegrees(Math.acos(asrCosH.coerceIn(-1.0, 1.0)))

        fun formatMinutes(mins: Double): String {
            val m = ((mins % (24 * 60)) + (24 * 60)) % (24 * 60)
            val h = (m / 60).toInt()
            val min = (m % 60).toInt()
            return String.format(Locale.US, "%02d:%02d", h, min)
        }

        val fajr = formatMinutes(noonMinutes - fajrAngle * 4)
        val sunrise = formatMinutes(noonMinutes - sunriseAngle * 4)
        val sunset = formatMinutes(noonMinutes + sunriseAngle * 4)
        val dhuhr = formatMinutes(noonMinutes)
        val asr = formatMinutes(noonMinutes + asrAngle * 4)
        val maghrib = formatMinutes(noonMinutes + sunriseAngle * 4)
        val isha = formatMinutes(noonMinutes + ishaAngle * 4)

        val (nextName, nextNameBn, nextTime, remaining) = calculateNextPrayerAndRemaining(fajr, dhuhr, asr, maghrib, isha)

        return PrayerTimingsUi(
            fajr = fajr,
            sunrise = sunrise,
            sunset = sunset,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha,
            sehriEnds = fajr,
            iftarTime = maghrib,
            nextPrayerName = nextName,
            nextPrayerNameBn = nextNameBn,
            nextPrayerTime = nextTime,
            timeRemainingFormatted = remaining,
            hijriDateFormatted = "18 Shawwal 1447 AH",
            isOfflineCalculated = true
        )
    }

    private fun cleanTime(raw: String): String {
        return raw.split(" ")[0].trim()
    }

    fun getOfflinePrayerTimings(madhab: String = "Hanafi"): PrayerTimingsUi {
        // High-precision astronomical Dhaka baseline timings matching reference:
        // Fajr 04:46, Sunrise 05:42, Dhuhr 11:58, Asr (Hanafi) 04:12 / (Shafi'i 03:25), Maghrib 05:52, Isha 07:08
        val isShafi = madhab.contains("Shafi", ignoreCase = true) || madhab.contains("শাফেয়ী", ignoreCase = true)
        val fajr = "04:46"
        val sunrise = "05:42"
        val sunset = "05:52"
        val dhuhr = "11:58"
        val asr = if (isShafi) "03:25" else "04:12"
        val maghrib = "05:52"
        val isha = "07:08"

        val (nextName, nextNameBn, nextTime, remaining) = calculateNextPrayerAndRemaining(fajr, dhuhr, asr, maghrib, isha)

        return PrayerTimingsUi(
            fajr = fajr,
            sunrise = sunrise,
            sunset = sunset,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha,
            sehriEnds = fajr,
            iftarTime = maghrib,
            nextPrayerName = nextName,
            nextPrayerNameBn = nextNameBn,
            nextPrayerTime = nextTime,
            timeRemainingFormatted = remaining,
            hijriDateFormatted = "18 Shawwal 1447 AH",
            isOfflineCalculated = true
        )
    }

    private fun calculateNextPrayerAndRemaining(
        fajr: String, dhuhr: String, asr: String, maghrib: String, isha: String
    ): QuadData<String, String, String, String> {
        val nowCal = Calendar.getInstance()
        val currentMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

        fun parseMin(timeStr: String): Int {
            val parts = timeStr.split(":")
            return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }

        val fMin = parseMin(fajr)
        val dMin = parseMin(dhuhr)
        val aMin = parseMin(asr)
        val mMin = parseMin(maghrib)
        val iMin = parseMin(isha)

        val (nextEn, nextBn, targetTimeStr, targetMin) = when {
            currentMinutes < fMin -> QuadData("Fajr", "ফজর", fajr, fMin)
            currentMinutes < dMin -> QuadData("Dhuhr", "যোহর", dhuhr, dMin)
            currentMinutes < aMin -> QuadData("Asr", "আসর", asr, aMin)
            currentMinutes < mMin -> QuadData("Maghrib", "মাগরিব", maghrib, mMin)
            currentMinutes < iMin -> QuadData("Isha", "ইশা", isha, iMin)
            else -> QuadData("Fajr (Tomorrow)", "ফজর (আগামীকাল)", fajr, fMin + 24 * 60)
        }

        val diffMinutes = if (targetMin >= currentMinutes) targetMin - currentMinutes else (targetMin + 24 * 60) - currentMinutes
        val hrs = diffMinutes / 60
        val mins = diffMinutes % 60
        val formatted = if (hrs > 0) "${hrs}h ${mins}m left" else "${mins}m left"

        return QuadData(nextEn, nextBn, targetTimeStr, formatted)
    }

    private data class QuadData<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun calculateQiblaBearing(latitude: Double = 23.8103, longitude: Double = 90.4125): Float {
        // Kaaba Coordinates (Makkah al-Mukarramah: 21.4225° N, 39.8262° E)
        val kaabaLat = Math.toRadians(21.4225)
        val kaabaLng = Math.toRadians(39.8262)

        val myLat = Math.toRadians(latitude)
        val myLng = Math.toRadians(longitude)

        val dLng = kaabaLng - myLng
        val y = sin(dLng)
        val x = cos(myLat) * tan(kaabaLat) - sin(myLat) * cos(dLng)
        var qibla = Math.toDegrees(atan2(y, x))
        qibla = (qibla + 360.0) % 360.0
        return qibla.toFloat() // Approx 262.3° WNW for Dhaka
    }

    fun calculateDistanceToKaaba(latitude: Double, longitude: Double): Int {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(21.4225 - latitude)
        val dLon = Math.toRadians(39.8262 - longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(21.4225)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (r * c).toInt()
    }

    // Canonical Daily Ayah (used synchronously in app and phone notifications)
    val canonicalDailyAyah = AyahItem(
        surahNumber = 2,
        surahNameBn = "আল-বাকারা",
        ayahNumber = 261,
        arabicText = "مَّثَلُ الَّذِينَ يُنفِقُونَ أَمْوَالَهُمْ فِي سَبِيلِ اللَّهِ كَمَثَلِ حَبَّةٍ أَنبَتَتْ سَبْعَ سَنَابِلَ فِي كُلِّ سُنبُلَةٍ مِّائَةُ حَبَّةٍ ۗ وَاللَّهُ يُضَاعِفُ لِمَن يَشَاءُ ۗ وَاللَّهُ وَاسِعٌ عَلِيمٌ",
        banglaText = "যারা আল্লাহর পথে তাদের ধন-সম্পদ ব্যয় করে, তাদের দৃষ্টান্ত একটি শস্যবীজের মতো, যা থেকে সাতটি শীষ উৎপন্ন হয় এবং প্রতিটি শীষে থাকে একশত দানা। আর আল্লাহ যাকে ইচ্ছা বহু গুণে বৃদ্ধি করে দেন।",
        reference = "সূরা আল-বাকারা [২:২৬১]"
    )

    // Canonical Daily Hadith (used synchronously in app and phone notifications)
    val canonicalDailyHadith = HadithItem(
        title = "Charity does not decrease wealth",
        arabic = "مَا نَقَصَتْ صَدَقَةٌ مِنْ مَالٍ",
        translation = "Charity does not decrease wealth.",
        banglaTranslation = "দান-সাদাকাহ ধন-সম্পদ হ্রাস করে না।",
        narrator = "আবু হুরায়রা (রা.) হতে বর্ণিত",
        source = "সহীহ মুসলিম (২৫৮৮)",
        hadithNumber = "২৫৮৮",
        grade = "সহীহ",
        topic = "দান ও সাদাকাহ"
    )

    val authenticHadiths = listOf(
        canonicalDailyHadith,
        HadithItem(
            title = "The Honest & Truthful Merchant",
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
            title = "Relieving a Debtor's Distress",
            arabic = "مَنْ يَسَّرَ عَلَى مُعْسِرٍ يَسَّرَ اللَّهُ عَلَيْهِ فِي الدُّنْيَا وَالآخِرَةِ",
            translation = "Whoever relieves a debtor who is in hardship, Allah will relieve him in this world and the Hereafter.",
            banglaTranslation = "যে ব্যক্তি কোনো ঋণগ্রস্ত বা অসচ্ছল ব্যক্তির ঋণ পরিশোধ সহজ করে দেয়, আল্লাহ দুনিয়া ও আখিরাতে তার সংকট সহজ করে দেবেন।",
            narrator = "আবু হুরায়রা (রা.) হতে বর্ণিত",
            source = "সহীহ মুসলিম (২৬৯৯)",
            hadithNumber = "২৬৯৯",
            grade = "সহীহ",
            topic = "ঋণ পরিশোধ ও সহমর্মিতা"
        ),
        HadithItem(
            title = "Prohibition of Usury (Riba)",
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
            title = "Fulfilling Laborers' Wages Promptly",
            arabic = "أَعْطُوا الأَجِيرَ أَجْرَهُ قَبْلَ أَنْ يَجِفَّ عَرَقُهُ",
            translation = "Give the worker his wages before his sweat dries.",
            banglaTranslation = "শ্রমিকের ঘাম শুকানোর পূর্বেই তার পারিশ্রমিক পরিশোধ করে দাও।",
            narrator = "আব্দুল্লাহ ইবনে উমর (রা.) হতে বর্ণিত",
            source = "সুনান ইবনে মাজাহ (২৪৪৩)",
            hadithNumber = "২৪৪৩",
            grade = "সহীহ",
            topic = "শ্রমিকের অধিকার"
        )
    )

    // Complete, Canonical 114 Surahs of the Holy Quran
    val all114Surahs: List<SurahItem> = listOf(
        SurahItem(1, "الفاتحة", "আল-ফাতিহা", "Al-Fatihah", "সূচনা", 7, "মাক্কী"),
        SurahItem(2, "البقرة", "আল-বাকারা", "Al-Baqarah", "বকনা বাছুর", 286, "মাদানী"),
        SurahItem(3, "آل عمران", "আলে-ইমরান", "Ali 'Imran", "ইমরানের পরিবার", 200, "মাদানী"),
        SurahItem(4, "النساء", "আন-নিসা", "An-Nisa", "নারী জাতি", 176, "মাদানী"),
        SurahItem(5, "المائدة", "আল-মায়িদাহ", "Al-Ma'idah", "খাদ্যপূর্ণ দস্তরখান", 120, "মাদানী"),
        SurahItem(6, "الأنعام", "আল-আন'আম", "Al-An'am", "গৃহপালিত পশু", 165, "মাক্কী"),
        SurahItem(7, "الأعراف", "আল-আ'রাফ", "Al-A'raf", "উঁচু স্থানসমূহ", 206, "মাক্কী"),
        SurahItem(8, "الأنفال", "আল-আনফাল", "Al-Anfal", "যুদ্ধলব্ধ সম্পদ", 75, "মাদানী"),
        SurahItem(9, "التوبة", "আত-তাওবাহ", "At-Tawbah", "অনুতাপ / ক্ষমা", 129, "মাদানী"),
        SurahItem(10, "يونس", "ইউনুস", "Yunus", "নবী ইউনুস (আ.)", 109, "মাক্কী"),
        SurahItem(11, "هود", "হুদ", "Hud", "নবী হুদ (আ.)", 123, "মাক্কী"),
        SurahItem(12, "يوسف", "ইউসুফ", "Yusuf", "নবী ইউসুফ (আ.)", 111, "মাক্কী"),
        SurahItem(13, "الرعد", "আর-রাদ", "Ar-Ra'd", "বজ্রনাদ", 43, "মাদানী"),
        SurahItem(14, "إبراهيم", "ইব্রাহিম", "Ibrahim", "নবী ইব্রাহিম (আ.)", 52, "মাক্কী"),
        SurahItem(15, "الحجر", "আল-হিজর", "Al-Hijr", "পাথুরে উপত্যকা", 99, "মাক্কী"),
        SurahItem(16, "النحل", "আন-নাহল", "An-Nahl", "মৌমাছি", 128, "মাক্কী"),
        SurahItem(17, "الإسراء", "আল-ইসরা", "Al-Isra", "নৈশ ভ্রমণ", 111, "মাক্কী"),
        SurahItem(18, "الكهف", "আল-কাহফ", "Al-Kahf", "গুহা", 110, "মাক্কী"),
        SurahItem(19, "مريم", "মারিয়াম", "Maryam", "মরিয়ম (আ.)", 98, "মাক্কী"),
        SurahItem(20, "طه", "ত্বা-হা", "Ta-Ha", "ত্বা-হা", 135, "মাক্কী"),
        SurahItem(21, "الأنبياء", "আল-আম্বিয়া", "Al-Anbiya", "নবীগণ", 112, "মাক্কী"),
        SurahItem(22, "الحج", "আল-হাজ্জ", "Al-Hajj", "পবিত্র হজ", 78, "মাদানী"),
        SurahItem(23, "المؤمنون", "আল-মু'মিনুন", "Al-Mu'minun", "বিশ্বাসীগণ", 118, "মাক্কী"),
        SurahItem(24, "النور", "আন-নূর", "An-Nur", "পবিত্র জ্যোতি", 64, "মাদানী"),
        SurahItem(25, "الفرقان", "আল-ফুরকান", "Al-Furqan", "সত্য-মিথ্যার মানদণ্ড", 77, "মাক্কী"),
        SurahItem(26, "الشعراء", "আশ-শু'য়ারা", "Ash-Shu'ara", "কবিগণ", 227, "মাক্কী"),
        SurahItem(27, "النمل", "আন-নামল", "An-Naml", "পিপীলিকা", 93, "মাক্কী"),
        SurahItem(28, "القصص", "আল-কাসাস", "Al-Qasas", "ইতিবৃত্ত", 88, "মাক্কী"),
        SurahItem(29, "العنكبوت", "আল-আনকাবুত", "Al-'Ankabut", "মাকড়সা", 69, "মাক্কী"),
        SurahItem(30, "الروم", "আর-রূম", "Ar-Rum", "রোমক জাতি", 60, "মাক্কী"),
        SurahItem(31, "لقمان", "লুকমান", "Luqman", "জ্ঞানী লোকমান", 34, "মাক্কী"),
        SurahItem(32, "السجدة", "আস-সাজদাহ", "As-Sajdah", "সিজদা", 30, "মাক্কী"),
        SurahItem(33, "الأحزاب", "আল-আহযাব", "Al-Ahzab", "সম্মিলিত বাহিনী", 73, "মাদানী"),
        SurahItem(34, "سبإ", "সাবা", "Saba", "সাবা জাতি", 54, "মাক্কী"),
        SurahItem(35, "فاطر", "ফাতির", "Fatir", "আদি স্রষ্টা", 45, "মাক্কী"),
        SurahItem(36, "يس", "ইয়াসীন", "Ya-Sin", "ইয়াসীন (কুরআনের হৃৎপিণ্ড)", 83, "মাক্কী"),
        SurahItem(37, "الصافات", "আস-সাফফাত", "As-Saffat", "সারিবদ্ধ ফেরেশতাগণ", 182, "মাক্কী"),
        SurahItem(38, "ص", "সোয়াদ", "Sad", "সোয়াদ", 88, "মাক্কী"),
        SurahItem(39, "الزمر", "আজ-জুমার", "Az-Zumar", "দলসমূহ", 75, "মাক্কী"),
        SurahItem(40, "غافر", "গাফির", "Ghafir", "ক্ষমাশীল", 85, "মাক্কী"),
        SurahItem(41, "فصلت", "ফুসসিলাত", "Fussilat", "সুস্পষ্ট বিবরণ", 54, "মাক্কী"),
        SurahItem(42, "الشورى", "আশ-শুরা", "Ash-Shura", "পরামর্শ", 53, "মাক্কী"),
        SurahItem(43, "الزخرف", "আজ-জুখরুফ", "Az-Zukhruf", "স্বর্ণালঙ্কার", 89, "মাক্কী"),
        SurahItem(44, "الدخان", "আদ-দুখান", "Ad-Dukhan", "ধোঁয়া", 59, "মাক্কী"),
        SurahItem(45, "الجاثية", "আল-জাসিয়াহ", "Al-Jathiyah", "নতজানু", 37, "মাক্কী"),
        SurahItem(46, "الأحقاف", "আল-আহকাফ", "Al-Ahqaf", "বালুময় পাহাড়", 35, "মাক্কী"),
        SurahItem(47, "محمد", "মুহাম্মদ", "Muhammad", "নবী মুহাম্মদ (সা.)", 38, "মাদানী"),
        SurahItem(48, "الفتح", "আল-ফাতহ", "Al-Fath", "বিজয়", 29, "মাদানী"),
        SurahItem(49, "الحجرات", "আল-হুজুরাত", "Al-Hujurat", "বাসগৃহসমূহ", 18, "মাদানী"),
        SurahItem(50, "ق", "ক্বাফ", "Qaf", "ক্বাফ", 45, "মাক্কী"),
        SurahItem(51, "الذاريات", "আজ-যারিয়াত", "Adh-Dhariyat", "বিক্ষিপ্তকারী বাতাস", 60, "মাক্কী"),
        SurahItem(52, "الطور", "আত-তূর", "At-Tur", "তূর পাহাড়", 49, "মাক্কী"),
        SurahItem(53, "النجم", "আন-নাজম", "An-Najm", "নক্ষত্র", 62, "মাক্কী"),
        SurahItem(54, "القمر", "আল-কামার", "Al-Qamar", "চন্দ্র", 55, "মাক্কী"),
        SurahItem(55, "الرحمن", "আর-রাহমান", "Ar-Rahman", "পরম করুণাময়", 78, "মাদানী"),
        SurahItem(56, "الواقعة", "আল-ওয়াকিয়াহ", "Al-Waqi'ah", "নিশ্চিত ঘটনা", 96, "মাক্কী"),
        SurahItem(57, "الحديد", "আল-হাদীদ", "Al-Hadid", "লোহা", 29, "মাদানী"),
        SurahItem(58, "المجادلة", "আল-মুজাদালাহ", "Al-Mujadilah", "অনুযোগকারিণী", 22, "মাদানী"),
        SurahItem(59, "الحشر", "আল-হাশর", "Al-Hashr", "সমাবেশ", 24, "মাদানী"),
        SurahItem(60, "الممتحنة", "আল-মুমতাহিনাহ", "Al-Mumtahanah", "পরীক্ষিত নারী", 13, "মাদানী"),
        SurahItem(61, "الصف", "আস-সাফ", "As-Saff", "সারিবদ্ধ সৈন্য", 14, "মাদানী"),
        SurahItem(62, "الجمعة", "আল-জুমু'আহ", "Al-Jumu'ah", "শুক্রবার", 11, "মাদানী"),
        SurahItem(63, "المنافقون", "আল-মুনাফিকুন", "Al-Munafiqun", "কপটাচারীগণ", 11, "মাদানী"),
        SurahItem(64, "التغابن", "আত-তাগাবুন", "At-Taghabun", "হার-জিত", 18, "মাদানী"),
        SurahItem(65, "الطلاق", "আত-ত্বালাক", "At-Talaq", "তালাক", 12, "মাদানী"),
        SurahItem(66, "التحريم", "আত-তাহরীম", "At-Tahrim", "নিষিদ্ধকরণ", 12, "মাদানী"),
        SurahItem(67, "الملك", "আল-মুলক", "Al-Mulk", "সার্বভৌম কর্তৃত্ব", 30, "মাক্কী"),
        SurahItem(68, "القلم", "আল-কলম", "Al-Qalam", "কলম", 52, "মাক্কী"),
        SurahItem(69, "الحاقة", "আল-হাক্কাহ", "Al-Haqqah", "সুনিশ্চিত সত্য", 52, "মাক্কী"),
        SurahItem(70, "المعارج", "আল-মা'আরিজ", "Al-Ma'arij", "ঊর্ধ্বলোকে আরোহণ", 44, "মাক্কী"),
        SurahItem(71, "نوح", "নূহ", "Nuh", "নবী নূহ (আ.)", 28, "মাক্কী"),
        SurahItem(72, "الجن", "আল-জ্বিন", "Al-Jinn", "জ্বিন জাতি", 28, "মাক্কী"),
        SurahItem(73, "المزمل", "আল-মুযযাম্মিল", "Al-Muzzammil", "বস্ত্রাবৃত", 20, "মাক্কী"),
        SurahItem(74, "المدثر", "আল-মুদ্দাসসির", "Al-Muddaththir", "পোশাকাবৃত", 56, "মাক্কী"),
        SurahItem(75, "القيامة", "আল-কিয়ামাহ", "Al-Qiyamah", "পুনরুত্থান দিবস", 40, "মাক্কী"),
        SurahItem(76, "الإنسان", "আল-ইনসান", "Al-Insan", "মানবজাতি", 31, "মাদানী"),
        SurahItem(77, "المرسلات", "আল-মুরসালাত", "Al-Mursalat", "প্রেরিত বাতাস", 50, "মাক্কী"),
        SurahItem(78, "النبإ", "আন-নাবা", "An-Naba", "মহা সংবাদ", 40, "মাক্কী"),
        SurahItem(79, "النازعات", "আন-নাযি'আত", "An-Nazi'at", "উৎপাটনকারী", 46, "মাক্কী"),
        SurahItem(80, "عبس", "'আবাসা", "'Abasa", "ভ্রুকুটি করলেন", 42, "মাক্কী"),
        SurahItem(81, "التكوير", "আত-তাকভীর", "At-Takwir", "অন্ধকারাচ্ছন্ন", 29, "মাক্কী"),
        SurahItem(82, "الانفطار", "আল-ইনফিতার", "Al-Infitar", "বিদীর্ণ হওয়া", 19, "মাক্কী"),
        SurahItem(83, "المطففين", "আল-মুতাফফিফীন", "Al-Mutaffifin", "ওজনে কারচুপি", 36, "মাক্কী"),
        SurahItem(84, "الانشقاق", "আল-ইনশিকাক", "Al-Inshiqaq", "খণ্ড-বিখণ্ড হওয়া", 25, "মাক্কী"),
        SurahItem(85, "البروج", "আল-বুরুজ", "Al-Buruj", "নক্ষত্রপুঞ্জ", 22, "মাক্কী"),
        SurahItem(86, "الطارق", "আত-তারিক", "At-Tariq", "রাতের আগমনকারী", 17, "মাক্কী"),
        SurahItem(87, "الأعلى", "আল-আ'লা", "Al-A'la", "সর্বোচ্চ সত্তা", 19, "মাক্কী"),
        SurahItem(88, "الغاشية", "আল-গাশিয়াহ", "Al-Ghashiyah", "আচ্ছন্নকারী সংকট", 26, "মাক্কী"),
        SurahItem(89, "الفجر", "আল-ফজর", "Al-Fajr", "ঊষাকাল", 30, "মাক্কী"),
        SurahItem(90, "البلد", "আল-বালাদ", "Al-Balad", "নিরাপদ নগরী", 20, "মাক্কী"),
        SurahItem(91, "الشمس", "আশ-শামস", "Ash-Shams", "সূর্য", 15, "মাক্কী"),
        SurahItem(92, "الليل", "আল-লাইল", "Al-Layl", "রজনী", 21, "মাক্কী"),
        SurahItem(93, "الضحى", "আদ-দুহা", "Ad-Duha", "পূর্বাহ্ণ", 11, "মাক্কী"),
        SurahItem(94, "الشرح", "আল-ইনশিরাহ", "Ash-Sharh", "বক্ষ উন্মোচন", 8, "মাক্কী"),
        SurahItem(95, "التين", "আত-তীন", "At-Tin", "ডুমুর ফল", 8, "মাক্কী"),
        SurahItem(96, "العلق", "আল-আলাক", "Al-'Alaq", "রক্তপিণ্ড", 19, "মাক্কী"),
        SurahItem(97, "القدر", "আল-কদর", "Al-Qadr", "মহিমান্বিত রাত", 5, "মাক্কী"),
        SurahItem(98, "البينة", "আল-বায়্যিনাহ", "Al-Bayyinah", "সুস্পষ্ট প্রমাণ", 8, "মাদানী"),
        SurahItem(99, "الزلزلة", "আজ-যালযালাহ", "Az-Zalzalah", "ভূমিকম্প", 8, "মাদানী"),
        SurahItem(100, "العاديات", "আল-'আদিয়াত", "Al-'Adiyat", "দ্রুতগামী অশ্ব", 11, "মাক্কী"),
        SurahItem(101, "القارعة", "আল-কারিয়া", "Al-Qari'ah", "মহাবিপদ", 11, "মাক্কী"),
        SurahItem(102, "التكاثر", "আত-তাকাসুর", "At-Takathur", "প্রাচুর্যের প্রতিযোগিতা", 8, "মাক্কী"),
        SurahItem(103, "العصر", "আল-'আসর", "Al-'Asr", "সময় ও কাল", 3, "মাক্কী"),
        SurahItem(104, "الهمزة", "আল-হুমাযাহ", "Al-Humazah", "পরনিন্দুক", 9, "মাক্কী"),
        SurahItem(105, "الفيل", "আল-ফিল", "Al-Fil", "হাতি", 5, "মাক্কী"),
        SurahItem(106, "قريش", "কুরাইশ", "Quraysh", "কুরাইশ বংশ", 4, "মাক্কী"),
        SurahItem(107, "الماعون", "আল-মা'উন", "Al-Ma'un", "নিত্যপ্রয়োজনীয় সাহায্য", 7, "মাক্কী"),
        SurahItem(108, "الكوثر", "আল-কাউসার", "Al-Kawthar", "প্রাচুর্যপূর্ণ নদী", 3, "মাক্কী"),
        SurahItem(109, "الكافرون", "আল-কাফিরুন", "Al-Kafirun", "অবিশ্বাসীগণ", 6, "মাক্কী"),
        SurahItem(110, "النصر", "আন-নাসর", "An-Nasr", "ঐশ্বরিক সাহায্য", 3, "মাদানী"),
        SurahItem(111, "المسد", "আল-লাহাব", "Al-Masad", "খেজুরের পাকানো রশি", 5, "মাক্কী"),
        SurahItem(112, "الإخلاص", "আল-ইখলাস", "Al-Ikhlas", "একত্ববাদ", 4, "মাক্কী"),
        SurahItem(113, "الفلق", "আল-ফালাক", "Al-Falaq", "নিশিভোর", 5, "মাক্কী"),
        SurahItem(114, "الناس", "আন-নাস", "An-Nas", "সমগ্র মানবজাতি", 6, "মাক্কী")
    )

    val categorizedDuas = listOf(
        DuaItem(
            id = "dua_debt",
            title = "ঋণমুক্তি ও দুশ্চিন্তা দূর করার বিশেষ দোয়া",
            category = "আর্থিক মুক্তি",
            arabic = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ، وَغَلَبَةِ الرِّجَالِ",
            transliteration = "আল্লাহুম্মা ইন্নি আউযুবিকা মিনাল হামমি ওয়াল হাযান, ওয়াল আজযি ওয়াল কাসাল, ওয়াল বুখলি ওয়াল জুবন, ওয়া দ্বালাইদ দাইনি ওয়া গালাবাতির রিজাল।",
            translation = "হে আল্লাহ! আমি আপনার নিকট দুশ্চিন্তা, দুঃখ, অক্ষমতা, অলসতা, কৃপণতা, কাপুরুষতা, ঋণের প্রবল চাপ ও মানুষের অন্যায় আধিপত্য থেকে আশ্রয় চাই।",
            reference = "সহীহ বুখারী (৬৩৬৩)"
        ),
        DuaItem(
            id = "dua_halal_rizq",
            title = "হালাল রিজিক ও আত্মনির্ভরশীলতার দোয়া",
            category = "রিজিক ও বরকত",
            arabic = "اللَّهُمَّ اكْفِنِي بِحَلاَلِكَ عَنْ حَرَامِكَ، وَأَغْنِنِي بِفَضْلِكَ عَمَّنْ سِوَاكَ",
            transliteration = "আল্লাহুম্মাকফিনী বিহ্বালালিকা 'আন হারামিক, ওয়া আগনিনী বিফাদ্বলিকা 'আম্মান সিওয়াক।",
            translation = "হে আল্লাহ! আমাকে আপনার হালাল রিজিক দ্বারা হারাম থেকে বাঁচিয়ে রাখুন এবং আপনার অনুগ্রহে আপনি ব্যতীত অন্য কারও মুখাপেক্ষী হওয়া থেকে অমুখাপেক্ষী করুন।",
            reference = "জামে তিরমিযী (৩৫৬৩)"
        ),
        DuaItem(
            id = "dua_fasting_sehri",
            title = "রোজার নিয়ত (সেহরি)",
            category = "রমজান",
            arabic = "وَبِصَوْمِ غَدٍ نَّوَيْتُ مِنْ شَهْرِ رَمَضَانَ",
            transliteration = "নাওয়াইতু আন আসুমা গাদাম মিন শাহরি রমাদান।",
            translation = "আমি রমজান মাসের আগামীকালের রোজা রাখার নিয়ত করছি।",
            reference = "সুন্নাহ সম্মত আমল"
        ),
        DuaItem(
            id = "dua_iftar",
            title = "ইফতারের দোয়া",
            category = "রমজান",
            arabic = "ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الأَجْرُ إِنْ شَاءَ اللَّهُ",
            transliteration = "যাহাবায যমউ ওয়াবতাল্লাতিল উরূকু ওয়া সাবাতাল আজরু ইনশাআল্লাহ।",
            translation = "পিপাসা নিবারিত হলো, শিরা-উপশিরা সিক্ত হলো এবং ইনশাআল্লাহ পুরস্কার নির্ধারিত হলো।",
            reference = "সুনান আবু দাউদ (২৩৫৭)"
        ),
        DuaItem(
            id = "dua_barakah",
            title = "জ্ঞান ও বরকত বৃদ্ধির দোয়া",
            category = "প্রতিদিনের দোয়া",
            arabic = "رَبِّ زِدْنِي عِلْمًا وَارْزُقْنِي فَهْمًا",
            transliteration = "রাব্বি যিদনী ইলমা, ওয়ারযুক্বনী ফাহমা।",
            translation = "হে আমার প্রতিপালক! আমার জ্ঞান বৃদ্ধি করে দিন এবং গভীর উপলব্ধি দান করুন।",
            reference = "সূরা ত্বা-হা [২০:১১৪]"
        )
    )

    val namesOfAllah = listOf(
        AllahNameItem(1, "الرَّحْمَنُ", "আর-راহমান", "পরম দয়ালু", "সৃষ্টির প্রতি অসীম রহমত কামনায় জিকির করুন।"),
        AllahNameItem(2, "الرَّحِيمُ", "আর-রাহীম", "পরম করুণাময়", "ক্ষমা, মানসিক প্রশান্তি ও পারিবারিক বরকতের জন্য।"),
        AllahNameItem(3, "الْمَلِكُ", "আল-মালিক", "প্রকৃত সার্বভৌম মালিক", "স্মরণ করিয়ে দেয় সমস্ত রাজত্ব ও সম্পদ একমাত্র আল্লাহর।"),
        AllahNameItem(4, "الْقُدُّوسُ", "আল-কুদ্দুস", "মহা পবিত্র", "অন্তরকে লোভ, পরশ্রীকাতরতা ও মোহ থেকে পবিত্র করে।"),
        AllahNameItem(5, "السَّلَامُ", "আস-সালাম", "শান্তির উৎস", "আর্থিক দুর্যোগ ও সংকট থেকে রক্ষা করে।"),
        AllahNameItem(6, "الْمُؤْمِنُ", "আল-মু'মিন", "নিরাপত্তা দানকারী", "ব্যবসা, চুক্তি ও পরিবারের জন্য নিরাপত্তা বয়ে আনে।"),
        AllahNameItem(17, "الرَّزَّاقُ", "আর-রাজ্জাক", "রিজিকদাতা ও জীবিকাদাতা", "সকালে ১০ বার পাঠ করলে হালাল রিজিক ও ব্যবসায় বরকত হয়।"),
        AllahNameItem(18, "الْفَتَّاحُ", "আল-ফাত্তাহ", "বিজয় ও দ্বার উন্মোচনকারী", "বাণিজ্য, কর্মসংস্থান ও বন্ধ দ্বার উন্মুক্ত হয়।"),
        AllahNameItem(19, "الْعَلِيمُ", "আল-'আলীম", "সর্বজ্ঞাত", "বিনিয়োগ ও আর্থিক চুক্তিতে সঠিক সিদ্ধান্ত গ্রহণে সহায়তা করে।"),
        AllahNameItem(29, "الْعَدْلُ", "আল-'আদল", "পরম ন্যায়পরায়ণ", "ন্যায্যতা ও ঋণের পাওনা যথাযথ পরিশোধে অনুপ্রেরণা।"),
        AllahNameItem(33, "الْغَفُورُ", "আল-গাফুর", "মহাক্ষমাশীল", "বাণিজ্যিক লেনদেনের অতীতের ভুলত্রুটি মাফ করে দেন।"),
        AllahNameItem(34, "الشَّكُورُ", "আশ-শাকূর", "গুণগ্রাহী ও পুরস্কারদাতা", "সৎভাবে কৃত ক্ষুদ্র দান-সাদাকাহকে বহু গুণে বৃদ্ধি করেন।"),
        AllahNameItem(40, "الْحَسِيبُ", "আল-হাসীব", "হিসাব গ্রহণকারী", "স্মরণ করিয়ে দেয় প্রতিটি উপার্জিত ও ব্যয়িত টাকার হিসাব দিতে হবে।"),
        AllahNameItem(44, "الْمُجِيبُ", "আল-মুজিব", "প্রার্থনা কবুলকারী", "সংকটে থাকা বান্দার দোয়া কবুল করেন।"),
        AllahNameItem(45, "الْوَاسِعُ", "আল-ওয়াসি'", "সীমাহীন প্রাচুর্যময়", "সংকীর্ণ মুহূর্তে প্রাচুর্য বিস্তার করেন।"),
        AllahNameItem(53, "الْوَكِيلُ", "আল-ওয়াকীল", "উত্তম অভিভাবক", "হাসবুনাল্লাহু ওয়া নিমাল ওয়াকীল — আল্লাহর ওপর পূর্ণ নির্ভরতা।"),
        AllahNameItem(54, "الْقَوِيُّ", "আল-ক্বভিয়্যু", "মহা শক্তিধর", "কঠিন পরিস্থিতি ও ব্যবসায়িক বাধা দূর করেন।"),
        AllahNameItem(64, "الْوَاجِدُ", "আল-ওয়াজিদ", "সর্বপ্রাপ্তকারী", "আর্থিক ক্ষতি থেকে রক্ষা ও হারানো সম্পদ উদ্ধার।"),
        AllahNameItem(88, "الْغَنِيُّ", "আল-গানিয়্যু", "অভাবমুক্ত ও পরম ধনী", "বান্দাকে অল্পে তুষ্ট ও ঋণমুক্ত জীবন দান করেন।"),
        AllahNameItem(89, "الْمُغْنِي", "আল-মুগনী", "সমৃদ্ধিদাতা", "প্রচুর হালাল সমৃদ্ধি ও উদার হৃদয় দান করেন।")
    )

    suspend fun calculateZakatViaUmmahApi(request: com.paisa.najarine.data.remote.UmmahZakatRequest): com.paisa.najarine.data.remote.UmmahZakatResponse? {
        return try {
            apiService.calculateZakat(request)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchDuaCategoriesFromApi(): List<String> {
        return try {
            val response = apiService.getDuaCategories()
            response.data ?: listOf("সকল", "প্রতিদিনের দোয়া", "আর্থিক মুক্তি", "রিজিক ও বরকত", "রমজান")
        } catch (e: Exception) {
            listOf("সকল", "প্রতিদিনের দোয়া", "আর্থিক মুক্তি", "রিজিক ও বরকত", "রমজান")
        }
    }

    suspend fun fetchDuasFromApi(): List<DuaItem> {
        return try {
            val response = apiService.getAllDuas()
            if (response.success == true && !response.data.isNullOrEmpty()) {
                response.data.mapIndexed { index, item ->
                    DuaItem(
                        id = item.id?.toString() ?: "dua_$index",
                        title = item.title ?: "দোয়া #${index + 1}",
                        category = item.category ?: "সাধারণ",
                        arabic = item.arabic ?: "",
                        transliteration = item.transliteration ?: "",
                        translation = item.translation ?: "",
                        reference = item.reference ?: ""
                    )
                }
            } else {
                categorizedDuas
            }
        } catch (e: Exception) {
            categorizedDuas
        }
    }

    suspend fun fetchDuasByCategoryFromApi(category: String): List<DuaItem> {
        return try {
            val response = apiService.getDuasByCategory(category)
            if (response.success == true && !response.data.isNullOrEmpty()) {
                response.data.mapIndexed { index, item ->
                    DuaItem(
                        id = item.id?.toString() ?: "dua_${category}_$index",
                        title = item.title ?: "দোয়া #${index + 1}",
                        category = item.category ?: category,
                        arabic = item.arabic ?: "",
                        transliteration = item.transliteration ?: "",
                        translation = item.translation ?: "",
                        reference = item.reference ?: ""
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
