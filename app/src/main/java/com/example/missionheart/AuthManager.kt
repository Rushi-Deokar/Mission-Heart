package com.example.missionheart

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

class AuthManager(context: Context) {
    private val auth = FirebaseAuth.getInstance()

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun logout() {
        auth.signOut()
    }

    fun getUserName(): String {
        return auth.currentUser?.displayName ?: "User"
    }
    
    fun getUserEmail(): String {
        return auth.currentUser?.email ?: ""
    }
}
