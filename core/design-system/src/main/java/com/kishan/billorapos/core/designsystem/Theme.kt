package com.kishan.billorapos.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Brand Colors
val PrimaryColor = Color(0xFF6C63FF)
val PrimaryLight = Color(0xFF8B7CFF)
val PrimaryDark = Color(0xFF5347E0)
val SecondaryColor = Color(0xFF03DAC6)

// Semantic Colors
val SuccessColor = Color(0xFF16A34A)
val SuccessLight = Color(0xFFDCFCE7)
val WarningColor = Color(0xFFF59E0B)
val WarningLight = Color(0xFFFEF3C7)
val DangerColor = Color(0xFFDC2626)
val DangerLight = Color(0xFFFEE2E2)

// Neutral Scale
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate400 = Color(0xFF94A3B8)
val Slate100 = Color(0xFFF1F5F9)

// Gradients
object BilloraGradients {
    val Disabled = Brush.linearGradient(listOf(PrimaryColor.copy(alpha = 0.4f), PrimaryLight.copy(alpha = 0.4f)))
    val Checkout = Brush.verticalGradient(listOf(PrimaryColor.copy(alpha = 0.03f), PrimaryColor.copy(alpha = 0.10f)))
    val Primary = Brush.linearGradient(listOf(PrimaryColor, PrimaryLight))
    val PrimaryVertical = Brush.verticalGradient(listOf(PrimaryColor, PrimaryDark))
    val PrimaryDiagonal = Brush.linearGradient(listOf(PrimaryColor, PrimaryLight))
    val Slate = Brush.linearGradient(listOf(Slate800, Slate900))
    val Success = Brush.linearGradient(listOf(SuccessColor, Color(0xFF15803D)))
    val Danger = Brush.linearGradient(listOf(DangerColor, Color(0xFFB91C1C)))
}

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    background = Color(0xFFF8FAFC), // Slate 50 equivalent
    surface = Color.White,
    error = DangerColor,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Slate900,
    onSurface = Slate900,
    onError = Color.White,
    outline = Slate100
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    background = Slate900,
    surface = Slate800,
    error = DangerColor,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE2E8F0), // Slate 200
    onSurface = Color(0xFFE2E8F0),
    onError = Color.White,
    outline = Slate700
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)

@Composable
fun BilloraPOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
