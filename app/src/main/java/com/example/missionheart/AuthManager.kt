package com.example.missionheart

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

class AuthManager(context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences("mission_heart_prefs", Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }

    fun logout() {
        auth.signOut()
        // Logout par onboarding reset nahi karni chahiye, 
        // lekin agar aap chahte hain naya user phir se onboarding dekhe toh yahan reset kar sakte hain.
    }

    fun getUserName(): String {
        return auth.currentUser?.displayName ?: "User"
    }
}
