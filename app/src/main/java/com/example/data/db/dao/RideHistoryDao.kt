package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entity.RideHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RideHistoryDao {

    @Query("SELECT * FROM ride_history ORDER BY detectedAt DESC")
    fun getAllHistory(): Flow<List<RideHistoryEntity>>

    @Query("SELECT * FROM ride_history ORDER BY detectedAt DESC")
    suspend fun getAllHistoryList(): List<RideHistoryEntity>

    @Query("SELECT * FROM ride_history ORDER BY detectedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<RideHistoryEntity>>

    @Query("SELECT * FROM ride_history ORDER BY detectedAt DESC LIMIT 1")
    fun getLatestRide(): Flow<RideHistoryEntity?>

    @Query("SELECT * FROM ride_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RideHistoryEntity?

    @Query("SELECT * FROM ride_history WHERE bookingFingerprint = :fingerprint LIMIT 1")
    suspend fun getByFingerprint(fingerprint: String): RideHistoryEntity?

    @Query("SELECT * FROM ride_history WHERE bookingId = :bookingId AND bookingId != '' LIMIT 1")
    suspend fun getByBookingId(bookingId: String): RideHistoryEntity?

    @Query("SELECT * FROM ride_history WHERE platform = :platform AND status = 'PROCESSING' ORDER BY detectedAt DESC LIMIT 1")
    suspend fun getActiveProcessingRide(platform: String): RideHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RideHistoryEntity)

    @Update
    suspend fun update(entity: RideHistoryEntity)

    @Query("DELETE FROM ride_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM ride_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM ride_history WHERE status = 'ACCEPTED'")
    fun getAcceptedCount(): Flow<Int>
}
