package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PetProfile
import com.example.data.model.ScanResult
import com.example.data.repository.PetRepository
import com.example.data.repository.ScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Uploading : ScanUiState
    object Analysing : ScanUiState
    data class Success(val result: ScanResult) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

data class SelectedPhoto(val id: String = UUID.randomUUID().toString(), val uri: Uri)

class ScanViewModel(
    private val scanRepository: ScanRepository,
    private val petRepository: PetRepository)
    : ViewModel() {

    companion object {
        private const val TAG = "ScanViewModel"
        private val ALLOWED_VIDEO_MIME_TYPES = setOf(
            "video/mp4", "video/mpeg", "video/quicktime", "video/webm", "video/3gpp"
        )
        private const val MAX_VIDEO_UPLOAD_BYTES = 50L * 1024 * 1024 // 50MB
        private val ALLOWED_PHOTO_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        private const val MAX_PHOTO_UPLOAD_BYTES = 4L * 1024 * 1024 // 4MB
        const val MAX_PHOTOS = 3 // optional supporting photos alongside the required video
        private const val POLL_MAX_ATTEMPTS = 30
        private const val POLL_INTERVAL_MS = 2_500L // 30 * 2.5s = 75s hard ceiling
    }

    private val _scanUiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanUiState: StateFlow<ScanUiState> = _scanUiState.asStateFlow()

    // Active screen result
    private val _currentScanResult = MutableStateFlow<ScanResult?>(null)
    val currentScanResult: StateFlow<ScanResult?> = _currentScanResult.asStateFlow()

    // Optional extras a scan can carry alongside its required video.
    private val _selectedPhotos = MutableStateFlow<List<SelectedPhoto>>(emptyList())
    val selectedPhotos: StateFlow<List<SelectedPhoto>> = _selectedPhotos.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    val canAddMorePhotos: StateFlow<Boolean> = _selectedPhotos
        .map { it.size < MAX_PHOTOS }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Profile input states (Setup / Editing)
    private val _inputPetName = MutableStateFlow("")
    val inputPetName: StateFlow<String> = _inputPetName.asStateFlow()

    private val _inputPetAge = MutableStateFlow("")
    val inputPetAge: StateFlow<String> = _inputPetAge.asStateFlow()

    private val _inputPetType = MutableStateFlow("Cat")
    val inputPetType: StateFlow<String> = _inputPetType.asStateFlow()

    private val _inputPetPhotoUri = MutableStateFlow<String?>(null)
    val inputPetPhotoUri: StateFlow<String?> = _inputPetPhotoUri.asStateFlow()

    private val _inputPetGender = MutableStateFlow("unknown")
    val inputPetGender: StateFlow<String> = _inputPetGender.asStateFlow()

    // Save profile status
    private val _profileSaveSuccess = MutableStateFlow(false)
    val profileSaveSuccess = _profileSaveSuccess.asStateFlow()

    private val _petProfile = MutableStateFlow<PetProfile?>(null)
    val petProfile: StateFlow<PetProfile?> = _petProfile.asStateFlow()

    /**
     * Re-fetches the current pet. Must be called explicitly (on login, and after a pet is
     * created/updated) rather than relying on first-subscription caching — MainActivity holds a
     * permanent top-level collector on [petProfile], so a passive stateIn(WhileSubscribed(...))
     * here would fetch once at app launch (before login/pet creation) and never refresh again.
     */
    fun refreshPetProfile() {
        viewModelScope.launch {
            _petProfile.value = try {
                petRepository.getPets().firstOrNull()
            } catch (_: Exception) {
                null
            }
        }
    }

    fun resetProfileSaveState() {
        _profileSaveSuccess.value = false
    }

    /** Adds up to however much room remains toward [MAX_PHOTOS]; extras beyond that are dropped
     * silently since the picker's own maxItems cap already prevents this in the common case. */
    fun onPhotosPicked(uris: List<Uri>) {
        val room = MAX_PHOTOS - _selectedPhotos.value.size
        if (room <= 0) return
        _selectedPhotos.update { it + uris.take(room).map { uri -> SelectedPhoto(uri = uri) } }
    }

    fun removePhoto(id: String) {
        _selectedPhotos.update { list -> list.filterNot { it.id == id } }
    }

    fun updateDescription(text: String) {
        _description.value = text
    }

    /**
     * Entry point for the real gallery-picker flow: uploads the video at [videoUri] (plus
     * whatever's currently selected in [selectedPhotos]/[description]), then polls until the
     * scan resolves (or times out).
     *
     * [petId] should come from [petProfile] — the app only supports one pet per account today,
     * so there's no separate "selected pet" concept to track independently.
     */
    fun startScan(petId: String, videoUri: Uri, context: Context) {
        viewModelScope.launch {
            _scanUiState.value = ScanUiState.Uploading
            _currentScanResult.value = null

            val prepared = try {
                copyVideoUriToCacheFile(context, videoUri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read video from $videoUri", e)
                _scanUiState.value = ScanUiState.Error(
                    "Couldn't read the selected video. Please pick another file."
                )
                return@launch
            }
            performUploadAndPoll(petId, prepared.first, prepared.second, context)
        }
    }

    /**
     * Entry point for the in-app CameraX recording flow in ScannerScreen, which already
     * hands us a recorded File directly instead of a content Uri. Shares the same
     * upload+poll logic. Counterpart to [startScan], which handles gallery-picked videos.
     */
    fun startAnalysis(videoFile: File, context: Context) {
        val petId = _petProfile.value?.id
        if (petId == null) {
            Log.w(TAG, "startAnalysis called with no pet profile loaded yet")
            _scanUiState.value = ScanUiState.Error(
                "We couldn't find your pet profile. Please set up your pet first."
            )
            return
        }
        viewModelScope.launch {
            performUploadAndPoll(petId, videoFile, guessMimeType(videoFile), context)
        }
    }

    private suspend fun performUploadAndPoll(petId: String, file: File, mimeType: String, context: Context) {
        _scanUiState.value = ScanUiState.Uploading
        _currentScanResult.value = null

        // Snapshot the currently-selected extras and clear them immediately — the next scan
        // (whether this one succeeds, fails, or times out) should start from a blank slate,
        // same as the video itself always requiring a fresh record/pick.
        val photosToUpload = _selectedPhotos.value
        val descriptionToUpload = _description.value.takeIf { it.isNotBlank() }
        _selectedPhotos.value = emptyList()
        _description.value = ""

        // Client-side pre-checks so we can surface a specific reason instead of a generic
        // "upload failed" for the documented 400 cases.
        if (mimeType !in ALLOWED_VIDEO_MIME_TYPES) {
            Log.w(TAG, "Rejected upload client-side: unsupported mime type '$mimeType'")
            _scanUiState.value = ScanUiState.Error(
                "Unsupported video format ($mimeType). Please use MP4, MOV, WEBM, MPEG, or 3GP."
            )
            file.delete()
            return
        }
        if (file.length() > MAX_VIDEO_UPLOAD_BYTES) {
            Log.w(TAG, "Rejected upload client-side: file too large (${file.length()} bytes)")
            _scanUiState.value = ScanUiState.Error(
                "This video is too large (max 50MB). Please trim it and try again."
            )
            file.delete()
            return
        }

        val preparedPhotos = mutableListOf<Pair<File, String>>()
        try {
            for (photo in photosToUpload) {
                preparedPhotos.add(copyPhotoUriToCacheFile(context, photo.uri))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read a selected photo", e)
            file.delete()
            preparedPhotos.forEach { it.first.delete() }
            _scanUiState.value = ScanUiState.Error("Couldn't read one of the selected photos. Please try again.")
            return
        }

        val badPhotoMime = preparedPhotos.firstOrNull { it.second !in ALLOWED_PHOTO_MIME_TYPES }
        if (badPhotoMime != null) {
            Log.w(TAG, "Rejected upload client-side: unsupported photo mime type '${badPhotoMime.second}'")
            file.delete()
            preparedPhotos.forEach { it.first.delete() }
            _scanUiState.value = ScanUiState.Error("Unsupported photo format. Please use JPEG, PNG, or WEBP.")
            return
        }
        val tooLargePhoto = preparedPhotos.firstOrNull { it.first.length() > MAX_PHOTO_UPLOAD_BYTES }
        if (tooLargePhoto != null) {
            Log.w(TAG, "Rejected upload client-side: photo too large (${tooLargePhoto.first.length()} bytes)")
            file.delete()
            preparedPhotos.forEach { it.first.delete() }
            _scanUiState.value = ScanUiState.Error("One of your photos is too large (max 4MB each).")
            return
        }

        val uploadResult = scanRepository.uploadScan(petId, file, mimeType, preparedPhotos, descriptionToUpload)
        file.delete() // temp copies no longer needed either way
        preparedPhotos.forEach { it.first.delete() }

        val uploaded = uploadResult.getOrElse { e ->
            val httpBody = (e as? HttpException)?.response()?.errorBody()?.string()
            Log.e(TAG, "Upload failed (petId=$petId, code=${(e as? HttpException)?.code()}, body=$httpBody)", e)
            _scanUiState.value = ScanUiState.Error(mapUploadError(e))
            return
        }

        // Spec says upload always comes back "processing", but handle an immediate
        // terminal status defensively before entering the poll loop.
        if (uploaded.status != "processing") {
            resolveTerminalScan(uploaded)
            return
        }

        pollForResult(petId, uploaded.id)
    }

    private suspend fun pollForResult(petId: String, scanId: String) {
        _scanUiState.value = ScanUiState.Analysing
        var attempts = 0
        while (attempts < POLL_MAX_ATTEMPTS) {
            delay(POLL_INTERVAL_MS.milliseconds)
            attempts++

            val scan = scanRepository.getScan(petId, scanId).getOrNull() ?: continue
            if (scan.status != "processing") {
                resolveTerminalScan(scan)
                return
            }
            // still "processing" (or a transient poll error) — keep going until the ceiling
        }

        val ceilingSeconds = (POLL_MAX_ATTEMPTS * POLL_INTERVAL_MS) / 1000
        Log.w(TAG, "Poll timed out after $attempts attempts (scanId=$scanId)")
        _scanUiState.value = ScanUiState.Error(
            "Analysis is taking longer than expected (over ${ceilingSeconds}s). " +
                "Check History in a bit — it may still finish in the background."
        )
    }

    private fun resolveTerminalScan(scan: ScanResult) {
        if (scan.status == "failed") {
            Log.e(TAG, "Scan failed (scanId=${scan.id}): ${scan.errorMessage}")
            _scanUiState.value = ScanUiState.Error(
                scan.errorMessage ?: "Analysis failed. Please try again."
            )
        } else {
            _currentScanResult.value = scan
            _scanUiState.value = ScanUiState.Success(scan)
        }
    }

    private fun mapUploadError(e: Throwable): String = when (e) {
        is HttpException if e.code() == 400 ->
            "Upload rejected — check that your video is MP4/MOV/WEBM/MPEG/3GP and under 50MB, " +
                "and any attached photos are JPEG/PNG/WEBP and under 4MB each."

        is HttpException if e.code() == 429 ->
            "You've reached the limit of 5 scans per hour. Please try again later."

        else -> e.message ?: "Upload failed. Please check your connection and try again."
    }

    private suspend fun copyVideoUriToCacheFile(context: Context, uri: Uri): Pair<File, String> =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "video/mp4"
            val extension = when (mimeType) {
                "video/mp4" -> "mp4"
                "video/mpeg" -> "mpeg"
                "video/quicktime" -> "mov"
                "video/webm" -> "webm"
                "video/3gpp" -> "3gp"
                else -> "mp4"
            }
            val outputFile = File(context.cacheDir, "scan_upload_${System.currentTimeMillis()}.$extension")
            val input = resolver.openInputStream(uri)
                ?: throw IllegalStateException("Unable to open selected video")
            input.use { stream ->
                outputFile.outputStream().use { output -> stream.copyTo(output) }
            }
            outputFile to mimeType
        }

    private suspend fun copyPhotoUriToCacheFile(context: Context, uri: Uri): Pair<File, String> =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val extension = when (mimeType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val outputFile = File.createTempFile("scan_photo_", ".$extension", context.cacheDir)
            val input = resolver.openInputStream(uri)
                ?: throw IllegalStateException("Unable to open selected photo")
            input.use { stream ->
                outputFile.outputStream().use { output -> stream.copyTo(output) }
            }
            outputFile to mimeType
        }

    private fun guessMimeType(file: File): String = when (file.extension.lowercase()) {
        "mp4" -> "video/mp4"
        "mpeg", "mpg" -> "video/mpeg"
        "mov" -> "video/quicktime"
        "webm" -> "video/webm"
        "3gp" -> "video/3gpp"
        else -> "video/mp4"
    }

    fun clearActiveResult() {
        _currentScanResult.value = null
        _scanUiState.value = ScanUiState.Idle
    }

    fun showScanDetails(scan: ScanResult) {
        _currentScanResult.value = scan
        _scanUiState.value = ScanUiState.Success(scan)
    }
}
