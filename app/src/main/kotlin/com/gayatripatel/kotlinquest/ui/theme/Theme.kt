package com.gayatripatel.kotlinquest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Kotlin's own purple-to-orange gradient, plus a learning-app green and red for feedback.
val KotlinPurple = Color(0xFF7F52FF)
val KotlinOrange = Color(0xFFF88909)
val KotlinPink = Color(0xFFE44857)
val Success = Color(0xFF2EA44F)
val SuccessContainer = Color(0xFFDDF6E3)
val Danger = Color(0xFFE5484D)
val DangerContainer = Color(0xFFFFE1E1)
val Flame = Color(0xFFFF8A00)
val HeartRed = Color(0xFFFF4B4B)
val Locked = Color(0xFFB9B6C4)

/** Code cards use the same dark palette in both themes, like an IDE. */
object CodeColors {
    val background = Color(0xFF1E1F22)
    val gutter = Color(0xFF6F737A)
    val plain = Color(0xFFBCBEC4)
    val keyword = Color(0xFFCF8E6D)
    val string = Color(0xFF6AAB73)
    val number = Color(0xFF2AACB8)
    val comment = Color(0xFF7A7E85)
    val type = Color(0xFFE6E6E6)
    val function = Color(0xFF56A8F5)
    val annotation = Color(0xFFB3AE60)
    val nullOp = Color(0xFFFF7EB6)
    val highlightLine = Color(0x33E5484D)
    val filledBlank = Color(0x557F52FF)
}

private val Light = lightColorScheme(
    primary = KotlinPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = KotlinOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0C2),
    onSecondaryContainer = Color(0xFF2E1500),
    tertiary = KotlinPink,
    background = Color(0xFFFCFAFF),
    surface = Color(0xFFFCFAFF),
    surfaceContainerLow = Color(0xFFF6F2FC),
    surfaceContainer = Color(0xFFF0ECF6),
    surfaceContainerHigh = Color(0xFFEAE6F1),
    outlineVariant = Color(0xFFD9D4E3),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFCFBCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF5B3FD0),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFFFB877),
    onSecondary = Color(0xFF4B2800),
    secondaryContainer = Color(0xFF6B3B00),
    onSecondaryContainer = Color(0xFFFFE0C2),
    tertiary = Color(0xFFFFB3B5),
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
    surfaceContainerLow = Color(0xFF1C1A21),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    outlineVariant = Color(0xFF49454F),
)

@Composable
fun QuestTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, content = content)
}
