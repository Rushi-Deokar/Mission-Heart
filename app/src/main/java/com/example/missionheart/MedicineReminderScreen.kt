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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.missionheart.ui.theme.*

// --- DATA MODEL ---
data class MedicineReminder(
    val id: Int,
    val name: String,
    val dosage: String,
    val time: String,
    val instruction: String,
    val frequency: String = "Daily",
    var isTaken: Boolean = false,
    var stock: Int = 10 // Default stock for demo
)

// --- MAIN SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineReminderScreen(navController: NavController) {
    // State
    // Using mutableStateListOf so UI updates when items change
    val medicines = remember {
        mutableStateListOf(
            MedicineReminder(1, "Paracetamol 650", "1 Tablet", "08:00 AM", "After Food", "Daily", stock = 15),
            MedicineReminder(2, "Vitamin C", "1 Capsule", "01:00 PM", "With Food", "Daily", stock = 4), // Low stock example
            MedicineReminder(3, "Metformin 500mg", "1 Tablet", "09:00 PM", "After Dinner", "Daily", stock = 8),
            MedicineReminder(4, "Aspirin", "1 Tablet", "10:00 PM", "Before Sleep", "Daily", stock = 2) // Critical stock
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Medicine Reminder",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontSize = 20.sp
                        )
                        Text(
                            "Never miss your dose",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
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
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Add Medicine",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = AppBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Stats
            item {
                MedicineStatsHeader(medicines)
            }

            // Today's Schedule Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Today's Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandTeal.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "${medicines.size} medicines",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTeal
                        )
                    }
                }
            }

            // Medicine List
            items(medicines) { med ->
                MedicineCardItem(
                    medicine = med,
                    onTakenClick = {
                        val index = medicines.indexOf(med)
                        if (index != -1) {
                            // Toggle taken status and update stock logic
                            // If untaking -> add back to stock. If taking -> reduce stock.
                            val newStatus = !med.isTaken
                            val newStock = if (newStatus) med.stock - 1 else med.stock + 1
                            medicines[index] = med.copy(isTaken = newStatus, stock = newStock)
                        }
                    }
                )
            }

            // Empty State
            if (medicines.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No medicines added yet.", color = TextSecondary)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Add Dialog
        if (showAddDialog) {
            AddMedicineDialogContent(
                onDismiss = { showAddDialog = false },
                onAdd = { name, dose, time, instruction ->
                    medicines.add(
                        MedicineReminder(
                            medicines.size + 1,
                            name,
                            dose,
                            time,
                            instruction,
                            "Daily",
                            stock = 10 // Default new stock
                        )
                    )
                    showAddDialog = false
                }
            )
        }
    }
}

// --- COMPONENTS ---

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Progress Circle
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(80.dp),
                    color = InputFieldBg,
                    strokeWidth = 8.dp,
                )
                CircularProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier.size(80.dp),
                    color = BrandTeal,
                    strokeWidth = 8.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text(
                    "${(progressValue * 100).toInt()}%",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Right: Stats Text
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Daily Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("$taken of $total doses taken", fontSize = 14.sp, color = TextSecondary)

                // Refill Alert Logic
                val lowStockCount = medicines.count { it.stock <= 5 }
                if (lowStockCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, null, tint = ActionOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$lowStockCount medicines low on stock", fontSize = 12.sp, color = ActionOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MedicineCardItem(
    medicine: MedicineReminder,
    onTakenClick: () -> Unit
) {
    val bgColor = if (medicine.isTaken) SurfaceWhite.copy(alpha = 0.6f) else SurfaceWhite
    val iconTint = if (medicine.isTaken) SuccessGreen else BrandTeal

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Medication,
                    null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (medicine.isTaken) TextSecondary else TextPrimary,
                    style = if (medicine.isTaken) androidx.compose.ui.text.TextStyle(textDecoration = TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${medicine.time} • ${medicine.dosage}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                // Stock Indicator
                if (medicine.stock <= 5) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (medicine.stock <= 0) "Out of Stock!" else "Low Stock: ${medicine.stock} left",
                        fontSize = 11.sp,
                        color = ActionOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action Checkbox (Custom)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (medicine.isTaken) SuccessGreen else InputFieldBg)
                    .clickable { onTakenClick() },
                contentAlignment = Alignment.Center
            ) {
                if (medicine.isTaken) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun AddMedicineDialogContent(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf("After Food") }
    var showInstructionMenu by remember { mutableStateOf(false) }

    val instructions = listOf("Before Food", "After Food", "With Food", "Empty Stomach", "Before Sleep")

    AlertDialog(
        containerColor = SurfaceWhite,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Medication, null, tint = BrandTeal)
        },
        title = {
            Text("Add Medicine", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BrandTeal,
                        unfocusedBorderColor = InputFieldBg
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dose,
                        onValueChange = { dose = it },
                        label = { Text("Dose") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BrandTeal,
                            unfocusedBorderColor = InputFieldBg
                        )
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BrandTeal,
                            unfocusedBorderColor = InputFieldBg
                        )
                    )
                }

                // Simple Dropdown for Instruction
                Box {
                    OutlinedTextField(
                        value = instruction,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Instruction") },
                        trailingIcon = {
                            IconButton(onClick = { showInstructionMenu = !showInstructionMenu }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { showInstructionMenu = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BrandTeal,
                            unfocusedBorderColor = InputFieldBg
                        )
                    )
                    DropdownMenu(
                        expanded = showInstructionMenu,
                        onDismissRequest = { showInstructionMenu = false },
                        modifier = Modifier.background(SurfaceWhite)
                    ) {
                        instructions.forEach { label ->
                            DropdownMenuItem(
                                text = { Text(label, color = TextPrimary) },
                                onClick = {
                                    instruction = label
                                    showInstructionMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, dose, time, instruction) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                enabled = name.isNotEmpty() && dose.isNotEmpty()
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}