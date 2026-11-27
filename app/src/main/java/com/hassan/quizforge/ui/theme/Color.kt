package com.hassan.quizforge.ui.theme

import androidx.compose.ui.graphics.Color

// QuizForge's brand palette: a calm, trustworthy indigo as the primary color, a muted teal as a
// lower-emphasis secondary, and a warm amber reserved for small accents (badges, highlighted
// counts). Deliberately NOT derived from Material You's per-device dynamic/wallpaper colors (see
// QuizForgeTheme in Theme.kt) so the app looks the same, intentionally-designed way on every
// phone instead of blending into whatever wallpaper happens to be set.

// Primary - indigo
val IndigoLight = Color(0xFF3B5BDB)
val OnIndigoLight = Color(0xFFFFFFFF)
val IndigoContainerLight = Color(0xFFDDE1FF)
val OnIndigoContainerLight = Color(0xFF00174A)

val IndigoDark = Color(0xFFB6C4FF)
val OnIndigoDark = Color(0xFF042878)
val IndigoContainerDark = Color(0xFF1F3D91)
val OnIndigoContainerDark = Color(0xFFDDE1FF)

// Secondary - slate teal, used for lower-emphasis chips/surfaces/secondary buttons
val TealLight = Color(0xFF4D6360)
val OnTealLight = Color(0xFFFFFFFF)
val TealContainerLight = Color(0xFFCFE9E3)
val OnTealContainerLight = Color(0xFF082019)

val TealDark = Color(0xFFB3CCC6)
val OnTealDark = Color(0xFF1C3530)
val TealContainerDark = Color(0xFF354B47)
val OnTealContainerDark = Color(0xFFCFE9E3)

// Tertiary - warm amber, used sparingly for small accents (badges, highlighted numbers)
val AmberLight = Color(0xFF8A5600)
val OnAmberLight = Color(0xFFFFFFFF)
val AmberContainerLight = Color(0xFFFFDDB1)
val OnAmberContainerLight = Color(0xFF2C1600)

val AmberDark = Color(0xFFFFB955)
val OnAmberDark = Color(0xFF482900)
val AmberContainerDark = Color(0xFF673D00)
val OnAmberContainerDark = Color(0xFFFFDDB1)

// Neutral surfaces
val SurfaceLight = Color(0xFFFBFAFF)
val OnSurfaceLight = Color(0xFF1B1B21)
val SurfaceVariantLight = Color(0xFFE2E1EC)
val OnSurfaceVariantLight = Color(0xFF45464F)
val OutlineLight = Color(0xFF767680)

val SurfaceDark = Color(0xFF121318)
val OnSurfaceDark = Color(0xFFE4E2E9)
val SurfaceVariantDark = Color(0xFF45464F)
val OnSurfaceVariantDark = Color(0xFFC6C5D0)
val OutlineDark = Color(0xFF8F909A)

// Semantic - quiz answer feedback. Kept as dedicated colors (rather than mapped onto a generic
// Material role) since "correct"/"incorrect" are a QuizForge concept, not a Material one, and
// need to read unambiguously as green/red regardless of the rest of the theme.
val CorrectGreen = Color(0xFF1B8A4A)
val OnCorrectGreen = Color(0xFFFFFFFF)
val CorrectGreenContainer = Color(0xFFB8F3CB)
val CorrectGreenDark = Color(0xFF7FE0AA)
val OnCorrectGreenDark = Color(0xFF00391C)
val CorrectGreenContainerDark = Color(0xFF00522A)

val WrongRed = Color(0xFFBA1A1A)
val WrongRedDark = Color(0xFFFFB4AB)
