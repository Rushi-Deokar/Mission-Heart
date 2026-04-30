package com.example.missionheart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.missionheart.ui.theme.*

@Composable
fun ContactScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground) // Fixed Color
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Contact Us",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary, // Fixed Color
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "We are here to help you. Reach out to us via any of the following channels.",
            fontSize = 14.sp,
            color = TextSecondary, // Fixed Color
            modifier = Modifier.padding(bottom = 24.dp)
        )

        ContactItem(
            icon = Icons.Default.Phone,
            title = "Customer Support",
            detail = "+91 98765 43210"
        )

        ContactItem(
            icon = Icons.Default.Email,
            title = "Email Us",
            detail = "support@missionheart.com"
        )

        ContactItem(
            icon = Icons.Default.LocationOn,
            title = "Head Office",
            detail = "123, Health Tech Park, Jalgaon, Maharashtra - 425001"
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { /* Handle Chat Logic */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue), // Fixed Color
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Chat with Support", color = SurfaceWhite) // Fixed Color
        }
    }
}

@Composable
fun ContactItem(icon: ImageVector, title: String, detail: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite), // Fixed Color
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandBlue, // Fixed Color
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary // Fixed Color
                )
                Text(
                    text = detail,
                    fontSize = 14.sp,
                    color = TextSecondary // Fixed Color
                )
            }
        }
    }
}