package com.paisa.najarine.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.paisa.najarine.R

// Google Font: Alkatra (Bangla font support)
val AlkatraFontFamily = FontFamily(
    Font(R.font.alkatra, FontWeight.Normal),
    Font(R.font.alkatra, FontWeight.Medium),
    Font(R.font.alkatra, FontWeight.SemiBold),
    Font(R.font.alkatra, FontWeight.Bold)
)

// Google Font: Josefin Sans (English & numbers)
val JosefinSansFontFamily = FontFamily(
    Font(R.font.josefin_sans, FontWeight.Normal),
    Font(R.font.josefin_sans, FontWeight.Bold)
)

// Material 3 Typography utilizing Alkatra for Bangla content and Josefin Sans for English & numbers
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = JosefinSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        color = Color.Unspecified
    ),
    displayMedium = TextStyle(
        fontFamily = JosefinSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp,
        color = Color.Unspecified
    ),
    displaySmall = TextStyle(
        fontFamily = JosefinSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = Color.Unspecified
    ),
    headlineLarge = TextStyle(
        fontFamily = JosefinSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = Color.Unspecified
    ),
    headlineMedium = TextStyle(
        fontFamily = JosefinSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = Color.Unspecified
    ),
    headlineSmall = TextStyle(
        fontFamily = AlkatraFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = Color.Unspecified
    ),
    titleLarge = TextStyle(
        fontFamily = AlkatraFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        color = Color.Unspecified
    ),
    titleMedium = TextStyle(
        fontFamily = AlkatraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Color.Unspecified
    ),
    titleSmall = TextStyle(
        fontFamily = AlkatraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = Color.Unspecified
    ),
    bodyLarge = TextStyle(
        fontFamily = AlkatraFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
        color = Color.Unspecified
    ),
    bodyMedium = TextStyle(
        fontFamily = AlkatraFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        color = Color.Unspecified
    ),
    bodySmall = TextStyle(
        fontFamily = AlkatraFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = Color.Unspecified
    ),
    labelLarge = TextStyle(
        fontFamily = AlkatraFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = Color.Unspecified
    ),
    labelMedium = TextStyle(
        fontFamily = AlkatraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = Color.Unspecified
    ),
    labelSmall = TextStyle(
        fontFamily = AlkatraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp,
        color = Color.Unspecified
    )
)
