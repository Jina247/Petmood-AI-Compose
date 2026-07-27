package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryButtonYellow
import com.example.ui.theme.TextPrimaryDarkBrown
import com.example.ui.theme.WarmCreamBackground

sealed class NavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : NavigationItem("home", "Home", Icons.Default.Home)
    object Scan : NavigationItem("scan", "Scan", Icons.Default.QrCodeScanner)
    object History : NavigationItem("history", "History", Icons.Default.History)
    object Settings : NavigationItem("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Scan,
        NavigationItem.History,
        NavigationItem.Settings
    )

    Column(
        modifier = modifier
            .testTag("app_bottom_nav")
            .background(WarmCreamBackground)
            .navigationBarsPadding()
    ) {
        // Soft separator matching dark brown
        Divider(color = TextPrimaryDarkBrown.copy(alpha = 0.15f), thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isActive = currentRoute == item.route
                val tintColor = if (isActive) PrimaryButtonYellow else TextPrimaryDarkBrown.copy(alpha = 0.5f)

                Column(
                    modifier = Modifier
                        .testTag("nav_tab_${item.route}")
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onNavigate(item.route) }
                        .padding(top = 10.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = tintColor,
                        modifier = Modifier.size(26.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = item.title,
                        color = tintColor,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
