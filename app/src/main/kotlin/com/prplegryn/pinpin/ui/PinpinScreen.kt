package com.prplegryn.pinpin.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.shadow.Shadow as ComposeShadow
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.prplegryn.pinpin.R
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

private val PrimaryText = Color(0xE6222A31)
private val SecondaryText = Color(0xA6444F58)
private val ComposerText = Color(0xFF172126)
private val ComposerSecondary = Color(0xFF68737A)
private val Accent = Color(0xFF087CFA)
private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

@Composable
fun PinpinApp() {
    PinpinScreen()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinpinScreen() {
    val backdrop = rememberLayerBackdrop()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var subtitle by remember { mutableStateOf("新的对话") }

    val dismissInput = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        dismissInput()
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.pinpin_minimal_flow),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .layerBackdrop(backdrop)
                .fillMaxSize()
        )

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { dismissInput() })
                }
        )

        TopBar(
            backdrop = backdrop,
            subtitle = subtitle,
            onMenuClick = {
                dismissInput()
                subtitle = if (subtitle == "菜单已就绪") "新的对话" else "菜单已就绪"
            },
            onTitleClick = {
                dismissInput()
                subtitle = "新的对话"
            },
            onMoreClick = {
                dismissInput()
                subtitle = if (subtitle == "更多选项") "新的对话" else "更多选项"
            }
        )

        AdaptiveComposer(
            onDismissInput = dismissInput,
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
    BoxWithConstraints(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        val titleMaximumWidth = maxOf(120.dp, maxWidth - 125.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                backdrop = backdrop,
                icon = PinpinIcon.Menu,
                contentDescription = "打开菜单",
                onClick = onMenuClick
            )

            Spacer(Modifier.width(9.dp))

            GlassTitle(
                backdrop = backdrop,
                subtitle = subtitle,
                maximumWidth = titleMaximumWidth,
                onClick = onTitleClick
            )

            Spacer(Modifier.weight(1f))

            GlassIconButton(
                backdrop = backdrop,
                icon = PinpinIcon.More,
                contentDescription = "更多选项",
                onClick = onMoreClick
            )
        }
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
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope)
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .appleAmbientShadow(
                shape = CircleShape,
                radius = 15.dp,
                alpha = 0.16f
            )
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(12.dp.toPx(), 24.dp.toPx())
                },
                shadow = null,
                layerBlock = {
                    val progress = interactiveHighlight.pressProgress
                    val scale = lerp(1f, 1f + 4.dp.toPx() / size.height, progress)
                    val maxOffset = size.minDimension
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
                    translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)

                    val maxDragScale = 4.dp.toPx() / size.height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX = scale + maxDragScale *
                        abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                        (size.width / size.height).fastCoerceAtMost(1f)
                    scaleY = scale + maxDragScale *
                        abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                        (size.height / size.width).fastCoerceAtMost(1f)
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.075f))
                }
            )
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
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
    maximumWidth: Dp,
    onClick: () -> Unit
) {
    val shape = remember { Capsule() }
    val shadowShape = remember { RoundedCornerShape(26.dp) }
    val density = LocalDensity.current
    val textSafetyInset = 26.dp + with(density) { 2.toDp() }
    val titleStyle = TextStyle(
        color = PrimaryText,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = Inter
    )
    val subtitleStyle = TextStyle(
        color = SecondaryText,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = Inter
    )
    val textMeasurer = rememberTextMeasurer()
    val measuredTextWidth = maxOf(
        textMeasurer.measure(
            text = "Pinpin",
            style = titleStyle,
            softWrap = false,
            maxLines = 1
        ).size.width,
        textMeasurer.measure(
            text = subtitle,
            style = subtitleStyle,
            softWrap = false,
            maxLines = 1
        ).size.width
    )
    val targetWidth = (
        6.dp + 40.dp + 10.dp + with(density) { measuredTextWidth.toDp() } + textSafetyInset
    ).coerceIn(120.dp, maximumWidth)
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
        label = "title capsule width"
    )
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope)
    }

    Row(
        modifier = Modifier
            .width(animatedWidth)
            .height(52.dp)
            .appleAmbientShadow(
                shape = shadowShape,
                radius = 15.dp,
                alpha = 0.16f
            )
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(12.dp.toPx(), 24.dp.toPx())
                },
                shadow = null,
                layerBlock = {
                    val progress = interactiveHighlight.pressProgress
                    val scale = lerp(1f, 1f + 4.dp.toPx() / size.height, progress)
                    val maxOffset = size.minDimension
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
                    translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)

                    val maxDragScale = 4.dp.toPx() / size.height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX = scale + maxDragScale *
                        abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                        (size.width / size.height).fastCoerceAtMost(1f)
                    scaleY = scale + maxDragScale *
                        abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                        (size.height / size.width).fastCoerceAtMost(1f)
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.075f)) }
            )
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .semantics { contentDescription = "打开对话详情" }
            .padding(start = 6.dp, end = textSafetyInset),
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
                    fontWeight = FontWeight.Bold,
                    fontFamily = Inter
                )
            )
        }

        Spacer(Modifier.width(10.dp))

        androidx.compose.foundation.layout.Column {
            BasicText(
                text = "Pinpin",
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                style = titleStyle
            )
            BasicText(
                text = subtitle,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                style = subtitleStyle
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.AdaptiveComposer(
    onDismissInput: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var imeHasOpened by remember { mutableStateOf(false) }
    val canSend = text.isNotBlank()
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val imeTargetBottom = WindowInsets.imeAnimationTarget.getBottom(density)

    // imeAnimationTarget changes at the beginning of an IME transition. Using
    // it avoids waiting for isImeVisible to turn false at the end of dismissal.
    val keyboardExpanded = if (imeTargetBottom != imeBottom) {
        imeTargetBottom > 0
    } else {
        imeBottom > 0
    }

    LaunchedEffect(keyboardExpanded) {
        if (keyboardExpanded) {
            imeHasOpened = true
        } else if (imeHasOpened) {
            imeHasOpened = false
            onDismissInput()
        }
    }

    val horizontalMargin by animateDpAsState(
        targetValue = if (keyboardExpanded) 8.dp else 18.dp,
        animationSpec = tween(durationMillis = 130),
        label = "composer horizontal margin"
    )
    val bottomMargin by animateDpAsState(
        targetValue = if (keyboardExpanded) 7.dp else 12.dp,
        animationSpec = tween(durationMillis = 130),
        label = "composer bottom margin"
    )
    val minimumHeight by animateDpAsState(
        targetValue = if (keyboardExpanded) 56.dp else 64.dp,
        animationSpec = tween(durationMillis = 130),
        label = "composer minimum height"
    )

    fun send() {
        val message = text.trim()
        if (message.isEmpty()) return
        onSend(message)
        text = ""
        onDismissInput()
    }

    val shape = remember { RoundedCornerShape(32.dp) }

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
            .animateContentSize(animationSpec = tween(durationMillis = 140))
            .appleAmbientShadow(shape = shape, radius = 18.dp, alpha = 0.18f)
            .clip(shape)
            .background(Color.White)
            .border(0.75.dp, Color.Black.copy(alpha = 0.055f), shape)
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
                .heightIn(min = 24.dp, max = 92.dp),
            textStyle = TextStyle(
                color = ComposerText,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = Inter
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
                            text = if (keyboardExpanded) "输入消息…" else "说点什么…",
                            style = TextStyle(
                                color = ComposerSecondary,
                                fontSize = 16.sp,
                                fontFamily = Inter
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )

        Crossfade(
            targetState = canSend,
            animationSpec = tween(durationMillis = 120),
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
    val background = if (emphasized) Accent else Color(0xFFF0F2F4)
    val foreground = if (emphasized) Color.White else ComposerText

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (emphasized) Modifier
                else Modifier.border(0.75.dp, Color.Black.copy(alpha = 0.045f), CircleShape)
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
    if (emphasizedColor == Color.White) Accent else Color.Transparent

private fun Modifier.appleAmbientShadow(
    shape: Shape,
    radius: Dp,
    alpha: Float
): Modifier = dropShadow(
    shape = shape,
    shadow = ComposeShadow(
        radius = radius,
        spread = 0.dp,
        offset = DpOffset(0.dp, 0.dp),
        color = Color.Black.copy(alpha = alpha)
    )
)
