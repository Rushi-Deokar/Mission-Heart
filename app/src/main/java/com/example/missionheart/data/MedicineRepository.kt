package com.example.missionheart.data

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Single Source of Truth for Medicine data.
 */
class MedicineRepository(
    private val medicineDao: MedicineDao,
    private val userId: String
) {
    private val firebaseRef: DatabaseReference = 
        FirebaseDatabase.getInstance().getReference("users/$userId/medicines")

    // --- Local Source of Truth (UI observes this) ---
    val allMedicines: Flow<List<MedicineEntity>> = medicineDao.getAllMedicines()

    // --- Actions ---

    suspend fun addMedicine(med: MedicineEntity) {
        // 1. Save locally with sync flag
        medicineDao.insertMedicine(med.copy(isSyncPending = true))
        
        // 2. Try to sync with Firebase
        try {
            firebaseRef.child(med.id).setValue(med).await()
            // 3. Mark as synced
            medicineDao.insertMedicine(med.copy(isSyncPending = false))
        } catch (e: Exception) {
            // Stay as syncPending, background sync will handle it
        }
    }

    suspend fun updateMedicine(med: MedicineEntity) {
        medicineDao.updateMedicine(med.copy(isSyncPending = true))
        try {
            firebaseRef.child(med.id).setValue(med).await()
            medicineDao.updateMedicine(med.copy(isSyncPending = false))
        } catch (e: Exception) {}
    }

    suspend fun deleteMedicine(med: MedicineEntity) {
        medicineDao.deleteMedicine(med)
        try {
            firebaseRef.child(med.id).removeValue().await()
        } catch (e: Exception) {}
    }
}
