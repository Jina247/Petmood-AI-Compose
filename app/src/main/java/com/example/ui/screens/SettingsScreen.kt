package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSurfaceCream
import com.example.ui.theme.TextPrimaryDarkBrown
import com.example.ui.theme.WarmCreamBackground
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.ScanViewModel

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    scanViewModel: ScanViewModel,
    onEditPetProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val petProfile by scanViewModel.petProfile.collectAsState()

    val userName = authViewModel.getUserName()
    val userEmail = authViewModel.getUserEmail()

    var abnormalAlerts by remember { mutableStateOf(true) }
    var dailyReminders by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About PetMood AI", color = TextPrimaryDarkBrown) },
            text = {
                Text(
                    text = "PetMood AI is a personal companion app engineered specifically for first-time pet owners. It utilizes visual-audio evaluation systems fine-tuned by AI structures like Google Gemini to inspect subtle gestures, sounds, and postures, helping detect distress symptoms early.",
                    color = TextPrimaryDarkBrown,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = TextPrimaryDarkBrown, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardSurfaceCream
        )
    }

    Box(
        modifier = modifier
            .testTag("settings_screen")
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

            // Screen Header
            Text(
                text = "Settings",
                color = TextPrimaryDarkBrown,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Start
            )

            Spacer(modifier = Modifier.height(20.dp))

            // User Profile Section
            SettingsSectionCard(title = "User Information", icon = Icons.Default.Person) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = userName,
                            color = TextPrimaryDarkBrown,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userEmail,
                            color = TextPrimaryDarkBrown.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "Edit",
                        color = TextPrimaryDarkBrown,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .testTag("edit_user_profile_button")
                            .clickable {
                                Toast.makeText(context, "User profile editing is managed by authorization server.", Toast.LENGTH_SHORT).show()
                            }
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pet Profile Section
            SettingsSectionCard(title = "Pet Profile", icon = Icons.Default.Pets) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val petName = petProfile?.name ?: "Bella"
                        val petType = petProfile?.petType ?: "Cat"
                        val petAge = petProfile?.age ?: "2 years"
                        
                        Text(
                            text = petName.replaceFirstChar { it.uppercase() },
                            color = TextPrimaryDarkBrown,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$petType • $petAge year(s) old",
                            color = TextPrimaryDarkBrown.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "Edit",
                        color = TextPrimaryDarkBrown,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .testTag("edit_pet_profile_button")
                            .clickable { onEditPetProfile() }
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notification preferences Section
            SettingsSectionCard(title = "Notification Preferences", icon = Icons.Default.Notifications) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Abnormal Mood Alerts",
                                color = TextPrimaryDarkBrown,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Notify immediately if stressful/pain cues are analyzed",
                                color = TextPrimaryDarkBrown.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = abnormalAlerts,
                            onCheckedChange = { abnormalAlerts = it },
                            modifier = Modifier.testTag("abnormal_alerts_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TextPrimaryDarkBrown,
                                uncheckedThumbColor = TextPrimaryDarkBrown.copy(alpha = 0.5f),
                                uncheckedTrackColor = CardSurfaceCream
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Health Check-ins",
                                color = TextPrimaryDarkBrown,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Remind me to scan Bella every afternoon",
                                color = TextPrimaryDarkBrown.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = dailyReminders,
                            onCheckedChange = { dailyReminders = it },
                            modifier = Modifier.testTag("daily_reminders_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TextPrimaryDarkBrown,
                                uncheckedThumbColor = TextPrimaryDarkBrown.copy(alpha = 0.5f),
                                uncheckedTrackColor = CardSurfaceCream
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, TextPrimaryDarkBrown, RoundedCornerShape(16.dp))
                    .clickable { showAboutDialog = true }
                    .testTag("about_app_row"),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceCream),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About icon",
                        tint = TextPrimaryDarkBrown,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "About PetMood AI",
                        color = TextPrimaryDarkBrown,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Log Out Button (text button, NOT yellow - use muted color)
            Button(
                onClick = { onLogout() },
                modifier = Modifier
                    .testTag("logout_button")
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFC53030) // Muted deep red as requested
                ),
                border = BorderStroke(1.5.dp, Color(0xFFC53030)),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text(
                    text = "Log Out",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(120.dp)) // Clearance padding
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, TextPrimaryDarkBrown, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceCream),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextPrimaryDarkBrown,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = TextPrimaryDarkBrown,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = TextPrimaryDarkBrown.copy(alpha = 0.15f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            
            content()
        }
    }
}
