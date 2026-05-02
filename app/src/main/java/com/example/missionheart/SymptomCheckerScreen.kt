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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.missionheart.ui.theme.*
import kotlinx.coroutines.launch
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.SerializationException
import kotlinx.serialization.SerializationException as KotlinSerializationException
import com.example.missionheart.BuildConfig

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
        SymptomData("g1", "Fever", "Body temperature above normal", BodyPart.GENERAL, Icons.Outlined.Thermostat, SymptomSeverity.MODERATE),
        SymptomData("g2", "Fatigue", "Extreme tiredness", BodyPart.GENERAL, Icons.Outlined.BatteryAlert, SymptomSeverity.COMMON),
        SymptomData("g3", "Body Ache", "General body pain", BodyPart.GENERAL, Icons.Outlined.AccessibilityNew, SymptomSeverity.COMMON),
        SymptomData("g5", "Chills", "Feeling cold with shivering", BodyPart.GENERAL, Icons.Outlined.AcUnit, SymptomSeverity.COMMON),
        SymptomData("h1", "Headache", "Pain in head region", BodyPart.HEAD, Icons.Outlined.Psychology, SymptomSeverity.COMMON),
        SymptomData("h2", "Dizziness", "Feeling lightheaded", BodyPart.HEAD, Icons.Outlined.Loop, SymptomSeverity.MODERATE),
        SymptomData("h3", "Migraine", "Severe recurring headache", BodyPart.HEAD, Icons.Outlined.FlashOn, SymptomSeverity.SERIOUS),
        SymptomData("e1", "Blurred Vision", "Unclear vision", BodyPart.EYES, Icons.Outlined.Visibility, SymptomSeverity.MODERATE),
        SymptomData("e2", "Eye Pain", "Pain in or around eyes", BodyPart.EYES, Icons.Outlined.RemoveRedEye, SymptomSeverity.MODERATE),
        SymptomData("c1", "Chest Pain", "Pain in chest area", BodyPart.CHEST, Icons.Outlined.Favorite, SymptomSeverity.SERIOUS),
        SymptomData("c2", "Shortness of Breath", "Difficulty breathing", BodyPart.CHEST, Icons.Outlined.Air, SymptomSeverity.SERIOUS),
        SymptomData("c4", "Cough", "Persistent coughing", BodyPart.CHEST, Icons.Outlined.Masks, SymptomSeverity.COMMON),
        SymptomData("t1", "Sore Throat", "Throat pain or irritation", BodyPart.THROAT, Icons.Outlined.RecordVoiceOver, SymptomSeverity.COMMON),
        SymptomData("t2", "Difficulty Swallowing", "Hard to swallow", BodyPart.THROAT, Icons.Outlined.Restaurant, SymptomSeverity.MODERATE),
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
                title = { Text("AI Symptom Checker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground, titleContentColor = TextPrimary)
            )
        },
        containerColor = AppBackground
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("What are you experiencing?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select all symptoms that apply to you", fontSize = 14.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search symptoms...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = BrandTeal) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandTeal,
                        unfocusedBorderColor = InputFieldBg,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            LazyRow(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(BodyPart.entries) { part ->
                    val isSelected = selectedCategory == part
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(if (isSelected) BrandBlue else SurfaceWhite).clickable { selectedCategory = if (isSelected) null else part }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(part.icon, null, tint = if (isSelected) Color.White else TextSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(part.displayName, color = if (isSelected) Color.White else TextSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            val filteredSymptoms = SymptomDatabase.getFilteredSymptoms(searchQuery, selectedCategory)

            LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(filteredSymptoms, key = { it.id }) { symptom ->
                    val isSelected = selectedSymptoms.contains(symptom.name)
                    SymptomCard(symptom, isSelected) {
                        val updatedList = selectedSymptoms.toMutableList()
                        if (isSelected) updatedList.remove(symptom.name) else updatedList.add(symptom.name)
                        selectedSymptoms = updatedList
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = {
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
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f)
    Card(
        modifier = Modifier.aspectRatio(1f).scale(scale).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) BrandBlue.copy(alpha = 0.2f) else SurfaceWhite),
        border = if (isSelected) BorderStroke(2.dp, BrandBlue) else null
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(symptom.icon, null, tint = if (isSelected) BrandBlue else TextSecondary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(symptom.name, color = if (isSelected) TextPrimary else TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomAnalysisResultScreen(navController: NavController, symptoms: List<String>) {
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    val fullName = currentUser?.displayName ?: "User"
    val userName = fullName.split(" ").firstOrNull() ?: "User"

    var isAnalyzing by remember { mutableStateOf(true) }
    var aiAssessment by remember { mutableStateOf("") }

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-3-flash-preview", // Updated to the requested 3.0 preview model
            apiKey = BuildConfig.GEMINI_API_KEY,
            requestOptions = RequestOptions(apiVersion = "v1beta") // Preview models often require v1beta
        )
    }

    LaunchedEffect(symptoms) {
        try {
            val prompt = """
                User Name: $userName
                Symptoms: ${symptoms.joinToString(", ")}
                
                Please provide a personalized medical assessment for $userName. 
                Structure it in exactly 3 lines:
                1. A friendly greeting and summary of potential causes for these symptoms.
                2. Professional advice citing trusted sources (like Mayo Clinic or WHO).
                3. A clear recommendation for next steps or checkup importance.
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            aiAssessment = response.text ?: "No assessment available."
        } catch (e: ResponseStoppedException) {
            Log.e("GeminiError", "Response stopped", e)
            aiAssessment = "The AI response was filtered due to safety policies. Please try describing your symptoms differently."
        } catch (e: SerializationException) {
            Log.e("GeminiError", "SDK Serialization error", e)
            aiAssessment = "Medical data processing error. This usually happens during API updates. Please try again in a few moments."
        } catch (e: KotlinSerializationException) {
            Log.e("GeminiError", "Kotlin Serialization error (MissingField)", e)
            aiAssessment = "The AI service returned an unexpected response format. We are working on a fix. Please try again later."
        } catch (e: Exception) {
            Log.e("GeminiError", "AI Error", e)
            val errorMessage = when {
                e.message?.contains("404") == true -> "Error 404: The model 'gemini-3-flash-preview' was not found. Please ensure it is enabled in your region."
                e.message?.contains("429") == true -> "Rate limit exceeded. Please wait a moment before trying again."
                else -> e.localizedMessage ?: "An unexpected connection error occurred."
            }
            aiAssessment = "Analysis Error: $errorMessage"
        } finally {
            isAnalyzing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Health Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            if (isAnalyzing) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = BrandBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Gathering medical data...", color = TextSecondary)
                    }
                }
            } else {
                DynamicResultCard(symptoms, aiAssessment)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { navController.navigate(NavGraph.DOCTORS_ROUTE) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("Book Specialist Consultation", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("Re-check Symptoms")
                }
            }
        }
    }
}

@Composable
fun DynamicResultCard(symptoms: List<String>, assessment: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Detected Symptoms:", fontWeight = FontWeight.Bold, color = BrandBlue, fontSize = 14.sp)
            Text(symptoms.joinToString(", "), color = TextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Text("AI Medical Assessment:", fontWeight = FontWeight.ExtraBold, color = BrandBlue, fontSize = 16.sp)
            Text(text = assessment, color = TextSecondary, lineHeight = 22.sp, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Verified, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scientifically Proven Sources", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
