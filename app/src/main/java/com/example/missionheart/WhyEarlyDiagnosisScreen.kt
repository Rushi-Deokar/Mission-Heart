package com.example.missionheart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.missionheart.ui.theme.*

data class Benefit(
    val icon: ImageVector,
    val title: String,
    val description: String
)

val benefits = listOf(
    Benefit(Icons.Default.BarChart, "Better Outcomes", "Many conditions are far more treatable—and often curable—when detected in their earliest stages."),
    Benefit(Icons.Default.Healing, "Less Invasive Treatment", "Early-stage treatments are often less invasive, involve fewer side effects, and have shorter recovery times."),
    Benefit(Icons.Default.Savings, "Lower Health Costs", "Proactive screening and early treatment can significantly reduce long-term healthcare expenses."),
    Benefit(Icons.Default.SentimentSatisfied, "Peace of Mind", "Understanding your health and taking control provides comfort and reduces anxiety about the unknown.")
)

data class Testimonial(
    val quote: String,
    val author: String
)

val testimonials = listOf(
    Testimonial("\"The symptom checker gave me the confidence to finally book an appointment. My doctor caught my pre-diabetes early, and I've been able to manage it with lifestyle changes.\"", "- J.S. (Anonymous User)"),
    Testimonial("\"I was scared to look up my symptoms, but the articles here were so calm and clear. It helped me understand what to ask my doctor. Thank you for this resource.\"", "- M.T. (Anonymous User)")
)

@Composable
fun WhyEarlyDiagnosisScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondary)
                .padding(vertical = 48.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Why Early Diagnosis Matters",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Catching health issues early can make a significant difference in treatment, outcomes, and peace of mind.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Benefits Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 48.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "The Benefits of Acting Early",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(benefits) { benefit ->
                    BenefitCard(benefit = benefit)
                }
            }
        }

        // Testimonials Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(vertical = 48.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "From Our Community",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                testimonials.forEach { testimonial ->
                    TestimonialCard(testimonial = testimonial)
                }
            }
        }
    }
}

@Composable
fun BenefitCard(benefit: Benefit) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.medium)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        M3Icon(
            imageVector = benefit.icon,
            contentDescription = benefit.title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp).padding(bottom = 8.dp)
        )
        Text(
            text = benefit.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = benefit.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TestimonialCard(testimonial: Testimonial) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), shape = MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Text(
            text = testimonial.quote,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = testimonial.author,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WhyEarlyDiagnosisScreenPreview() {
    MissionHeartTheme {
        WhyEarlyDiagnosisScreen(rememberNavController())
    }
}
