package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.core.app.NotificationManagerCompat
import com.example.ui.theme.PrimaryButtonYellow
import com.example.ui.theme.TextPrimaryDarkBrown
import com.example.ui.theme.WarmCreamBackground
import com.example.viewmodel.ScanUiState
import com.example.viewmodel.ScanViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun AnalysingScreen(
    viewModel: ScanViewModel,
    onAnalysisFinished: () -> Unit,
    onAnalysisFailed: (String) -> Unit,
    onContinueInBackground: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val petProfile by viewModel.petProfile.collectAsState()
    val scanUiState by viewModel.scanUiState.collectAsState()

    val petName = (petProfile?.name ?: "your pet").replaceFirstChar { it.uppercase() }
    val isUploading = scanUiState is ScanUiState.Uploading

    // The backend only reports "processing", so there's no real percentage to show. Instead
    // of a fake progress bar, cycle through what the analysis is doing.
    val analysingMessages = listOf(
        "Watching $petName's body language…",
        "Reading ears, tail and posture…",
        "Putting together suggestions for you…"
    )
    var messageIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(isUploading) {
        if (isUploading) return@LaunchedEffect
        while (true) {
            delay(6.seconds)
            messageIndex = (messageIndex + 1) % analysingMessages.size
        }
    }

    LaunchedEffect(scanUiState) {
        when (val state = scanUiState) {
            is ScanUiState.Success -> {
                delay(400.milliseconds) // just long enough to not feel like a jump cut
                onAnalysisFinished()
            }
            is ScanUiState.Error -> onAnalysisFailed(state.message)
            else -> Unit
        }
    }

    // Leaving is only safe once the upload is done: after that the result is produced
    // server-side and arrives as a push (and in History) even if this screen is gone.
    val notificationsEnabled = remember {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

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

            Text(
                text = if (isUploading) "Uploading $petName's video…" else "Analysing $petName's mood",
                color = TextPrimaryDarkBrown,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = if (isUploading) "Keep the app open until the upload finishes"
                else analysingMessages[messageIndex],
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "analysing_message"
            ) { message ->
                Text(
                    text = message,
                    color = TextPrimaryDarkBrown.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .testTag("scan_analysis_progress")
                    .fillMaxWidth(0.8f)
                    .height(6.dp),
                color = PrimaryButtonYellow,
                trackColor = TextPrimaryDarkBrown.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            AnimatedVisibility(visible = scanUiState is ScanUiState.Analysing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        onClick = onContinueInBackground,
                        modifier = Modifier
                            .testTag("continue_in_background_button")
                            .fillMaxWidth(0.8f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDarkBrown),
                        border = BorderStroke(2.dp, TextPrimaryDarkBrown),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = if (notificationsEnabled) "Notify me when it's ready" else "Continue in background",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!notificationsEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notifications are off — the result will show up in History.",
                            color = TextPrimaryDarkBrown.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
