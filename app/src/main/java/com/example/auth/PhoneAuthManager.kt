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

    // Test bypass and Admin credentials
    const val TEST_PHONE_NUMBER = "+919949957404"
    const val TEST_PHONE_RAW = "9949957404"
    const val TEST_OTP_CODE = "123456"
    const val ADMIN_EMAIL = "webkaleem@gmail.com"
    private const val TEST_VERIFICATION_ID = "SMARTDRIVO_TEST_VERIFICATION_ID"

    fun isAdminAccount(identifier: String): Boolean {
        val digits = identifier.filter { it.isDigit() }
        val isPhone = digits == TEST_PHONE_RAW || digits == "91$TEST_PHONE_RAW" || normalizePhoneNumber(identifier) == TEST_PHONE_NUMBER
        val isEmail = identifier.trim().equals(ADMIN_EMAIL, ignoreCase = true)
        return isPhone || isEmail
    }

    private var auth: FirebaseAuth? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

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

    fun isTestPhoneNumber(input: String): Boolean {
        val digits = input.filter { it.isDigit() }
        return digits == TEST_PHONE_RAW ||
                digits == "91$TEST_PHONE_RAW" ||
                normalizePhoneNumber(input) == TEST_PHONE_NUMBER
    }

    fun isTestOtpValid(phone: String, otp: String): Boolean {
        return isTestPhoneNumber(phone) && otp.trim() == TEST_OTP_CODE
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

        // 1. Check for test phone number bypass
        if (isTestPhoneNumber(phoneNumber)) {
            Log.i(TAG, "🧪 Test phone number detected: $formattedPhone. Fast-tracking test OTP verification ($TEST_OTP_CODE).")
            // Instantly notify code sent for test phone
            onCodeSent(TEST_VERIFICATION_ID)

            // Also trigger standard Firebase Phone Auth in background if Firebase is configured
            val firebaseAuth = getAuthInstance()
            if (firebaseAuth != null) {
                try {
                    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            Log.i(TAG, "Firebase test verification completed automatically")
                        }
                        override fun onVerificationFailed(e: FirebaseException) {
                            Log.d(TAG, "Firebase test verification background check note: ${e.message}")
                        }
                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            resendToken = token
                            Log.i(TAG, "Firebase test code registered: $verificationId")
                        }
                    }
                    val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(formattedPhone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setCallbacks(callbacks)
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)
                } catch (e: Exception) {
                    Log.d(TAG, "Firebase background test phone verification skipped: ${e.message}")
                }
            }
            return
        }

        // 2. Standard Firebase Phone Authentication
        val firebaseAuth = getAuthInstance()
        if (firebaseAuth == null) {
            onError("Firebase Auth is not available. Please check network connection.")
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.i(TAG, "Phone auto-verification completed automatically")
                val code = credential.smsCode
                if (!code.isNullOrEmpty()) {
                    Log.d(TAG, "SMS code auto-retrieved")
                }
                signInWithCredential(credential, formattedPhone, onAutoVerified, onError)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "Phone verification failed: ${e.message}", e)
                val friendlyMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException ->
                        "Invalid phone number or verification code. Please check your details."
                    is FirebaseTooManyRequestsException ->
                        "Too many SMS requests sent. Please wait a few minutes or use the test phone number."
                    else ->
                        e.localizedMessage ?: "Failed to send SMS code. Please try again."
                }
                onError(friendlyMessage)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.i(TAG, "SMS Code sent successfully. Verification ID: $verificationId")
                resendToken = token
                onCodeSent(verificationId)
            }
        }

        try {
            val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            resendToken?.let { token ->
                optionsBuilder.setForceResendingToken(token)
            }

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error calling verifyPhoneNumber: ${e.message}", e)
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

        // 1. Check Test Phone Number & Test OTP Bypass
        if (isTestPhoneNumber(phone)) {
            if (trimmedOtp == TEST_OTP_CODE) {
                Log.i(TAG, "✓ Test OTP 123456 verified successfully for $formattedPhone! Bypassing SMS verification.")
                // Attempt anonymous sign in if not logged in to get a valid Firebase user UID
                val firebaseAuth = getAuthInstance()
                if (firebaseAuth != null && firebaseAuth.currentUser == null) {
                    firebaseAuth.signInAnonymously().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.i(TAG, "Signed in anonymously for test session UID: ${firebaseAuth.currentUser?.uid}")
                        }
                        onSuccess(formattedPhone)
                    }
                } else {
                    onSuccess(formattedPhone)
                }
                return
            } else {
                onError("Incorrect test OTP. For $TEST_PHONE_RAW, please enter code: $TEST_OTP_CODE")
                return
            }
        }

        // 2. Standard Firebase OTP Verification
        val firebaseAuth = getAuthInstance()
        if (firebaseAuth == null) {
            onError("Firebase Auth is not available.")
            return
        }

        if (verificationId.isEmpty()) {
            onError("Missing verification session. Please resend the OTP.")
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
            onSuccess(phone)
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
                        is FirebaseAuthInvalidCredentialsException -> "The OTP code entered is incorrect or has expired."
                        else -> e?.localizedMessage ?: "Failed to verify OTP. Please try again."
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
                        val emailOrUid = user?.email ?: user?.uid ?: ADMIN_EMAIL
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
