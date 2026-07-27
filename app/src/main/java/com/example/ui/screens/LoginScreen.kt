package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val email by viewModel.loginEmail.collectAsState()
    val password by viewModel.loginPassword.collectAsState()
    val isPasswordVisible by viewModel.isLoginPasswordVisible.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Handle success transition
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            viewModel.resetState()
            onLoginSuccess()
        }
    }

    Box(
        modifier = modifier
            .testTag("login_screen")
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Cute Logo: Paw + Sparkle centered at top
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = "Paw Icon",
                    tint = TextPrimaryDarkBrown,
                    modifier = Modifier.size(72.dp)
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Sparkle Icon",
                    tint = PrimaryButtonYellow,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.TopEnd)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "PetMood AI",
                color = TextPrimaryDarkBrown,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displayLarge
            )

            Text(
                text = "Decode your furry companion's health & emotion",
                color = TextPrimaryDarkBrown.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Form inputs
            TextField(
                value = email,
                onValueChange = { viewModel.loginEmail.value = it },
                label = { Text("Email Address", color = TextPrimaryDarkBrown.copy(alpha = 0.6f)) },
                placeholder = { Text("yourname@example.com") },
                singleLine = true,
                modifier = Modifier
                    .testTag("email_input")
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

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = password,
                onValueChange = { viewModel.loginPassword.value = it },
                label = { Text("Password", color = TextPrimaryDarkBrown.copy(alpha = 0.6f)) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    val description = if (isPasswordVisible) "Hide password" else "Show password"
                    IconButton(
                        onClick = { viewModel.isLoginPasswordVisible.value = !isPasswordVisible },
                        modifier = Modifier.testTag("toggle_password_visibility")
                    ) {
                        Icon(imageVector = image, contentDescription = description, tint = TextPrimaryDarkBrown)
                    }
                },
                modifier = Modifier
                    .testTag("password_input")
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

            // Error display
            if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = Color(0xFFC53030),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .testTag("login_error_message")
                        .padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login yellow button
            Button(
                onClick = { viewModel.login() },
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier
                    .testTag("login_button")
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
                        text = "Log In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Register Link
            Text(
                text = "Don't have an account? Register",
                color = TextPrimaryDarkBrown,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .testTag("register_link")
                    .clickable { onNavigateToRegister() }
                    .padding(8.dp)
            )
        }
    }
}
