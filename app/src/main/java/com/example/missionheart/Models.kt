package com.example.missionheart

import androidx.compose.ui.graphics.vector.ImageVector

// --- Doctor Models ---
data class Doctor(
    val id: String,
    val name: String,
    val specialty: String,
    val category: String,
    val experience: String,
    val rating: Double,
    val reviews: Int,
    val fee: Int,
    val about: String,
    val isAvailable: Boolean = true,
    val location: String = "Jalgaon"
)

// --- Pharmacy Models ---
data class Medicine(
    val id: String,
    val name: String,
    val manufacturer: String,
    val power: String, // e.g., 500mg
    val price: Double,
    val mrp: Double,
    val discount: String,
    val category: String,
    val description: String,
    val icon: ImageVector
)

// --- Lab Test Models ---
data class LabTest(
    val id: String,
    val testName: String,
    val description: String,
    val price: Double,
    val mrp: Double,
    val discount: String,
    val parametersIncluded: Int,
    val isHomeSample: Boolean = true,
    val category: String
)

// --- Common UI Models ---
data class Category(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)
