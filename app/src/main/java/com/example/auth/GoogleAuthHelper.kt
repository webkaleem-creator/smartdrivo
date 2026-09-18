package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"

    // Primary Web OAuth Client ID for Firebase project smartdrivo (726018491881)
    const val DEFAULT_SERVER_CLIENT_ID = "726018491881-mnmel1slshsfippd0npar09s6j42e3fj.apps.googleusercontent.com"

    fun getServerClientId(context: Context): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val fromRes = context.getString(resId)
                if (fromRes.isNotBlank() && !fromRes.contains("examplewebclientid")) {
                    Log.d(TAG, "Using web client ID from resources: $fromRes")
                    return fromRes
                }
            }
            DEFAULT_SERVER_CLIENT_ID
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve default_web_client_id from resources: ${e.message}")
            DEFAULT_SERVER_CLIENT_ID
        }
    }

    suspend fun initiateGoogleSignIn(
        context: Context,
        serverClientId: String? = null,
        onTokenReceived: (idToken: String) -> Unit,
        onFallbackPrompt: (errorMessage: String) -> Unit
    ) {
        val targetClientId = if (serverClientId.isNullOrBlank() ||
            serverClientId.contains("examplewebclientid")
        ) {
            getServerClientId(context)
        } else {
            serverClientId
        }

        Log.i(TAG, "Initiating Google Sign-In with serverClientId: $targetClientId")
        val credentialManager = CredentialManager.create(context)

        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(targetClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    Log.i(TAG, "Google ID token retrieved successfully")
                    onTokenReceived(idToken)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse Google ID Token: ${e.message}", e)
                    onFallbackPrompt("Error reading Google credentials. Please select your account.")
                }
            } else {
                Log.w(TAG, "Unexpected credential type: ${credential.type}")
                onFallbackPrompt("Unexpected credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled Google credential dialog: ${e.message}")
            onFallbackPrompt("Sign-in cancelled")
        } catch (e: NoCredentialException) {
            Log.w(TAG, "NoCredentialException: ${e.message}. Package: ${context.packageName}, ClientId: $targetClientId")
            onFallbackPrompt("No Google credentials available on device. Ensure a Google account is logged in or use Admin Login.")
        } catch (e: GetCredentialException) {
            Log.w(TAG, "GetCredentialException: ${e.message}")
            onFallbackPrompt("Google Sign-In: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "General exception in Google sign-in: ${e.message}", e)
            onFallbackPrompt("Google Sign-In error: ${e.message}")
        }
    }
}
