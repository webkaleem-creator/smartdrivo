package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design #3: outlined capsule with label and circular indicator.
 * Used ONLY for Home's Smart Assistance and No Condition Match Auto Reject.
 * Controlled by existing persisted settings and existing callbacks — no local state.
 */
internal object HomeOutlineToggleStyle {
    const val ON_BORDER = 0xFF1677F2
    const val OFF_BORDER = 0xFF94A3B8
    const val ON_BACKGROUND = 0xFFF0F7FF
    const val OFF_BACKGROUND = 0xFFF8FAFC
}

@Composable
internal fun HomeOutlineToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = Color(HomeOutlineToggleStyle.ON_BORDER)
    val inactive = Color(HomeOutlineToggleStyle.OFF_BORDER)
    val accent = if (checked) active else inactive
    val outlineShape = RoundedCornerShape(25.dp)

    Box(
        modifier = modifier
            .size(width = 100.dp, height = 42.dp)
            .clip(outlineShape)
            .background(if (checked) Color(HomeOutlineToggleStyle.ON_BACKGROUND)
                else Color(HomeOutlineToggleStyle.OFF_BACKGROUND))
            .border(2.dp, accent, outlineShape)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
    ) {
        Text(
            text = if (checked) "ON" else "OFF",
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(if (checked) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(start = if (checked) 13.dp else 0.dp,
                         end = if (checked) 0.dp else 12.dp)
        )

        Box(
            modifier = Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(5.dp)
                .size(27.dp)
                .background(accent, CircleShape)
        )
    }
}
