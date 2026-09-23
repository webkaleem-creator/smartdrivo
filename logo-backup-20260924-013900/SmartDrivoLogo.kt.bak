package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SmartDrivoBluePrimary = Color(0xFF1E88E5)
val SmartDrivoBlueDark = Color(0xFF1565C0)
val SmartDrivoBlueLight = Color(0xFF42A5F5)

/**
 * SmartDrivo Combination Logo
 * - Blue circle (#1E88E5)
 * - "SD" bold white text
 * - Center: White car
 * - Left: Small bike
 * - Right: Small auto
 */
@Composable
fun SmartDrivoLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showBorder: Boolean = true
) {
    val bgBrush = Brush.linearGradient(
        colors = listOf(SmartDrivoBlueLight, SmartDrivoBluePrimary, SmartDrivoBlueDark)
    )

    Box(
        modifier = modifier
            .size(size)
            .background(bgBrush, CircleShape)
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = if (size > 60.dp) 2.dp else 1.dp,
                        color = Color.White.copy(alpha = 0.85f),
                        shape = CircleShape
                    )
                } else Modifier
            )
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = size * 0.08f, vertical = size * 0.06f)
        ) {
            // "SD" bold white text
            Text(
                text = "SD",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.22f).coerceAtLeast(8f).sp,
                lineHeight = (size.value * 0.22f).coerceAtLeast(8f).sp,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height((size.value * 0.02f).coerceAtLeast(1f).dp))

            // Combination vehicle row: Bike - Car - Auto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: small bike
                Icon(
                    imageVector = Icons.Default.DirectionsBike,
                    contentDescription = "Bike",
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.25f).coerceAtLeast(10f).dp)
                )

                // Center: white car (slightly larger)
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Car",
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.38f).coerceAtLeast(14f).dp)
                )

                // Right: small auto
                Icon(
                    imageVector = Icons.Default.ElectricRickshaw,
                    contentDescription = "Auto",
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.25f).coerceAtLeast(10f).dp)
                )
            }
        }
    }
}
