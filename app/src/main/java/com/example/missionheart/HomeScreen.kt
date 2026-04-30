package com.example.missionheart

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.missionheart.ui.theme.*

// ── Data Models ──────────────────────────

data class ServiceItem(val title: String, val subtitle: String, val icon: ImageVector, val route: String, val color: Color)
data class DoctorItem(val name: String, val specialty: String, val rating: String, val isVerified: Boolean = true)
data class BannerItem(val title: String, val subtitle: String, val buttonText: String, val icon: ImageVector, val gradientColors: List<Color>)

data class HealthChallenge(
    val id: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val current: Int,
    val icon: ImageVector,
    val color: Color,
    val daysLeft: Int,
    val points: Int,
    val streak: Int = 0
)

data class LeaderboardEntry(val rank: Int, val name: String, val steps: Int, val isCurrentUser: Boolean = false)

// ── Theme Aliases ────────────────────────

private val ThemeBg = AppBackground
private val ThemeCardSurface = SurfaceWhite
private val ThemeInputSurface = InputFieldBg
private val ThemePrimary = BrandTeal
private val ThemeSecondary = BrandBlue
private val ThemeTextMain = TextPrimary
private val ThemeTextDim = TextSecondary
private val StatusGreenColor = SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val userName = "Rushi"
    
    // --- SENSOR STATES ---
    var totalStepsAtStart by rememberSaveable { mutableFloatStateOf(-1f) }
    var currentSensorSteps by remember { mutableFloatStateOf(0f) }
    
    // Steps to display (Difference between current sensor value and baseline)
    val displaySteps = if (totalStepsAtStart != -1f) {
        (currentSensorSteps - totalStepsAtStart).toInt().coerceAtLeast(0)
    } else {
        0 
    }

    // --- PERMISSION HANDLER ---
    val activityPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Steps won't update without Physical Activity permission.", Toast.LENGTH_LONG).show()
        }
    }

    // --- SENSOR LISTENER ---
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.firstOrNull()?.let { sensorValue ->
                    Log.d("StepSensor", "Raw Value from Sensor: $sensorValue")
                    currentSensorSteps = sensorValue
                    
                    // Capture the very first reading as our baseline if not already captured
                    if (totalStepsAtStart == -1f) {
                        totalStepsAtStart = sensorValue
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Check and Request Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
            } else {
                activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        } else {
            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // --- OTHER STATES ---
    var location by rememberSaveable { mutableStateOf("Detecting...") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showDailyWalkerSheet by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Health Tips
    val healthTips = remember {
        listOf(
            "Drink at least 3 liters of water today for better skin.",
            "Take a 10-minute walk after your lunch to aid digestion.",
            "Practice deep breathing for 5 minutes to reduce stress.",
            "Try to get at least 7-8 hours of sound sleep tonight."
        )
    }
    val currentTip = remember { healthTips.random() }

    // Challenges (Using live steps)
    val dailyWalkerChallenge = remember(displaySteps) {
        HealthChallenge(
            id = "walk_challenge",
            title = "Daily Walker",
            description = "Walk 8,000 steps today",
            progress = ((displaySteps / 8000f) * 100).toInt().coerceIn(0, 100),
            target = 8000,
            current = displaySteps,
            icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
            color = ActionOrange,
            daysLeft = 1,
            points = 50,
            streak = 7
        )
    }

    Scaffold(
        containerColor = ThemeBg,
        floatingActionButton = {
            if (searchQuery.isEmpty()) {
                FloatingActionButton(onClick = { }, containerColor = ErrorRed, contentColor = SurfaceWhite, shape = CircleShape) {
                    Icon(Icons.Rounded.Phone, "SOS", Modifier.size(28.dp))
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            HomeTopBar(
                userName = userName,
                location = location,
                onLocationClick = { /* Handle Location Logic */ },
                onProfileClick = { navController.navigate(NavGraph.PROFILE_ROUTE) },
                navController = navController,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onSearchClear = { searchQuery = "" },
                onCartClick = { navController.navigate(NavGraph.CART_ROUTE) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { DailyHealthTip(currentTip) }
                item { MedicineReminderCard { navController.navigate(NavGraph.MEDICINE_REMINDER_ROUTE) } }
                item { AISymptomCheckerCard { navController.navigate(NavGraph.SYMPTOM_CHECKER_ROUTE) } }

                if (dailyWalkerChallenge.streak > 0) {
                    item { StreakBanner(dailyWalkerChallenge.streak) }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    ChallengesSection(listOf(dailyWalkerChallenge)) { 
                        showDailyWalkerSheet = true 
                    }
                }

                item {
                    SectionHeader("Services")
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ServiceItem("Pharmacy", "Meds", Icons.Default.LocalPharmacy, NavGraph.PHARMACY_ROUTE, StatusGreenColor),
                            ServiceItem("Labs", "Tests", Icons.Default.Science, NavGraph.LAB_TESTS_ROUTE, Color(0xFF7E57C2)),
                            ServiceItem("Doctors", "Consult", Icons.Default.MedicalServices, NavGraph.DOCTORS_ROUTE, BrandBlue)
                        ).forEach { ServiceCard(it, navController, Modifier.weight(1f)) }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    if (showDailyWalkerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDailyWalkerSheet = false },
            containerColor = ThemeBg,
            contentColor = ThemeTextMain
        ) {
            DailyWalkerDetailScreen(dailyWalkerChallenge, displaySteps.toFloat()) { showDailyWalkerSheet = false }
        }
    }
}

// ── Components ───────────────────────────

@Composable
fun DailyHealthTip(tip: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThemePrimary.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.2f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lightbulb, null, tint = ThemePrimary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Daily Health Tip", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(tip, color = ThemeTextDim, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun ChallengesSection(challenges: List<HealthChallenge>, onChallengeClick: (HealthChallenge) -> Unit) {
    Column {
        SectionHeader("Active Challenges")
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(challenges) { challenge ->
                MiniChallengeCard(challenge) { onChallengeClick(challenge) }
            }
        }
    }
}

@Composable
fun MiniChallengeCard(challenge: HealthChallenge, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(160.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(challenge.icon, null, tint = challenge.color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(challenge.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ThemeTextMain)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { challenge.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = challenge.color,
                trackColor = challenge.color.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(8.dp))
            Text("${challenge.current}/${challenge.target}", fontSize = 11.sp, color = challenge.color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DailyWalkerDetailScreen(challenge: HealthChallenge, actualSteps: Float, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Activity Detail", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            CircularProgressIndicator(
                progress = { actualSteps / challenge.target },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
                color = challenge.color,
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(actualSteps.toInt().toString(), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = challenge.color)
                Text("Steps Today", fontSize = 14.sp, color = ThemeTextDim)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

// Other UI components (HomeTopBar, SearchBar, etc.) remain as previously defined.
// ... (omitted for brevity but ensuring the app remains consistent)
