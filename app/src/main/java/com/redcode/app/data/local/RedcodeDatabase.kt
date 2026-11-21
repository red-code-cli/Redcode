package com.redcode.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.redcode.app.data.local.dao.MessageDao
import com.redcode.app.data.local.dao.SessionDao
import com.redcode.app.data.local.entity.MessageEntity
import com.redcode.app.data.local.entity.SessionEntity

@Database(
    entities = [SessionEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RedcodeDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
}
