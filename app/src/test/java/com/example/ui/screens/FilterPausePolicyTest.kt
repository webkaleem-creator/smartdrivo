package com.example.ui.screens

import com.example.model.AppSettings
import com.example.model.AreaEntry
import com.example.model.AreaGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterPausePolicyTest {
    private val home = AreaGroup(
        name = "Home1", isEnabled = true,
        areas = listOf(AreaEntry(name = "Falaknuma"))
    )

    @Test fun fastestWinsAndPausesAllNonFastestControls() {
        val state = FilterPausePolicy.resolve(
            AppSettings(isFastestModeEnabled = true),
            listOf(home)
        )
        assertTrue(state.fastest)
        assertEquals("Fastest Mode", state.normalFilterPauseReason)
        assertEquals("Fastest Mode", state.fastestPauseReason)
    }

    @Test fun gotoPreservesPlusAmountAndNoGoAccess() {
        val state = FilterPausePolicy.resolve(AppSettings(), listOf(home))
        assertTrue(state.goTo)
        assertEquals("GO TO", state.normalFilterPauseReason)
        assertNull(state.fastestPauseReason)
    }

    @Test fun disabledHomeGroupDoesNotPauseNormalFilters() {
        val state = FilterPausePolicy.resolve(
            AppSettings(), listOf(home.copy(isEnabled = false))
        )
        assertNull(state.normalFilterPauseReason)
        assertNull(state.fastestPauseReason)
    }

    @Test fun existingKeywordGroupsAreRecognizedWithoutSettingsWrites() {
        val group = AreaGroup(name = "Home1", keywords = listOf("Shalibanda"))
        assertTrue(FilterPausePolicy.resolve(AppSettings(), listOf(group)).goTo)
    }
}
