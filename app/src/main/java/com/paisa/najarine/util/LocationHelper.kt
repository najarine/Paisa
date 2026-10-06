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
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
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
        val (city, country) = resolveCityAndCountry(context, lat, lon)

        val displayName = when {
            city.isNotBlank() && country.isNotBlank() -> "$city, $country"
            city.isNotBlank() -> city
            country.isNotBlank() -> country
            else -> String.format(Locale.US, "%.4f, %.4f", lat, lon)
        }

        UserLocationInfo(
            latitude = lat,
            longitude = lon,
            cityName = city.ifBlank { "Unknown" },
            countryName = country.ifBlank { "Worldwide" },
            displayName = displayName
        )
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

    private fun resolveCityAndCountry(context: Context, lat: Double, lon: Double): Pair<String, String> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Async geocoder supported
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addr = addresses?.firstOrNull()
                val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: ""
                val country = addr?.countryName ?: ""
                Pair(city, country)
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addr = addresses?.firstOrNull()
                val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: ""
                val country = addr?.countryName ?: ""
                Pair(city, country)
            }
        } catch (_: Exception) {
            Pair("", "")
        }
    }
}
