package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.PrimaryButtonYellow
import com.example.ui.theme.TextPrimaryDarkBrown
import java.io.File

/**
 * Current CAMERA permission status plus a way to (re)request it.
 * Kept separate from [CameraPreview] so a screen can decide *when* to prompt
 * (e.g. once on first composition) while the preview just reacts to the result.
 */
data class CameraPermissionState(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit
)

@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    return remember(hasPermission, launcher) {
        CameraPermissionState(
            hasPermission = hasPermission,
            requestPermission = { launcher.launch(Manifest.permission.CAMERA) }
        )
    }
}

/**
 * Drives a bound [VideoCapture] use case: starts/stops a muted (video-only)
 * recording and reports [VideoRecordEvent]s back to the caller.
 *
 * [discardAndStop] is the "the screen is going away mid-recording" exit —
 * it still stops the in-flight [Recording] (which still fires a `Finalize`
 * event internally), but swallows that event instead of forwarding it, so a
 * caller that's already navigating away never gets an upload/navigate
 * callback for a recording it abandoned.
 */
class VideoRecorderController(
    private val context: Context,
    private val videoCapture: VideoCapture<Recorder>
) {
    private var activeRecording: Recording? = null
    private var discardResult = false

    fun startRecording(outputFile: File, onEvent: (VideoRecordEvent) -> Unit) {
        discardResult = false
        val outputOptions = FileOutputOptions.Builder(outputFile).build()
        activeRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            // No withAudioEnabled(...) — muted by design, so no RECORD_AUDIO permission needed.
            .start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize && discardResult) {
                    outputFile.delete()
                    return@start
                }
                onEvent(event)
            }
    }

    /** Manual stop or the auto-cap timer — the caller's Finalize handling still runs. */
    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    /** Leaving the screen mid-recording — stop, but ignore the resulting Finalize. */
    fun discardAndStop() {
        discardResult = true
        stopRecording()
    }
}

/**
 * Live CameraX preview bound to the back camera when permission is granted,
 * otherwise a friendly placeholder that lets the user (re)request it.
 * Also binds a muted [VideoCapture] use case alongside the preview and hands
 * a [VideoRecorderController] for it to [onRecorderReady] once bound — CameraX
 * only allows one active use-case set per [ProcessCameraProvider.bindToLifecycle]
 * call, so both use cases must be built and bound together.
 */
@Composable
fun CameraPreview(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onRecorderReady: (VideoRecorderController) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // ScannerScreen shares the Activity's LifecycleOwner rather than owning its
    // own, so CameraX won't auto-unbind just from navigating to another screen —
    // unbind explicitly once CameraPreview itself leaves composition.
    DisposableEffect(Unit) {
        onDispose { cameraProvider?.unbindAll() }
    }

    // Wrapping Box owns the caller's modifier (e.g. fillMaxSize) and keeps the
    // placeholder centered the same way the AndroidView fills the space.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val provider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = this.surfaceProvider
                            }
                            val recorder = Recorder.Builder()
                                .setQualitySelector(QualitySelector.from(Quality.HD))
                                .build()
                            val videoCapture = VideoCapture.withOutput(recorder)
                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    videoCapture
                                )
                                cameraProvider = provider
                                onRecorderReady(VideoRecorderController(ctx, videoCapture))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
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
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryButtonYellow, contentColor = TextPrimaryDarkBrown),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
