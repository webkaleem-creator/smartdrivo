package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Style 1: prominent power symbol instead of an ordinary slider.
 *
 * Visual-only child of HomeScreen's existing clickable rows.
 * Switching, permission checks and persisted settings remain owned by HomeScreen.
 */
@Composable
fun SmartDrivoPowerButton(
    enabled: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (enabled) activeColor else Color(0xFF67758A),
        shape = RoundedCornerShape(13.dp),
        shadowElevation = if (enabled) 5.dp else 1.dp,
        border = BorderStroke(
            1.dp,
            if (enabled) activeColor.copy(alpha = 0.75f) else Color(0xFF9BA8B8)
        )
    ) {
        Row(
            modifier = Modifier
                .width(110.dp)
                .height(48.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.width(27.dp).height(27.dp)
            )
            Text(
                text = if (enabled) "ON" else "OFF",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
