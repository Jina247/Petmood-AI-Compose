package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryButtonYellow
import com.example.ui.theme.TextPrimaryDarkBrown
import com.example.ui.theme.WarmCreamBackground
import com.example.viewmodel.AuthUiState
import com.example.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name by viewModel.registerName.collectAsState()
    val email by viewModel.registerEmail.collectAsState()
    val password by viewModel.registerPassword.collectAsState()
    val confirmPassword by viewModel.registerConfirmPassword.collectAsState()
    val isPasswordVisible by viewModel.isRegisterPasswordVisible.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            viewModel.resetState()
            onRegisterSuccess()
        }
    }

    Box(
        modifier = modifier
            .testTag("register_screen")
            .fillMaxSize()
            .background(WarmCreamBackground)
            .imePadding()
            .navigationBarsPadding()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Cute Logo: Paw + Sparkle centered at top
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = "Paw Icon",
                    tint = TextPrimaryDarkBrown,
                    modifier = Modifier.size(56.dp)
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Sparkle Icon",
                    tint = PrimaryButtonYellow,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = "PetMood AI",
                color = TextPrimaryDarkBrown,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displayLarge
            )

            Text(
                text = "Join our community of mindfully caring owners",
                color = TextPrimaryDarkBrown.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Form Fields
            TextField(
                value = name,
                onValueChange = { viewModel.registerName.value = it },
                label = { Text("Your Name", color = TextPrimaryDarkBrown.copy(alpha = 0.6f)) },
                placeholder = { Text("John Doe") },
                singleLine = true,
                modifier = Modifier
                    .testTag("register_name_input")
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

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = email,
                onValueChange = { viewModel.registerEmail.value = it },
                label = { Text("Email Address", color = TextPrimaryDarkBrown.copy(alpha = 0.6f)) },
                placeholder = { Text("yourname@example.com") },
                singleLine = true,
                modifier = Modifier
                    .testTag("register_email_input")
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = password,
                onValueChange = { viewModel.registerPassword.value = it },
                label = { Text("Password", color = TextPrimaryDarkBrown.copy(alpha = 0.6f)) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .testTag("register_password_input")
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = confirmPassword,
                onValueChange = { viewModel.registerConfirmPassword.value = it },
                label = { Text("Confirm Password", color = TextPrimaryDarkBrown.copy(alpha = 0.6f)) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(
                        onClick = { viewModel.isRegisterPasswordVisible.value = !isPasswordVisible },
                        modifier = Modifier.testTag("toggle_register_password_visibility")
                    ) {
                        Icon(imageVector = image, contentDescription = "Toggle mask", tint = TextPrimaryDarkBrown)
                    }
                },
                modifier = Modifier
                    .testTag("register_confirm_password_input")
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = Color(0xFFC53030),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Create account yellow button
            Button(
                onClick = { viewModel.register() },
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier
                    .testTag("create_account_button")
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryButtonYellow,
                    contentColor = TextPrimaryDarkBrown
                ),
                shape = RoundedCornerShape(26.dp)
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(color = TextPrimaryDarkBrown, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Create Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Log In back link
            Text(
                text = "Already have an account? Log in",
                color = TextPrimaryDarkBrown,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .testTag("login_link")
                    .clickable { onNavigateToLogin() }
                    .padding(8.dp)
            )
        }
    }
}
