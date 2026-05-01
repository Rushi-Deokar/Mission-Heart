package com.example.missionheart

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import java.text.SimpleDateFormat
import java.util.*
import com.example.missionheart.ui.theme.*

// ── Data Models ──────────────────────────

data class ServiceItem(val title: String, val subtitle: String, val icon: ImageVector, val route: String, val color: Color)

data class HealthChallenge(
    val id: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val current: Int,
    val icon: ImageVector,
    val color: Color,
    val points: Int,
    val streak: Int = 0,
    val unit: String = "units"
)

data class WaterLog(val amount: Int, val time: String)
data class HydrationTask(val id: String, val text: String, var isCompleted: Boolean = false)

// ── Theme Aliases ────────────────────────

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
    val userName = "Rushi"
    
    // --- PERSISTENT TRACKING LOGIC ---
    val sharedPrefs = remember { context.getSharedPreferences("HealthTrackerPrefs", Context.MODE_PRIVATE) }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    
    // Step Tracking
    var dailySteps by remember { mutableIntStateOf(0) }
    var stepTarget by remember { mutableIntStateOf(sharedPrefs.getInt("step_target", 8000)) }

    // Water Tracking
    var dailyWaterIntake by remember { mutableIntStateOf(sharedPrefs.getInt("water_intake_$today", 0)) }
    var waterTarget by remember { mutableIntStateOf(sharedPrefs.getInt("water_target", 3000)) }
    val waterLogs = remember { mutableStateListOf<WaterLog>() }
    val hydrationTasks = remember { mutableStateListOf<HydrationTask>() }

    // Load initial logs and tasks
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

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Activity Permission required for steps.", Toast.LENGTH_SHORT).show()
        }
    }

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

    // --- UI STATES ---
    var location by rememberSaveable { mutableStateOf("Jalgaon 425002") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showDetailSheet by remember { mutableStateOf<String?>(null) } 

    // Challenges
    val allChallenges = remember(dailySteps, stepTarget, dailyWaterIntake, waterTarget) {
        listOf(
            HealthChallenge("walk_challenge", "Daily Walker", "Walk $stepTarget steps today", ((dailySteps.toFloat() / stepTarget) * 100).toInt().coerceIn(0, 100), stepTarget, dailySteps, Icons.AutoMirrored.Outlined.DirectionsWalk, ActionOrange, 50, 7, "steps"),
            HealthChallenge("hydration", "Hydration Hero", "Drink $waterTarget ml water", ((dailyWaterIntake.toFloat() / waterTarget) * 100).toInt().coerceIn(0, 100), waterTarget, dailyWaterIntake, Icons.Outlined.WaterDrop, BrandBlue, 100, 3, "ml"),
            HealthChallenge("medicine_streak", "Medicine Master", "30 day medicine streak", 80, 30, 24, Icons.Rounded.Alarm, SuccessGreen, 200, 24, "days")
        )
    }

    val healthTips = listOf("Drink 3L of water for glowing skin.", "A 10-minute walk aids digestion.", "Deep breathing for 5 minutes reduces stress.")
    val currentTip = remember { healthTips.random() }

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
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            HomeTopBar(userName, location, {}, { navController.navigate(NavGraph.PROFILE_ROUTE) }, navController, searchQuery, {searchQuery = it}, {searchQuery = ""}, { navController.navigate(NavGraph.CART_ROUTE) })

            Spacer(Modifier.height(16.dp))

            LazyColumn(Modifier.fillMaxSize()) {
                item { DailyHealthTip(currentTip) }
                item { MedicineReminderCard { navController.navigate(NavGraph.MEDICINE_REMINDER_ROUTE) } }
                item { AISymptomCheckerCard { navController.navigate(NavGraph.SYMPTOM_CHECKER_ROUTE) } }

                val walkChallenge = allChallenges.find { it.id == "walk_challenge" }
                if (walkChallenge != null && walkChallenge.streak > 0) {
                    item { StreakBanner(walkChallenge.streak) }
                }

                item { Spacer(Modifier.height(16.dp)) }

                item {
                    ChallengesSection(allChallenges) { challenge ->
                        if (challenge.id == "walk_challenge") showDetailSheet = "walk"
                        else if (challenge.id == "hydration") showDetailSheet = "water"
                    }
                }

                item {
                    SectionHeader("Our Services")
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ServiceCard(ServiceItem("Pharmacy", "Meds", Icons.Default.LocalPharmacy, NavGraph.PHARMACY_ROUTE, StatusGreenColor), navController, Modifier.weight(1f))
                        ServiceCard(ServiceItem("Labs", "Tests", Icons.Default.Science, NavGraph.LAB_TESTS_ROUTE, Color(0xFF7E57C2)), navController, Modifier.weight(1f))
                        ServiceCard(ServiceItem("Doctors", "Consult", Icons.Default.MedicalServices, NavGraph.DOCTORS_ROUTE, BrandBlue), navController, Modifier.weight(1f))
                    }
                }
                
                item { TrustFooter() }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    if (showDetailSheet != null) {
        ModalBottomSheet(onDismissRequest = { showDetailSheet = null }, containerColor = ThemeBg, contentColor = ThemeTextMain) {
            when (showDetailSheet) {
                "walk" -> {
                    val challenge = allChallenges.find { it.id == "walk_challenge" }!!
                    DailyWalkerDetailScreen(challenge, { new -> stepTarget = new; sharedPrefs.edit().putInt("step_target", new).apply() }, { showDetailSheet = null })
                }
                "water" -> {
                    val challenge = allChallenges.find { it.id == "hydration" }!!
                    HydrationDetailScreen(
                        challenge = challenge,
                        logs = waterLogs,
                        tasks = hydrationTasks,
                        onIntakeChange = { added ->
                            val newIntake = dailyWaterIntake + added
                            dailyWaterIntake = newIntake
                            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            waterLogs.add(0, WaterLog(added, time))
                            sharedPrefs.edit().putInt("water_intake_$today", newIntake).apply()
                            sharedPrefs.edit().putStringSet("water_logs_$today", waterLogs.map { "${it.amount}|${it.time}" }.toSet()).apply()
                        },
                        onTargetChange = { new -> waterTarget = new; sharedPrefs.edit().putInt("water_target", new).apply() },
                        onAddTask = { taskText -> 
                            hydrationTasks.add(HydrationTask(UUID.randomUUID().toString(), taskText))
                            sharedPrefs.edit().putStringSet("water_tasks", hydrationTasks.map { "${it.id}|${it.text}|${it.isCompleted}" }.toSet()).apply()
                        },
                        onToggleTask = { task ->
                            val index = hydrationTasks.indexOf(task)
                            if (index != -1) {
                                hydrationTasks[index] = task.copy(isCompleted = !task.isCompleted)
                                sharedPrefs.edit().putStringSet("water_tasks", hydrationTasks.map { "${it.id}|${it.text}|${it.isCompleted}" }.toSet()).apply()
                            }
                        },
                        onDismiss = { showDetailSheet = null }
                    )
                }
            }
        }
    }
}

// ── Components ───────────────────────────

@Composable
fun HydrationDetailScreen(
    challenge: HealthChallenge,
    logs: List<WaterLog>,
    tasks: List<HydrationTask>,
    onIntakeChange: (Int) -> Unit,
    onTargetChange: (Int) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (HydrationTask) -> Unit,
    onDismiss: () -> Unit
) {
    var customAmount by remember { mutableStateOf("") }
    var newTaskText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Hydration Hero", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ThemeTextMain)
        Spacer(Modifier.height(24.dp))
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            CircularProgressIndicator(
                progress = challenge.current.toFloat() / challenge.target, 
                modifier = Modifier.fillMaxSize(), 
                strokeWidth = 14.dp, 
                color = challenge.color,
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(challenge.current.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = ThemeTextMain)
                Text("ml drank", fontSize = 12.sp, color = ThemeTextDim)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text("Quick Add", fontWeight = FontWeight.Bold, color = ThemeTextMain, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAddIcon(250, Icons.Outlined.LocalDrink, "Glass", onIntakeChange)
            QuickAddIcon(500, Icons.Outlined.WaterDrop, "Bottle", onIntakeChange)
            QuickAddIcon(1000, Icons.Outlined.Opacity, "Large", onIntakeChange)
        }

        Spacer(Modifier.height(24.dp))
        
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = customAmount,
                onValueChange = { if (it.all { char -> char.isDigit() }) customAmount = it },
                label = { Text("Custom ml") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ThemeTextMain, unfocusedTextColor = ThemeTextMain)
            )
            Button(onClick = { if (customAmount.isNotEmpty()) { onIntakeChange(customAmount.toInt()); customAmount = "" } }, modifier = Modifier.height(56.dp), shape = RoundedCornerShape(12.dp)) { Text("Add") }
        }

        Spacer(Modifier.height(32.dp))
        
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ThemeCardSurface), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Daily Water Goal", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeTextMain)
                Spacer(Modifier.height(12.dp))
                val targets = listOf(2000, 2500, 3000, 3500, 4000)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(targets) { target ->
                        val isSelected = target == challenge.target
                        Surface(onClick = { onTargetChange(target) }, shape = RoundedCornerShape(20.dp), color = if (isSelected) BrandBlue else ThemeBg, border = BorderStroke(1.dp, if (isSelected) BrandBlue else ThemeTextDim.copy(alpha = 0.3f))) {
                            Text("${target/1000f}L", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = if (isSelected) Color.White else ThemeTextMain, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        
        Text("Custom Daily Tasks", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), color = ThemeTextMain)
        Spacer(Modifier.height(8.dp))
        tasks.forEach { task ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleTask(task) })
                Text(task.text, fontSize = 14.sp, color = ThemeTextMain)
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(value = newTaskText, onValueChange = { newTaskText = it }, placeholder = { Text("Enter new task...") }, modifier = Modifier.weight(1f), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = ThemeTextMain, unfocusedTextColor = ThemeTextMain))
            IconButton(onClick = { if(newTaskText.isNotEmpty()) { onAddTask(newTaskText); newTaskText = "" } }) { Icon(Icons.Default.AddCircle, null, tint = BrandBlue) }
        }

        Spacer(Modifier.height(32.dp))
        
        Text("Today's History", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), color = ThemeTextMain)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ThemeCardSurface)) {
            Column(Modifier.padding(12.dp)) {
                if (logs.isEmpty()) Text("No intake recorded yet.", fontSize = 12.sp, color = ThemeTextDim)
                logs.take(5).forEach { log ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween) {
                        Text("${log.amount}ml", fontSize = 14.sp, color = ThemeTextMain, fontWeight = FontWeight.SemiBold)
                        Text(log.time, fontSize = 12.sp, color = ThemeTextDim)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Close") }
    }
}

@Composable
fun QuickAddIcon(amount: Int, icon: ImageVector, label: String, onClick: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick(amount) }) {
        Box(Modifier.size(60.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.1f)).border(1.dp, BrandBlue.copy(alpha = 0.3f), CircleShape), Alignment.Center) {
            Icon(icon, null, tint = BrandBlue)
        }
        Spacer(Modifier.height(4.dp))
        Text("${amount}ml", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ThemeTextMain)
        Text(label, fontSize = 10.sp, color = ThemeTextDim)
    }
}

@Composable
fun DailyWalkerDetailScreen(challenge: HealthChallenge, onTargetChange: (Int) -> Unit, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Activity Progress", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ThemeTextMain)
        Spacer(Modifier.height(32.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            CircularProgressIndicator(progress = challenge.current.toFloat() / challenge.target, modifier = Modifier.fillMaxSize(), strokeWidth = 16.dp, color = challenge.color, strokeCap = StrokeCap.Round)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(challenge.current.toString(), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = ThemeTextMain)
                Text("Steps today", fontSize = 14.sp, color = ThemeTextDim)
            }
        }
        Spacer(Modifier.height(40.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ThemeCardSurface), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Adjust Your Daily Goal", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeTextMain)
                Spacer(Modifier.height(12.dp))
                val goals = listOf(5000, 8000, 10000, 12000, 15000)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(goals) { goal ->
                        val isSelected = goal == challenge.target
                        Surface(onClick = { onTargetChange(goal) }, shape = RoundedCornerShape(20.dp), color = if (isSelected) BrandBlue else ThemeBg, border = BorderStroke(1.dp, if (isSelected) BrandBlue else ThemeTextDim.copy(alpha = 0.3f))) {
                            Text("${goal / 1000}K steps", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = if (isSelected) Color.White else ThemeTextMain, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Close") }
    }
}

@Composable
fun HomeTopBar(userName: String, location: String, onLoc: () -> Unit, onProf: () -> Unit, nav: NavController, query: String, onQ: (String) -> Unit, onClear: () -> Unit, onCart: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("Namaste,", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Text(userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Row(Modifier.padding(top = 4.dp).clickable { onLoc() }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(location, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
            IconButton(onClick = onProf) { Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        SearchBar(query, onQ, onClear, onCart)
    }
}

@Composable
fun SearchBar(value: String, onValueChange: (String) -> Unit, onClear: () -> Unit, onCartClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = value, onValueChange = onValueChange,
            placeholder = { Text("Search meds, doctors...", color = ThemeTextDim) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = ThemePrimary) },
            trailingIcon = { if (value.isNotEmpty()) IconButton(onClick = onClear) { Icon(Icons.Default.Close, null) } },
            modifier = Modifier.weight(1f).height(52.dp).shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = ThemeCardSurface, unfocusedContainerColor = ThemeCardSurface, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = ThemeTextMain, unfocusedTextColor = ThemeTextMain)
        )
        Spacer(Modifier.width(12.dp))
        IconButton(onClick = onCartClick, modifier = Modifier.size(52.dp).background(ThemeCardSurface, RoundedCornerShape(16.dp))) {
            Icon(Icons.Default.ShoppingCart, null, tint = ThemeTextMain)
        }
    }
}

@Composable
fun DailyHealthTip(tip: String) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ThemePrimary.copy(alpha = 0.05f)), border = BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.2f))) {
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
fun AISymptomCheckerCard(onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrandTeal.copy(alpha = 0.1f)), border = BorderStroke(1.dp, BrandTeal.copy(alpha = 0.3f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(BrandTeal), Alignment.Center) { Icon(Icons.Rounded.Verified, null, tint = Color.White) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("AI Symptom Checker", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeTextMain)
                Text("Feeling unwell? Check symptoms instantly.", fontSize = 12.sp, color = ThemeTextDim)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = BrandTeal)
        }
    }
}

@Composable
fun MedicineReminderCard(onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ThemeCardSurface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(ThemePrimary.copy(alpha = 0.1f)), Alignment.Center) { Icon(Icons.Rounded.Alarm, null, tint = ThemePrimary) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("My Medicines", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeTextMain)
                Text("Check your schedule & refills", fontSize = 12.sp, color = ThemeTextDim)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = ThemeTextDim)
        }
    }
}

@Composable
fun StreakBanner(streak: Int) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), color = ActionOrange.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, ActionOrange.copy(alpha = 0.3f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val scale by rememberInfiniteTransition().animateFloat(1f, 1.2f, infiniteRepeatable(tween(600), RepeatMode.Reverse))
            Text("🔥", fontSize = 24.sp, modifier = Modifier.scale(scale))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("$streak Day Streak!", fontWeight = FontWeight.Bold, color = ActionOrange)
                Text("Keep going! Don't break the chain", fontSize = 12.sp, color = ThemeTextDim)
            }
        }
    }
}

@Composable
fun ChallengesSection(challenges: List<HealthChallenge>, onChallengeClick: (HealthChallenge) -> Unit) {
    Column {
        SectionHeader("Active Challenges")
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(challenges) { challenge ->
                Card(Modifier.width(180.dp).clickable { onChallengeClick(challenge) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ThemeCardSurface), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Icon(challenge.icon, null, tint = challenge.color, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(challenge.title, fontWeight = FontWeight.Bold, color = ThemeTextMain)
                        Text(challenge.description, fontSize = 10.sp, color = ThemeTextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = challenge.progress / 100f, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = challenge.color, trackColor = challenge.color.copy(alpha = 0.1f))
                        Spacer(Modifier.height(8.dp))
                        Text("${challenge.current} / ${challenge.target} ${challenge.unit}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = challenge.color)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceCard(item: ServiceItem, nav: NavController, modifier: Modifier = Modifier) {
    Card(modifier.clickable { nav.navigate(item.route) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ThemeCardSurface)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(item.color.copy(alpha = 0.1f)), Alignment.Center) { Icon(item.icon, null, tint = item.color, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.height(8.dp))
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ThemeTextMain)
        }
    }
}

@Composable
fun SectionHeader(title: String) { Text(title, Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = ThemeTextMain, fontSize = 18.sp) }

@Composable
fun TrustFooter() {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("100% Secure & HIPAA Compliant", color = ThemeTextDim, fontSize = 12.sp)
        }
    }
}
