package com.example.campusbustracker.data.local

import android.content.Context
import com.example.campusbustracker.network.ApiClient
import com.example.campusbustracker.network.ScanRequest

object OfflineQueue {

    // Call this when a scan fails due to no internet
    suspend fun saveForLater(context: Context, busCode: String, checkpointValue: String) {
        val db = AppDatabase.getInstance(context)
        db.pendingScanDao().insert(PendingScan(busCode = busCode, checkpointValue = checkpointValue))
    }

    // Call this when internet comes back (e.g. app resume, or a "Sync Now" button)
    suspend fun trySyncAll(context: Context): Int {
        val db = AppDatabase.getInstance(context)
        val pending = db.pendingScanDao().getUnsynced()
        var syncedCount = 0

        for (scan in pending) {
            try {
                val response = ApiClient.api.submitScan(ScanRequest(scan.busCode, scan.checkpointValue))
                if (response.isSuccessful) {
                    db.pendingScanDao().markSynced(scan.id)
                    syncedCount++
                }
            } catch (e: Exception) {
                // Still no internet, stop trying, will retry next time
                break
            }
        }
        return syncedCount
    }

    suspend fun pendingCount(context: Context): Int {
        return AppDatabase.getInstance(context).pendingScanDao().unsyncedCount()
    }
}