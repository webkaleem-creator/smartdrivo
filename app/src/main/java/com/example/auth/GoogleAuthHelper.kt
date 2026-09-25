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
import kotlinx.coroutines.delay

object GoogleAuthHelper {

    private const val TAG = "GoogleAuthHelper"

    const val DEFAULT_SERVER_CLIENT_ID =
        "726018491881-mnmel1slshsfippd0npar09s6j42e3fj.apps.googleusercontent.com"

    fun getServerClientId(context: Context): String {
        return try {
            val resId = context.resources.getIdentifier(
                "default_web_client_id",
                "string",
                context.packageName
            )

            if (resId != 0) {
                val fromRes = context.getString(resId)

                if (
                    fromRes.isNotBlank() &&
                    !fromRes.contains("examplewebclientid")
                ) {
                    return fromRes
                }
            }

            DEFAULT_SERVER_CLIENT_ID

        } catch (e: Exception) {
            Log.w(
                TAG,
                "Failed to resolve default_web_client_id: ${e.message}"
            )

            DEFAULT_SERVER_CLIENT_ID
        }
    }

    suspend fun initiateGoogleSignIn(
        context: Context,
        serverClientId: String? = null,
        onTokenReceived: (idToken: String) -> Unit,
        onFallbackPrompt: (errorMessage: String) -> Unit
    ) {

        val targetClientId =
            if (
                serverClientId.isNullOrBlank() ||
                serverClientId.contains("examplewebclientid")
            ) {
                getServerClientId(context)
            } else {
                serverClientId
            }

        val credentialManager =
            CredentialManager.create(context)

        suspend fun performAttempt(
            allowRetry: Boolean
        ) {

            try {

                val googleIdOption =
                    GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(targetClientId)
                        .setAutoSelectEnabled(false)
                        .build()

                val request =
                    GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                val result =
                    credentialManager.getCredential(
                        context = context,
                        request = request
                    )

                val credential =
                    result.credential

                if (
                    credential is CustomCredential &&
                    credential.type ==
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {

                    val googleCredential =
                        GoogleIdTokenCredential.createFrom(
                            credential.data
                        )

                    val idToken =
                        googleCredential.idToken

                    Log.i(
                        TAG,
                        "Google ID token retrieved successfully"
                    )

                    onTokenReceived(idToken)

                } else {

                    if (allowRetry) {
                        Log.w(
                            TAG,
                            "Unexpected credential response. Retrying once."
                        )

                        delay(500)

                        performAttempt(false)

                    } else {
                        onFallbackPrompt(
                            "Google sign-in could not start. Please try again."
                        )
                    }
                }

            } catch (
                e: GetCredentialCancellationException
            ) {

                Log.d(
                    TAG,
                    "Google sign-in cancelled by user"
                )

                onFallbackPrompt(
                    "Sign-in cancelled"
                )

            } catch (
                e: NoCredentialException
            ) {

                Log.w(
                    TAG,
                    "NoCredentialException: ${e.message}"
                )

                if (allowRetry) {

                    Log.i(
                        TAG,
                        "Retrying Google Sign-In once after temporary credential failure"
                    )

                    delay(600)

                    performAttempt(false)

                } else {

                    onFallbackPrompt(
                        "No Google credentials available. Please try again."
                    )
                }

            } catch (
                e: GetCredentialException
            ) {

                Log.w(
                    TAG,
                    "Credential Manager error: ${e.message}"
                )

                if (allowRetry) {

                    Log.i(
                        TAG,
                        "Retrying Google Sign-In once after Credential Manager error"
                    )

                    delay(600)

                    performAttempt(false)

                } else {

                    onFallbackPrompt(
                        "Google sign-in could not start. Please try again."
                    )
                }

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "Google Sign-In error: ${e.message}",
                    e
                )

                if (allowRetry) {

                    delay(600)

                    performAttempt(false)

                } else {

                    onFallbackPrompt(
                        "Google sign-in could not start. Please try again."
                    )
                }
            }
        }

        performAttempt(true)
    }
}