package com.example.missionheart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.missionheart.ui.theme.*

@Composable
fun ConditionsScreen() {
    val conditions = listOf(
        "Diabetes Management", "Hypertension", "Thyroid Disorder",
        "PCOS / PCOD", "Heart Disease", "Arthritis", "Asthma"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground) // Fixed Color
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite) // Fixed Color
                .padding(16.dp)
        ) {
            Text(
                text = "Health Conditions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary // Fixed Color
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(conditions) { condition ->
                ConditionItem(condition)
            }
        }
    }
}

@Composable
fun ConditionItem(name: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate */ },
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite), // Fixed Color
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary // Fixed Color
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary // Fixed Color
            )
        }
    }
}