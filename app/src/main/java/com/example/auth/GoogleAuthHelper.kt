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

    const val DEFAULT_SERVER_CLIENT_ID = "726018491881-apps.googleusercontent.com"

    suspend fun initiateGoogleSignIn(
        context: Context,
        serverClientId: String = DEFAULT_SERVER_CLIENT_ID,
        onTokenReceived: (idToken: String) -> Unit,
        onFallbackPrompt: (errorMessage: String) -> Unit
    ) {
        val credentialManager = CredentialManager.create(context)

        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
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
            Log.d(TAG, "User cancelled Google credential dialog")
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google accounts available on device/emulator: ${e.message}")
            onFallbackPrompt("No active Google account found on device. Sign in with Google account or admin email.")
        } catch (e: GetCredentialException) {
            Log.w(TAG, "GetCredentialException: ${e.message}")
            onFallbackPrompt("Google Sign-In: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "General exception in Google sign-in: ${e.message}", e)
            onFallbackPrompt("Google Sign-In: ${e.message}")
        }
    }
}
