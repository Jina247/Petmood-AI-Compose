package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanResult
import com.example.ui.theme.CardSurfaceCream
import com.example.ui.theme.TextPrimaryDarkBrown
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScanHistoryCard(
    scan: ScanResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mood = scan.mood
    val summary: String? = scan.summary
    val displayDate = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(scan.timestamp)
        val formatter = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        date?.let { formatter.format(it) } ?: scan.timestamp
    } catch (e: Exception) {
        scan.timestamp
    }

    Row(
        modifier = modifier
            .testTag("scan_history_card_${scan.id}")
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurfaceCream)
            .border(1.5.dp, TextPrimaryDarkBrown, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PetAvatar(
            photoUri = null, // Cached or customizable local asset
            size = 50.dp
        )
        
        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = displayDate,
                color = TextPrimaryDarkBrown.copy(alpha = 0.7f),
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = summary ?: "No summary recorded",
                color = TextPrimaryDarkBrown,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        MoodTagBadge(
            mood = mood ?: "No mood recorded",
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}
