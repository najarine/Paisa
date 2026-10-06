package com.paisa.najarine.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CryptoRate(
    val symbol: String,
    val name: String,
    val priceUsd: Double,
    val priceBdt: Double
)

class CryptoRateService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Default USD to BDT baseline rate
    var usdToBdtRate: Double = 122.5
        private set

    suspend fun fetchLiveCryptoRates(): Map<String, CryptoRate> = withContext(Dispatchers.IO) {
        val rates = mutableMapOf<String, CryptoRate>()

        // 1. Try Binance public API (Free, fast, no auth required)
        try {
            val symbols = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT")
            for (sym in symbols) {
                val req = Request.Builder()
                    .url("https://api.binance.com/api/v3/ticker/price?symbol=$sym")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)
                        val price = json.optDouble("price", 0.0)
                        val coin = when (sym) {
                            "BTCUSDT" -> Pair("BTC", "Bitcoin")
                            "ETHUSDT" -> Pair("ETH", "Ethereum")
                            "SOLUSDT" -> Pair("SOL", "Solana")
                            "BNBUSDT" -> Pair("BNB", "BNB")
                            "XRPUSDT" -> Pair("XRP", "Ripple")
                            else -> Pair("USDT", "Tether")
                        }
                        rates[coin.first] = CryptoRate(
                            symbol = coin.first,
                            name = coin.second,
                            priceUsd = price,
                            priceBdt = price * usdToBdtRate
                        )
                    }
                }
            }
            rates["USDT"] = CryptoRate("USDT", "Tether USD", 1.0, usdToBdtRate)
        } catch (_: Exception) {}

        // 2. If Binance failed, fallback to CoinGecko public API
        if (rates.isEmpty()) {
            try {
                val url = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,tether,solana,binancecoin&vs_currencies=usd,bdt"
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)

                        val btc = json.optJSONObject("bitcoin")
                        if (btc != null) {
                            val usd = btc.optDouble("usd", 95000.0)
                            val bdt = btc.optDouble("bdt", usd * 122.5)
                            rates["BTC"] = CryptoRate("BTC", "Bitcoin", usd, bdt)
                        }

                        val eth = json.optJSONObject("ethereum")
                        if (eth != null) {
                            val usd = eth.optDouble("usd", 2700.0)
                            val bdt = eth.optDouble("bdt", usd * 122.5)
                            rates["ETH"] = CryptoRate("ETH", "Ethereum", usd, bdt)
                        }

                        val usdt = json.optJSONObject("tether")
                        if (usdt != null) {
                            rates["USDT"] = CryptoRate("USDT", "Tether USD", 1.0, 122.5)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Guaranteed reliable fallback baseline if completely offline
        if (rates.isEmpty()) {
            rates["BTC"] = CryptoRate("BTC", "Bitcoin", 96500.0, 96500.0 * 122.5)
            rates["ETH"] = CryptoRate("ETH", "Ethereum", 2750.0, 2750.0 * 122.5)
            rates["USDT"] = CryptoRate("USDT", "Tether USD", 1.0, 122.5)
            rates["SOL"] = CryptoRate("SOL", "Solana", 185.0, 185.0 * 122.5)
            rates["BNB"] = CryptoRate("BNB", "BNB Chain", 640.0, 640.0 * 122.5)
        }

        rates
    }
}
