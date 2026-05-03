package com.example.missionheart.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline entity for Medicine Reminders
 */
@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val time: String,
    val hour: Int,
    val minute: Int,
    val totalStock: Int,
    val currentStock: Int,
    val isSyncPending: Boolean = false // Flag for offline-first sync
)
