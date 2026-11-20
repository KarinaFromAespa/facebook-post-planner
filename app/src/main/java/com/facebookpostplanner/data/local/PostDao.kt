package com.facebookpostplanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE status != 'DRAFT' ORDER BY scheduledAtMillis ASC, createdAtMillis ASC")
    fun queue(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE status = 'DRAFT' ORDER BY updatedAtMillis DESC")
    fun drafts(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE id = :id LIMIT 1")
    suspend fun get(id: String): Post?

    @Query("SELECT * FROM posts WHERE status = 'PUBLISHED' ORDER BY scheduledAtMillis DESC")
    fun published(): Flow<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(post: Post)

    @Update
    suspend fun update(post: Post)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun delete(id: String)

    /** Removes a stale local row for a backend post when the same post is
     * also stored under its backend id — i.e. the row created locally with
     * a random UUID before the backend assigned an id. Without this, one
     * scheduled post shows up twice in the queue. */
    @Query("DELETE FROM posts WHERE backendId = :backendId AND id != :backendId")
    suspend fun deleteDuplicatesForBackendId(backendId: String)
}
