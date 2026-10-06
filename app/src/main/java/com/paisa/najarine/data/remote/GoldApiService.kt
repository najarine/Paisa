package com.paisa.najarine.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

data class GoldPriceResponse(
    val currency: String?,
    val price: Double, // Price per ounce in USD
    val price_gram_24k: Double? = null,
    val price_gram_22k: Double? = null,
    val price_gram_21k: Double? = null,
    val price_gram_18k: Double? = null,
    val updatedAt: String? = null
)

interface GoldApiService {
    @GET("price/XAU")
    suspend fun getGoldPrice(): GoldPriceResponse

    companion object {
        private const val BASE_URL = "https://api.gold-api.com/"

        fun create(): GoldApiService {
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
                .create(GoldApiService::class.java)
        }
    }
}
