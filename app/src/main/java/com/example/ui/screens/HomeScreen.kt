package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PetProfile
import com.example.data.repository.PetRepository
import com.example.ui.components.MoodTagBadge
import com.example.ui.components.PetAvatar
import com.example.ui.components.ScanHistoryCard
import com.example.ui.theme.CardSurfaceCream
import com.example.ui.theme.PrimaryButtonYellow
import com.example.ui.theme.TextPrimaryDarkBrown
import com.example.ui.theme.WarmCreamBackground
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.HistoryViewModel
import com.example.viewmodel.ScanViewModel

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    scanViewModel: ScanViewModel,
    historyViewModel: HistoryViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToScanDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val petProfile by scanViewModel.petProfile.collectAsState()
    val scanHistory by historyViewModel.scanHistory.collectAsState()
    val latestScan by historyViewModel.latestScan.collectAsState()

    val mood = latestScan?.mood ?: "No mood available"
    val summary = latestScan?.summary ?: "No summary available"
    val ownerName = authViewModel.getUserName()
    val petName = petProfile?.name ?: "your pet"

    // Latest scan for the card summary
    Box(
        modifier = modifier
            .testTag("home_screen")
            .fillMaxSize()
            .background(WarmCreamBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
        ) {
            // Header: Greeting + Avatar side-by-side
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hi $ownerName! 👋",
                            color = TextPrimaryDarkBrown,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "How is ${petName.replaceFirstChar { it.uppercase() }} today?",
                            color = TextPrimaryDarkBrown.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    PetAvatar(
                        photoUri = petProfile?.photoUri,
                        size = 64.dp,
                        modifier = Modifier
                            .testTag("home_pet_avatar_trigger")
                            .clickable { onNavigateToProfile() }
                    )
                }
            }

            // Today's Mood Summary Card (#FFECD2 surface)
            item {
                Card(
                    modifier = Modifier
                        .testTag("today_mood_card")
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .border(1.5.dp, TextPrimaryDarkBrown, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceCream),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Latest Health Diagnostic",
                            color = TextPrimaryDarkBrown.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        if (latestScan != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Current Mood:",
                                    color = TextPrimaryDarkBrown,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                MoodTagBadge(mood = mood)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = summary,
                                color = TextPrimaryDarkBrown,
                                fontSize = 14.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = "No mood scans for today",
                                color = TextPrimaryDarkBrown,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Analyze your pet's video with Gemini inside our smart scanner to see results here.",
                                color = TextPrimaryDarkBrown.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Scan Now prominent CTA
            item {
                Button(
                    onClick = { onNavigateToScan() },
                    modifier = Modifier
                        .testTag("scan_now_cta")
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryButtonYellow,
                        contentColor = TextPrimaryDarkBrown
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan icon",
                        tint = TextPrimaryDarkBrown,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan Now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Recent History heading
            item {
                Text(
                    text = "Recent Scans",
                    color = TextPrimaryDarkBrown,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
            }

            // Preview last 2-3 logs
            val recentScans = scanHistory?.take(3)
            if (recentScans?.isEmpty() == true) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recorded behaviors yet. Tap 'Scan Now' to inspect their body language!",
                            color = TextPrimaryDarkBrown.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                items(recentScans ?: emptyList()) { scan ->
                    ScanHistoryCard(
                        scan = scan,
                        onClick = { onNavigateToScanDetails(scan.id) },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
        }
    }
}
