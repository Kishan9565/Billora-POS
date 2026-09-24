package com.kishan.billorapos.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PrimaryColor = Color(0xFF6C63FF)
val SecondaryColor = Color(0xFF03DAC6)
val BackgroundColor = Color(0xFFF2F2F7)
val SurfaceColor = Color(0xFFFFFFFF)
val ErrorColor = Color(0xFFB00020)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    background = BackgroundColor,
    surface = SurfaceColor,
    error = ErrorColor,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color.Black,
    onError = Color.White
)

val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        color = Color.Black
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = Color(0xDE000000)
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = Color.Black
    )
)

@Composable
fun BilloraPOSTheme(
    content: @Composable () -> Unit
) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
