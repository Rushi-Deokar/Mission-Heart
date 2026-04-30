package com.example.missionheart

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.missionheart.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabTestScreen(cartViewModel: CartViewModel) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Popular") }

    val categories = listOf("Popular", "Full Body", "Diabetes", "Heart", "Vitamin")

    val labTests = remember {
        listOf(
            LabTest("1", "Full Body Checkup", "Includes 80+ parameters to assess your overall health.", 699.0, 1999.0, "65% OFF", 82, true, "Full Body"),
            LabTest("2", "Diabetes Care Mini", "Basic check for blood sugar and HbA1c levels.", 399.0, 800.0, "50% OFF", 3, true, "Diabetes"),
            LabTest("3", "Healthy Heart Package", "Comprehensive heart health evaluation including Lipid profile.", 1299.0, 2500.0, "48% OFF", 12, true, "Heart"),
            LabTest("4", "Vitamin Deficiency Profile", "Checks Vitamin D and B12 levels.", 899.0, 1500.0, "40% OFF", 2, true, "Vitamin"),
            LabTest("5", "CBC (Complete Blood Count)", "Basic blood test to detect a variety of disorders.", 299.0, 500.0, "40% OFF", 24, true, "Popular"),
            LabTest("6", "Thyroid Profile (T3, T4, TSH)", "To check the functioning of the thyroid gland.", 450.0, 900.0, "50% OFF", 3, true, "Popular")
        )
    }

    val filteredTests = labTests.filter {
        (selectedCategory == "Popular" || it.category == selectedCategory) &&
                it.testName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lab Tests", fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search tests (e.g. CBC, Thyroid)", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite,
                    focusedIndicatorColor = BrandBlue,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Categories
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBlue,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceWhite,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                            enabled = true,
                            selected = selectedCategory == category
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredTests) { test ->
                    LabTestCard(test) {
                        cartViewModel.addLabTest(test)
                        Toast.makeText(context, "${test.testName} added to cart", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@Composable
fun LabTestCard(test: LabTest, onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        test.testName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        test.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (test.isHomeSample) {
                    Surface(
                        color = BrandTeal.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Home, null, tint = BrandTeal, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Home", color = BrandTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.List, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${test.parametersIncluded} Parameters included", fontSize = 12.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "₹${test.price.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "₹${test.mrp.toInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    Text(test.discount, color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Add to Cart", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
