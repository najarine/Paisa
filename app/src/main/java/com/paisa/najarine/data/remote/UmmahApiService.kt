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
