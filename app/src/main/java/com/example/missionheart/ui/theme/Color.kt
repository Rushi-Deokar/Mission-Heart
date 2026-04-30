package com.example.missionheart.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// BRAND COLORS (Consistent across themes)
// ============================================
val BrandBlue = Color(0xFF3E8BFF)
val BrandTeal = Color(0xFF00E5FF)
val BrandCoral = Color(0xFFFF7043)
val BrandRed = Color(0xFFFF5252)

// ============================================
// DARK THEME PALETTE (Primary - Premium Feel)
// ============================================
val DarkBackground = Color(0xFF101216)
val DarkSurface = Color(0xFF1E232C)          // Slightly bluish tint for depth
val DarkSurfaceElevated = Color(0xFF252B36)  // Cards, dialogs
val DarkInput = Color(0xFF2A3140)

val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFF9DA3AE)
val DarkTextDisabled = Color(0xFF5A6070)

// ============================================
// LIGHT THEME PALETTE (Secondary)
// ============================================
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF1F3F4)
val LightInput = Color(0xFFE8EAED)

val LightTextPrimary = Color(0xFF1A1D21)
val LightTextSecondary = Color(0xFF5F6368)
val LightTextDisabled = Color(0xFF9AA0A6)

// ============================================
// STATUS COLORS (Universal)
// ============================================
val SuccessGreen = Color(0xFF00C853)
val WarningYellow = Color(0xFFFFD700)
val InfoBlue = Color(0xFF448AFF)

// ============================================
// LEGACY MAPPINGS (For backward compatibility)
// ============================================
val AppBackground = DarkBackground
val CardSurface = DarkSurface
val SurfaceWhite = DarkSurface
val InputFieldBg = DarkInput
val InputSurface = DarkInput
val TextPrimary = DarkTextPrimary
val TextSecondary = DarkTextSecondary
val TextHint = DarkTextSecondary
val ActionOrange = BrandCoral
val StatusGreen = SuccessGreen
val ErrorRed = BrandRed
val Primary = BrandBlue
val Secondary = BrandTeal
val White = Color.White
val Black = Color.Black
val LightText = DarkTextPrimary
val DimText = DarkTextSecondary