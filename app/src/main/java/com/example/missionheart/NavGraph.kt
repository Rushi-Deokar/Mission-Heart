package com.example.missionheart

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * MISSION HEART - CENTRALIZED NAVIGATION GRAPH
 * * Ye file app ke saare routes aur navigation logic ko handle karti hai.
 */

object NavGraph {
    // --- AUTHENTICATION ROUTES ---
    const val LOGIN_ROUTE = "login"
    const val SIGNUP_ROUTE = "signup"
    const val ONBOARDING_ROUTE = "onboarding" // ✅ Naya Route

    // --- MAIN BOTTOM NAVIGATION ROUTES ---
    const val HOME_ROUTE = "home"
    const val DOCTORS_ROUTE = "doctors"
    const val PHARMACY_ROUTE = "pharmacy"
    const val LAB_TESTS_ROUTE = "lab_tests"
    const val INSURANCE_ROUTE = "insurance"
    const val MY_HEALTH_ROUTE = "my_health"

    // --- CORE FEATURE ROUTES ---
    const val SCREENING_ROUTE = "screening"
    const val NOTIFICATIONS_ROUTE = "notifications"
    const val CART_ROUTE = "cart"
    const val PROFILE_ROUTE = "profile_screen"
    const val EDIT_PROFILE_ROUTE = "edit_profile" 
    const val SYMPTOM_CHECKER_ROUTE = "symptom_checker"
    const val SYMPTOM_RESULT_ROUTE = "symptom_analysis_result/{symptoms}"
    const val MEDICINE_REMINDER_ROUTE = "medicine_reminder"
    const val AI_CHAT_ROUTE = "ai_chat"

    // --- SPECIALIZED CATEGORY ROUTES ---
    const val CATEGORY_CANCER_ROUTE = "category/cancer"
    const val CATEGORY_HEART_ROUTE = "category/heart"
    const val CATEGORY_METABOLIC_ROUTE = "category/metabolic"
    const val CATEGORY_NEUROLOGICAL_ROUTE = "category/neurological"

    // --- INFORMATION & SUPPORT ROUTES ---
    const val ABOUT_ROUTE = "about"
    const val CONTACT_ROUTE = "contact"
    const val CONDITIONS_ROUTE = "conditions"
    const val BLOG_ROUTE = "blog"
    const val FIND_SPECIALIST_ROUTE = "find_specialist"
    const val WHY_EARLY_DIAGNOSIS_ROUTE = "why_early_diagnosis"
}

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem(NavGraph.HOME_ROUTE, "Home", Icons.Default.Home)
    object AIChat : BottomNavItem(NavGraph.AI_CHAT_ROUTE, "AI Chat", Icons.Default.SmartToy)
    object Doctors : BottomNavItem(NavGraph.DOCTORS_ROUTE, "Doctors", Icons.Default.MedicalServices)
    object Pharmacy : BottomNavItem(NavGraph.PHARMACY_ROUTE, "Pharmacy", Icons.Default.LocalPharmacy)
    object LabTests : BottomNavItem(NavGraph.LAB_TESTS_ROUTE, "Lab Tests", Icons.Default.Science)
    object Insurance : BottomNavItem(NavGraph.INSURANCE_ROUTE, "Insurance", Icons.Default.Security)
    object MyHealth : BottomNavItem(NavGraph.MY_HEALTH_ROUTE, "My Health", Icons.Default.Favorite)
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    cartViewModel: CartViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavGraph.LOGIN_ROUTE) { LoginScreen(navController) }
        composable(NavGraph.SIGNUP_ROUTE) { SignupScreen(navController) }
        
        // ✅ Onboarding Screen Entry
        composable(NavGraph.ONBOARDING_ROUTE) {
            OnboardingScreen(navController)
        }

        composable(NavGraph.HOME_ROUTE) { HomeScreen(navController) }
        composable(NavGraph.DOCTORS_ROUTE) { DoctorsScreen() }
        composable(NavGraph.PHARMACY_ROUTE) { PharmacyScreen(navController, cartViewModel) }
        composable(NavGraph.LAB_TESTS_ROUTE) { LabTestScreen(cartViewModel) }
        composable(NavGraph.INSURANCE_ROUTE) { InsuranceScreen() }
        composable(NavGraph.MY_HEALTH_ROUTE) { MyHealthScreen() }
        composable(NavGraph.SCREENING_ROUTE) { ScreeningScreen(navController) }
        composable(NavGraph.NOTIFICATIONS_ROUTE) { NotificationScreen(navController) }
        composable(NavGraph.CART_ROUTE) { CartScreen(navController, cartViewModel) }
        composable(NavGraph.PROFILE_ROUTE) { ProfileScreen(navController) }
        composable(NavGraph.EDIT_PROFILE_ROUTE) { EditProfileScreen(navController) }

        composable(NavGraph.SYMPTOM_CHECKER_ROUTE) { SymptomCheckerScreen(navController) }

        composable(NavGraph.AI_CHAT_ROUTE) { AIHealthChatScreen(navController) }

        composable(
            route = NavGraph.SYMPTOM_RESULT_ROUTE,
            arguments = listOf(navArgument("symptoms") { type = NavType.StringType })
        ) { backStackEntry ->
            val symptoms = backStackEntry.arguments?.getString("symptoms")?.split(",") ?: emptyList()
            SymptomAnalysisResultScreen(navController, symptoms)
        }

        composable(NavGraph.MEDICINE_REMINDER_ROUTE) { MedicineReminderScreen(navController) }
        composable(NavGraph.CATEGORY_CANCER_ROUTE) { CategoryCancerScreen(navController) }
        composable(NavGraph.CATEGORY_HEART_ROUTE) { CategoryHeartScreen(navController) }
        composable(NavGraph.CATEGORY_METABOLIC_ROUTE) { CategoryMetabolicScreen(navController) }
        composable(NavGraph.CATEGORY_NEUROLOGICAL_ROUTE) { CategoryNeurologicalScreen(navController) }
        composable(NavGraph.ABOUT_ROUTE) { AboutScreen() }
        composable(NavGraph.CONTACT_ROUTE) { ContactScreen() }
        composable(NavGraph.CONDITIONS_ROUTE) { ConditionsScreen() }
        composable(NavGraph.BLOG_ROUTE) { BlogScreen() }
        composable(NavGraph.FIND_SPECIALIST_ROUTE) { FindASpecialistScreen() }
        composable(NavGraph.WHY_EARLY_DIAGNOSIS_ROUTE) { WhyEarlyDiagnosisScreen(navController) }
    }
}