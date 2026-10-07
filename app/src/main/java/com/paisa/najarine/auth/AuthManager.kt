package com.paisa.najarine.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.paisa.najarine.R
import com.paisa.najarine.util.CertificateDiagnostics
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object SigningIn : AuthState()
    object GoogleAccountSelector : AuthState()
    object AuthenticatingWithFirebase : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    object Cancelled : AuthState()
    data class Error(
        val title: String,
        val message: String,
        val rawException: String? = null,
        val errorCode: String? = null,
        val isConfigIssue: Boolean = false
    ) : AuthState()
    data class Timeout(val message: String) : AuthState()

    val isLoading: Boolean
        get() = this is SigningIn || this is GoogleAccountSelector || this is AuthenticatingWithFirebase
}

class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth get() = Firebase.auth
    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentSession()
    }

    fun checkCurrentSession() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.uid.isNotBlank()) {
            _authState.value = AuthState.Success(currentUser)
        } else {
            _authState.value = AuthState.Idle
        }
    }

    fun resetState() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.uid.isNotBlank()) {
            _authState.value = AuthState.Success(currentUser)
        } else {
            _authState.value = AuthState.Idle
        }
    }

    fun getWebClientId(): String {
        return try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else ""
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: (FirebaseUser) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val clientId = getWebClientId()
        if (clientId.isBlank()) {
            val errorMsg = "Google Sign-In configuration missing: default_web_client_id not found"
            _authState.value = AuthState.Error(
                title = "Configuration Missing",
                message = errorMsg,
                errorCode = "MISSING_CLIENT_ID",
                isConfigIssue = true
            )
            onError(errorMsg)
            return
        }

        _authState.value = AuthState.SigningIn

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                _authState.value = AuthState.GoogleAccountSelector
                val result = try {
                    credentialManager.getCredential(activity, request)
                } catch (e: Exception) {
                    if (e is GetCredentialCancellationException) throw e
                    Log.w("Auth", "Primary GetSignInWithGoogleOption request failed, attempting GetGoogleIdOption fallback: ${e.message}")
                    val fallbackGoogleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(clientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(fallbackGoogleIdOption)
                        .build()
                    credentialManager.getCredential(activity, fallbackRequest)
                }

                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    _authState.value = AuthState.AuthenticatingWithFirebase
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user

                    if (user != null && user.uid.isNotBlank()) {
                        CertificateDiagnostics.recordAuthSuccess(user.uid, user.email)
                        _authState.value = AuthState.Success(user)
                        onSuccess(user)
                    } else {
                        val errorMsg = "Firebase user authentication returned null UID."
                        _authState.value = AuthState.Error(
                            title = "Firebase Session Error",
                            message = errorMsg,
                            errorCode = "NULL_USER"
                        )
                        onError(errorMsg)
                    }
                } else {
                    val errorMsg = "Unexpected credential response type"
                    _authState.value = AuthState.Error(
                        title = "Authentication Token Missing",
                        message = errorMsg,
                        errorCode = "UNEXPECTED_CREDENTIAL_TYPE"
                    )
                    onError(errorMsg)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                val isConfigIssue = e.message?.contains("10") == true || e.message?.contains("12500") == true || e.message?.contains("28444") == true
                CertificateDiagnostics.recordAuthException(e.javaClass.name, e.message ?: "Unknown", "EXCEPTION")
                val errorMsg = if (e.message?.contains("28444") == true) {
                    "Google কনসোলে নতুন ক্লায়েন্ট আইডি সিঙ্ক হতে ২-৫ মিনিট সময় নেয়। অনুগ্রহ করে নতুন APK ইনস্টল করে কিছুক্ষণ পর আবার চেষ্টা করুন।"
                } else {
                    e.localizedMessage ?: "Sign-in failed. Please verify network and Google account."
                }
                _authState.value = AuthState.Error(
                    title = "Authentication Failure",
                    message = errorMsg,
                    rawException = "${e.javaClass.name}: ${e.message}",
                    errorCode = "CREDENTIAL_MANAGER_EXCEPTION",
                    isConfigIssue = isConfigIssue
                )
                onError(errorMsg)
            }
        }
    }

    fun attemptAutoSignIn(
        scope: CoroutineScope,
        onSuccess: (FirebaseUser) -> Unit = {}
    ) {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.uid.isNotBlank()) {
            _authState.value = AuthState.Success(currentUser)
            onSuccess(currentUser)
            return
        }

        val clientId = getWebClientId()
        if (clientId.isBlank()) return

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null && user.uid.isNotBlank()) {
                        CertificateDiagnostics.recordAuthSuccess(user.uid, user.email)
                        _authState.value = AuthState.Success(user)
                        onSuccess(user)
                    }
                }
            } catch (_: Exception) {
                // Background silent auto sign-in was unable to resolve stored token
            }
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Failed to clear credential state", e)
            }
            _authState.value = AuthState.Idle
            Result.success(Unit)
        } catch (_: Exception) {
            _authState.value = AuthState.Idle
            Result.success(Unit)
        }
    }
}
