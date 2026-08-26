package com.prplegryn.pinpin.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.prplegryn.pinpin.R

private val PrimaryText = Color(0xFFF5FBFF)
private val SecondaryText = Color(0xBFD8E8EF)
private val Accent = Color(0xFF78DBFF)

@Composable
fun PinpinApp() {
    PinpinScreen()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinpinScreen() {
    val backdrop = rememberLayerBackdrop()
    var subtitle by remember { mutableStateOf("新的对话") }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.pinpin_alpine_lake),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .layerBackdrop(backdrop)
                .fillMaxSize()
        )

        // Preserve the photographic detail while giving white glass controls a
        // stable contrast floor at both system-bar edges.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x57020B11),
                        0.22f to Color.Transparent,
                        0.68f to Color.Transparent,
                        1f to Color(0x8A02090E)
                    )
                )
        )

        TopBar(
            backdrop = backdrop,
            subtitle = subtitle,
            onMenuClick = {
                subtitle = if (subtitle == "菜单已就绪") "新的对话" else "菜单已就绪"
            },
            onTitleClick = { subtitle = "新的对话" },
            onMoreClick = {
                subtitle = if (subtitle == "更多选项") "新的对话" else "更多选项"
            }
        )

        AdaptiveComposer(
            backdrop = backdrop,
            onSend = { subtitle = "刚刚发送" }
        )
    }
}

@Composable
private fun BoxScope.TopBar(
    backdrop: Backdrop,
    subtitle: String,
    onMenuClick: () -> Unit,
    onTitleClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(
            backdrop = backdrop,
            icon = PinpinIcon.Menu,
            contentDescription = "打开菜单",
            onClick = onMenuClick
        )

        GlassTitle(
            backdrop = backdrop,
            subtitle = subtitle,
            modifier = Modifier.weight(1f),
            onClick = onTitleClick
        )

        GlassIconButton(
            backdrop = backdrop,
            icon = PinpinIcon.More,
            contentDescription = "更多选项",
            onClick = onMoreClick
        )
    }
}

@Composable
private fun GlassIconButton(
    backdrop: Backdrop,
    icon: PinpinIcon,
    contentDescription: String,
    onClick: () -> Unit
) {
    val shape = remember { Capsule() }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(52.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(12.dp.toPx(), 24.dp.toPx(), depthEffect = true)
                },
                highlight = { Highlight.Plain },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.12f))
                    drawRect(Color(0xFF9DE8FF).copy(alpha = 0.05f), blendMode = BlendMode.Screen)
                }
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        PinpinIcon(icon, Modifier.size(22.dp), PrimaryText)
    }
}

@Composable
private fun GlassTitle(
    backdrop: Backdrop,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = remember { Capsule() }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .height(52.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(12.dp.toPx(), 24.dp.toPx(), depthEffect = true)
                },
                highlight = { Highlight.Plain },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF4BC7E8), Color(0xFF636EDB), Color(0xFFC26AD8))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "P",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(Modifier.width(10.dp))

        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            BasicText(
                text = "Pinpin",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = PrimaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            BasicText(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(Modifier.width(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.AdaptiveComposer(
    backdrop: Backdrop,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible
    val canSend = text.isNotBlank()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val horizontalMargin by animateDpAsState(
        targetValue = when {
            imeVisible -> 8.dp
            focused || canSend -> 12.dp
            else -> 18.dp
        },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 480f),
        label = "composer horizontal margin"
    )
    val bottomMargin by animateDpAsState(
        targetValue = if (imeVisible) 7.dp else 12.dp,
        label = "composer bottom margin"
    )
    val minimumHeight by animateDpAsState(
        targetValue = if (imeVisible) 56.dp else if (focused || canSend) 60.dp else 64.dp,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 520f),
        label = "composer minimum height"
    )
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (imeVisible || focused) 0.86f else 0.78f,
        label = "composer opacity"
    )

    fun send() {
        val message = text.trim()
        if (message.isEmpty()) return
        onSend(message)
        text = ""
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    val shape = remember { RoundedRectangle(32.dp) }

    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(
                start = horizontalMargin,
                end = horizontalMargin,
                bottom = bottomMargin
            )
            .fillMaxWidth()
            .heightIn(min = minimumHeight, max = 132.dp)
            .animateContentSize(
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 520f)
            )
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(12.dp.toPx())
                    lens(6.dp.toPx(), 10.dp.toPx())
                },
                highlight = { Highlight.Plain },
                onDrawSurface = {
                    drawRect(Color(0xFF07161E).copy(alpha = surfaceAlpha))
                    drawRect(Color(0xFFBFEFFF).copy(alpha = 0.035f), blendMode = BlendMode.Screen)
                }
            )
            .clip(shape)
            .border(0.75.dp, Color.White.copy(alpha = 0.26f), shape)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ComposerButton(
            icon = PinpinIcon.Plus,
            contentDescription = "添加内容",
            emphasized = false,
            onClick = { }
        )

        BasicTextField(
            value = text,
            onValueChange = { text = it.take(2000) },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .heightIn(min = 24.dp, max = 92.dp)
                .onFocusChanged { focused = it.isFocused },
            textStyle = TextStyle(
                color = PrimaryText,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(Accent),
            minLines = 1,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(onSend = { send() }),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) {
                        BasicText(
                            text = if (imeVisible) "输入消息…" else "说点什么…",
                            style = TextStyle(
                                color = SecondaryText.copy(alpha = 0.74f),
                                fontSize = 16.sp
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )

        Crossfade(
            targetState = canSend,
            animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
            label = "send control"
        ) { sendingEnabled ->
            ComposerButton(
                icon = if (sendingEnabled) PinpinIcon.Send else PinpinIcon.Mic,
                contentDescription = if (sendingEnabled) "发送" else "语音输入",
                emphasized = sendingEnabled,
                onClick = { if (sendingEnabled) send() }
            )
        }
    }
}

@Composable
private fun ComposerButton(
    icon: PinpinIcon,
    contentDescription: String,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val background = if (emphasized) Accent else Color.White.copy(alpha = 0.09f)
    val foreground = if (emphasized) Color(0xFF06222C) else PrimaryText

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (emphasized) Modifier
                else Modifier.border(0.75.dp, Color.White.copy(alpha = 0.17f), CircleShape)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        PinpinIcon(icon, Modifier.size(21.dp), foreground)
    }
}

private enum class PinpinIcon {
    Menu,
    More,
    Plus,
    Mic,
    Send
}

@Composable
private fun PinpinIcon(
    icon: PinpinIcon,
    modifier: Modifier = Modifier,
    color: Color = PrimaryText
) {
    Canvas(modifier) {
        val strokeWidth = 1.9.dp.toPx()
        val roundStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height

        when (icon) {
            PinpinIcon.Menu -> {
                listOf(0.28f, 0.5f, 0.72f).forEach { y ->
                    drawLine(
                        color = color,
                        start = Offset(w * 0.22f, h * y),
                        end = Offset(w * 0.78f, h * y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            PinpinIcon.More -> {
                listOf(0.27f, 0.5f, 0.73f).forEach { x ->
                    drawCircle(color = color, radius = w * 0.075f, center = Offset(w * x, h * 0.5f))
                }
            }

            PinpinIcon.Plus -> {
                drawLine(
                    color,
                    Offset(w * 0.24f, h * 0.5f),
                    Offset(w * 0.76f, h * 0.5f),
                    strokeWidth,
                    StrokeCap.Round
                )
                drawLine(
                    color,
                    Offset(w * 0.5f, h * 0.24f),
                    Offset(w * 0.5f, h * 0.76f),
                    strokeWidth,
                    StrokeCap.Round
                )
            }

            PinpinIcon.Mic -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.36f, h * 0.16f),
                    size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.48f),
                    cornerRadius = CornerRadius(w * 0.14f),
                    style = roundStroke
                )
                val arcPath = Path().apply {
                    moveTo(w * 0.23f, h * 0.49f)
                    cubicTo(w * 0.23f, h * 0.75f, w * 0.77f, h * 0.75f, w * 0.77f, h * 0.49f)
                }
                drawPath(arcPath, color, style = roundStroke)
                drawLine(
                    color,
                    Offset(w * 0.5f, h * 0.76f),
                    Offset(w * 0.5f, h * 0.88f),
                    strokeWidth,
                    StrokeCap.Round
                )
                drawLine(
                    color,
                    Offset(w * 0.34f, h * 0.88f),
                    Offset(w * 0.66f, h * 0.88f),
                    strokeWidth,
                    StrokeCap.Round
                )
            }

            PinpinIcon.Send -> {
                val sendPath = Path().apply {
                    moveTo(w * 0.2f, h * 0.51f)
                    lineTo(w * 0.79f, h * 0.2f)
                    lineTo(w * 0.58f, h * 0.81f)
                    lineTo(w * 0.47f, h * 0.57f)
                    close()
                }
                drawPath(sendPath, color = color)
                drawLine(
                    color = backgroundForCutout(emphasizedColor = color),
                    start = Offset(w * 0.47f, h * 0.57f),
                    end = Offset(w * 0.76f, h * 0.23f),
                    strokeWidth = strokeWidth * 0.7f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun backgroundForCutout(emphasizedColor: Color): Color =
    if (emphasizedColor == Color(0xFF06222C)) Accent else Color.Transparent
