package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Presentation-only. Never accesses the ride engine, filters or history store. */
@Composable
fun premiumIsDark(): Boolean =
    MaterialTheme.colorScheme.background == Color(0xFF0A1528)

@Composable
fun premiumPageBrush(): Brush =
    Brush.verticalGradient(
        if (premiumIsDark()) listOf(
            Color(0xFF122849), Color(0xFF0A1528), Color(0xFF10243E)
        ) else listOf(
            Color(0xFFE8F4FF), Color(0xFFF4F9FF), Color(0xFFE9F4FF)
        )
    )

@Composable
fun premiumCardColor(): Color =
    if (premiumIsDark()) Color(0xFF172A49) else Color.White

@Composable
fun premiumCardBorder(): Color =
    if (premiumIsDark()) Color(0xFF365A86) else Color(0xFFCDE3FC)

@Composable
fun premiumTitleColor(): Color =
    if (premiumIsDark()) Color(0xFFF3F8FF) else Color(0xFF0E1E3C)

@Composable
fun premiumSubtextColor(): Color =
    if (premiumIsDark()) Color(0xFFBED1EB) else Color(0xFF617797)
