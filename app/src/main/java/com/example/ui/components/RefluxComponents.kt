package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TriggerLikelihood
import com.example.ui.theme.BotanicalCardBg
import com.example.ui.theme.BotanicalCardBorder
import com.example.ui.theme.BotanicalTextPrimary
import com.example.ui.theme.BotanicalTextSecondary
import com.example.ui.theme.TriggerHighRed
import com.example.ui.theme.TriggerHighRedBg
import com.example.ui.theme.TriggerLowGreen
import com.example.ui.theme.TriggerLowGreenBg
import com.example.ui.theme.TriggerMediumAmber
import com.example.ui.theme.TriggerMediumAmberBg
import com.example.ui.theme.TriggerNeutralGrey
import com.example.ui.theme.TriggerNeutralGreyBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HighDensityCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BotanicalCardBg,
    borderColor: Color = BotanicalCardBorder,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        color = backgroundColor
    ) {
        content()
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.3.sp
            ),
            color = BotanicalTextPrimary
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = BotanicalTextSecondary
            )
        }
    }
}

@Composable
fun TriggerBadge(
    likelihood: TriggerLikelihood,
    percentage: Int? = null,
    modifier: Modifier = Modifier
) {
    val (bg, textColor) = when (likelihood) {
        TriggerLikelihood.HOCH -> TriggerHighRedBg to TriggerHighRed
        TriggerLikelihood.MITTEL -> TriggerMediumAmberBg to TriggerMediumAmber
        TriggerLikelihood.GERING -> TriggerLowGreenBg to TriggerLowGreen
        TriggerLikelihood.UNBEKANNT -> TriggerNeutralGreyBg to TriggerNeutralGrey
    }

    val labelText = if (percentage != null && likelihood != TriggerLikelihood.UNBEKANNT) {
        "$percentage% Trigger"
    } else {
        likelihood.label
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = labelText,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
