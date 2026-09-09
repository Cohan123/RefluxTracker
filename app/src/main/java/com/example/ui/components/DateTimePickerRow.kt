package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BotanicalCardBorder
import com.example.ui.theme.BotanicalForest
import com.example.ui.theme.BotanicalMint
import com.example.ui.theme.BotanicalSurface
import com.example.ui.theme.BotanicalTextPrimary
import com.example.ui.theme.BotanicalTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateTimePickerRow(
    timestamp: Long,
    onTimestampChanged: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }

    val dateFormat = SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMAN)
    val timeFormat = SimpleDateFormat("HH:mm 'Uhr'", Locale.GERMAN)

    val dateStr = dateFormat.format(Date(timestamp))
    val timeStr = timeFormat.format(Date(timestamp))

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Datumsauswahl-Karte
            Surface(
                modifier = Modifier
                    .weight(1.3f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, BotanicalCardBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = timestamp
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                }
                                onTimestampChanged(cal.timeInMillis)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .testTag("date_picker_button"),
                color = BotanicalSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Datum wählen",
                        tint = BotanicalForest,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Datum",
                            fontSize = 10.sp,
                            color = BotanicalTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = dateStr,
                            fontSize = 13.sp,
                            color = BotanicalTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Uhrzeitauswahl-Karte
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, BotanicalCardBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = timestamp
                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    set(Calendar.MINUTE, minute)
                                }
                                onTimestampChanged(cal.timeInMillis)
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    }
                    .testTag("time_picker_button"),
                color = BotanicalSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Uhrzeit wählen",
                        tint = BotanicalForest,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Uhrzeit",
                            fontSize = 10.sp,
                            color = BotanicalTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = timeStr,
                            fontSize = 13.sp,
                            color = BotanicalTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Schnellauswahl-Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickTimeChip(
                label = "Jetzt",
                onClick = { onTimestampChanged(System.currentTimeMillis()) }
            )
            QuickTimeChip(
                label = "Vor 30 Min",
                onClick = { onTimestampChanged(System.currentTimeMillis() - 30 * 60 * 1000L) }
            )
            QuickTimeChip(
                label = "Vor 1 Std",
                onClick = { onTimestampChanged(System.currentTimeMillis() - 60 * 60 * 1000L) }
            )
            QuickTimeChip(
                label = "Vor 2 Std",
                onClick = { onTimestampChanged(System.currentTimeMillis() - 120 * 60 * 1000L) }
            )
            QuickTimeChip(
                label = "Gestern selbe Zeit",
                onClick = { onTimestampChanged(timestamp - 24 * 60 * 60 * 1000L) }
            )
        }
    }
}

@Composable
private fun QuickTimeChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, BotanicalCardBorder, RoundedCornerShape(6.dp))
            .background(BotanicalSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = BotanicalForest,
            fontWeight = FontWeight.Medium
        )
    }
}
