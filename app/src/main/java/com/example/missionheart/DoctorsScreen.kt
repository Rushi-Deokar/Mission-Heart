package com.example.missionheart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.missionheart.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- Main Screen ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DoctorsScreen() {
    // Categories using the global Category model from Models.kt
    val categories = listOf(
        Category("All", "All", Icons.AutoMirrored.Filled.List, BrandBlue),
        Category("General", "General", Icons.Default.MedicalServices, BrandBlue),
        Category("Dentist", "Dentist", Icons.Default.Face, BrandBlue),
        Category("Heart", "Heart", Icons.Default.Favorite, BrandBlue),
        Category("Eye", "Eye", Icons.Default.Visibility, BrandBlue),
        Category("Skin", "Skin", Icons.Default.Person, BrandBlue),
        Category("Neuro", "Neuro", Icons.Default.Psychology, BrandBlue)
    )

    // Doctors Data using the global Doctor model from Models.kt
    val allDoctors = listOf(
        Doctor("1", "Dr. Rushi Patil", "Cardiologist", "Heart", "12 Yrs", 4.9, 120, 800, "Expert in heart health.", true),
        Doctor("2", "Dr. Anjali Sharma", "Dentist", "Dentist", "5 Yrs", 4.5, 85, 400, "Gentle dental care.", true),
        Doctor("3", "Dr. Sameer Khan", "Physician", "General", "8 Yrs", 4.7, 210, 500, "Experienced general physician.", false),
        Doctor("4", "Dr. Priya Deshmukh", "Dermatologist", "Skin", "6 Yrs", 4.8, 95, 700, "Skin and hair specialist.", true),
        Doctor("5", "Dr. Vikram Singh", "Neurologist", "Neuro", "15 Yrs", 5.0, 300, 1200, "Brain and nerve specialist.", true),
        Doctor("6", "Dr. Neha Gupta", "Eye Specialist", "Eye", "10 Yrs", 4.6, 150, 600, "Eye surgery expert.", true),
        Doctor("7", "Dr. Arjun Reddy", "Surgeon", "Heart", "9 Yrs", 4.8, 110, 1500, "Specialized in complex surgeries.", true)
    )

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    // Bottom Sheet Logic
    var showBookingSheet by remember { mutableStateOf(false) }
    var selectedDoctorForBooking by remember { mutableStateOf<Doctor?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filteredDoctors = allDoctors.filter { doctor ->
        val matchesCategory = selectedCategory == "All" || doctor.category == selectedCategory
        val matchesSearch = doctor.name.contains(searchQuery, ignoreCase = true) ||
                doctor.specialty.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    // --- Main UI ---
    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text("Find Specialist", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search doctor...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary) }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceWhite, unfocusedContainerColor = SurfaceWhite,
                    focusedIndicatorColor = BrandBlue, unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = BrandBlue
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Categories
            Text("Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(categories) { category ->
                    CategoryChip(category, selectedCategory == category.id) { selectedCategory = category.id }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Doctors List
            Text(if (selectedCategory == "All") "Top Doctors" else "$selectedCategory Specialists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (filteredDoctors.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No doctors found", color = TextSecondary) } }
                } else {
                    items(filteredDoctors) { doctor ->
                        DoctorCard(doctor = doctor) {
                            selectedDoctorForBooking = doctor
                            showBookingSheet = true
                        }
                    }
                }
            }
        }

        // --- Bottom Sheet Implementation ---
        if (showBookingSheet && selectedDoctorForBooking != null) {
            ModalBottomSheet(
                onDismissRequest = { showBookingSheet = false },
                sheetState = sheetState,
                containerColor = SurfaceWhite,
                contentColor = TextPrimary,
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                BookingBottomSheetContent(
                    doctor = selectedDoctorForBooking!!,
                    onConfirm = {
                        showBookingSheet = false
                    }
                )
            }
        }
    }
}

// --- Booking Bottom Sheet Content ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingBottomSheetContent(doctor: Doctor, onConfirm: () -> Unit) {
    var selectedDateIndex by remember { mutableIntStateOf(0) }
    var selectedTimeSlot by remember { mutableStateOf("10:00 AM") }
    var consultationType by remember { mutableStateOf("Clinic Visit") }
    var patientName by remember { mutableStateOf("Rushi") }
    var symptoms by remember { mutableStateOf("") }

    val dateList = remember {
        val calendar = Calendar.getInstance()
        val list = mutableListOf<Date>()
        for (i in 0..6) {
            list.add(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val morningSlots = listOf("09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM")
    val eveningSlots = listOf("04:00 PM", "04:30 PM", "05:00 PM", "05:30 PM", "06:00 PM", "06:30 PM")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        // 1. Doctor Summary
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(16.dp)).background(InputFieldBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(35.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(doctor.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(doctor.specialty, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = WarningYellow, modifier = Modifier.size(14.dp))
                    Text(" ${doctor.rating} Rating", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = InputFieldBg)

        // 2. Consultation Type
        Text("Consultation Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ConsultationModeCard(
                icon = Icons.Outlined.LocalHospital,
                title = "Clinic Visit",
                isSelected = consultationType == "Clinic Visit",
                modifier = Modifier.weight(1f)
            ) { consultationType = "Clinic Visit" }

            ConsultationModeCard(
                icon = Icons.Outlined.Videocam,
                title = "Video Call",
                isSelected = consultationType == "Video Call",
                modifier = Modifier.weight(1f)
            ) { consultationType = "Video Call" }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Date Selection
        Text("Select Date", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(dateList) { index, date ->
                DateCard(date = date, isSelected = selectedDateIndex == index) { selectedDateIndex = index }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Time Slots
        Text("Available Slots", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Morning", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            morningSlots.forEach { slot ->
                TimeSlotChip(slot, selectedTimeSlot == slot) { selectedTimeSlot = slot }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Evening", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            eveningSlots.forEach { slot ->
                TimeSlotChip(slot, selectedTimeSlot == slot) { selectedTimeSlot = slot }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Patient Details
        Text("Patient Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))

        CustomTextField(value = patientName, onValueChange = { patientName = it }, label = "Patient Name", icon = Icons.Default.Person)
        Spacer(modifier = Modifier.height(12.dp))
        CustomTextField(value = symptoms, onValueChange = { symptoms = it }, label = "Write your problem (Optional)", icon = Icons.Default.Edit)

        Spacer(modifier = Modifier.height(30.dp))

        // 6. Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Total to Pay", fontSize = 12.sp, color = TextSecondary)
                Text("₹${doctor.fee}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                modifier = Modifier.width(200.dp).height(50.dp)
            ) {
                Text("Pay & Confirm", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --- Components ---

@Composable
fun ConsultationModeCard(icon: ImageVector, title: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(70.dp)
            .border(1.5.dp, if (isSelected) BrandBlue else InputFieldBg, RoundedCornerShape(12.dp))
            .background(if (isSelected) BrandBlue.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (isSelected) BrandBlue else TextSecondary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.SemiBold, color = if (isSelected) BrandBlue else TextSecondary)
        }
    }
}

@Composable
fun DateCard(date: Date, isSelected: Boolean, onClick: () -> Unit) {
    val dayName = SimpleDateFormat("EEE", Locale.ENGLISH).format(date).uppercase()
    val dayNum = SimpleDateFormat("d", Locale.ENGLISH).format(date)

    Column(
        modifier = Modifier.width(60.dp).height(75.dp).clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) BrandBlue else InputFieldBg)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(dayName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Color.White.copy(alpha = 0.7f) else TextSecondary.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(4.dp))
        Text(dayNum, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else TextPrimary)
    }
}

@Composable
fun TimeSlotChip(time: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) BrandBlue else InputFieldBg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(time, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Color.White else TextSecondary)
    }
}

@Composable
fun CustomTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label, color = TextSecondary) },
        leadingIcon = { Icon(icon, null, tint = TextSecondary) }, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = BrandBlue, unfocusedIndicatorColor = InputFieldBg,
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = BrandBlue
        ),
        singleLine = true
    )
}

@Composable
fun CategoryChip(category: Category, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) BrandBlue else SurfaceWhite
    val contentColor = if (isSelected) Color.White else TextSecondary
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(4.dp)) {
        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
            Icon(category.icon, null, tint = contentColor, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(category.name, style = MaterialTheme.typography.bodySmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) BrandBlue else TextSecondary)
    }
}

@Composable
fun DoctorCard(doctor: Doctor, onBookClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box {
                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(InputFieldBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = BrandBlue)
                }
                if (doctor.isAvailable) Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(10.dp).clip(CircleShape).background(SuccessGreen))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(doctor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Surface(shape = RoundedCornerShape(6.dp), color = InputFieldBg) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = WarningYellow, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(doctor.rating.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
                Text(doctor.specialty, style = MaterialTheme.typography.bodyMedium, color = BrandBlue)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(doctor.experience, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("• ₹${doctor.fee}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBookClick, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = BrandBlue), modifier = Modifier.fillMaxWidth().height(40.dp), contentPadding = PaddingValues(0.dp)) {
                    Text("Book Appointment", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}
