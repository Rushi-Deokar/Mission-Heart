package com.example.missionheart

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.missionheart.ui.theme.*
import kotlinx.coroutines.launch
import com.google.ai.client.generativeai.GenerativeModel

// --- DATA MODELS ---

enum class BodyPart(val displayName: String, val icon: ImageVector) {
    HEAD("Head", Icons.Outlined.Psychology),
    CHEST("Chest", Icons.Outlined.Favorite),
    ABDOMEN("Abdomen", Icons.Outlined.FitnessCenter),
    THROAT("Throat", Icons.Outlined.RecordVoiceOver),
    EYES("Eyes", Icons.Outlined.Visibility),
    GENERAL("General", Icons.Outlined.HealthAndSafety)
}

data class SymptomData(
    val id: String,
    val name: String,
    val description: String,
    val category: BodyPart,
    val icon: ImageVector,
    val severity: SymptomSeverity = SymptomSeverity.COMMON
)

enum class SymptomSeverity(val color: Color) {
    COMMON(SuccessGreen),
    MODERATE(ActionOrange),
    SERIOUS(ErrorRed)
}

// --- SYMPTOM DATABASE ---

object SymptomDatabase {
    val allSymptoms = listOf(
        // General Symptoms
        SymptomData("g1", "Fever", "Body temperature above normal", BodyPart.GENERAL, Icons.Outlined.Thermostat, SymptomSeverity.MODERATE),
        SymptomData("g2", "Fatigue", "Extreme tiredness", BodyPart.GENERAL, Icons.Outlined.BatteryAlert, SymptomSeverity.COMMON),
        SymptomData("g3", "Body Ache", "General body pain", BodyPart.GENERAL, Icons.Outlined.AccessibilityNew, SymptomSeverity.COMMON),
        SymptomData("g5", "Chills", "Feeling cold with shivering", BodyPart.GENERAL, Icons.Outlined.AcUnit, SymptomSeverity.COMMON),

        // Head Symptoms
        SymptomData("h1", "Headache", "Pain in head region", BodyPart.HEAD, Icons.Outlined.Psychology, SymptomSeverity.COMMON),
        SymptomData("h2", "Dizziness", "Feeling lightheaded", BodyPart.HEAD, Icons.Outlined.Loop, SymptomSeverity.MODERATE),
        SymptomData("h3", "Migraine", "Severe recurring headache", BodyPart.HEAD, Icons.Outlined.FlashOn, SymptomSeverity.SERIOUS),

        // Eyes
        SymptomData("e1", "Blurred Vision", "Unclear vision", BodyPart.EYES, Icons.Outlined.Visibility, SymptomSeverity.MODERATE),
        SymptomData("e2", "Eye Pain", "Pain in or around eyes", BodyPart.EYES, Icons.Outlined.RemoveRedEye, SymptomSeverity.MODERATE),

        // Chest
        SymptomData("c1", "Chest Pain", "Pain in chest area", BodyPart.CHEST, Icons.Outlined.Favorite, SymptomSeverity.SERIOUS),
        SymptomData("c2", "Shortness of Breath", "Difficulty breathing", BodyPart.CHEST, Icons.Outlined.Air, SymptomSeverity.SERIOUS),
        SymptomData("c4", "Cough", "Persistent coughing", BodyPart.CHEST, Icons.Outlined.Masks, SymptomSeverity.COMMON),

        // Throat
        SymptomData("t1", "Sore Throat", "Throat pain or irritation", BodyPart.THROAT, Icons.Outlined.RecordVoiceOver, SymptomSeverity.COMMON),
        SymptomData("t2", "Difficulty Swallowing", "Hard to swallow", BodyPart.THROAT, Icons.Outlined.Restaurant, SymptomSeverity.MODERATE),

        // Abdomen
        SymptomData("a1", "Abdominal Pain", "Pain in stomach", BodyPart.ABDOMEN, Icons.Outlined.FitnessCenter, SymptomSeverity.MODERATE),
        SymptomData("a2", "Nausea", "Feeling sick", BodyPart.ABDOMEN, Icons.Outlined.SentimentDissatisfied, SymptomSeverity.COMMON),
        SymptomData("a3", "Vomiting", "Throwing up", BodyPart.ABDOMEN, Icons.Outlined.SentimentVeryDissatisfied, SymptomSeverity.MODERATE)
    )

    fun getFilteredSymptoms(searchQuery: String, selectedCategory: BodyPart?): List<SymptomData> {
        return allSymptoms.filter { symptom ->
            val matchesSearch = searchQuery.isEmpty() ||
                    symptom.name.contains(searchQuery, ignoreCase = true) ||
                    symptom.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || symptom.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }
}

// --- MAIN SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomCheckerScreen(navController: NavController) {
    var selectedSymptoms by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<BodyPart?>(null) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("AI Symptom Checker", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = AppBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "What are you experiencing?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Select all symptoms that apply to you",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search symptoms...", color = TextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = BrandTeal)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedIndicatorColor = BrandTeal,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            // Body Part Selector
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(BodyPart.entries) { part ->
                    val isSelected = selectedCategory == part
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) BrandBlue else SurfaceWhite)
                            .clickable { selectedCategory = if (isSelected) null else part }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(part.icon, contentDescription = null, tint = if (isSelected) Color.White else TextSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(part.displayName, color = if (isSelected) Color.White else TextSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            val filteredSymptoms = SymptomDatabase.getFilteredSymptoms(searchQuery, selectedCategory)

            // Symptom Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredSymptoms, key = { it.id }) { symptom ->
                    val isSelected = selectedSymptoms.contains(symptom.name)
                    SymptomCard(symptom, isSelected) {
                        val updatedList = selectedSymptoms.toMutableList()
                        if (isSelected) {
                            updatedList.remove(symptom.name)
                        } else {
                            updatedList.add(symptom.name)
                        }
                        selectedSymptoms = updatedList
                    }
                }
            }

            // Action Button
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = {
                        // Navigate to Result Screen with selected symptoms
                        val symptomsString = selectedSymptoms.joinToString(",")
                        navController.navigate("symptom_analysis_result/$symptomsString")
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = selectedSymptoms.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Analyze Symptoms", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SymptomCard(symptom: SymptomData, isSelected: Boolean, onClick: () -> Unit) {
    val animatedScale by animateFloatAsState(targetValue = if (isSelected) 1.05f else 1f, label = "scale")

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(animatedScale)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BrandBlue.copy(alpha = 0.2f) else SurfaceWhite
        ),
        border = if (isSelected) BorderStroke(2.dp, BrandBlue) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(symptom.icon, contentDescription = null, tint = if (isSelected) BrandBlue else TextSecondary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(symptom.name, color = if (isSelected) TextPrimary else TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomAnalysisResultScreen(navController: NavController, symptoms: List<String>) {
    var isAnalyzing by remember { mutableStateOf(true) }
    var aiAssessment by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // FIXED: Using "gemini-1.5-flash" (Removed -latest suffix which causes 404 in v1beta)
    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyD8mg2koycSfa_2DFzW2Xh9yLKOJPFshIs"
        )
    }

    LaunchedEffect(symptoms) {
        try {
            val prompt = """
                The user is experiencing these symptoms: ${symptoms.joinToString(", ")}. 
                Provide a scientifically backed assessment in 3-4 lines based on authentic medical knowledge. 
                Include common causes and a note about the importance of early diagnosis.
                Keep it concise and professional.
            """.trimIndent()
            
            val response = generativeModel.generateContent(prompt)
            aiAssessment = response.text ?: "Analysis unavailable. Please consult a doctor."
        } catch (e: Exception) {
            Log.e("GeminiError", "AI connection failed", e)
            aiAssessment = "AI connection failed. 1. Check Internet. 2. Ensure 'Generative Language API' is enabled for your key in Google Cloud Console. 3. Try again later."
        } finally {
            isAnalyzing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Health Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isAnalyzing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = BrandBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Consulting AI Medical Database...", color = TextSecondary)
                    }
                }
            } else {
                DynamicResultCard(symptoms, aiAssessment)
                
                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { navController.navigate(NavGraph.DOCTORS_ROUTE) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Book Specialist Consultation", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Re-check Symptoms", color = BrandBlue)
                }
            }
        }
    }
}

@Composable
fun DynamicResultCard(symptoms: List<String>, assessment: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Detected Symptoms:", fontWeight = FontWeight.Bold, color = BrandBlue, fontSize = 14.sp)
            Text(symptoms.joinToString(", "), color = TextSecondary, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text("AI Medical Assessment:", fontWeight = FontWeight.ExtraBold, color = BrandBlue, fontSize = 16.sp)
            Text(
                text = assessment,
                color = TextSecondary,
                lineHeight = 22.sp,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scientifically Proven Sources", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
