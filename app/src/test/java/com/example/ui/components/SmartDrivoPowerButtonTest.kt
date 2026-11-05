package com.example.ui.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Regression coverage for the ON/OFF label of the new visual-only assistant button. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SmartDrivoPowerButtonTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun reflects_saved_state_without_owning_toggle_logic() {
        val enabled = mutableStateOf(false)
        composeRule.setContent {
            SmartDrivoPowerButton(
                enabled = enabled.value,
                activeColor = Color(0xFF1677F2)
            )
        }
        composeRule.onNodeWithText("OFF").assertExists()
        composeRule.onNodeWithText("ON").assertDoesNotExist()

        composeRule.runOnIdle { enabled.value = true }
        composeRule.onNodeWithText("ON").assertExists()
        composeRule.onNodeWithText("OFF").assertDoesNotExist()
    }
}
