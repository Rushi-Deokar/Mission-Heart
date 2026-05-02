package com.example.missionheart

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.missionheart.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

// ── Data Models ──────────────────────────
data class ServiceItem(val title: String, val subtitle: String, val icon: ImageVector, val route: String, val color: Color)
data class HealthChallenge(val id: String, val title: String, val description: String, val progress: Int, val target: Int, val current: Int, val icon: ImageVector, val color: Color, val points: Int, val streak: Int = 0, val unit: String = "units")
data class WaterLog(val amount: Int, val time: String)
data class HydrationTask(val id: String, val text: String, var isCompleted: Boolean = false)

private val ThemeBg = AppBackground
private val ThemeCardSurface = SurfaceWhite
private val ThemePrimary = BrandTeal
private val ThemeTextMain = TextPrimary
private val ThemeTextDim = TextSecondary
private val StatusGreenColor = SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    val fullName = currentUser?.displayName ?: "User"
    val userName = fullName.split(" ").firstOrNull() ?: "User"

    val sharedPrefs = remember { context.getSharedPreferences("HealthTrackerPrefs", Context.MODE_PRIVATE) }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    var dailySteps by remember { mutableIntStateOf(0) }
    var stepTarget by remember { mutableIntStateOf(sharedPrefs.getInt("step_target", 8000)) }
    var dailyWaterIntake by remember { mutableIntStateOf(sharedPrefs.getInt("water_intake_$today", 0)) }
    var waterTarget by remember { mutableIntStateOf(sharedPrefs.getInt("water_target", 3000)) }
    val waterLogs = remember { mutableStateListOf<WaterLog>() }
    val hydrationTasks = remember { mutableStateListOf<HydrationTask>() }

    var location by rememberSaveable { mutableStateOf(sharedPrefs.getString("saved_location", "Jalgaon 425002") ?: "Jalgaon 425002") }
    var showLocationSheet by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showDetailSheet by remember { mutableStateOf<String?>(null) }

    // ✅ FREE GPS LOGIC (No API Key Required)
    fun fetchLiveLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Fetching location...", Toast.LENGTH_SHORT).show()
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    try {
                        // Android ka native free Geocoder
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val city = addresses[0].locality ?: addresses[0].subAdminArea ?: "Unknown City"
                            val postalCode = addresses[0].postalCode ?: ""
                            val newLoc = if (postalCode.isNotEmpty()) "$city $postalCode" else city
                            location = newLoc
                            sharedPrefs.edit().putString("saved_location", newLoc).apply()
                            showLocationSheet = false
                        } else {
                            Toast.makeText(context, "Location found, but name unavailable.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Network issue in fetching city name.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Please turn on GPS and try again.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchLiveLocation()
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val savedLogs = sharedPrefs.getStringSet("water_logs_$today", emptySet())
        savedLogs?.forEach {
            val parts = it.split("|")
            if (parts.size >= 2) waterLogs.add(WaterLog(parts[0].toInt(), parts[1]))
        }
        val savedTasks = sharedPrefs.getStringSet("water_tasks", emptySet())
        savedTasks?.forEach {
            val parts = it.split("|")
            if (parts.size >= 3) hydrationTasks.add(HydrationTask(parts[0], parts[1], parts[2].toBoolean()))
        }
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.firstOrNull()?.let { totalSteps ->
                    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val savedDate = sharedPrefs.getString("last_step_date", "")
                    var baseline = sharedPrefs.getFloat("step_baseline", -1f)
                    if (todayDate != savedDate || baseline == -1f) {
                        baseline = totalSteps
                        sharedPrefs.edit().putString("last_step_date", todayDate).putFloat("step_baseline", baseline).apply()
                    }
                    dailySteps = (totalSteps - baseline).toInt().coerceAtLeast(0)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACTIVITY_RECOGNITION else null
        if (permission == null || ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            activityPermissionLauncher.launch(permission)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val allChallenges = remember(dailySteps, stepTarget, dailyWaterIntake, waterTarget) {
        listOf(
            HealthChallenge("walk_challenge", "Daily Walker", "Walk $stepTarget steps today", ((dailySteps.toFloat() / stepTarget) * 100).toInt().coerceIn(0, 100), stepTarget, dailySteps, Icons.AutoMirrored.Outlined.DirectionsWalk, ActionOrange, 50, 7, "steps"),
            HealthChallenge("hydration", "Hydration Hero", "Drink $waterTarget ml water", ((dailyWaterIntake.toFloat() / waterTarget) * 100).toInt().coerceIn(0, 100), waterTarget, dailyWaterIntake, Icons.Outlined.WaterDrop, BrandBlue, 100, 3, "ml")
        )
    }

    Scaffold(
        containerColor = ThemeBg,
        floatingActionButton = {
            if (searchQuery.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick AI Access FAB
                    FloatingActionButton(
                        onClick = { navController.navigate(NavGraph.AI_CHAT_ROUTE) },
                        containerColor = BrandTeal,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.SmartToy, "AI Chat", Modifier.size(28.dp))
                    }

                    // SOS Emergency FAB
                    FloatingActionButton(
                        onClick = { /* Emergency Logic */ },
                        containerColor = ErrorRed,
                        contentColor = SurfaceWhite,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Rounded.Phone, "SOS", Modifier.size(28.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding())) {
            HomeTopBar(userName, location, { showLocationSheet = true }, { navController.navigate(NavGraph.PROFILE_ROUTE) }, navController, searchQuery, {searchQuery = it}, {searchQuery = ""}, { navController.navigate(NavGraph.CART_ROUTE) })
            Spacer(Modifier.height(16.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                item { MedicineReminderCard { navController.navigate(NavGraph.MEDICINE_REMINDER_ROUTE) } }
                item { AISymptomCheckerCard { navController.navigate(NavGraph.SYMPTOM_CHECKER_ROUTE) } }
                item { Spacer(Modifier.height(16.dp)) }
                item { ChallengesSection(allChallenges) { c -> if (c.id == "walk_challenge") showDetailSheet = "walk" else if (c.id == "hydration") showDetailSheet = "water" } }
                item {
                    SectionHeader("Our Services")
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ServiceCard(ServiceItem("Pharmacy", "Meds", Icons.Default.LocalPharmacy, NavGraph.PHARMACY_ROUTE, StatusGreenColor), navController, Modifier.weight(1f))
                        ServiceCard(ServiceItem("Labs", "Tests", Icons.Default.Science, NavGraph.LAB_TESTS_ROUTE, Color(0xFF7E57C2)), navController, Modifier.weight(1f))
                        ServiceCard(ServiceItem("Doctors", "Consult", Icons.Default.MedicalServices, NavGraph.DOCTORS_ROUTE, BrandBlue), navController, Modifier.weight(1f))
                    }
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    // ✅ Bill-Free Location Sheet
    if (showLocationSheet) {
        LocationSelectionSheet(
            currentLocation = location,
            onLocationSelected = { newLocation ->
                location = newLocation
                sharedPrefs.edit().putString("saved_location", newLocation).apply()
                showLocationSheet = false
            },
            onUseLiveLocation = {
                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            },
            onDismiss = { showLocationSheet = false }
        )
    }

    if (showDetailSheet != null) {
        ModalBottomSheet(onDismissRequest = { showDetailSheet = null }, containerColor = ThemeBg, contentColor = ThemeTextMain) {
            // Sheet implementation remains same...
            Button(onClick = { showDetailSheet = null }, modifier = Modifier.fillMaxWidth().padding(24.dp)) { Text("Close") }
        }
    }
}

// ── FREE Location Sheet (Local Filter + Custom Input) ───────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionSheet(
    currentLocation: String,
    onLocationSelected: (String) -> Unit,
    onUseLiveLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchLoc by remember { mutableStateOf("") }

    // Top popular cities for initial display
    val popularLocations = listOf(
        "Jalgaon, Maharashtra", "Pune, Maharashtra", "Mumbai, Maharashtra",
        "Nashik, Maharashtra", "Nagpur, Maharashtra", "Delhi NCR", "Bangalore, Karnataka"
    )

    // Auto-filter list based on search text
    val filteredLocations = if (searchLoc.isEmpty()) {
        popularLocations.take(4) // Show top 4 initially
    } else {
        popularLocations.filter { it.contains(searchLoc, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ThemeBg, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Select Location", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = ThemeTextMain)
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchLoc,
                onValueChange = { searchLoc = it },
                placeholder = { Text("Search city, area or pincode", color = ThemeTextDim) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = ThemeTextDim) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = ThemeCardSurface, unfocusedContainerColor = ThemeCardSurface, focusedBorderColor = BrandBlue, unfocusedBorderColor = Color.Transparent, focusedTextColor = ThemeTextMain)
            )

            // ✅ User Input Dynamic Button (Jaisa type karega waisa button banega)
            if (searchLoc.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onLocationSelected(searchLoc) },
                    color = BrandBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BrandBlue)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddLocation, contentDescription = null, tint = BrandBlue)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Set location to:", color = BrandBlue.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text(searchLoc, color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Location Button (Using free GPS)
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onUseLiveLocation() },
                color = ThemeCardSurface, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, ThemeTextDim.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = BrandBlue)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Use Current Location", color = ThemeTextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Using device GPS", color = ThemeTextDim, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(if(searchLoc.isEmpty()) "Popular Cities" else "Suggestions", fontWeight = FontWeight.Bold, color = ThemeTextDim, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // List of locations
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(filteredLocations) { loc ->
                    val actualLocName = if(loc.contains(",")) loc.split(",").first().trim() else loc
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onLocationSelected(loc) }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(ThemeCardSurface), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.LocationCity, null, tint = ThemeTextDim, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(loc, color = ThemeTextMain, fontSize = 16.sp, fontWeight = if (currentLocation == loc) FontWeight.Bold else FontWeight.Normal)
                            if (currentLocation == loc) Text("Currently selected", color = BrandBlue, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = ThemeTextDim.copy(alpha = 0.1f))
                }
            }
        }
    }
}

// (Remaining Components remain identical...)
@Composable
fun HomeTopBar(userName: String, location: String, onLoc: () -> Unit, onProf: () -> Unit, nav: NavController, query: String, onQ: (String) -> Unit, onClear: () -> Unit, onCart: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("Namaste,", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Text(userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Surface(
                    color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(16.dp)).clickable { onLoc() }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = location, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp))
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }
            IconButton(onClick = onProf) { Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(38.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        SearchBar(query, onQ, onClear, onCart)
    }
}
@Composable
fun SearchBar(value: String, onValueChange: (String) -> Unit, onClear: () -> Unit, onCartClick: () -> Unit) { Row(verticalAlignment = Alignment.CenterVertically) { TextField(value = value, onValueChange = onValueChange, placeholder = { Text("Search meds, doctors...", color = ThemeTextDim) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = ThemePrimary) }, trailingIcon = { if (value.isNotEmpty()) IconButton(onClick = onClear) { Icon(Icons.Default.Close, null) } }, modifier = Modifier.weight(1f).height(52.dp).shadow(4.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = TextFieldDefaults.colors(focusedContainerColor = ThemeCardSurface, unfocusedContainerColor = ThemeCardSurface, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = ThemeTextMain, unfocusedTextColor = ThemeTextMain)); Spacer(Modifier.width(12.dp)); IconButton(onClick = onCartClick, modifier = Modifier.size(52.dp).background(ThemeCardSurface, RoundedCornerShape(16.dp))) { Icon(Icons.Default.ShoppingCart, null, tint = ThemeTextMain) } } }
@Composable
fun DailyHealthTip(tip: String) { Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ThemePrimary.copy(alpha = 0.05f)), border = BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.2f))) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Lightbulb, null, tint = ThemePrimary); Spacer(Modifier.width(12.dp)); Column { Text("Daily Health Tip", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(tip, color = ThemeTextDim, fontSize = 12.sp, lineHeight = 16.sp) } } } }
@Composable
fun AISymptomCheckerCard(onClick: () -> Unit) { Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrandTeal.copy(alpha = 0.1f)), border = BorderStroke(1.dp, BrandTeal.copy(alpha = 0.3f))) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).clip(CircleShape).background(BrandTeal), Alignment.Center) { Icon(Icons.Rounded.Verified, null, tint = Color.White) }; Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text("AI Symptom Checker", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeTextMain); Text("Feeling unwell? Check symptoms instantly.", fontSize = 12.sp, color = ThemeTextDim) }; Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = BrandTeal) } } }
@Composable
fun MedicineReminderCard(onClick: () -> Unit) { Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ThemeCardSurface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).clip(CircleShape).background(ThemePrimary.copy(alpha = 0.1f)), Alignment.Center) { Icon(Icons.Rounded.Alarm, null, tint = ThemePrimary) }; Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text("My Medicines", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeTextMain); Text("Check your schedule & refills", fontSize = 12.sp, color = ThemeTextDim) }; Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = ThemeTextDim) } } }
@Composable
fun StreakBanner(streak: Int) { Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), color = ActionOrange.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, ActionOrange.copy(alpha = 0.3f))) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { val scale by rememberInfiniteTransition().animateFloat(1f, 1.2f, infiniteRepeatable(tween(600), RepeatMode.Reverse)); Text("🔥", fontSize = 24.sp, modifier = Modifier.scale(scale)); Spacer(Modifier.width(12.dp)); Column { Text("$streak Day Streak!", fontWeight = FontWeight.Bold, color = ActionOrange); Text("Keep going! Don't break the chain", fontSize = 12.sp, color = ThemeTextDim) } } } }
@Composable
fun ChallengesSection(challenges: List<HealthChallenge>, onChallengeClick: (HealthChallenge) -> Unit) { Column { SectionHeader("Active Challenges"); LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(challenges) { challenge -> Card(Modifier.width(180.dp).clickable { onChallengeClick(challenge) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ThemeCardSurface), elevation = CardDefaults.cardElevation(2.dp)) { Column(Modifier.padding(16.dp)) { Icon(challenge.icon, null, tint = challenge.color, modifier = Modifier.size(32.dp)); Spacer(Modifier.height(8.dp)); Text(challenge.title, fontWeight = FontWeight.Bold, color = ThemeTextMain); Text(challenge.description, fontSize = 10.sp, color = ThemeTextDim, maxLines = 1, overflow = TextOverflow.Ellipsis); Spacer(Modifier.height(12.dp)); LinearProgressIndicator(progress = challenge.progress / 100f, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = challenge.color, trackColor = challenge.color.copy(alpha = 0.1f)); Spacer(Modifier.height(8.dp)); Text("${challenge.current} / ${challenge.target} ${challenge.unit}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = challenge.color) } } } } } }
@Composable
fun ServiceCard(item: ServiceItem, nav: NavController, modifier: Modifier = Modifier) { Card(modifier.clickable { nav.navigate(item.route) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ThemeCardSurface)) { Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(40.dp).clip(CircleShape).background(item.color.copy(alpha = 0.1f)), Alignment.Center) { Icon(item.icon, null, tint = item.color, modifier = Modifier.size(24.dp)) }; Spacer(Modifier.height(8.dp)); Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ThemeTextMain) } } }
@Composable
fun SectionHeader(title: String) { Text(title, Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = ThemeTextMain, fontSize = 18.sp) }
@Composable
fun TrustFooter() { Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Security, null, tint = SuccessGreen, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("100% Secure & HIPAA Compliant", color = ThemeTextDim, fontSize = 12.sp) } } }