package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Gender
import com.example.ui.components.PetAvatar
import com.example.ui.theme.CardSurfaceCream
import com.example.ui.theme.PrimaryButtonYellow
import com.example.ui.theme.TextPrimaryDarkBrown
import com.example.ui.theme.WarmCreamBackground
import com.example.viewmodel.PetUiState
import com.example.viewmodel.PetViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetProfileScreen(
    viewModel: PetViewModel,
    onProfileSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // collect input fields from ViewModel
    val petName by viewModel.inputPetName.collectAsState()
    val petAge by viewModel.inputPetAge.collectAsState()
    val petType by viewModel.inputPetType.collectAsState()
    val photoUri by viewModel.inputPetPhotoUri.collectAsState()
    val petGender by viewModel.inputPetGender.collectAsState()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsState()

    val samplePhotos = listOf(
        "https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&q=80&w=400",
        "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&q=80&w=400",
        "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?auto=format&fit=crop&q=80&w=400",
        "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?auto=format&fit=crop&q=80&w=400"
    )

    // navigate away when save succeeds
    LaunchedEffect(uiState) {
        if (uiState is PetUiState.Success) {
            viewModel.resetState()
            onProfileSaved()
        }
    }

    Box(
        modifier = modifier
            .testTag("pet_profile_screen")
            .fillMaxSize()
            .background(WarmCreamBackground)
            .imePadding()
            .navigationBarsPadding()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tell us about your pet",
                color = TextPrimaryDarkBrown,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Help us tailor the health analysis specifically for them",
                color = TextPrimaryDarkBrown.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // Pet Photo
            PetAvatar(
                photoUri = photoUri,
                size = 110.dp,
                showCameraOverlay = true,
                modifier = Modifier
                    .testTag("pet_photo_upload_trigger")
                    .clickable {
                        val currentIndex = samplePhotos.indexOf(photoUri)
                        val nextIndex = (currentIndex + 1) % samplePhotos.size
                        viewModel.onPhotoUriChange(samplePhotos[nextIndex])
                    }
            )

            Text(
                text = "Tap to choose photo",
                color = TextPrimaryDarkBrown.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Pet Name
            TextField(
                value = petName,
                onValueChange = { viewModel.onNameChange(it) },
                label = { Text("Pet's Name", color = TextPrimaryDarkBrown.copy(alpha = 0.6f)) },
                placeholder = { Text("e.g. Bella, Max") },
                singleLine = true,
                modifier = Modifier
                    .testTag("pet_name_input")
                    .fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = TextPrimaryDarkBrown,
                    unfocusedTextColor = TextPrimaryDarkBrown,
                    focusedIndicatorColor = TextPrimaryDarkBrown,
                    unfocusedIndicatorColor = TextPrimaryDarkBrown.copy(alpha = 0.3f),
                    cursorColor = TextPrimaryDarkBrown
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Pet Type
            Text(
                text = "Pet Type",
                color = TextPrimaryDarkBrown,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .testTag("pet_type_selector_row")
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(CardSurfaceCream, RoundedCornerShape(24.dp))
                    .border(1.dp, TextPrimaryDarkBrown.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Cat", "Dog", "Other").forEach { type ->
                    val isSelected = petType == type
                    Box(
                        modifier = Modifier
                            .testTag("pet_type_option_$type")
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isSelected) PrimaryButtonYellow else Color.Transparent)
                            .clickable { viewModel.onTypeChange(type) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type,
                            color = TextPrimaryDarkBrown,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pet Gender
            Text(
                text = "Gender",
                color = TextPrimaryDarkBrown,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .testTag("pet_gender_selector_row")
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(CardSurfaceCream, RoundedCornerShape(24.dp))
                    .border(1.dp, TextPrimaryDarkBrown.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Gender.entries.forEach { gender ->
                    val isSelected = petGender == gender
                    Box(
                        modifier = Modifier
                            .testTag("pet_type_option_$gender")
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isSelected) PrimaryButtonYellow else Color.Transparent)
                            .clickable { viewModel.onGenderChange(gender) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = gender.name.capitalizeWords(),
                            color = TextPrimaryDarkBrown,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pet Age
            TextField(
                value = petAge,
                onValueChange = { 
                    // Optional: only allow digits
                    if (it.all { char -> char.isDigit() }) {
                        viewModel.onAgeChange(it)
                    }
                },
                label = { Text("Pet's Age", color = TextPrimaryDarkBrown.copy(alpha = 0.6f)) },
                placeholder = { Text("e.g. 3") },
                singleLine = true,
                modifier = Modifier
                    .testTag("pet_age_input")
                    .fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = TextPrimaryDarkBrown,
                    unfocusedTextColor = TextPrimaryDarkBrown,
                    focusedIndicatorColor = TextPrimaryDarkBrown,
                    unfocusedIndicatorColor = TextPrimaryDarkBrown.copy(alpha = 0.3f),
                    cursorColor = TextPrimaryDarkBrown
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // show error if any
            if (uiState is PetUiState.Error) {
                Text(
                    text = (uiState as PetUiState.Error).message,
                    color = Color.Red,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = { viewModel.savePetProfile() },
                enabled = isSaveEnabled,
                modifier = Modifier
                    .testTag("save_profile_button")
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryButtonYellow,
                    contentColor = TextPrimaryDarkBrown,
                    disabledContainerColor = PrimaryButtonYellow.copy(alpha = 0.5f),
                    disabledContentColor = TextPrimaryDarkBrown.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(26.dp)
            ) {
                if (uiState is PetUiState.Loading) {
                    CircularProgressIndicator(
                        color = TextPrimaryDarkBrown,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Save Profile",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}
