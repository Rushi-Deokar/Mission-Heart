package com.example.missionheart

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Sprint 1: Professional AI Chat ViewModel with Streaming & Multi-session logic
 */
class AIChatViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val userId = auth.currentUser?.uid ?: "anonymous"
    private val userName = auth.currentUser?.displayName?.split(" ")?.firstOrNull() ?: "User"

    var currentSessionId by mutableStateOf(UUID.randomUUID().toString())
        private set

    val messages = mutableStateListOf<HealthChatMessage>()
    val chatSessions = mutableStateListOf<ChatSession>()
    
    var isTyping by mutableStateOf(false)
    var selectedImage by mutableStateOf<Bitmap?>(null)

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = BuildConfig.GEMINI_API_KEY,
        requestOptions = RequestOptions(apiVersion = "v1beta")
    )

    init {
        loadSessions()
    }

    private fun loadSessions() {
        db.collection("users").document(userId).collection("chat_sessions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    chatSessions.clear()
                    for (doc in it.documents) {
                        chatSessions.add(ChatSession(
                            id = doc.id,
                            title = doc.getString("title") ?: "Untitled Chat",
                            timestamp = doc.getLong("timestamp") ?: 0
                        ))
                    }
                }
            }
    }

    fun startNewSession(existingId: String?) {
        currentSessionId = existingId ?: UUID.randomUUID().toString()
        loadMessages()
    }

    private fun loadMessages() {
        db.collection("users").document(userId).collection("chat_sessions")
            .document(currentSessionId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                messages.clear()
                if (snapshot.isEmpty) {
                    messages.add(HealthChatMessage(text = "Namaste $userName! I'm your MissionHeart Vision AI. How can I help you today?", isUser = false))
                } else {
                    for (doc in snapshot.documents) {
                        messages.add(HealthChatMessage(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            isUser = doc.getBoolean("isUser") ?: false,
                            timestamp = doc.getLong("timestamp") ?: 0
                        ))
                    }
                }
            }
    }

    fun sendMessage(text: String, image: Bitmap?) {
        val userMsgImg = image?.let { ImageUtils.compressBitmap(it) }
        val userMsg = HealthChatMessage(text = text, isUser = true, image = userMsgImg)
        
        messages.add(userMsg)
        saveMessageToDb(userMsg)
        
        selectedImage = null
        isTyping = true

        viewModelScope.launch {
            try {
                val inputContent = content {
                    userMsgImg?.let { image(it) }
                    text("User: $userName\nContext: Medical Assistant\nQuestion: $text")
                }

                var fullResponse = ""
                generativeModel.generateContentStream(inputContent).collect { chunk ->
                    fullResponse += chunk.text ?: ""
                    
                    if (messages.isNotEmpty() && !messages.last().isUser) {
                        val last = messages.removeAt(messages.size - 1)
                        messages.add(last.copy(text = fullResponse))
                    } else {
                        messages.add(HealthChatMessage(text = fullResponse, isUser = false))
                    }
                }
                
                if (messages.isNotEmpty()) {
                    saveMessageToDb(messages.last())
                }
                
            } catch (e: Exception) {
                Log.e("AIChatVM", "Error", e)
                messages.add(HealthChatMessage(text = "Analysis Error: ${e.localizedMessage}", isUser = false))
            } finally {
                isTyping = false
            }
        }
    }

    private fun saveMessageToDb(message: HealthChatMessage) {
        val sessionRef = db.collection("users").document(userId).collection("chat_sessions").document(currentSessionId)
        
        if (messages.size <= 2) {
            sessionRef.set(hashMapOf(
                "title" to if (message.isUser) message.text.take(30) + "..." else "New Health Consultation",
                "timestamp" to System.currentTimeMillis()
            ))
        }

        val messageData = hashMapOf(
            "text" to message.text,
            "isUser" to message.isUser,
            "timestamp" to message.timestamp
        )
        
        sessionRef.collection("messages").add(messageData)
    }
}
