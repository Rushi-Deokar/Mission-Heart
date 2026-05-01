package com.example.missionheart

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.missionheart.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmacyScreen(navController: NavController, cartViewModel: CartViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    // Calculate total medicine items in cart
    val cartCount = cartViewModel.cartItems.filter { it.type == CartItemType.MEDICINE }.sumOf { it.quantity }

    var showUploadSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var isUploading by remember { mutableStateOf(false) }
    var uploadSuccess by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            showUploadSheet = false
            isUploading = true
            scope.launch {
                delay(2000)
                isUploading = false
                uploadSuccess = true
                Toast.makeText(context, "Prescription Uploaded Successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            showUploadSheet = false
            isUploading = true
            scope.launch {
                delay(2000)
                isUploading = false
                uploadSuccess = true
                Toast.makeText(context, "Photo Uploaded Successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) takePictureLauncher.launch(null)
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) pickImageLauncher.launch("image/*")
    }

    val categories = listOf(
        Category("All", "All", Icons.Default.GridView, BrandBlue),
        Category("Diabetes", "Diabetes", Icons.Default.WaterDrop, Color(0xFFE57373)),
        Category("Heart", "Heart", Icons.Default.Favorite, Color(0xFFF06292)),
        Category("Stomach", "Stomach", Icons.Default.Spa, Color(0xFF81C784)),
        Category("Skin", "Skin", Icons.Default.Face, Color(0xFFFFB74D)),
        Category("Baby", "Baby", Icons.Default.ChildCare, Color(0xFF64B5F6))
    )

    val medicines = remember {
        listOf(
            Medicine("1", "Multivitamin A-Z", "HealthKart", "30 Tabs", 399.0, 499.0, "20% OFF", "All", "Essential vitamins for daily needs.", Icons.Default.Medication),
            Medicine("2", "Whey Protein", "MuscleBlaze", "1kg", 2499.0, 2999.0, "15% OFF", "Fitness", "High quality protein.", Icons.Default.FitnessCenter),
            Medicine("3", "Digene Syrup", "Abbott", "200ml", 120.0, 130.0, "5% OFF", "Stomach", "Antacid for quick relief.", Icons.Default.LocalDrink),
            Medicine("4", "Digital Thermometer", "Omron", "1 unit", 250.0, 300.0, "10% OFF", "All", "Accurate body temperature reading.", Icons.Default.Thermostat),
            Medicine("5", "Sugar Free Gold", "Zydus", "500 pellets", 180.0, 200.0, "10% OFF", "Diabetes", "Low calorie sweetener.", Icons.Default.InvertColors),
            Medicine("6", "OneTouch Glucometer", "LifeScan", "1 kit", 899.0, 1200.0, "25% OFF", "Diabetes", "Monitor blood glucose at home.", Icons.Default.MonitorHeart),
            Medicine("7", "BP Monitor B2", "Microlife", "1 unit", 1299.0, 1800.0, "30% OFF", "Heart", "Automated blood pressure monitor.", Icons.Default.FavoriteBorder),
            Medicine("8", "Cetaphil Face Wash", "Galderma", "125ml", 210.0, 250.0, "16% OFF", "Skin", "Gentle skin cleanser.", Icons.Default.Face)
        )
    }

    val filteredMedicines = medicines.filter { medicine ->
        val matchesSearch = medicine.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || medicine.category == selectedCategory
        matchesSearch && matchesCategory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pharmacy", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Deliver to Jalgaon 425002", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(onClick = { navController.navigate(NavGraph.CART_ROUTE) }) {
                            Icon(Icons.Outlined.ShoppingCart, contentDescription = "Cart", tint = TextPrimary)
                        }
                        if (cartCount > 0) {
                            AnimatedContent(
                                targetState = cartCount,
                                transitionSpec = {
                                    scaleIn(animationSpec = tween(300)) togetherWith scaleOut(animationSpec = tween(300))
                                }, label = "cart_animation"
                            ) { targetCount ->
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 4.dp, end = 4.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(ActionOrange),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(targetCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { paddingValues ->

        // Single LazyVerticalGrid to handle scrolling perfectly without nested scroll issues
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 1. Search Bar
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchBarMeds(searchQuery) { searchQuery = it }
            }

            // 2. Banner Section
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (uploadSuccess) {
                    SuccessBanner()
                } else {
                    UploadPrescriptionBanner(isUploading) { showUploadSheet = true }
                }
            }

            // 3. Category Carousel
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    PharmaSectionTitle("Shop by Category")
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(categories) { cat ->
                            CategoryItem(cat, selectedCategory == cat.id) { selectedCategory = cat.id }
                        }
                    }
                }
            }

            // 4. Section Title
            item(span = { GridItemSpan(maxLineSpan) }) {
                PharmaSectionTitle(if (selectedCategory == "All") "Popular Products" else "$selectedCategory Medicines")
            }

            // 5. Medicines Grid
            if (filteredMedicines.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No medicines found", color = TextSecondary)
                    }
                }
            } else {
                items(filteredMedicines) { med ->
                    MedicineCard(
                        medicine = med,
                        onCardClick = {
                            // TODO: Navigate to Product Detail Screen later
                            Toast.makeText(context, "Details for ${med.name}", Toast.LENGTH_SHORT).show()
                        },
                        onAddClick = {
                            cartViewModel.addMedicine(med)
                            Toast.makeText(context, "${med.name} added to cart", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // 6. Bottom Padding
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(30.dp)) }
        }

        // Bottom Sheet for Prescription Upload
        if (showUploadSheet) {
            ModalBottomSheet(
                onDismissRequest = { showUploadSheet = false },
                sheetState = sheetState,
                containerColor = SurfaceWhite
            ) {
                UploadPrescriptionContent(
                    onCameraClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) takePictureLauncher.launch(null)
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onGalleryClick = {
                        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
                        if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) pickImageLauncher.launch("image/*")
                        else galleryPermissionLauncher.launch(perm)
                    }
                )
            }
        }
    }
}

@Composable
fun MedicineCard(medicine: Medicine, onAddClick: () -> Unit, onCardClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .clickable { onCardClick() } // Main card click
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(12.dp)).background(InputFieldBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(medicine.icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(medicine.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(medicine.power, color = TextSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("₹${medicine.price.toInt()}", color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Text(medicine.discount, color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, BrandBlue, RoundedCornerShape(8.dp))
                .clickable { onAddClick() }, // Separate click for Add to Cart
            contentAlignment = Alignment.Center
        ) {
            Text("ADD TO CART", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun UploadPrescriptionContent(onCameraClick: () -> Unit, onGalleryClick: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp).padding(bottom = 20.dp)) {
        Text("Upload Prescription", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Please upload a valid prescription from your doctor.", color = TextSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
        PrescriptionStep("1", "Upload a photo of your prescription")
        PrescriptionStep("2", "Our pharmacist will call you to confirm")
        PrescriptionStep("3", "Medicines will be delivered to your door")
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onCameraClick, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = InputFieldBg), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Outlined.CameraAlt, null, tint = BrandBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Camera", color = TextPrimary)
            }
            Button(onClick = onGalleryClick, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = BrandBlue), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Outlined.PhotoLibrary, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gallery", color = Color.White)
            }
        }
    }
}

@Composable
fun PrescriptionStep(num: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Text(num, color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = TextPrimary, fontSize = 14.sp)
    }
}

@Composable
fun SearchBarMeds(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Search medicines, health products...", color = TextSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                }
            }
        },
        modifier = Modifier.fillMaxWidth().height(52.dp).border(1.dp, InputFieldBg, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = InputFieldBg,
            unfocusedContainerColor = InputFieldBg,
            focusedIndicatorColor = BrandBlue,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        singleLine = true
    )
}

@Composable
fun UploadPrescriptionBanner(isUploading: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(BrandBlue, Color(0xFF1976D2)))).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Order via Prescription", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Upload a photo of your Rx", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color.White), modifier = Modifier.height(36.dp), enabled = !isUploading) {
                if (isUploading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BrandBlue, strokeWidth = 2.dp)
                else Text("Upload Now", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Icon(Icons.Outlined.FileUpload, null, tint = Color.White, modifier = Modifier.size(48.dp))
    }
}

@Composable
fun SuccessBanner() {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SuccessGreen.copy(alpha = 0.1f)).border(1.dp, SuccessGreen, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Prescription Uploaded!", color = SuccessGreen, fontWeight = FontWeight.Bold)
            Text("We will verify and call you shortly.", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun CategoryItem(category: Category, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp).clickable { onClick() }) {
        Box(
            modifier = Modifier.size(60.dp).clip(CircleShape).background(if (isSelected) category.color else SurfaceWhite).border(if (isSelected) 0.dp else 1.dp, InputFieldBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(category.icon, null, tint = if (isSelected) Color.White else category.color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(category.name, fontSize = 12.sp, color = if (isSelected) category.color else TextSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun PharmaSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
}