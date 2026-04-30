package com.example.missionheart

import android.content.Context
import android.content.SharedPreferences

class AuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mission_heart_auth", Context.MODE_PRIVATE)

    fun registerUser(name: String, email: String, pass: String) {
        prefs.edit().apply {
            putString("name", name)
            putString("email", email)
            putString("pass", pass)
            putBoolean("is_logged_in", true)
            apply()
        }
    }

    fun loginUser(email: String, pass: String): Boolean {
        val savedEmail = prefs.getString("email", null)
        val savedPass = prefs.getString("pass", null)

        return if (email == savedEmail && pass == savedPass) {
            prefs.edit().putBoolean("is_logged_in", true).apply()
            true
        } else {
            false
        }
    }

    fun logout() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun getUserName(): String {
        return prefs.getString("name", "User") ?: "User"
    }
}
