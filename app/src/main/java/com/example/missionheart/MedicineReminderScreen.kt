package com.example.missionheart

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.missionheart.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

// ── Data Model ──────────────────────────
data class MedicineItem(
    val id: String = "",
    val name: String = "",
    val time: String = "",
    val hour: Int = 0,
    val minute: Int = 0,
    val totalStock: Int = 0,
    val currentStock: Int = 0,
    val history: Map<String, Boolean> = emptyMap()
)

// ── Background Alarm Scheduler ──────────
fun scheduleMedicineAlarms(context: Context, medId: String, medName: String, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intentBefore = Intent(context, MedicineAlarmReceiver::class.java).apply {
        putExtra("MED_ID", medId)
        putExtra("MED_NAME", medName)
        putExtra("TYPE", "BEFORE")
    }
    val pendingBefore = PendingIntent.getBroadcast(context, medId.hashCode() + 1, intentBefore, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val calBefore = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        add(Calendar.MINUTE, -10)
        if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
    }

    val intentAfter = Intent(context, MedicineAlarmReceiver::class.java).apply {
        putExtra("MED_ID", medId)
        putExtra("MED_NAME", medName)
        putExtra("TYPE", "AFTER")
    }
    val pendingAfter = PendingIntent.getBroadcast(context, medId.hashCode() + 2, intentAfter, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val calAfter = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        add(Calendar.MINUTE, 10)
        if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
    }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calBefore.timeInMillis, pendingBefore)
            alarmManager.set(AlarmManager.RTC_WAKEUP, calAfter.timeInMillis, pendingAfter)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calBefore.timeInMillis, pendingBefore)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calAfter.timeInMillis, pendingAfter)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun cancelMedicineAlarms(context: Context, medId: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, MedicineAlarmReceiver::class.java)
    val pendingBefore = PendingIntent.getBroadcast(context, medId.hashCode() + 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val pendingAfter = PendingIntent.getBroadcast(context, medId.hashCode() + 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    alarmManager.cancel(pendingBefore)
    alarmManager.cancel(pendingAfter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineReminderScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val userId = auth.currentUser?.uid ?: ""
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // ✅ Cloud Reference
    val database = remember { FirebaseDatabase.getInstance().getReference("users/$userId/medicines") }
    val medicines = remember { mutableStateListOf<MedicineItem>() }
    var showAddDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // ✅ Sync Data with Cloud
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    medicines.clear()
                    for (medSnapshot in snapshot.children) {
                        val med = medSnapshot.getValue(MedicineItem::class.java)
                        med?.let { medicines.add(it) }
                    }
                    isLoading = false
                }
                override fun onCancelled(error: DatabaseError) {
                    isLoading = false
                }
            })
        }
    }

    // Permission Request for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("My Medicines", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BrandBlue, contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add Med", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BrandBlue) }
        } else if (medicines.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudOff, null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No medicines found in Cloud.", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { Spacer(Modifier.height(8.dp)) }
                items(medicines, key = { it.id }) { med ->
                    MedicineCard(
                        medicine = med, todayStr = todayStr,
                        onTake = {
                            val updated = med.copy(
                                currentStock = med.currentStock - 1,
                                history = med.history.toMutableMap().apply { put(todayStr, true) }
                            )
                            database.child(med.id).setValue(updated)
                        },
                        onSkip = {
                            val updated = med.copy(history = med.history.toMutableMap().apply { put(todayStr, false) })
                            database.child(med.id).setValue(updated)
                        },
                        onDelete = {
                            cancelMedicineAlarms(context, med.id)
                            database.child(med.id).removeValue()
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddMedicineSheet(
            context = context,
            onDismiss = { showAddDialog = false },
            onSave = { name, time, hour, min, stock ->
                val newId = database.push().key ?: UUID.randomUUID().toString()
                val newMed = MedicineItem(newId, name, time, hour, min, stock, stock, emptyMap())
                database.child(newId).setValue(newMed)
                scheduleMedicineAlarms(context, newId, name, hour, min)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MedicineCard(medicine: MedicineItem, todayStr: String, onTake: () -> Unit, onSkip: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(BrandBlue.copy(0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Medication, null, tint = BrandBlue) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(medicine.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(medicine.time, color = TextSecondary, fontSize = 14.sp)
                    }
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, null, tint = ErrorRed.copy(0.7f)) }
            }
            Spacer(Modifier.height(16.dp))

            val stockPercentage = if (medicine.totalStock > 0) medicine.currentStock.toFloat() / medicine.totalStock else 0f
            val stockColor = when {
                stockPercentage > 0.5f -> SuccessGreen
                stockPercentage > 0.2f -> ActionOrange
                else -> ErrorRed
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Stock Left", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text("${medicine.currentStock} / ${medicine.totalStock}", fontSize = 12.sp, color = stockColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = stockPercentage, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = stockColor, trackColor = stockColor.copy(alpha = 0.15f), strokeCap = StrokeCap.Round)
            Spacer(Modifier.height(20.dp))
            Text("Last 7 Days Compliance", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                getLast7Days().forEach { date ->
                    val status = medicine.history[date]
                    val dotColor = when (status) { true -> SuccessGreen false -> ErrorRed null -> TextSecondary.copy(alpha = 0.2f) }
                    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)!!)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(dotColor))
                        Spacer(Modifier.height(4.dp))
                        Text(dayName.take(1), fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            val todayStatus = medicine.history[todayStr]
            if (todayStatus == null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed), border = BorderStroke(1.dp, ErrorRed)) { Text("Skip") }
                    Button(onClick = onTake, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), enabled = medicine.currentStock > 0) { Text(if (medicine.currentStock > 0) "Mark Taken" else "Out of Stock") }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (todayStatus) SuccessGreen.copy(0.1f) else ErrorRed.copy(0.1f)).padding(12.dp), contentAlignment = Alignment.Center) {
                    Text(if (todayStatus) "✅ Taken Today" else "❌ Missed Today", color = if (todayStatus) SuccessGreen else ErrorRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineSheet(context: Context, onDismiss: () -> Unit, onSave: (String, String, Int, Int, Int) -> Unit) {
    var medName by remember { mutableStateOf("") }
    var medStock by remember { mutableStateOf("") }

    var selectedTimeStr by remember { mutableStateOf("Select Time") }
    var selectedHour by remember { mutableIntStateOf(8) }
    var selectedMinute by remember { mutableIntStateOf(0) }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            selectedHour = hourOfDay
            selectedMinute = minute
            val amPm = if (hourOfDay >= 12) "PM" else "AM"
            val displayHour = if (hourOfDay == 0) 12 else if (hourOfDay > 12) hourOfDay - 12 else hourOfDay
            selectedTimeStr = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
        },
        selectedHour, selectedMinute, false
    )

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppBackground) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Add New Medicine", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = medName, onValueChange = { medName = it },
                label = { Text("Medicine Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f).clickable { timePickerDialog.show() }) {
                    OutlinedTextField(
                        value = selectedTimeStr, onValueChange = {},
                        label = { Text("Time") },
                        enabled = false,
                        trailingIcon = { Icon(Icons.Default.AccessTime, null, tint = BrandBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            disabledBorderColor = TextSecondary.copy(alpha = 0.5f),
                            disabledLabelColor = TextSecondary
                        )
                    )
                }

                OutlinedTextField(
                    value = medStock, onValueChange = { if (it.all { char -> char.isDigit() }) medStock = it },
                    label = { Text("Total Stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    if (medName.isNotBlank() && selectedTimeStr != "Select Time" && medStock.isNotBlank()) {
                        onSave(medName.trim(), selectedTimeStr, selectedHour, selectedMinute, medStock.toInt())
                    } else {
                        Toast.makeText(context, "Please fill all details and select time", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Text("Save Medicine", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun getLast7Days(): List<String> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dates = mutableListOf<String>()
    for (i in 0..6) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -i)
        dates.add(dateFormat.format(cal.time))
    }
    return dates.reversed()
}