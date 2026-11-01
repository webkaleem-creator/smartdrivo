package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BankingModeControllerTest {
    private lateinit var context: Context

    @Before
    fun reset() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("smartdrivo_banking_mode", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun bankingModeStartsOffAndPersistsAcrossReads() {
        assertFalse(BankingModeController.isActive(context))
        assertTrue(BankingModeController.persistStart(context, true))
        assertTrue(BankingModeController.isActive(context))
        assertTrue(BankingModeController.previousMasterWasActive(context))
    }

    @Test
    fun repeatedActivationDoesNotOverwriteOriginalMasterSetting() {
        assertTrue(BankingModeController.persistStart(context, true))
        assertTrue(BankingModeController.persistStart(context, false))
        assertTrue(BankingModeController.previousMasterWasActive(context))
    }

    @Test
    fun finishingBankingClearsStateAndOldRestorePreference() {
        assertTrue(BankingModeController.persistStart(context, true))
        assertTrue(BankingModeController.persistFinish(context))
        assertFalse(BankingModeController.isActive(context))
        assertFalse(BankingModeController.previousMasterWasActive(context))
    }

    @Test
    fun initiallyDisabledMasterDoesNotBecomeEnabledByBankingMode() {
        assertTrue(BankingModeController.persistStart(context, false))
        assertFalse(BankingModeController.previousMasterWasActive(context))
        assertTrue(BankingModeController.persistFinish(context))
    }
}
