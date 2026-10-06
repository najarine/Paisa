package com.paisa.najarine.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AlAdhanResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: AlAdhanData?
)

@JsonClass(generateAdapter = true)
data class AlAdhanData(
    @Json(name = "timings") val timings: TimingsData?,
    @Json(name = "date") val date: DateData?,
    @Json(name = "meta") val meta: MetaData?
)

@JsonClass(generateAdapter = true)
data class TimingsData(
    @Json(name = "Fajr") val fajr: String?,
    @Json(name = "Sunrise") val sunrise: String?,
    @Json(name = "Dhuhr") val dhuhr: String?,
    @Json(name = "Asr") val asr: String?,
    @Json(name = "Sunset") val sunset: String?,
    @Json(name = "Maghrib") val maghrib: String?,
    @Json(name = "Isha") val isha: String?,
    @Json(name = "Imsak") val imsak: String?,
    @Json(name = "Midnight") val midnight: String?,
    @Json(name = "Firstthird") val firstthird: String?,
    @Json(name = "Lastthird") val lastthird: String?
)

@JsonClass(generateAdapter = true)
data class DateData(
    @Json(name = "readable") val readable: String?,
    @Json(name = "timestamp") val timestamp: String?,
    @Json(name = "hijri") val hijri: HijriDate?
)

@JsonClass(generateAdapter = true)
data class HijriDate(
    @Json(name = "date") val date: String?,
    @Json(name = "day") val day: String?,
    @Json(name = "month") val month: HijriMonth?,
    @Json(name = "year") val year: String?
)

@JsonClass(generateAdapter = true)
data class HijriMonth(
    @Json(name = "number") val number: Int?,
    @Json(name = "en") val en: String?,
    @Json(name = "ar") val ar: String?
)

@JsonClass(generateAdapter = true)
data class MetaData(
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "timezone") val timezone: String?,
    @Json(name = "method") val method: MethodData?
)

@JsonClass(generateAdapter = true)
data class MethodData(
    @Json(name = "id") val id: Int?,
    @Json(name = "name") val name: String?
)
