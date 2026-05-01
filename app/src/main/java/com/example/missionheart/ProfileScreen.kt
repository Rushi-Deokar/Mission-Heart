package com.example.missionheart

import androidx.compose.foundation.background
import androidx.compose.foundation.border // ✅ Added border import
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.missionheart.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser

    val userName = currentUser?.displayName ?: "Rushikesh Deokar"
    val userEmail = currentUser?.email ?: "No email provided"

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
            item {
                UserProfileHeader(
                    name = userName,
                    details = userEmail,
                    onEditClick = { navController.navigate(NavGraph.EDIT_PROFILE_ROUTE) }
                )
            }

            item {
                ProfileSectionTitle("My Activity")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileOptionItem(icon = Icons.Default.Event, title = "My Appointments", subtitle = "Check upcoming visits", onClick = {})
                    ProfileOptionItem(icon = Icons.Default.ShoppingBag, title = "My Orders", subtitle = "Medicines & Lab tests", onClick = {})
                    ProfileOptionItem(icon = Icons.Default.Description, title = "Health Records", subtitle = "Prescriptions & Reports", onClick = {})
                }
            }

            item {
                ProfileSectionTitle("Account Settings")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileOptionItem(icon = Icons.Default.People, title = "Family Members", subtitle = "Manage patient profiles", onClick = {})
                    ProfileOptionItem(icon = Icons.Default.LocationOn, title = "Saved Addresses", subtitle = "Home, Office", onClick = {})
                    ProfileOptionItem(icon = Icons.Default.AccountBalanceWallet, title = "Wallet & Payments", subtitle = "Refunds & Cards", onClick = {})
                }
            }

            item {
                ProfileSectionTitle("Support")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileOptionItem(icon = Icons.Default.Help, title = "Help & Support", subtitle = "FAQs & Customer Care", onClick = {})
                    LogoutButton(onClick = {
                        auth.signOut()
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                        GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                            navController.navigate(NavGraph.LOGIN_ROUTE) {
                                popUpTo(0)
                            }
                        }
                    })
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
                    Text("Version 1.0.0", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun UserProfileHeader(name: String, details: String, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ✅ THE FIX: Outer Box with NO CLIP! Size is slightly larger to fit the badge
        Box(
            modifier = Modifier.size(76.dp)
        ) {
            // 1. Main Avatar (Sirf isko clip kiya hai)
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(InputFieldBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(35.dp))
            }

            // 2. Edit Pencil Badge (Overlapping perfectly at the bottom-right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp) // Thoda bada size
                    .clip(CircleShape)
                    .background(BrandBlue)
                    .border(2.dp, SurfaceWhite, CircleShape) // ✅ PRO TRICK: White Border matching card color!
                    .clickable { onEditClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(details, fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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