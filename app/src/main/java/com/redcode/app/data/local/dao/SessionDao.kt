package com.redcode.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.redcode.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("UPDATE sessions SET isPinned = :isPinned WHERE id = :sessionId")
    suspend fun updatePinned(sessionId: String, isPinned: Boolean)

    @Query("UPDATE sessions SET title = :newTitle WHERE id = :sessionId")
    suspend fun updateTitle(sessionId: String, newTitle: String)

    @Query("UPDATE sessions SET isUnread = :isUnread WHERE id = :sessionId")
    suspend fun updateUnread(sessionId: String, isUnread: Boolean)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM sessions")
    suspend fun clearAll()
}
