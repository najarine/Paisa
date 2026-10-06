package com.paisa.najarine.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class QuranSurahEditionsResponse(
    val code: Int,
    val status: String,
    val data: List<QuranSurahEditionData>
)

data class QuranSurahEditionData(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val numberOfAyahs: Int,
    val revelationType: String,
    val edition: QuranEditionInfo,
    val ayahs: List<QuranAyahApiItem>
)

data class QuranEditionInfo(
    val identifier: String, // "quran-uthmani" or "bn.bengali"
    val language: String,
    val name: String,
    val englishName: String,
    val format: String,
    val type: String
)

data class QuranAyahApiItem(
    val number: Int,
    val text: String,
    val numberInSurah: Int,
    val juz: Int = 1,
    val manzil: Int = 1,
    val page: Int = 1,
    val ruku: Int = 1,
    val hizbQuarter: Int = 1
)

interface QuranApiService {
    @GET("v1/surah/{surahNumber}/editions/quran-uthmani,bn.bengali")
    suspend fun getSurahAyahs(
        @Path("surahNumber") surahNumber: Int
    ): QuranSurahEditionsResponse

    companion object {
        private const val BASE_URL = "https://api.alquran.cloud/"

        fun create(): QuranApiService {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(QuranApiService::class.java)
        }
    }
}
