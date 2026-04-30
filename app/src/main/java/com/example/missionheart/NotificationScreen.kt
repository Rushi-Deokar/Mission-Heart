package com.example.missionheart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// --- Colors ---
private val AppBg = Color(0xFF101216)
private val CardSurface = Color(0xFF1E232C)
private val TextMain = Color(0xFFFFFFFF)
private val TextDim = Color(0xFF9DA3AE)
private val BrandBlue = Color(0xFF3E8BFF)
private val BrandGreen = Color(0xFF00C853)
private val BrandOrange = Color(0xFFFF7043)

// --- Data Models ---
data class NotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val time: String,
    val type: NotificationType,
    val isUnread: Boolean = false
)

enum class NotificationType {
    APPOINTMENT, MEDICINE, OFFER, SYSTEM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(navController: NavController) {
    // --- Mock Data ---
    val todayNotifications = listOf(
        NotificationItem(1, "Appointment Confirmed", "Dr. Aditi is ready for your visit at 10:00 AM.", "2h ago", NotificationType.APPOINTMENT, true),
        NotificationItem(2, "Medicine Reminder", "Time to take your Vitamin D supplement.", "4h ago", NotificationType.MEDICINE, true)
    )

    val yesterdayNotifications = listOf(
        NotificationItem(3, "Lab Report Ready", "Your full body checkup report is now available.", "Yesterday", NotificationType.SYSTEM),
        NotificationItem(4, "20% Off on Medicines", "Use code HEALTH20 for your next order.", "Yesterday", NotificationType.OFFER)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold, color = TextMain) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextMain)
                    }
                },
                actions = {
                    TextButton(onClick = { /* Mark all read logic */ }) {
                        Text("Mark all read", color = BrandBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBg)
            )
        },
        containerColor = AppBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today Section
            item { NotifSectionHeader("Today") } // Renamed function used here
            items(todayNotifications) { item ->
                NotifRow(item) // Renamed function used here
            }

            // Yesterday Section
            item { NotifSectionHeader("Yesterday") } // Renamed function used here
            items(yesterdayNotifications) { item ->
                NotifRow(item) // Renamed function used here
            }

            // Bottom Padding
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

// --- Renamed Components to avoid conflicts with HomeScreen ---

@Composable
private fun NotifSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = TextDim,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun NotifRow(item: NotificationItem) {
    val icon = when (item.type) {
        NotificationType.APPOINTMENT -> Icons.Outlined.Schedule
        NotificationType.MEDICINE -> Icons.Filled.LocalPharmacy
        NotificationType.OFFER -> Icons.Outlined.LocalOffer
        NotificationType.SYSTEM -> Icons.Outlined.CheckCircle
    }

    val iconColor = when (item.type) {
        NotificationType.APPOINTMENT -> BrandBlue
        NotificationType.MEDICINE -> BrandOrange
        NotificationType.OFFER -> Color(0xFFFFD700) // Gold
        NotificationType.SYSTEM -> BrandGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (item.isUnread) CardSurface.copy(alpha = 0.8f) else Color.Transparent)
            .clickable { /* Handle Click */ }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
                // Red Dot for Unread
                if (item.isUnread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BrandOrange)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.time,
                style = MaterialTheme.typography.labelSmall,
                color = TextDim.copy(alpha = 0.6f)
            )
        }
    }
    // Divider
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CardSurface)
    )
}