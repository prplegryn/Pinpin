package com.prplegryn.pinpin.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.sp
import com.prplegryn.pinpin.R

internal val PrimaryText = Color(0xE6222A31)
internal val SecondaryText = Color(0xA6444F58)
internal val ComposerText = Color(0xFF172126)
internal val ComposerSecondary = Color(0xFF68737A)
internal val Accent = Color(0xFF087CFA)
internal val Destructive = Color(0xFFD94747)
internal val AmbientShadowTint = Color(0xFF7890AD)
internal val Panel = Color(0xFFF8FAFC)
internal val ThinBorder = Color(0xFFCDD7E3).copy(alpha = 0.72f)

internal val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)
internal val TitleTextStyle = TextStyle(
    color = PrimaryText,
    fontSize = 15.sp,
    fontWeight = FontWeight.SemiBold,
    fontFamily = Inter
)
internal val SubtitleTextStyle = TextStyle(
    color = SecondaryText,
    fontSize = 11.sp,
    fontWeight = FontWeight.Medium,
    fontFamily = Inter
)
internal val BodyTextStyle = TextStyle(
    color = PrimaryText,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    fontFamily = Inter
)
internal val SmallTextStyle = TextStyle(
    color = SecondaryText,
    fontSize = 12.sp,
    lineHeight = 17.sp,
    fontFamily = Inter
)
internal val ComposerInputStyle = TextStyle(
    color = ComposerText,
    fontSize = 16.sp,
    lineHeight = 21.sp,
    fontWeight = FontWeight.Normal,
    fontFamily = Inter
)
internal val ComposerPlaceholderStyle = TextStyle(
    color = ComposerSecondary,
    fontSize = 16.sp,
    fontFamily = Inter
)
internal val ComposerCursorBrush = SolidColor(Accent)
internal val ComposerKeyboardOptions = KeyboardOptions(
    capitalization = KeyboardCapitalization.Sentences,
    imeAction = ImeAction.Send
)
internal val AvatarBrush = Brush.linearGradient(
    listOf(Color(0xFF4BC7E8), Color(0xFF636EDB), Color(0xFFC26AD8))
)
internal val AvatarTextStyle = TextStyle(
    color = Color.White,
    fontSize = 19.sp,
    fontWeight = FontWeight.Bold,
    fontFamily = Inter
)
