package com.example.missionheart.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Offline entity for Chat Sessions
 */
@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val timestamp: Long
)

/**
 * Offline entity for Individual Chat Messages
 */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val id: String = "", // Remote ID from Firestore if synced
    val sessionId: String,
    val text: String,
    val isUser: Boolean,
    val imageUri: String? = null,
    val timestamp: Long,
    val isSyncPending: Boolean = false
)
