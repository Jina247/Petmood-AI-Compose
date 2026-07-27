package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ScanResult
import com.example.ui.components.ScanHistoryCard
import com.example.ui.theme.CardSurfaceCream
import com.example.ui.theme.TextPrimaryDarkBrown
import com.example.ui.theme.WarmCreamBackground
import com.example.viewmodel.HistoryUIState
import com.example.viewmodel.HistoryViewModel
import com.example.viewmodel.ScanViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel,
    scanViewModel: ScanViewModel,
    onNavigateToDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scanHistory by historyViewModel.scanHistory.collectAsStateWithLifecycle()
    val uiState by historyViewModel.uiState.collectAsState()
    // Grouping helper
    val groupedHistory = rememberGroupedHistory(scanHistory)

    Box(
        modifier = modifier
            .testTag("history_screen")
            .fillMaxSize()
            .background(WarmCreamBackground)
    ) {
        when (uiState) {
            is HistoryUIState.Loading -> {
                CircularProgressIndicator(color = TextPrimaryDarkBrown)
            }
            is HistoryUIState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
                ) {
                    item {
                        HistoryHeader()
                    }

                    // Group-by rendering (Sticky header style)
                    groupedHistory.forEach { (dateGroup, list) ->
                        stickyHeader(key = dateGroup) {
                            StickyDateHeader(title = dateGroup)
                        }

                        items(list, key = { it.id }) { scan ->
                            ScanHistoryCard(
                                scan = scan,
                                onClick = {
                                    scanViewModel.showScanDetails(scan)
                                    onNavigateToDetails()
                                },
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                }
            }
            is HistoryUIState.Empty -> {
                EmptyHistoryState()
            }
        }
    }
}

@Composable
private fun HistoryHeader() {
    Text(
        text = stringResource(R.string.history_title),
        color = TextPrimaryDarkBrown,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
private fun StickyDateHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(WarmCreamBackground)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = TextPrimaryDarkBrown,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(CardSurfaceCream),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Pets,
                contentDescription = stringResource(R.string.history_empty_paw_content_description),
                tint = TextPrimaryDarkBrown.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.history_empty_title),
            color = TextPrimaryDarkBrown,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.history_empty_description),
            color = TextPrimaryDarkBrown.copy(alpha = 0.6f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun rememberGroupedHistory(scans: List<ScanResult>?): Map<String, List<ScanResult>> {
    val today = stringResource(R.string.history_date_today)
    val yesterday = stringResource(R.string.history_date_yesterday)
    val previousScans = stringResource(R.string.history_date_previous)
    
    return remember(scans, today, yesterday, previousScans) {
        if (scans.isNullOrEmpty()) return@remember emptyMap()
        
        val sortedScans = scans.sortedByDescending { it.timestamp }
        val groups = LinkedHashMap<String, MutableList<ScanResult>>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val prettyFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        
        val todayStr = dateFormat.format(Date())
        val yesterdayStr = dateFormat.format(Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time)

        for (scan in scans) {
            val datePart = scan.timestamp.substringBefore("T")
            val groupTitle = when (datePart) {
                todayStr -> today
                yesterdayStr -> yesterday
                else -> {
                    try {
                        val parsed = isoFormat.parse(scan.timestamp)
                        if (parsed != null) prettyFormat.format(parsed) else previousScans
                    } catch (e: Exception) {
                        previousScans
                    }
                }
            }
            groups.getOrPut(groupTitle) { mutableListOf() }.add(scan)
        }
        groups
    }
}
