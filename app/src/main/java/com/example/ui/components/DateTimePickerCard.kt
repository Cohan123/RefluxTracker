package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.DateTimeUtils
import java.util.Calendar

/**
 * Shared timestamp card for entries that can be recorded retroactively.
 *
 * Shows the current date/time and opens the platform date/time pickers. Future
 * timestamps are rejected (with a toast) so entries always stay in the past.
 */
@Composable
fun DateTimePickerCard(
    label: String,
    timestamp: Long,
    onTimestampChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun applyTimestamp(candidate: Long) {
        if (candidate > System.currentTimeMillis() + 60_000L) {
            Toast.makeText(
                context,
                "Der Zeitpunkt darf nicht in der Zukunft liegen.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            onTimestampChange(candidate)
        }
    }

    fun openDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val candidate = Calendar.getInstance().apply {
                    timeInMillis = timestamp
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }.timeInMillis
                applyTimestamp(candidate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Retroactive tracking only: no future dates.
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    fun openTimePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val candidate = Calendar.getInstance().apply {
                    timeInMillis = timestamp
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                applyTimestamp(candidate)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${DateTimeUtils.formatDateWithYear(timestamp)}, " +
                    "${DateTimeUtils.formatTime(timestamp)} Uhr",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { openDatePicker() },
                    modifier = Modifier.testTag("pick_date_button")
                ) {
                    Text("Datum ändern")
                }
                TextButton(
                    onClick = { openTimePicker() },
                    modifier = Modifier.testTag("pick_time_button")
                ) {
                    Text("Uhrzeit ändern")
                }
            }
        }
    }
}
