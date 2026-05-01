package com.example.missionheart

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.missionheart.ui.theme.*
import java.util.Calendar

// --- DATA MODEL ---
data class MedicineReminder(
    val id: Int,
    val name: String,
    val dosage: String,
    val time: String, // format "HH:mm AM/PM"
    val instruction: String,
    val frequency: String = "Daily",
    var isTaken: Boolean = false,
    var stock: Int = 10
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineReminderScreen(navController: NavController) {
    val context = LocalContext.current
    
    val medicines = remember {
        mutableStateListOf(
            MedicineReminder(1, "Paracetamol 650", "1 Tablet", "08:00 AM", "After Food", "Daily", stock = 15),
            MedicineReminder(2, "Vitamin C", "1 Capsule", "01:00 PM", "With Food", "Daily", stock = 4),
            MedicineReminder(3, "Metformin 500mg", "1 Tablet", "09:00 PM", "After Dinner", "Daily", stock = 8),
            MedicineReminder(4, "Aspirin", "1 Tablet", "10:00 PM", "Before Sleep", "Daily", stock = 2)
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Medicine Reminder", fontWeight = FontWeight.ExtraBold, color = TextPrimary, fontSize = 20.sp)
                        Text("Never miss your dose", color = TextSecondary, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BrandTeal,
                contentColor = Color.White,
                modifier = Modifier.shadow(8.dp, CircleShape)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Medicine", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = AppBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { MedicineStatsHeader(medicines) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Today's Schedule", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Surface(shape = RoundedCornerShape(8.dp), color = BrandTeal.copy(alpha = 0.15f)) {
                        Text("${medicines.size} medicines", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandTeal)
                    }
                }
            }
            items(medicines) { med ->
                MedicineCardItem(
                    medicine = med,
                    onTakenClick = {
                        val index = medicines.indexOf(med)
                        if (index != -1) {
                            val newStatus = !med.isTaken
                            val newStock = if (newStatus) med.stock - 1 else med.stock + 1
                            medicines[index] = med.copy(isTaken = newStatus, stock = newStock)
                        }
                    },
                    onDeleteClick = {
                        medicines.remove(med)
                        cancelMedicineAlarm(context, med)
                    }
                )
            }
            if (medicines.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No medicines added yet.", color = TextSecondary)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (showAddDialog) {
            AddMedicineDialogContent(
                onDismiss = { showAddDialog = false },
                onAdd = { name, dose, time, instruction ->
                    val newMed = MedicineReminder(medicines.size + 1, name, dose, time, instruction, "Daily", stock = 10)
                    medicines.add(newMed)
                    scheduleMedicineAlarm(context, newMed)
                    showAddDialog = false
                    Toast.makeText(context, "Reminder set for $time", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

private fun scheduleMedicineAlarm(context: Context, medicine: MedicineReminder) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    // Exact alarm permission handling for API 31+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Please allow exact alarms in settings", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
            return
        }
    }

    val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
        putExtra("MEDICINE_NAME", medicine.name)
        putExtra("DOSAGE", medicine.dosage)
        putExtra("MEDICINE_ID", medicine.id)
    }
    
    val pendingIntent = PendingIntent.getBroadcast(
        context, medicine.id, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
        val parts = medicine.time.split(" ", ":")
        var hour = parts[0].toInt()
        val minute = parts[1].toInt()
        val amPm = parts[2]

        if (amPm.equals("PM", ignoreCase = true) && hour < 12) hour += 12
        if (amPm.equals("AM", ignoreCase = true) && hour == 12) hour = 0

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    } catch (e: Exception) {
        Log.e("MedicineReminder", "Failed to set alarm", e)
    }
}

private fun cancelMedicineAlarm(context: Context, medicine: MedicineReminder) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, MedicineAlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, medicine.id, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}

@Composable
fun MedicineStatsHeader(medicines: List<MedicineReminder>) {
    val total = medicines.size
    val taken = medicines.count { it.isTaken }
    val progressValue = if (total > 0) taken.toFloat() / total else 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = 1f, modifier = Modifier.size(80.dp), color = InputFieldBg, strokeWidth = 8.dp)
                CircularProgressIndicator(progress = progressValue, modifier = Modifier.size(80.dp), color = BrandTeal, strokeWidth = 8.dp)
                Text("${(progressValue * 100).toInt()}%", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Daily Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("$taken of $total doses taken", fontSize = 14.sp, color = TextSecondary)
                val lowStockCount = medicines.count { it.stock <= 5 }
                if (lowStockCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, null, tint = ActionOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$lowStockCount low on stock", fontSize = 12.sp, color = ActionOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MedicineCardItem(medicine: MedicineReminder, onTakenClick: () -> Unit, onDeleteClick: () -> Unit) {
    val bgColor = if (medicine.isTaken) SurfaceWhite.copy(alpha = 0.6f) else SurfaceWhite
    val iconTint = if (medicine.isTaken) SuccessGreen else BrandTeal
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = bgColor), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Medication, null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = medicine.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (medicine.isTaken) TextSecondary else TextPrimary, textDecoration = if (medicine.isTaken) TextDecoration.LineThrough else TextDecoration.None)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${medicine.time} • ${medicine.dosage}", fontSize = 13.sp, color = TextSecondary)
                }
                if (medicine.stock <= 5) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (medicine.stock <= 0) "Out of Stock!" else "Low Stock: ${medicine.stock} left", fontSize = 11.sp, color = ActionOrange, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
            }
            
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (medicine.isTaken) SuccessGreen else InputFieldBg).clickable { onTakenClick() }, contentAlignment = Alignment.Center) {
                if (medicine.isTaken) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun AddMedicineDialogContent(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("08") }
    var minute by remember { mutableStateOf("00") }
    var amPm by remember { mutableStateOf("AM") }
    var instruction by remember { mutableStateOf("After Food") }
    var showInstructionMenu by remember { mutableStateOf(false) }
    val instructions = listOf("Before Food", "After Food", "With Food", "Empty Stomach", "Before Sleep")

    AlertDialog(
        containerColor = SurfaceWhite,
        onDismissRequest = onDismiss,
        title = { Text("Add Medicine Reminder", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Medicine Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dose, onValueChange = { dose = it }, label = { Text("Dose (e.g. 1 Tablet)") }, modifier = Modifier.fillMaxWidth())
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = hour, onValueChange = { if(it.length <= 2) hour = it }, label = { Text("HH") }, modifier = Modifier.width(64.dp))
                    Text(":", fontWeight = FontWeight.Bold, color = TextPrimary)
                    OutlinedTextField(value = minute, onValueChange = { if(it.length <= 2) minute = it }, label = { Text("MM") }, modifier = Modifier.width(64.dp))
                    Button(onClick = { amPm = if(amPm == "AM") "PM" else "AM" }, modifier = Modifier.height(56.dp)) { Text(amPm) }
                }

                Box {
                    OutlinedTextField(value = instruction, onValueChange = {}, readOnly = true, label = { Text("Instruction") }, trailingIcon = { IconButton(onClick = { showInstructionMenu = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth().clickable { showInstructionMenu = true })
                    DropdownMenu(expanded = showInstructionMenu, onDismissRequest = { showInstructionMenu = false }) {
                        instructions.forEach { label ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { instruction = label; showInstructionMenu = false })
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onAdd(name, dose, "${hour.padStart(2, '0')}:${minute.padStart(2, '0')} $amPm", instruction) }, enabled = name.isNotBlank() && dose.isNotEmpty()) { Text("Save Reminder") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
