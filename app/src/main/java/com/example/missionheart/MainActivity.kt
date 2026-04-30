package com.example.missionheart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.missionheart.ui.theme.*

class MainActivity : ComponentActivity() {

    private val cartViewModel: CartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MissionHeartTheme {
                MainApp(cartViewModel)
            }
        }
    }
}

@Composable
fun MainApp(cartViewModel: CartViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Doctors,
        BottomNavItem.Pharmacy,
        BottomNavItem.LabTests
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    // Auto-Login logic
    val startRoute = if (authManager.isLoggedIn()) NavGraph.HOME_ROUTE else NavGraph.LOGIN_ROUTE

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                ProfessionalBottomBar(
                    navController = navController,
                    items = bottomNavItems.map { TabItem(it.route, it.label, it.icon, it.icon) }
                )
            }
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(navController = navController, startDestination = startRoute) {
                // Auth Routes
                composable(NavGraph.LOGIN_ROUTE) { LoginScreen(navController) }
                composable(NavGraph.SIGNUP_ROUTE) { SignupScreen(navController) }

                // Main Bottom Nav Screens
                composable(NavGraph.HOME_ROUTE) { HomeScreen(navController) }
                composable(NavGraph.DOCTORS_ROUTE) { DoctorsScreen() }
                composable(NavGraph.PHARMACY_ROUTE) { PharmacyScreen(navController, cartViewModel) }
                composable(NavGraph.LAB_TESTS_ROUTE) { LabTestScreen(cartViewModel) }

                // All Feature Routes
                composable(NavGraph.SCREENING_ROUTE) { ScreeningScreen(navController) }
                composable(NavGraph.NOTIFICATIONS_ROUTE) { NotificationScreen(navController) }
                composable(NavGraph.CART_ROUTE) { CartScreen(navController, cartViewModel) }
                composable(NavGraph.PROFILE_ROUTE) { ProfileScreen(navController) }
                
                // Symptom Checker Routes
                composable(NavGraph.SYMPTOM_CHECKER_ROUTE) { SymptomCheckerScreen(navController) }
                composable(
                    route = NavGraph.SYMPTOM_RESULT_ROUTE,
                    arguments = listOf(navArgument("symptoms") { type = NavType.StringType })
                ) { backStackEntry ->
                    val symptoms = backStackEntry.arguments?.getString("symptoms")?.split(",") ?: emptyList()
                    SymptomAnalysisResultScreen(navController, symptoms)
                }

                composable(NavGraph.MEDICINE_REMINDER_ROUTE) { MedicineReminderScreen(navController) }

                // Categories
                composable(NavGraph.CATEGORY_CANCER_ROUTE) { CategoryCancerScreen(navController) }
                composable(NavGraph.CATEGORY_HEART_ROUTE) { CategoryHeartScreen(navController) }
                composable(NavGraph.CATEGORY_METABOLIC_ROUTE) { CategoryMetabolicScreen(navController) }
                composable(NavGraph.CATEGORY_NEUROLOGICAL_ROUTE) { CategoryNeurologicalScreen(navController) }

                // Support Screens
                composable(NavGraph.ABOUT_ROUTE) { AboutScreen() }
                composable(NavGraph.CONTACT_ROUTE) { ContactScreen() }
                composable(NavGraph.CONDITIONS_ROUTE) { ConditionsScreen() }
                composable(NavGraph.BLOG_ROUTE) { BlogScreen() }
                composable(NavGraph.FIND_SPECIALIST_ROUTE) { FindASpecialistScreen() }
                composable(NavGraph.WHY_EARLY_DIAGNOSIS_ROUTE) { WhyEarlyDiagnosisScreen(navController) }
            }
        }
    }
}

@Composable
fun ProfessionalBottomBar(navController: NavController, items: List<TabItem>) {
    NavigationBar(
        containerColor = SurfaceWhite,
        tonalElevation = 0.dp,
        modifier = Modifier.height(76.dp)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                alwaysShowLabel = true,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Box(
                        modifier = Modifier
                            .height(22.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            letterSpacing = 0.sp,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandBlue,
                    selectedTextColor = BrandBlue,
                    indicatorColor = BrandBlue.copy(alpha = 0.12f),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}

data class TabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
