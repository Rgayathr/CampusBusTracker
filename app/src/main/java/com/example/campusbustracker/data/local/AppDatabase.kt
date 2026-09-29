package com.example.campusbustracker.data.local

import android.content.Context
import androidx.room.*

@Entity(tableName = "pending_scans")
data class PendingScan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val busCode: String,
    val checkpointValue: String,
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

@Dao
interface PendingScanDao {
    @Insert
    suspend fun insert(scan: PendingScan)

    @Query("SELECT * FROM pending_scans WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsynced(): List<PendingScan>

    @Query("UPDATE pending_scans SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Int)

    @Query("SELECT COUNT(*) FROM pending_scans WHERE synced = 0")
    suspend fun unsyncedCount(): Int

    @Query("SELECT COUNT(*) FROM pending_scans")
    suspend fun totalScans(): Int
}

@Database(entities = [PendingScan::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingScanDao(): PendingScanDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campus_bus_tracker_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}