package com.redcode.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val project: String,
    val snippet: String,
    val timeAgo: String,
    val isUnread: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
