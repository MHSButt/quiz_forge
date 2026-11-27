package com.hassan.quizforge.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// A slightly rounder shape scale than Material 3's default, used consistently for every card,
// button, and text field across the app so corners feel like a deliberate design choice rather
// than whatever each component's default happens to be.
val QuizForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
