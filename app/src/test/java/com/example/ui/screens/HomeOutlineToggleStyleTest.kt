package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HomeOutlineToggleStyleTest {
    @Test fun blueOnAndSlateOffPaletteMatchesApprovedHomeDesign() {
        assertEquals(0xFF1677F2, HomeOutlineToggleStyle.ON_BORDER)
        assertEquals(0xFF94A3B8, HomeOutlineToggleStyle.OFF_BORDER)
        assertNotEquals(HomeOutlineToggleStyle.ON_BORDER, HomeOutlineToggleStyle.OFF_BORDER)
    }

    @Test fun outlinedStatesHaveDistinctBackgroundColors() {
        assertEquals(0xFFF0F7FF, HomeOutlineToggleStyle.ON_BACKGROUND)
        assertEquals(0xFFF8FAFC, HomeOutlineToggleStyle.OFF_BACKGROUND)
        assertNotEquals(HomeOutlineToggleStyle.ON_BACKGROUND, HomeOutlineToggleStyle.OFF_BACKGROUND)
    }
}
