package com.example.missionheart

import android.graphics.Bitmap

/**
 * Sprint 1: Consolidated Chat Models
 */
data class HealthChatMessage(
    val id: String = "",
    val text: String = "",
    val isUser: Boolean = false,
    val image: Bitmap? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String = "",
    val title: String = "",
    val timestamp: Long = 0
)
