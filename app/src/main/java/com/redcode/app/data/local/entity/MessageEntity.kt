package com.redcode.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val toolsJson: String = "",
    val sourcesJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
