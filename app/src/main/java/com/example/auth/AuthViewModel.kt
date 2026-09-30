package com.example.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.data.FirebaseRepository
import com.example.data.PreferencesManager
import com.example.model.UserProfile
import com.example.model.VehicleType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AuthViewModel manages Google Sign-In authentication and Post-Google Profile Setup
 * (Mobile Number, City, State, Vehicle Type).
 */
class AuthViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    fun init(context: Context) {
        PhoneAuthManager.init(context)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Sign in via Google Id Token.
     */
    fun signInWithGoogleToken(
        idToken: String,
        onSuccess: (emailOrUid: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        PhoneAuthManager.signInWithGoogleToken(
            idToken = idToken,
            onSuccess = { emailOrUid ->
                _isLoading.value = false
                onSuccess(emailOrUid)
            },
            onError = { error ->
                _isLoading.value = false
                _errorMessage.value = error
                onError(error)
            }
        )
    }

    /**
     * Save complete user profile (including mobile number, city, state) to both
     * local PreferencesManager and Firebase Firestore under the "users" collection.
     */
    fun saveUserProfile(
        prefs: PreferencesManager,
        repository: FirebaseRepository,
        name: String,
        mobileNumber: String,
        city: String,
        state: String,
        vehicleType: VehicleType,
        onComplete: (UserProfile) -> Unit
    ) {
        val currentProfile = prefs.userProfile.value
        val firebaseUser = getAuthInstance()?.currentUser
        val uid = firebaseUser?.uid?.ifEmpty { null }
            ?: currentProfile.uid.ifEmpty { null }
            ?: repository.getCurrentUid()

        val email = firebaseUser?.email ?: currentProfile.email

        val cleanPhone = if (mobileNumber.startsWith("+91")) {
            mobileNumber
        } else {
            "+91${mobileNumber.trim().filter { it.isDigit() }}"
        }

        // FREE_TRIAL_AUTH_V3
        val now =
            System.currentTimeMillis()

        val shouldStartFreeTrial =
            !currentProfile.isAdmin &&
                !currentProfile.isApproved &&
                !currentProfile.isPlanValid &&
                (
                    currentProfile.plan.isBlank() ||
                        currentProfile.plan.equals(
                            "NONE",
                            ignoreCase = true
                        ) ||
                        currentProfile.plan.equals(
                            "7DAYS",
                            ignoreCase = true
                        )
                )

        val updatedProfile =
            currentProfile.copy(
                uid = uid,
                name =
                    name.trim().ifEmpty {
                        currentProfile.name.ifEmpty {
                            "Captain"
                        }
                    },
                email = email,
                phone = cleanPhone,
                city = city.trim(),
                state = state.trim(),
                vehicleType = vehicleType,
                plan =
                    if (shouldStartFreeTrial)
                        "FREE_TRIAL"
                    else
                        currentProfile.plan,
                planPrice =
                    if (shouldStartFreeTrial)
                        0
                    else
                        currentProfile.planPrice,
                planExpireMillis =
                    if (shouldStartFreeTrial)
                        now +
                            (
                                24L *
                                    60L *
                                    60L *
                                    1000L
                            )
                    else
                        currentProfile.planExpireMillis,
                isApproved =
                    if (shouldStartFreeTrial)
                        false
                    else
                        currentProfile.isApproved,
                isAdmin = currentProfile.isAdmin,
                isActive = true
            )
        prefs.saveUserProfile(updatedProfile)
        repository.saveUserProfile(updatedProfile) {
            onComplete(updatedProfile)
        }
    }

    fun signOut() {
        PhoneAuthManager.signOut()
    }

    fun getAuthInstance(): FirebaseAuth? = PhoneAuthManager.getAuthInstance()
}
