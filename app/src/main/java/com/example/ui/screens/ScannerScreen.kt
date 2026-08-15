package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.video.VideoRecordEvent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ui.components.CameraPreview
import com.example.ui.components.VideoRecorderController
import com.example.ui.components.rememberCameraPermissionState
import com.example.ui.theme.CardSurfaceCream
import com.example.ui.theme.PrimaryButtonYellow
import com.example.ui.theme.TextPrimaryDarkBrown
import com.example.ui.theme.WarmCreamBackground
import com.example.viewmodel.ScanUiState
import com.example.viewmodel.ScanViewModel
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration.Companion.seconds

private const val MAX_DESCRIPTION_LENGTH = 500

@Composable
fun ScannerScreen(
    viewModel: ScanViewModel,
    onNavigateToAnalysing: () -> Unit,
    onClickBack:() -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Camera permission status
    val cameraPermissionState = rememberCameraPermissionState()

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.hasPermission) {
            cameraPermissionState.requestPermission()
        }
    }

    val petProfile by viewModel.petProfile.collectAsState()
    val scanUiState by viewModel.scanUiState.collectAsState()
    val isScanInFlight = scanUiState is ScanUiState.Uploading || scanUiState is ScanUiState.Analysing

    val selectedPhotos by viewModel.selectedPhotos.collectAsState()
    val description by viewModel.description.collectAsState()
    val canAddMorePhotos by viewModel.canAddMorePhotos.collectAsState()

    // Tap-to-zoom preview target for a selected supporting photo; null means no preview showing.
    var zoomedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val petId = petProfile?.id
        if (petId == null) {
            Toast.makeText(
                context, "We couldn't find your pet profile. Please set up your pet first.", Toast.LENGTH_SHORT
            ).show()
        } else {
            viewModel.startScan(petId, uri, context)
            onNavigateToAnalysing()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(ScanViewModel.MAX_PHOTOS)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.onPhotosPicked(uris)
    }

    // Recording states
    var recorderController by remember { mutableStateOf<VideoRecorderController?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    // 30s hard cap: at Quality.HD's device-dependent ~8-12 Mbps, a full 60s
    // clip could exceed ScanViewModel's 50MB upload limit on its own — 30s
    // keeps even a worst-case HD clip comfortably under that cap.
    LaunchedEffect(isRecording) {
        if (isRecording) {
            delay(30.seconds)
            if (isRecording) recorderController?.stopRecording()
        }
    }

    // Leaving the screen mid-recording: stop it, but discard the result —
    // don't upload/navigate on behalf of a screen that's already gone.
    DisposableEffect(Unit) {
        onDispose { recorderController?.discardAndStop() }
    }

    // Standard scanning pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .testTag("scanner_screen")
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

            IconButton(onClick = onClickBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back")
            }
            // Title
            Text(
                text = "Smart Scanner",
                color = TextPrimaryDarkBrown,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Large Camera Preview Viewport Area (Rounded Rectangle)
            Box(
                modifier = Modifier
                    .testTag("camera_preview_viewport")
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .border(2.dp, TextPrimaryDarkBrown, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                CameraPreview(
                    hasCameraPermission = cameraPermissionState.hasPermission,
                    onRequestPermission = cameraPermissionState.requestPermission,
                    onRecorderReady = { recorderController = it },
                    modifier = Modifier.fillMaxSize()
                )

                // If recording, show red blinking badge and timer
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Red, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recording: %d:%02d".format(recordingSeconds / 60, recordingSeconds % 60),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instructions text
            Text(
                text = if (isRecording) "Great, keeping camera steady!" else "Point at your pet and hold still",
                color = TextPrimaryDarkBrown,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Large Circular Yellow Record Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PrimaryButtonYellow)
                    .border(3.dp, TextPrimaryDarkBrown, CircleShape)
                    .clickable {
                        val controller = recorderController ?: return@clickable
                        if (!isRecording) {
                            val outputFile = File(context.cacheDir, "pet_scan_${System.currentTimeMillis()}.mp4")
                            isRecording = true
                            controller.startRecording(outputFile) { event ->
                                when (event) {
                                    is VideoRecordEvent.Status ->
                                        recordingSeconds = (event.recordingStats.recordedDurationNanos / 1_000_000_000).toInt()
                                    is VideoRecordEvent.Finalize -> {
                                        isRecording = false
                                        if (!event.hasError()) {
                                            viewModel.startAnalysis(outputFile, context)
                                            onNavigateToAnalysing()
                                        } else {
                                            outputFile.delete()
                                            Toast.makeText(
                                                context, "Recording failed. Please try again.", Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        } else {
                            controller.stopRecording()
                        }
                    }
                    .testTag("record_trigger_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isRecording) 30.dp else 45.dp)
                        .clip(if (isRecording) RoundedCornerShape(4.dp) else CircleShape)
                        .background(Color.Red)
                        .padding(if (isRecording) 0.dp else 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Option to upload existing video from gallery
            Row(
                modifier = Modifier
                    .testTag("gallery_upload_option")
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isScanInFlight) {
                        galleryLauncher.launch("video/*")
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isScanInFlight) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = TextPrimaryDarkBrown
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery upload icon",
                        tint = TextPrimaryDarkBrown,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (scanUiState) {
                        is ScanUiState.Uploading -> "Uploading video…"
                        is ScanUiState.Analysing -> "Analysing…"
                        else -> "Upload existing video"
                    },
                    color = TextPrimaryDarkBrown.copy(alpha = if (isScanInFlight) 0.6f else 1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Optional extras: supporting photos + a text description, attached to whichever
            // video gets recorded/picked next. Purely optional — video alone is still enough.
            Text(
                text = "Add supporting photos or notes (optional):",
                color = TextPrimaryDarkBrown,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier
                    .testTag("photo_thumbnail_row")
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(selectedPhotos, key = { it.id }) { photo ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = "Selected supporting photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { zoomedPhotoUri = photo.uri }
                        )
                        IconButton(
                            onClick = { viewModel.removePhoto(photo.id) },
                            modifier = Modifier
                                .testTag("remove_photo_${photo.id}")
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(22.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove photo",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                if (canAddMorePhotos) {
                    item {
                        Box(
                            modifier = Modifier
                                .testTag("add_photos_tile")
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardSurfaceCream)
                                .border(1.5.dp, TextPrimaryDarkBrown.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .clickable(enabled = !isScanInFlight) {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Add supporting photos",
                                tint = TextPrimaryDarkBrown
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= MAX_DESCRIPTION_LENGTH) viewModel.updateDescription(it) },
                label = { Text("Describe their behavior") },
                placeholder = { Text("e.g. \"meows a lot at night\", \"hides under the bed when guests arrive\"") },
                modifier = Modifier
                    .testTag("description_input")
                    .fillMaxWidth(),
                minLines = 2,
                supportingText = { Text("${description.length}/$MAX_DESCRIPTION_LENGTH") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tip Cards row / section
            Text(
                text = "Tips for accurate AI results:",
                color = TextPrimaryDarkBrown,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tip Card 1
            TipCard(
                icon = Icons.Default.Lightbulb,
                text = "Best results in good lighting. Bring your pet near a window or well-lit area."
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tip Card 2
            TipCard(
                icon = Icons.Default.Info,
                text = "Capture 5–10 seconds of active behavior (posture, body movements or calls)."
            )

            Spacer(modifier = Modifier.height(120.dp)) // Extra space for BottomNav scroll clearance
        }
    }

    // Tap-to-zoom preview overlay for a selected supporting photo
    zoomedPhotoUri?.let { uri ->
        Dialog(onDismissRequest = { zoomedPhotoUri = null }) {
            Box(
                modifier = Modifier
                    .testTag("photo_zoom_preview")
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .clickable { zoomedPhotoUri = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Zoomed supporting photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun TipCard(
    imageVector: ImageVector, // This was previously hardcoded but let's declare custom names
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurfaceCream, RoundedCornerShape(12.dp))
            .border(1.dp, TextPrimaryDarkBrown.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Tip Icon",
            tint = TextPrimaryDarkBrown,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = TextPrimaryDarkBrown,
            fontSize = 13.sp,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// Overload helper for convenience
@Composable
fun TipCard(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurfaceCream, RoundedCornerShape(12.dp))
            .border(1.dp, TextPrimaryDarkBrown.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = "Tip Icon",
            tint = TextPrimaryDarkBrown,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = TextPrimaryDarkBrown,
            fontSize = 13.sp,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
