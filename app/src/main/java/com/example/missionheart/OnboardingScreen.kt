package com.example.missionheart

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    // Step Management
    var currentStep by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }

    // --- STEP 1 STATES (Basic Info) ---
    var gender by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var calculatedAge by remember { mutableStateOf(0) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var expandedBloodGroup by remember { mutableStateOf(false) }
    val bloodGroups = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

    // --- STEP 2 STATES (Medical History) ---
    val conditionOptions = listOf("None", "Diabetes", "Hypertension", "Asthma", "Thyroid", "Heart Disease")
    var selectedConditions by remember { mutableStateOf(setOf<String>()) }

    val allergyOptions = listOf("None", "Dust", "Pollen", "Peanuts", "Dairy", "Seafood")
    var selectedAllergies by remember { mutableStateOf(setOf<String>()) }

    // --- STEP 3 STATES (Lifestyle & Habits) ---
    val activityOptions = listOf("Sedentary (Desk Job)", "Lightly Active", "Very Active", "Athlete")
    var activityLevel by remember { mutableStateOf("") }

    val sleepOptions = listOf("< 5 Hours", "5-7 Hours", "7-9 Hours", "> 9 Hours")
    var sleepDuration by remember { mutableStateOf("") }

    // Helper: Age Calculator
    fun calculateAge(selectedMillis: Long) {
        val dobCalendar = Calendar.getInstance().apply { timeInMillis = selectedMillis }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) age--
        calculatedAge = if (age < 0) 0 else age
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dob = formatter.format(Date(selectedMillis))
    }

    // Helper: Save to Firebase
    fun saveUserDataAndNavigate(isSkipped: Boolean = false) {
        val currentUser = auth.currentUser
        if (currentUser == null) return
        isLoading = true

        val userData = hashMapOf(
            // Basic
            "name" to (currentUser.displayName ?: ""),
            "email" to (currentUser.email ?: ""),
            "dob" to if (isSkipped) "" else dob,
            "age" to if (isSkipped) 0 else calculatedAge,
            "gender" to if (isSkipped) "" else gender,
            "bloodGroup" to if (isSkipped) "" else bloodGroup,
            "height" to if (isSkipped) "" else height,
            "weight" to if (isSkipped) "" else weight,
            // Medical
            "conditions" to if (isSkipped) emptyList<String>() else selectedConditions.toList(),
            "allergies" to if (isSkipped) emptyList<String>() else selectedAllergies.toList(),
            // Lifestyle
            "activityLevel" to if (isSkipped) "" else activityLevel,
            "sleepDuration" to if (isSkipped) "" else sleepDuration,

            "onboardingCompleted" to true
        )

        db.collection("users").document(currentUser.uid)
            .set(userData)
            .addOnSuccessListener {
                isLoading = false
                navController.navigate(NavGraph.HOME_ROUTE) { popUpTo(0) }
            }
            .addOnFailureListener {
                isLoading = false
                navController.navigate(NavGraph.HOME_ROUTE) { popUpTo(0) }
            }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                // Top App Bar with Back Button and Skip
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp)) // Placeholder for balance
                    }

                    Text("Step $currentStep of 3", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    TextButton(onClick = { saveUserDataAndNavigate(isSkipped = true) }) {
                        Text("Skip", color = Color.Gray)
                    }
                }
                // Premium Progress Bar
                LinearProgressIndicator(
                    progress = currentStep / 3f,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Content dynamically changes based on Step
                when (currentStep) {
                    1 -> {
                        // ================= STEP 1: BASIC PROFILE =================
                        Icon(Icons.Default.PersonSearch, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Basic Profile", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Text("Let's start with the basics", color = Color.Gray, modifier = Modifier.padding(bottom = 32.dp))

                        // Gender
                        Text("Gender", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Male", "Female", "Other").forEach { option ->
                                FilterChip(
                                    selected = gender == option,
                                    onClick = { gender = option },
                                    label = { Text(option) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // DOB
                        OutlinedTextField(
                            value = dob, onValueChange = {}, readOnly = true,
                            label = { Text("Date of Birth") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                            trailingIcon = { if (dob.isNotEmpty()) Text("$calculatedAge yrs", modifier = Modifier.padding(end=12.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLeadingIconColor = MaterialTheme.colorScheme.primary, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Blood Group
                        ExposedDropdownMenuBox(expanded = expandedBloodGroup, onExpandedChange = { expandedBloodGroup = !expandedBloodGroup }) {
                            OutlinedTextField(
                                value = bloodGroup, onValueChange = {}, readOnly = true,
                                label = { Text("Blood Group") },
                                leadingIcon = { Icon(Icons.Default.WaterDrop, null, tint = Color.Red.copy(0.7f)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBloodGroup) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = expandedBloodGroup, onDismissRequest = { expandedBloodGroup = false }) {
                                bloodGroups.forEach { selectionOption ->
                                    DropdownMenuItem(text = { Text(selectionOption) }, onClick = { bloodGroup = selectionOption; expandedBloodGroup = false })
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Vitals
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height") }, suffix = { Text("cm") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight") }, suffix = { Text("kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                        }
                    }

                    2 -> {
                        // ================= STEP 2: MEDICAL HISTORY =================
                        Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Medical History", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Text("Any past conditions we should know about?", color = Color.Gray, modifier = Modifier.padding(bottom = 32.dp))

                        // Existing Conditions (Multi-select)
                        Text("Existing Conditions", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            conditionOptions.forEach { option ->
                                val isSelected = selectedConditions.contains(option)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (option == "None") { selectedConditions = setOf("None") }
                                        else {
                                            val newSet = selectedConditions.toMutableSet()
                                            newSet.remove("None")
                                            if (isSelected) newSet.remove(option) else newSet.add(option)
                                            selectedConditions = newSet
                                        }
                                    },
                                    label = { Text(option) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // Allergies (Multi-select)
                        Text("Known Allergies", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            allergyOptions.forEach { option ->
                                val isSelected = selectedAllergies.contains(option)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (option == "None") { selectedAllergies = setOf("None") }
                                        else {
                                            val newSet = selectedAllergies.toMutableSet()
                                            newSet.remove("None")
                                            if (isSelected) newSet.remove(option) else newSet.add(option)
                                            selectedAllergies = newSet
                                        }
                                    },
                                    label = { Text(option) }
                                )
                            }
                        }
                    }

                    3 -> {
                        // ================= STEP 3: LIFESTYLE & HABITS =================
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Lifestyle & Habits", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Text("Help us understand your daily routine", color = Color.Gray, modifier = Modifier.padding(bottom = 32.dp))

                        // Activity Level
                        Text("Daily Activity Level", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            activityOptions.forEach { option ->
                                FilterChip(
                                    selected = activityLevel == option,
                                    onClick = { activityLevel = option },
                                    label = { Text(option) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // Sleep Duration
                        Text("Average Sleep Duration", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            sleepOptions.forEach { option ->
                                FilterChip(
                                    selected = sleepDuration == option,
                                    onClick = { sleepDuration = option },
                                    label = { Text(option) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Action Button (Next or Save)
                Button(
                    onClick = {
                        if (currentStep < 3) {
                            currentStep++ // Agle step par jao
                        } else {
                            saveUserDataAndNavigate(isSkipped = false) // Database me save kardo
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (currentStep == 3) "Finish & Save" else "Next", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Date Picker Dialog Logic
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { datePickerState.selectedDateMillis?.let { calculateAge(it) }; showDatePicker = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}