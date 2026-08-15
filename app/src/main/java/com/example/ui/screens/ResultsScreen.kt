package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanResult
import com.example.ui.components.MoodTagBadge
import com.example.ui.components.PetAvatar
import com.example.ui.theme.CardSurfaceCream
import com.example.ui.theme.PrimaryButtonYellow
import com.example.ui.theme.TextPrimaryDarkBrown
import com.example.ui.theme.WarmCreamBackground
import com.example.viewmodel.ScanViewModel

@Composable
fun ResultsScreen(
    viewModel: ScanViewModel,
    onScanAgain: () -> Unit,
    onSaveToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val petProfile by viewModel.petProfile.collectAsState()
    val currentScan by viewModel.currentScanResult.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }

    val petName = petProfile?.name ?: "your pet"

    // Default mock data if empty (safety fallback)
    val scan = currentScan ?: ScanResult(
        id = "mock_uuid",
        mood = "Happy / Calm",
        confidence = 0.87,
        summary = "Your cat appears relaxed and content. Their muscles are soft and ears are pointing forward, indicating healthy behavior.",
        timestamp = "2026-06-03T10:30:00Z",
        petName = petName
    )

    Box(
        modifier = modifier
            .testTag("results_screen")
            .fillMaxSize()
            .background(WarmCreamBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Pet Name & Avatar at the top
            PetAvatar(
                photoUri = petProfile?.photoUri,
                size = 80.dp,
                modifier = Modifier.testTag("results_pet_avatar")
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${petName.replaceFirstChar { it.uppercase() }}'s Diagnosis",
                color = TextPrimaryDarkBrown,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Results Card (#FFECD2 surface)
            Card(
                modifier = Modifier
                    .testTag("results_main_card")
                    .fillMaxWidth()
                    .border(1.5.dp, TextPrimaryDarkBrown, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceCream),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large Mood Tag Badge
                    MoodTagBadge(
                        mood = scan.mood ?: "Analyzing...",
                        modifier = Modifier.scale(1.2f) // Slightly larger weight
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Confidence Score
                    Text(
                        text = "${scan.confidence}% confident",
                        color = TextPrimaryDarkBrown,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // AI generated summary text
                    Text(
                        text = scan.summary ?: "No summary available",
                        color = TextPrimaryDarkBrown,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dynamic Expandable "What this means" section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .background(WarmCreamBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, TextPrimaryDarkBrown.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .clickable { isExpanded = !isExpanded }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "What this means",
                                color = TextPrimaryDarkBrown,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand info icon",
                                tint = TextPrimaryDarkBrown
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val meaningText = when (scan.mood) {
                                "Happy / Calm" -> "A happy/calm mood indicates your pet feels fully safe. This is the optimal physical state showing balanced metabolic rates, soft muscles, stable heart rate, and willingness to rest or socialize."
                                "Needs attention" -> "Your pet is signaling a mild immediate boundary/need such as interactive play, thirst, or bathroom access. They are engaging with you rather than showing fear."
                                "Showing stress" -> "This indicates environmental overstimulation. Provide a quiet shelter/crate away from loud audio, keep lighting softer, and let them approach you on their own terms."
                                "In pain" -> "Pain responses represent defensive tensing. Watch closely for low appetite, persistent heavy breathing, guarding of sensitive areas, or sleeping in hidden tight spaces. A checkup is recommended."
                                else -> "A balanced observation period. Keep monitoring."
                            }
                            Text(
                                text = meaningText,
                                color = TextPrimaryDarkBrown.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Suggestions from the photo-scan pipeline. Video-scan results leave
                    // this null/empty, so the section simply doesn't render for them.
                    if (!scan.suggestions.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .testTag("results_suggestions")
                                .fillMaxWidth()
                                .background(WarmCreamBackground, RoundedCornerShape(12.dp))
                                .border(1.dp, TextPrimaryDarkBrown.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Suggestions for you",
                                color = TextPrimaryDarkBrown,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            scan.suggestions.forEach { suggestion ->
                                Row(modifier = Modifier.padding(bottom = 6.dp)) {
                                    Text(
                                        text = "•",
                                        color = TextPrimaryDarkBrown,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = suggestion,
                                        color = TextPrimaryDarkBrown.copy(alpha = 0.8f),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Yellow "Scan Again" button
            Button(
                onClick = {
                    viewModel.clearActiveResult()
                    onScanAgain()
                },
                modifier = Modifier
                    .testTag("scan_again_button")
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryButtonYellow,
                    contentColor = TextPrimaryDarkBrown
                ),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(
                    text = "Scan Again",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Outlined "Save to History" button
            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Scan saved to history successfully!", Toast.LENGTH_SHORT).show()
                    viewModel.clearActiveResult()
                    onSaveToHistory() // Transitions back
                },
                modifier = Modifier
                    .testTag("save_to_history_button")
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimaryDarkBrown
                ),
                border = BorderStroke(2.dp, TextPrimaryDarkBrown),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(
                    text = "Save to History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(120.dp)) // Clearance scroll padding
        }
    }
}
