package com.paisa.najarine.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AdMobManager {
    private const val TAG = "AdMobManager"
    const val AD_UNIT_ID = "ca-app-pub-5571621417978572/3802447896"
    const val BANNER_AD_UNIT_ID = "ca-app-pub-5571621417978572/1238461366"
    const val MEDIATION_AD_UNIT_ID = "ca-app-pub-5571621417978572/1238461366"

    // Anti-Abuse & AdMob Account Protection Limits
    const val MAX_DAILY_ADS = 4
    const val COOLDOWN_DURATION_SECONDS = 180L // 3 minutes cooldown between ads

    private const val PREFS_NAME = "paisa_admob_frequency_prefs"
    private const val KEY_LAST_AD_TIMESTAMP = "last_rewarded_ad_timestamp"
    private const val KEY_DAILY_AD_COUNT = "daily_rewarded_ad_count"
    private const val KEY_DAILY_DATE = "daily_rewarded_ad_date"

    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _adStatusMessage = MutableStateFlow<String?>(null)
    val adStatusMessage: StateFlow<String?> = _adStatusMessage.asStateFlow()

    private val _isLoadingAd = MutableStateFlow(false)
    val isLoadingAd: StateFlow<Boolean> = _isLoadingAd.asStateFlow()

    private val _cooldownRemainingSeconds = MutableStateFlow(0L)
    val cooldownRemainingSeconds: StateFlow<Long> = _cooldownRemainingSeconds.asStateFlow()

    private val _todayAdCount = MutableStateFlow(0)
    val todayAdCount: StateFlow<Int> = _todayAdCount.asStateFlow()

    private val _canWatchAd = MutableStateFlow(true)
    val canWatchAd: StateFlow<Boolean> = _canWatchAd.asStateFlow()

    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                MobileAds.initialize(context) { initStatus ->
                    isInitialized = true
                    Log.d(TAG, "Google Mobile Ads SDK Initialized: $initStatus")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to initialize MobileAds: ${e.localizedMessage}")
            }
        }
        refreshFrequencyState(context)
        startCooldownTimer(context)
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun refreshFrequencyState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = getTodayDateString()
        val savedDate = prefs.getString(KEY_DAILY_DATE, "")

        var count = prefs.getInt(KEY_DAILY_AD_COUNT, 0)
        if (savedDate != todayStr) {
            // New day -> Reset daily counter
            count = 0
            prefs.edit()
                .putString(KEY_DAILY_DATE, todayStr)
                .putInt(KEY_DAILY_AD_COUNT, 0)
                .apply()
        }
        _todayAdCount.value = count

        val lastTimestamp = prefs.getLong(KEY_LAST_AD_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - lastTimestamp) / 1000L
        val remainingCooldown = (COOLDOWN_DURATION_SECONDS - elapsedSeconds).coerceAtLeast(0L)
        _cooldownRemainingSeconds.value = remainingCooldown

        val isDailyLimitReached = count >= MAX_DAILY_ADS
        val isCooldownActive = remainingCooldown > 0L
        _canWatchAd.value = !isDailyLimitReached && !isCooldownActive
    }

    private var isTimerRunning = false
    private fun startCooldownTimer(context: Context) {
        if (isTimerRunning) return
        isTimerRunning = true
        scope.launch {
            while (isActive) {
                refreshFrequencyState(context)
                delay(1000L)
            }
        }
    }

    fun canUserWatchAd(context: Context): Pair<Boolean, String?> {
        refreshFrequencyState(context)
        val count = _todayAdCount.value
        val cooldown = _cooldownRemainingSeconds.value

        if (count >= MAX_DAILY_ADS) {
            return Pair(false, "আজকের জন্য দৈনিক সহায়তা সম্পন্ন ($MAX_DAILY_ADS/$MAX_DAILY_ADS)। আপনার অবদানের জন্য আন্তরিক ধন্যবাদ!")
        }
        if (cooldown > 0L) {
            val mins = cooldown / 60
            val secs = cooldown % 60
            return Pair(false, String.format(Locale.US, "পরবর্তী বিজ্ঞাপনের জন্য অপেক্ষা করুন: %02d:%02d", mins, secs))
        }
        return Pair(true, null)
    }

    private fun recordAdWatched(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = getTodayDateString()
        val savedDate = prefs.getString(KEY_DAILY_DATE, "")
        var count = prefs.getInt(KEY_DAILY_AD_COUNT, 0)

        if (savedDate != todayStr) {
            count = 1
        } else {
            count += 1
        }

        prefs.edit()
            .putString(KEY_DAILY_DATE, todayStr)
            .putInt(KEY_DAILY_AD_COUNT, count)
            .putLong(KEY_LAST_AD_TIMESTAMP, System.currentTimeMillis())
            .apply()

        refreshFrequencyState(context)
    }

    /**
     * Loads and shows a full-screen ad (Rewarded or Interstitial) for developer support with strict frequency capping.
     */
    fun showSupportAd(
        activity: Activity,
        onAdStarted: () -> Unit = {},
        onAdCompleted: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        initialize(activity)

        // Strict Pre-flight Rate Limit & Frequency Capping Check to protect AdMob Account
        val (canWatch, reason) = canUserWatchAd(activity)
        if (!canWatch) {
            val msg = reason ?: "বিজ্ঞাপন সীমা সক্রিয় রয়েছে।"
            _adStatusMessage.value = msg
            onError(msg)
            return
        }

        try {
            _isLoadingAd.value = true
            _adStatusMessage.value = "বিজ্ঞাপন লোড হচ্ছে..."
            onAdStarted()

            val adRequest = AdRequest.Builder().build()

            // 1. Try loading Rewarded Ad first
            RewardedAd.load(
                activity,
                AD_UNIT_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        _isLoadingAd.value = false
                        _adStatusMessage.value = "বিজ্ঞাপন প্রদর্শিত হচ্ছে..."

                        rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                Log.d(TAG, "Rewarded Ad dismissed.")
                                recordAdWatched(activity)
                                val msg = "বিজ্ঞাপন দেখার জন্য ধন্যবাদ! আপনার সহায়তা সফলভাবে গৃহীত হয়েছে।"
                                _adStatusMessage.value = msg
                                onAdCompleted(msg)
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                Log.e(TAG, "Ad failed to show: ${adError.message}")
                                _isLoadingAd.value = false
                                _adStatusMessage.value = "বিজ্ঞাপন প্রদর্শনে সমস্যা: ${adError.message}"
                                onError("বিজ্ঞাপন প্রদর্শনে সমস্যা: ${adError.message}")
                            }

                            override fun onAdShowedFullScreenContent() {
                                Log.d(TAG, "Rewarded Ad showed full screen.")
                            }
                        }

                        rewardedAd.show(activity) { rewardItem ->
                            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.w(TAG, "Rewarded Ad failed to load: ${loadAdError.message}. Trying Interstitial fallback...")
                        loadInterstitialFallback(activity, adRequest, onAdCompleted, onError)
                    }
                }
            )
        } catch (e: Throwable) {
            _isLoadingAd.value = false
            val errorMsg = "বিজ্ঞাপন সার্ভিস উপলব্ধ নয়: ${e.localizedMessage ?: "অজ্ঞাত সমস্যা"}"
            Log.e(TAG, errorMsg)
            _adStatusMessage.value = errorMsg
            onError(errorMsg)
        }
    }

    private fun loadInterstitialFallback(
        activity: Activity,
        adRequest: AdRequest,
        onAdCompleted: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            InterstitialAd.load(
                activity,
                AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        _isLoadingAd.value = false
                        _adStatusMessage.value = "বিজ্ঞাপন প্রদর্শিত হচ্ছে..."

                        interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                Log.d(TAG, "Interstitial Ad dismissed.")
                                recordAdWatched(activity)
                                val msg = "বিজ্ঞাপন দেখার জন্য ধন্যবাদ! আপনার সহায়তা সফল হয়েছে।"
                                _adStatusMessage.value = msg
                                onAdCompleted(msg)
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                Log.e(TAG, "Interstitial failed to show: ${adError.message}")
                                _isLoadingAd.value = false
                                _adStatusMessage.value = "বিজ্ঞাপন প্রদর্শনে ত্রুটি: ${adError.message}"
                                onError("বিজ্ঞাপন প্রদর্শনে ত্রুটি: ${adError.message}")
                            }
                        }

                        interstitialAd.show(activity)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        _isLoadingAd.value = false
                        val errorMsg = "বিজ্ঞাপন লোড করা যায়নি (কোড ${loadAdError.code}): ${loadAdError.message}"
                        Log.e(TAG, errorMsg)
                        _adStatusMessage.value = errorMsg
                        onError(errorMsg)
                    }
                }
            )
        } catch (e: Throwable) {
            _isLoadingAd.value = false
            val errorMsg = "বিজ্ঞাপন লোড ব্যর্থ হয়েছে: ${e.localizedMessage ?: "সার্ভার উপলব্ধ নেই"}"
            Log.e(TAG, errorMsg)
            _adStatusMessage.value = errorMsg
            onError(errorMsg)
        }
    }
}
