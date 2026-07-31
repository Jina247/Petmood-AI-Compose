package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.CardSurfaceCream
import com.example.ui.theme.PrimaryButtonYellow
import com.example.ui.theme.TextPrimaryDarkBrown
import com.example.ui.theme.WarmCreamBackground
import com.example.viewmodel.ScanViewModel
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ScannerScreen(
    viewModel: ScanViewModel,
    onNavigateToAnalysing: () -> Unit,
    onClickBack:() -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Camera permission status
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Recording states
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (recordingSeconds < 5) {
                delay(1000.milliseconds)
                recordingSeconds++
            }
            // Finished recording! Create mock temp video file and process
            isRecording = false
            val mockVideoFile = File(context.cacheDir, "pet_scan_${System.currentTimeMillis()}.mp4")
            mockVideoFile.createNewFile()
            viewModel.startAnalysis(mockVideoFile)
            onNavigateToAnalysing()
        }
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
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = this.surfaceProvider
                                    }
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Friendly illustrative camera placeholder
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "No camera permission icon",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Camera access is needed for real-time diagnostic scanning",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryButtonYellow, contentColor = TextPrimaryDarkBrown),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Grant Permission", fontWeight = FontWeight.Bold)
                        }
                    }
                }

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
                                text = "Recording: 0:0$recordingSeconds",
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
                    .clickable(enabled = !isRecording) {
                        isRecording = true
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
                    .clickable {
                        // Immediately simulate file analysis from gallery
                        val mockFile = File(context.cacheDir, "gallery_video.mp4")
                        mockFile.createNewFile()
                        viewModel.startAnalysis(mockFile)
                        onNavigateToAnalysing()
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery upload icon",
                    tint = TextPrimaryDarkBrown,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upload existing video",
                    color = TextPrimaryDarkBrown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

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
