package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryButtonYellow
import com.example.ui.theme.TextPrimaryDarkBrown
import com.example.ui.theme.WarmCreamBackground
import com.example.viewmodel.ScanUiState
import com.example.viewmodel.ScanViewModel
import kotlinx.coroutines.delay

@Composable
fun AnalysingScreen(
    viewModel: ScanViewModel,
    onAnalysisFinished: () -> Unit,
    onAnalysisFailed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val petProfile by viewModel.petProfile.collectAsState()
    val scanUiState by viewModel.scanUiState.collectAsState()

    val petName = petProfile?.name ?: "your pet"

    // Simulate progress bar value increase
    var progressVal by remember { mutableStateOf(0.0f) }

    LaunchedEffect(Unit) {
        while (progressVal < 0.95f) {
            delay(150)
            progressVal += 0.05f
        }
    }

    LaunchedEffect(scanUiState) {
        if (scanUiState is ScanUiState.Success) {
            progressVal = 1.0f
            delay(300)
            onAnalysisFinished()
        } else if (scanUiState is ScanUiState.Error) {
            onAnalysisFailed((scanUiState as ScanUiState.Error).message)
        }
    }

    // Centered rotating or pulsing paw logo animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_paw")
    val pawScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .testTag("analysing_screen")
            .fillMaxSize()
            .background(WarmCreamBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Heart of the screen: Animated Yellow Paw logo
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pawScale),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = "Analysing paw indicator",
                    tint = PrimaryButtonYellow,
                    modifier = Modifier.size(96.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Loading message with PetName
            Text(
                text = "Analysing ${petName.replaceFirstChar { it.uppercase() }}'s behavior…",
                color = TextPrimaryDarkBrown,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "This may take a few seconds",
                color = TextPrimaryDarkBrown.copy(alpha = 0.6f),
                fontSize = 14.sp,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Linear Progress Indicator
            LinearProgressIndicator(
                progress = { progressVal },
                modifier = Modifier
                    .testTag("scan_analysis_progress")
                    .fillMaxWidth(0.8f)
                    .height(6.dp),
                color = PrimaryButtonYellow,
                trackColor = TextPrimaryDarkBrown.copy(alpha = 0.1f)
            )
        }
    }
}
