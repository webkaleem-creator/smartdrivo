package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FirebaseRepository
import com.example.data.PreferencesManager
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** No Firebase app/network is configured: only synthetic local records are used. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminMutationGuardTest {
    @Test fun staleCachedAdminCannotMutateSubscriptionsPaymentsOrStartPrivilegedListeners() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager.getInstance(context)
        val previousProfile = prefs.userProfile.value
        val previousUsers = prefs.allUsers.value
        val previousPayments = prefs.paymentSubmissions.value
        try {
            prefs.saveUserProfile(UserProfile(uid = "synthetic-owner", isAdmin = true))
            val driver = UserProfile(uid = "synthetic-driver", plan = "7DAYS", planExpireMillis = 1234L)
            val payment = PaymentSubmission(paymentId = "synthetic-payment", uid = driver.uid,
                utrNumber = "000000000001", planSelected = "7DAYS", amount = 129)
            prefs.setAllUsers(listOf(driver))
            prefs.setPaymentSubmissions(listOf(payment))
            val repository = FirebaseRepository(context, prefs)
            assertFalse(repository.hasAdminAccess())
            var denied = 0
            repository.adminToggleUserActiveStatus(driver.uid, true) { assertFalse(it); denied++ }
            repository.adminExtendUserPlan(driver.uid, 7, "7DAYS", 129) { assertFalse(it); denied++ }
            repository.adminRejectPayment(payment.paymentId) { assertFalse(it); denied++ }
            repository.adminApprovePayment(payment) { ok, _ -> assertFalse(ok); denied++ }
            repository.adminUpdateUserProfile(driver.copy(isApproved = true)) { assertFalse(it); denied++ }
            assertEquals(5, denied)
            assertEquals(listOf(driver), prefs.allUsers.value)
            assertEquals(listOf(payment), prefs.paymentSubmissions.value)
            assertNull(repository.listenToAllUsers())
            assertNull(repository.listenToAllPayments())
        } finally {
            prefs.saveUserProfile(previousProfile)
            prefs.setAllUsers(previousUsers)
            prefs.setPaymentSubmissions(previousPayments)
        }
    }
}
