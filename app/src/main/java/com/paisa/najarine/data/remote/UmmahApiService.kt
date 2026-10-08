package com.paisa.najarine.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class UmmahPrayerResponse(
    val code: Int?,
    val status: String?,
    val data: UmmahPrayerData?
)

data class UmmahPrayerData(
    val timings: Map<String, String>?,
    val date: UmmahDate?,
    val meta: UmmahMeta?
)

data class UmmahDate(
    val readable: String?,
    val timestamp: String?
)

data class UmmahMeta(
    val timezone: String?
)

data class UmmahZakatRequest(
    val gold_price_per_gram: Double = 9500.0,
    val silver_price_per_gram: Double = 110.0,
    val nisab_standard: String = "Silver",
    val cash_and_bank_savings: Double,
    val gold_owned_grams: Double,
    val silver_owned_grams: Double,
    val stocks_and_shares: Double,
    val business_trade_goods: Double,
    val other_investments: Double,
    val debts_and_liabilities: Double
)

data class UmmahZakatResponse(
    val code: Int?,
    val status: String?,
    val data: UmmahZakatData?
)

data class UmmahZakatData(
    val net_zakatable_wealth: Double?,
    val nisab_threshold: Double?,
    val is_nisab_reached: Boolean?,
    val zakat_payable: Double?,
    val nisab_standard_used: String?
)

data class UmmahDuaItem(
    val id: Int?,
    val title: String?,
    val category: String?,
    val arabic: String?,
    val transliteration: String?,
    val translation: String?,
    val reference: String?
)

data class UmmahDuasResponse(
    val success: Boolean?,
    val data: List<UmmahDuaItem>?
)

data class UmmahSingleDuaResponse(
    val success: Boolean?,
    val data: UmmahDuaItem?
)

data class UmmahDuaCategoriesResponse(
    val success: Boolean?,
    val data: List<String>?
)

data class UmmahHadithItem(
    val id: Int?,
    val collection: String?,
    val number: Int?,
    val arabic: String?,
    val translation: String?,
    val banglaTranslation: String?,
    val narrator: String?,
    val source: String?,
    val grade: String?,
    val topic: String?
)

data class UmmahHadithResponse(
    val success: Boolean?,
    val data: UmmahHadithItem?
)

interface UmmahApiService {
    @GET("v1/timingsByCity")
    suspend fun getTimingsByCity(
        @Query("city") city: String = "Dhaka",
        @Query("country") country: String = "Bangladesh",
        @Query("method") method: Int = 1,
        @Query("school") school: Int = 1
    ): UmmahPrayerResponse

    @GET("v1/timings")
    suspend fun getTimingsByCoordinates(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 1,
        @Query("school") school: Int = 1
    ): UmmahPrayerResponse

    @retrofit2.http.POST("v1/zakat/calculate")
    suspend fun calculateZakat(
        @retrofit2.http.Body request: UmmahZakatRequest
    ): UmmahZakatResponse

    @GET("api/duas/categories")
    suspend fun getDuaCategories(): UmmahDuaCategoriesResponse

    @GET("api/duas/category/{category}")
    suspend fun getDuasByCategory(
        @retrofit2.http.Path("category") category: String
    ): UmmahDuasResponse

    @GET("api/duas")
    suspend fun getAllDuas(): UmmahDuasResponse

    @GET("api/duas/random")
    suspend fun getRandomDua(): UmmahSingleDuaResponse

    @GET("api/hadith/{collection}/{number}")
    suspend fun getHadithByCollectionAndNumber(
        @retrofit2.http.Path("collection") collection: String,
        @retrofit2.http.Path("number") number: Int
    ): UmmahHadithResponse

    companion object {
        private const val BASE_URL = "https://ummahapi.com/"

        fun create(): UmmahApiService {
            val okClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(UmmahApiService::class.java)
        }
    }
}
