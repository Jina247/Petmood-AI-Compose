package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.CardSurfaceCream
import com.example.ui.theme.PrimaryButtonYellow
import com.example.ui.theme.TextPrimaryDarkBrown

@Composable
fun PetAvatar(
    photoUri: String?,
    size: Dp = 90.dp,
    showCameraOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("pet_avatar_container")
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        // Main Avatar Circle
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(CardSurfaceCream)
                .border(2.dp, TextPrimaryDarkBrown, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUri.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Pet photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size)
                )
            } else {
                // Paw silhouette fallback list
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = "Default Pet Paw",
                    tint = TextPrimaryDarkBrown.copy(alpha = 0.6f),
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }

        // Camera Icon Overlay (typically bottom-right)
        if (showCameraOverlay) {
            Box(
                modifier = Modifier
                    .size(size * 0.35f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(PrimaryButtonYellow)
                    .border(1.5.dp, TextPrimaryDarkBrown, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Upload Photo Icon",
                    tint = TextPrimaryDarkBrown,
                    modifier = Modifier.size(size * 0.18f)
                )
            }
        }
    }
}
