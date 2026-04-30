package com.example.missionheart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.missionheart.ui.theme.*

// Data Model for Blog
data class BlogPost(
    val id: Int,
    val title: String,
    val description: String,
    val date: String,
    val category: String
)

@Composable
fun BlogScreen() {
    // Mock Data
    val blogPosts = listOf(
        BlogPost(
            1,
            "10 Tips for a Healthy Heart",
            "Learn how small lifestyle changes can have a big impact on your heart health. From diet to exercise, we cover it all.",
            "Dec 2, 2025",
            "Heart Health"
        ),
        BlogPost(
            2,
            "Understanding Diabetes: A Guide",
            "Diabetes affects millions. Understand the symptoms, causes, and how to manage it effectively with our comprehensive guide.",
            "Nov 28, 2025",
            "Chronic Care"
        ),
        BlogPost(
            3,
            "The Importance of Mental Wellness",
            "Mental health is just as important as physical health. Discover ways to destress and maintain a healthy mind.",
            "Nov 20, 2025",
            "Wellness"
        ),
        BlogPost(
            4,
            "Nutrition Myths Debunked",
            "Are carbs really bad for you? Do you need supplements? We debunk common nutrition myths backed by science.",
            "Nov 15, 2025",
            "Nutrition"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground) // Fixed Color
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite) // Fixed Color
                .padding(16.dp)
        ) {
            Text(
                text = "Health Blog",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary // Fixed Color
            )
        }

        // Blog List
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(blogPosts) { post ->
                BlogCard(post)
            }
        }
    }
}

@Composable
fun BlogCard(post: BlogPost) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite), // Fixed Color
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to Detail */ }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category Chip
            Surface(
                color = BrandBlue.copy(alpha = 0.1f), // Fixed Color
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = post.category,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandBlue // Fixed Color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = post.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary // Fixed Color
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = post.description,
                fontSize = 14.sp,
                color = TextSecondary, // Fixed Color
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer (Date & Read More)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = TextHint, // Fixed Color
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.date,
                        fontSize = 12.sp,
                        color = TextHint // Fixed Color
                    )
                }

                Text(
                    text = "Read More",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue // Fixed Color
                )
            }
        }
    }
}