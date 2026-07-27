package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun MoodTagBadge(
    mood: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        mood.contains("Happy", ignoreCase = true) || mood.contains("Calm", ignoreCase = true) -> MoodHappyBlue
        mood.contains("Needs attention", ignoreCase = true) -> MoodNeedsAttentionOrange
        mood.contains("Stress", ignoreCase = true) -> MoodShowingStressPink
        mood.contains("Pain", ignoreCase = true) -> MoodInPainRed
        else -> MoodHappyBlue // Default fallback
    }

    Box(
        modifier = modifier
            .testTag("mood_badge_$mood")
            .background(color = backgroundColor, shape = RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = mood,
            color = TextPrimaryDarkBrown,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
