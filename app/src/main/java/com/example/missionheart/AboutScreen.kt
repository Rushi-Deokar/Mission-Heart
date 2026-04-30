package com.example.missionheart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.missionheart.ui.theme.*

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground) // Fixed: Used AppBackground (mapped to DarkBackground in Color.kt)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // App Logo / Icon Placeholder
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(BrandBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "App Logo",
                tint = BrandBlue,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Mission Heart",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary // Fixed: Used TextPrimary (mapped to LightText)
        )

        Text(
            text = "Version 1.0.0",
            fontSize = 14.sp,
            color = TextSecondary // Fixed: Used TextSecondary (mapped to DimText)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // About Description
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite), // Fixed: Used SurfaceWhite (mapped to DarkSurface)
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "About Us",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary // Fixed
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mission Heart is a comprehensive healthcare application designed to simplify your medical needs. From booking doctor appointments to ordering medicines and booking lab tests, we bring healthcare to your fingertips.",
                    fontSize = 14.sp,
                    color = TextPrimary, // Fixed
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mission Statement
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite), // Fixed
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Our Mission",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary // Fixed
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To provide accessible, affordable, and high-quality healthcare services to everyone, everywhere. We believe in a future where healthcare is simple, transparent, and patient-centric.",
                    fontSize = 14.sp,
                    color = TextPrimary, // Fixed
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Footer
        Text(
            text = "© 2025 Mission Heart. All rights reserved.",
            fontSize = 12.sp,
            color = TextSecondary, // Fixed
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}