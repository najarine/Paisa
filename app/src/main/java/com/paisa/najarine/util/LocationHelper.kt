package com.paisa.najarine.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class UserLocationInfo(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val countryName: String,
    val displayName: String
)

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): UserLocationInfo? = withContext(Dispatchers.IO) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        val location: Location? = try {
            suspendCancellableCoroutine { continuation ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            continuation.resume(loc)
                        } else {
                            // Fallback to last known location
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                                continuation.resume(lastLoc)
                            }.addOnFailureListener {
                                continuation.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }

                continuation.invokeOnCancellation {
                    cts.cancel()
                }
            }
        } catch (_: Exception) {
            // Fallback to system LocationManager if Google Play Services fails
            getSystemLocation(context)
        } ?: getSystemLocation(context)

        if (location == null) return@withContext null

        val lat = location.latitude
        val lon = location.longitude
        val (rawCity, rawCountry) = resolveCityAndCountry(context, lat, lon)

        val city = translateCityToBangla(rawCity)
        val country = translateCountryToBangla(rawCountry)

        val displayName = when {
            city.isNotBlank() && country.isNotBlank() -> "$city, $country"
            city.isNotBlank() -> city
            country.isNotBlank() -> country
            else -> String.format(Locale.US, "%.2f°, %.2f°", lat, lon)
        }

        UserLocationInfo(
            latitude = lat,
            longitude = lon,
            cityName = city.ifBlank { rawCity.ifBlank { "Unknown" } },
            countryName = country.ifBlank { rawCountry.ifBlank { "Worldwide" } },
            displayName = displayName
        )
    }

    private fun translateCountryToBangla(rawCountry: String): String {
        if (rawCountry.isBlank()) return ""
        val normalized = rawCountry.trim().lowercase(Locale.US)
        return when {
            normalized.contains("bangladesh") || normalized.contains("বাংলাদেশ") -> "বাংলাদেশ"
            normalized.contains("saudi") || normalized.contains("makkah") || normalized.contains("madinah") -> "সৌদি আরব"
            normalized.contains("emirates") || normalized.contains("uae") || normalized.contains("dubai") -> "সংযুক্ত আরব আমিরাত"
            normalized.contains("united states") || normalized.contains("usa") || normalized.contains("america") -> "যুক্তরাষ্ট্র"
            normalized.contains("united kingdom") || normalized.contains("uk") || normalized.contains("england") || normalized.contains("britain") -> "যুক্তরাজ্য"
            normalized.contains("india") || normalized.contains("ভারত") -> "ভারত"
            normalized.contains("pakistan") -> "পাকিস্তান"
            normalized.contains("malaysia") -> "মালয়েশিয়া"
            normalized.contains("singapore") -> "সিঙ্গাপুর"
            normalized.contains("turkey") || normalized.contains("türkiye") -> "তুরস্ক"
            normalized.contains("qatar") -> "কাতার"
            normalized.contains("oman") -> "ওমান"
            normalized.contains("kuwait") -> "কুয়েত"
            normalized.contains("japan") -> "জাপান"
            normalized.contains("canada") -> "কানাডা"
            normalized.contains("australia") -> "অস্ট্রেলিয়া"
            normalized.contains("germany") -> "জার্মানি"
            normalized.contains("france") -> "ফ্রান্স"
            normalized.contains("italy") -> "ইতালি"
            normalized.contains("egypt") -> "মিশর"
            normalized.contains("indonesia") -> "ইন্দোনেশিয়া"
            normalized.contains("china") -> "চীন"
            normalized.contains("south korea") || normalized.contains("korea") -> "দক্ষিণ কোরিয়া"
            normalized.contains("russia") -> "রাশিয়া"
            normalized.contains("spain") -> "স্পেন"
            else -> rawCountry
        }
    }

    private fun translateCityToBangla(rawCity: String): String {
        if (rawCity.isBlank()) return ""
        val normalized = rawCity.trim().lowercase(Locale.US)
        return when {
            normalized == "dhaka" -> "ঢাকা"
            normalized == "chittagong" || normalized == "chattogram" -> "চট্টগ্রাম"
            normalized == "sylhet" -> "সিলেট"
            normalized == "rajshahi" -> "রাজশাহী"
            normalized == "khulna" -> "খুলনা"
            normalized == "barisal" || normalized == "barishal" -> "বরিশাল"
            normalized == "rangpur" -> "রংপুর"
            normalized == "comilla" || normalized == "cumilla" -> "কুমিল্লা"
            normalized == "makkah" || normalized == "mecca" -> "মক্কা শরিফ"
            normalized == "madinah" || normalized == "medina" -> "মদীনা শরিফ"
            normalized == "riyadh" -> "রিয়াদ"
            normalized == "jeddah" -> "জিদ্দা"
            normalized == "dubai" -> "দুবাই"
            normalized == "abu dhabi" -> "আবু ধাবি"
            normalized == "london" -> "লন্ডন"
            normalized == "new york" -> "নিউ ইয়র্ক"
            normalized == "tokyo" -> "টোকিও"
            normalized == "kuala lumpur" -> "কুয়ালালামপুর"
            normalized == "toronto" -> "টরন্টো"
            normalized == "sydney" -> "সিডনি"
            normalized == "istanbul" -> "ইস্তাম্বুল"
            normalized == "doha" -> "দোহা"
            normalized == "muscat" -> "মাস্কাট"
            normalized == "cairo" -> "কায়রো"
            else -> rawCity
        }
    }

    @SuppressLint("MissingPermission")
    private fun getSystemLocation(context: Context): Location? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = lm.getProviders(true)
            var bestLoc: Location? = null
            for (provider in providers) {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                if (bestLoc == null || loc.accuracy < bestLoc.accuracy) {
                    bestLoc = loc
                }
            }
            bestLoc
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveCityAndCountry(context: Context, lat: Double, lon: Double): Pair<String, String> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: ""
            val country = addr?.countryName ?: ""
            Pair(city, country)
        } catch (_: Exception) {
            Pair("", "")
        }
    }
}
