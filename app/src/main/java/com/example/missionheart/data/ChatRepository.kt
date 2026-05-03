package com.example.missionheart.data

import kotlinx.coroutines.flow.Flow

/**
 * Single Source of Truth for Chat data.
 */
class ChatRepository(
    private val chatDao: ChatDao
) {
    // --- Observables ---
    fun getAllSessions(): Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()
    
    fun getMessages(sessionId: String): Flow<List<ChatMessageEntity>> = 
        chatDao.getMessagesForSession(sessionId)

    // --- Actions ---
    suspend fun createNewSession(session: ChatSessionEntity) {
        chatDao.insertSession(session)
    }

    suspend fun saveMessage(message: ChatMessageEntity) {
        chatDao.insertMessage(message)
    }

    suspend fun deleteSession(session: ChatSessionEntity) {
        chatDao.deleteMessagesForSession(session.id)
        chatDao.deleteSession(session)
    }
}
