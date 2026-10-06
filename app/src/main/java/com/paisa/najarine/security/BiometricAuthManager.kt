package com.paisa.najarine.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BiometricAuthManager(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("paisa_security_prefs", Context.MODE_PRIVATE)

    private val _isUnlocked = MutableStateFlow(!isSecurityEnabled())
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun isSecurityEnabled(): Boolean {
        // Biometric & PIN app protection disabled by default; user can enable in Settings
        return sharedPrefs.getBoolean("security_lock_enabled", false)
    }

    fun setSecurityEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("security_lock_enabled", enabled).apply()
        if (!enabled) {
            _isUnlocked.value = true
        }
    }

    fun getStoredPin(): String {
        return sharedPrefs.getString("security_pin", "1234") ?: "1234"
    }

    fun setStoredPin(pin: String) {
        sharedPrefs.edit().putString("security_pin", pin).apply()
    }

    fun canAuthenticateWithBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun verifyPin(enteredPin: String): Boolean {
        val matches = enteredPin == getStoredPin()
        if (matches) {
            _isUnlocked.value = true
        }
        return matches
    }

    fun unlock() {
        _isUnlocked.value = true
    }

    fun lock() {
        if (isSecurityEnabled()) {
            _isUnlocked.value = false
        }
    }

    fun promptBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!canAuthenticateWithBiometrics()) {
            onError("ডিভাইসে বায়োমেট্রিক সেন্সর সক্রিয় নেই। অনুগ্রহ করে পিন ব্যবহার করুন।")
            return
        }

        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        _isUnlocked.value = true
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // Error code 10 or 13 is user cancelled to use PIN, which is normal
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            onError(errString.toString())
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("বায়োমেট্রিক যাচাই ব্যর্থ হয়েছে। পুনরায় চেষ্টা করুন।")
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Paisa বায়োমেট্রিক নিরাপত্তা")
                .setSubtitle("আর্থিক ড্যাশবোর্ডে প্রবেশের জন্য ফিঙ্গারপ্রিন্ট বা ফেসলক স্ক্যান করুন")
                .setNegativeButtonText("পিন (PIN) ব্যবহার করুন")
                .setConfirmationRequired(false)
                .build()

            prompt.authenticate(promptInfo)
        } catch (e: Throwable) {
            onError("বায়োমেট্রিক প্রম্পট শুরু করা সম্ভব হয়নি: ${e.localizedMessage}")
        }
    }
}
