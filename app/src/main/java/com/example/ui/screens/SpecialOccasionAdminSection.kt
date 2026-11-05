package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.SpecialOccasionOffer

@Composable
internal fun SpecialOccasionAdminSection(
    offer: SpecialOccasionOffer,
    onSave: (title: String, description: String, days: Int, enabled: Boolean) -> Unit
) {
    var title by remember(offer.id, offer.title) { mutableStateOf(offer.title) }
    var description by remember(offer.id, offer.description) { mutableStateOf(offer.description) }
    var days by remember(offer.id, offer.days) { mutableStateOf(offer.days) }
    var enabled by remember(offer.id, offer.enabled) { mutableStateOf(offer.enabled) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Special Occasion Free Plan", fontWeight = FontWeight.Bold)
            Text("Diwali, Dasera or any event. All eligible free users get access while campaign is live. Paid plans stay intact.")
            OutlinedTextField(value = title, onValueChange = { title = it.take(80) },
                label = { Text("Offer title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description,
                onValueChange = { description = it.take(500) },
                label = { Text("Offer message") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Text("Free duration", fontWeight = FontWeight.SemiBold)
            listOf(1, 3, 7, 30, 0).forEach { value ->
                FilterChip(selected = days == value, onClick = { days = value },
                    label = { Text(if (value == 0) "Unlimited (until OFF)" else "$value day(s)") })
            }
            Text("Campaign status: " + if (offer.isLive()) "LIVE" else "OFF / EXPIRED")
            Switch(checked = enabled, onCheckedChange = { enabled = it })
            Text(if (enabled) "ON after publishing" else "OFF after saving")
            Button(onClick = { onSave(title.trim(), description.trim(), days, enabled) },
                enabled = !enabled || (title.isNotBlank() && description.isNotBlank()),
                modifier = Modifier.fillMaxWidth()) {
                Text(if (enabled) "Save & Publish Offer" else "Save / Switch OFF")
            }
            if (offer.enabled) {
                OutlinedButton(onClick = {
                    enabled = false
                    onSave(title.trim(), description.trim(), days, false)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("STOP OFFER NOW")
                }
            }
            Text("1/3/7/30 days run from ON time; Unlimited runs only until OFF. " +
                "Switching OFF ends promotional access (including Unlimited). Existing paid memberships never change.")
        }
    }
}
