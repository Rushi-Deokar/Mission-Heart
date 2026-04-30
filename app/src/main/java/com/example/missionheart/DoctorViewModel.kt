package com.example.missionheart

import android.location.Location
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Real model jisme Coordinates (Lat/Lng) aur Live Distance hai
data class RealDoctor(
    val id: String,
    val name: String,
    val specialty: String,
    val rating: Double,
    val isVerified: Boolean,
    val latitude: Double,
    val longitude: Double,
    var distanceInKm: Double = 0.0
)

class DoctorViewModel : ViewModel() {

    // Mock Database - Different locations across India
    // Real app mein ye data Firebase ya kisi backend server se aayega
    private val allDoctors = listOf(
        RealDoctor("1", "Dr. Aditi", "Heart", 4.9, true, 21.0100, 75.5600), // Jalgaon Center
        RealDoctor("2", "Dr. Raj", "Neuro", 4.8, true, 21.0500, 75.5800), // Jalgaon Outskirts
        RealDoctor("3", "Dr. Neha", "Skin", 4.7, true, 20.9900, 75.5500), // Jalgaon City
        RealDoctor("4", "Dr. Prem", "Dentist", 5.0, true, 18.5204, 73.8567), // Pune
        RealDoctor("5", "Dr. Vikram", "Ortho", 4.5, true, 19.0760, 72.8777), // Mumbai
        RealDoctor("6", "Dr. Snehal", "Pediatric", 4.6, true, 21.1458, 79.0882), // Nagpur
        RealDoctor("7", "Dr. Kabir", "General", 4.4, false, 28.7041, 77.1025) // Delhi
    )

    private val _nearestDoctors = MutableStateFlow<List<RealDoctor>>(allDoctors)
    val nearestDoctors: StateFlow<List<RealDoctor>> = _nearestDoctors.asStateFlow()

    // Location milne par list ko distance ke hisab se sort karega
    fun fetchNearestDoctors(userLat: Double, userLng: Double) {
        val sortedList = allDoctors.map { doctor ->
            val results = FloatArray(1)
            // Ye Android ka in-built function hai jo 2 coordinates ke beech exact distance meter me nikalta hai
            Location.distanceBetween(userLat, userLng, doctor.latitude, doctor.longitude, results)

            // Meter ko Kilometer me convert kar rahe hain
            val distanceKm = (results[0] / 1000).toDouble()
            doctor.copy(distanceInKm = distanceKm)
        }.sortedBy { it.distanceInKm } // Sabse paas wala doctor list me sabse upar aayega

        _nearestDoctors.value = sortedList
    }
}