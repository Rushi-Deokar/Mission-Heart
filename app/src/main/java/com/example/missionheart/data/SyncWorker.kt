package com.example.missionheart.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Sprint 2: Background Sync Worker.
 * Pushes pending local changes to Firebase when internet is back.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = MissionHeartDatabase.getDatabase(applicationContext)
        val medicineDao = database.medicineDao()
        val userId = inputData.getString("userId") ?: return Result.failure()

        val firebaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/medicines")

        return try {
            // Get all medicines with pending sync
            val pendingMedicines = medicineDao.getAllMedicines().first().filter { it.isSyncPending }

            pendingMedicines.forEach { med ->
                firebaseRef.child(med.id).setValue(med).await()
                medicineDao.updateMedicine(med.copy(isSyncPending = false))
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
