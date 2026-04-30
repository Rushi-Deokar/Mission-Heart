package com.example.missionheart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.missionheart.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. User Info Header
            item {
                UserProfileHeader(
                    name = "Rushi Patil",
                    phone = "+91 98765 43210",
                    onEditClick = { /* Navigate to Edit Profile */ }
                )
            }

            // 2. Health & Orders Section
            item {
                ProfileSectionTitle("My Activity")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileOptionItem(icon = Icons.Default.Event, title = "My Appointments", subtitle = "Check upcoming visits", onClick = {})
                    ProfileOptionItem(icon = Icons.Default.ShoppingBag, title = "My Orders", subtitle = "Medicines & Lab tests", onClick = {})
                    ProfileOptionItem(icon = Icons.Default.Description, title = "Health Records", subtitle = "Prescriptions & Reports", onClick = {})
                }
            }

            // 3. Account Settings
            item {
                ProfileSectionTitle("Account Settings")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileOptionItem(icon = Icons.Default.People, title = "Family Members", subtitle = "Manage patient profiles", onClick = {})
                    ProfileOptionItem(icon = Icons.Default.LocationOn, title = "Saved Addresses", subtitle = "Home, Office", onClick = {})
                    ProfileOptionItem(icon = Icons.Default.AccountBalanceWallet, title = "Wallet & Payments", subtitle = "Refunds & Cards", onClick = {})
                }
            }

            // 4. App Info & Logout
            item {
                ProfileSectionTitle("Support")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileOptionItem(icon = Icons.Default.Help, title = "Help & Support", subtitle = "FAQs & Customer Care", onClick = {})
                    LogoutButton(onClick = { /* Handle Logout */ })
                }
            }

            // Version Info
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
                    Text("Version 1.0.0", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun UserProfileHeader(name: String, phone: String, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(InputFieldBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(35.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandBlue)
                    .clickable { onEditClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(phone, fontSize = 14.sp, color = TextSecondary)
        }
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        color = BrandBlue,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun ProfileOptionItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceWhite)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(InputFieldBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }

        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
    }
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ErrorRed)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Log Out", color = ErrorRed, fontWeight = FontWeight.Bold)
    }
}
