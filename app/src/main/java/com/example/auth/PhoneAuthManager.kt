package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

object PhoneAuthManager {
    private const val TAG = "PhoneAuthManager"

    private var auth: FirebaseAuth? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var lastPhoneNumber: String? = null

    fun init(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.i(TAG, "FirebaseApp initialized successfully")
            }
            auth = FirebaseAuth.getInstance().apply {
                setLanguageCode("en")
            }
            Log.i(TAG, "FirebaseAuth initialized. Current user: ${auth?.currentUser?.uid ?: "None"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase Auth: ${e.message}", e)
        }
    }

    fun getAuthInstance(): FirebaseAuth? {
        if (auth == null) {
            try {
                auth = FirebaseAuth.getInstance()
            } catch (e: Exception) {
                Log.w(TAG, "FirebaseAuth.getInstance() failed: ${e.message}")
            }
        }
        return auth
    }

    fun normalizePhoneNumber(input: String): String {
        val digits = input.filter { it.isDigit() }
        return when {
            input.startsWith("+") -> "+$digits"
            digits.length == 10 -> "+91$digits"
            digits.length == 12 && digits.startsWith("91") -> "+$digits"
            else -> "+$digits"
        }
    }

    fun sendOtp(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onAutoVerified: (phone: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val formattedPhone = normalizePhoneNumber(phoneNumber)
        Log.i(TAG, "Initiating OTP send for: $formattedPhone")

        val firebaseAuth = getAuthInstance()
        if (firebaseAuth == null) {
            Log.e(TAG, "FirebaseAuth is null during sendOtp")
            onError("Firebase Authentication is not available. Please check network connection.")
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.i(TAG, "✓ Phone auto-verification completed automatically for $formattedPhone")
                val code = credential.smsCode
                if (!code.isNullOrEmpty()) {
                    Log.d(TAG, "SMS code auto-retrieved: $code")
                }
                signInWithCredential(credential, formattedPhone, onAutoVerified, onError)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "Firebase phone verification failed for $formattedPhone: ${e.message}", e)
                val friendlyMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException ->
                        "Invalid phone number or verification code. Please check your details."
                    is FirebaseTooManyRequestsException ->
                        "SMS quota exceeded or too many requests. Please wait a few minutes."
                    else ->
                        e.localizedMessage ?: "Failed to send SMS code. Please try again."
                }
                onError(friendlyMessage)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.i(TAG, "Firebase SMS code sent successfully to $formattedPhone. Verification ID: $verificationId")
                resendToken = token
                lastPhoneNumber = formattedPhone
                onCodeSent(verificationId)
            }
        }

        try {
            val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            if (resendToken != null && lastPhoneNumber == formattedPhone) {
                optionsBuilder.setForceResendingToken(resendToken!!)
            }

            Log.i(TAG, "Triggering PhoneAuthProvider.verifyPhoneNumber for SMS OTP to $formattedPhone")
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error calling PhoneAuthProvider.verifyPhoneNumber: ${e.message}", e)
            onError("Error initiating phone verification: ${e.localizedMessage}")
        }
    }

    fun verifyOtp(
        phone: String,
        verificationId: String,
        otpCode: String,
        onSuccess: (phoneOrUid: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val formattedPhone = normalizePhoneNumber(phone)
        val trimmedOtp = otpCode.trim()

        if (verificationId.isEmpty()) {
            onError("Invalid verification session. Please request a new SMS OTP.")
            return
        }

        val firebaseAuth = getAuthInstance()
        if (firebaseAuth == null) {
            onError("Firebase Authentication is not available. Please check network connection.")
            return
        }

        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, trimmedOtp)
            signInWithCredential(credential, formattedPhone, onSuccess, onError)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create PhoneAuthCredential: ${e.message}", e)
            onError("Invalid verification details: ${e.localizedMessage}")
        }
    }

    private fun signInWithCredential(
        credential: PhoneAuthCredential,
        phone: String,
        onSuccess: (phoneOrUid: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val firebaseAuth = getAuthInstance()
        if (firebaseAuth == null) {
            Log.e(TAG, "signInWithCredential failed: FirebaseAuth is null")
            onError("Firebase Authentication is not available.")
            return
        }

        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val phoneOrUid = user?.phoneNumber ?: phone
                    Log.i(TAG, "✓ Firebase Phone Sign-In Successful! UID: ${user?.uid}, Phone: $phoneOrUid")
                    onSuccess(phoneOrUid)
                } else {
                    val e = task.exception
                    Log.e(TAG, "Firebase signInWithCredential failed: ${e?.message}", e)
                    val message = when (e) {
                        is FirebaseAuthInvalidCredentialsException -> "The SMS OTP code entered is incorrect or has expired."
                        else -> e?.localizedMessage ?: "Failed to verify SMS OTP. Please try again."
                    }
                    onError(message)
                }
            }
    }

    fun signInWithGoogleToken(
        idToken: String,
        onSuccess: (emailOrUid: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val firebaseAuth = getAuthInstance()
        if (firebaseAuth == null) {
            onError("Firebase Auth not initialized")
            return
        }
        try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        val emailOrUid = user?.email ?: user?.uid ?: ""
                        Log.i(TAG, "✓ Firebase Google Sign-In Successful! UID: ${user?.uid}, Email: $emailOrUid")
                        onSuccess(emailOrUid)
                    } else {
                        val err = task.exception?.localizedMessage ?: "Google Sign-In failed in Firebase"
                        Log.e(TAG, "Google sign-in error: $err")
                        onError(err)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Google credential sign in: ${e.message}", e)
            onError(e.localizedMessage ?: "Failed to authenticate with Google")
        }
    }

    fun signOut() {
        try {
            getAuthInstance()?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out: ${e.message}")
        }
    }
}
