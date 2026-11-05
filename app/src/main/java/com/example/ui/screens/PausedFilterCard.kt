package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Greys out a card and prevents edits while a higher-priority mode is selected.
 * Never changes model values, callbacks, underlying rules, or storage.
 */
@Composable
internal fun PausedFilterCard(
    reason: String?,
    content: @Composable () -> Unit
) {
    if (reason == null) {
        content()
        return
    }
    val interactionSource = remember { MutableInteractionSource() }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().alpha(0.45f)) {
                content()
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .semantics {
                        disabled()
                        contentDescription = "Paused by $reason; saved settings unchanged"
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {}
                    )
            )
        }
        Text(
            text = "Paused by $reason • Saved settings preserved",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 5.dp)
        )
    }
}
