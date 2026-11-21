package com.postscheduler.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Cached per-post performance, synced from the backend / Facebook Insights. */
@Entity(tableName = "insights")
data class InsightEntity(
    @PrimaryKey val postId: String,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    val views: Int = 0,
    /** Approx earnings in USD for this post, when monetization data is available. */
    val earningsUsd: Double? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val engagement: Int get() = likes + comments + shares
}

@Dao
interface InsightDao {
    @Query("SELECT * FROM insights ORDER BY earningsUsd DESC")
    fun all(): Flow<List<InsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<InsightEntity>)
}
