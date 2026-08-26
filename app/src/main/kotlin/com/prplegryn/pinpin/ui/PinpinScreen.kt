package com.prplegryn.pinpin.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
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
import androidx.compose.ui.unit.Constraints
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
private val AmbientShadowTint = Color(0xFF627B9E)
private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)
private val TitleTextStyle = TextStyle(
    color = PrimaryText,
    fontSize = 15.sp,
    fontWeight = FontWeight.SemiBold,
    fontFamily = Inter
)
private val SubtitleTextStyle = TextStyle(
    color = SecondaryText,
    fontSize = 11.sp,
    fontWeight = FontWeight.Medium,
    fontFamily = Inter
)
private val ComposerInputStyle = TextStyle(
    color = ComposerText,
    fontSize = 16.sp,
    lineHeight = 21.sp,
    fontWeight = FontWeight.Normal,
    fontFamily = Inter
)
private val ComposerPlaceholderStyle = TextStyle(
    color = ComposerSecondary,
    fontSize = 16.sp,
    fontFamily = Inter
)
private val ComposerCursorBrush = SolidColor(Accent)
private val ComposerKeyboardOptions = KeyboardOptions(
    capitalization = KeyboardCapitalization.Sentences,
    imeAction = ImeAction.Send
)
private val AvatarBrush = Brush.linearGradient(
    listOf(Color(0xFF4BC7E8), Color(0xFF636EDB), Color(0xFFC26AD8))
)
private val AvatarTextStyle = TextStyle(
    color = Color.White,
    fontSize = 19.sp,
    fontWeight = FontWeight.Bold,
    fontFamily = Inter
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

    val dismissInput = remember(keyboardController, focusManager) {
        {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
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
private fun rememberLiquidGlassVisualModifier(
    backdrop: Backdrop,
    glassShape: Shape,
    shadowShape: Shape,
    interactiveHighlight: InteractiveHighlight
): Modifier = remember(backdrop, glassShape, shadowShape, interactiveHighlight) {
    Modifier
        .appleAmbientShadow(
            shape = shadowShape,
            radius = 18.dp,
            alpha = 0.13f
        )
        .drawBackdrop(
            backdrop = backdrop,
            shape = { glassShape },
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
    val glassVisualModifier = rememberLiquidGlassVisualModifier(
        backdrop = backdrop,
        glassShape = shape,
        shadowShape = CircleShape,
        interactiveHighlight = interactiveHighlight
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .then(glassVisualModifier)
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
    val textMeasurer = rememberTextMeasurer()
    val measuredTextWidth = remember(textMeasurer, subtitle) {
        maxOf(
            textMeasurer.measure(
                text = "Pinpin",
                style = TitleTextStyle,
                softWrap = false,
                maxLines = 1
            ).size.width,
            textMeasurer.measure(
                text = subtitle,
                style = SubtitleTextStyle,
                softWrap = false,
                maxLines = 1
            ).size.width
        )
    }
    val targetWidth = (
        6.dp + 40.dp + 10.dp + with(density) { measuredTextWidth.toDp() } + textSafetyInset
    ).coerceIn(120.dp, maximumWidth)
    val animatedWidth = animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
        label = "title capsule width"
    )
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope)
    }
    val glassVisualModifier = rememberLiquidGlassVisualModifier(
        backdrop = backdrop,
        glassShape = shape,
        shadowShape = shadowShape,
        interactiveHighlight = interactiveHighlight
    )

    Layout(
        modifier = Modifier
            .height(52.dp)
            .then(glassVisualModifier)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .semantics { contentDescription = "打开对话详情" }
            // Only the content is clipped. The glass and its press deformation
            // stay outside this inner modifier and can still grow freely.
            .clip(shadowShape),
        content = {
            Row(
                modifier = Modifier
                    .height(52.dp)
                    .padding(start = 6.dp, end = textSafetyInset),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AvatarBrush)
                        .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "P",
                        style = AvatarTextStyle
                    )
                }

                Spacer(Modifier.width(10.dp))

                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    BasicText(
                        text = "Pinpin",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = TitleTextStyle
                    )
                    BasicText(
                        text = subtitle,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = SubtitleTextStyle
                    )
                }
            }
        }
    ) { measurables, constraints ->
        // The content always receives the final width and stays anchored at x=0.
        // Only this layout's reported width follows animatedWidth, so BasicText
        // no longer recomputes ellipsis and glyph layout on every animation frame.
        val content = measurables.single().measure(
            Constraints.fixed(targetWidth.roundToPx(), 52.dp.roundToPx())
        )
        val layoutWidth = animatedWidth.value.roundToPx()
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = 52.dp.roundToPx()
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(layoutWidth, layoutHeight) {
            content.placeRelative(0, 0)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.AdaptiveComposer(
    onDismissInput: () -> Unit,
    onSend: (String) -> Unit
) {
    val textState = remember { mutableStateOf("") }
    var imeHasOpened by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val imeTargetBottom = WindowInsets.imeAnimationTarget.getBottom(density)

    // imeAnimationTarget changes once at the beginning of the transition. Do
    // not observe the current IME bottom here: it changes on every animation
    // frame and would recompose the entire editor tree.
    val keyboardExpanded = imeTargetBottom > 0

    LaunchedEffect(keyboardExpanded) {
        if (keyboardExpanded) {
            imeHasOpened = true
        } else if (imeHasOpened) {
            imeHasOpened = false
            onDismissInput()
        }
    }

    // edgePadding and the corner radius differ by 21dp in both IME states.
    // Adding this inset places the text past the left arc's tangent, plus 2px.
    val textStartPadding = 21.dp + with(density) { 2.toDp() }
    val updateText = remember {
        { value: String -> textState.value = value.take(2000) }
    }
    val send = remember(onDismissInput, onSend) {
        {
            val message = textState.value.trim()
            if (message.isNotEmpty()) {
                onSend(message)
                textState.value = ""
                onDismissInput()
            }
        }
    }

    ComposerSurface(keyboardExpanded = keyboardExpanded) {
        ComposerEditor(
            text = textState.value,
            keyboardExpanded = keyboardExpanded,
            textStartPadding = textStartPadding,
            onTextChange = updateText,
            onSend = send
        )
    }
}

@Composable
private fun BoxScope.ComposerSurface(
    keyboardExpanded: Boolean,
    content: @Composable RowScope.() -> Unit
) {
    // One animation clock drives every geometric change, keeping all values in
    // phase and replacing five independent animation states.
    val progress by animateFloatAsState(
        targetValue = if (keyboardExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 130),
        label = "composer IME geometry"
    )
    val horizontalMargin = 18.dp + (8.dp - 18.dp) * progress
    val bottomMargin = 12.dp + (7.dp - 12.dp) * progress
    val minimumHeight = 64.dp + (56.dp - 64.dp) * progress
    val cornerRadius = 32.dp + (28.dp - 32.dp) * progress
    val edgePadding = 11.dp + (7.dp - 11.dp) * progress
    val shape = RoundedCornerShape(cornerRadius)

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
            .appleAmbientShadow(shape = shape, radius = 24.dp, alpha = 0.13f)
            .heightIn(min = minimumHeight, max = 132.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 140))
            .clip(shape)
            .background(Color.White)
            .border(0.75.dp, Color(0xFFCCD7E5).copy(alpha = 0.72f), shape)
            .padding(edgePadding),
        verticalAlignment = Alignment.Bottom,
        content = content
    )
}

@Composable
private fun RowScope.ComposerEditor(
    text: String,
    keyboardExpanded: Boolean,
    textStartPadding: Dp,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 42.dp, max = 102.dp),
        textStyle = ComposerInputStyle,
        cursorBrush = ComposerCursorBrush,
        minLines = 1,
        maxLines = 4,
        keyboardOptions = ComposerKeyboardOptions,
        keyboardActions = KeyboardActions(onSend = { onSend() }),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = textStartPadding,
                        end = 8.dp,
                        top = 9.dp,
                        bottom = 9.dp
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty()) {
                    BasicText(
                        text = if (keyboardExpanded) "输入消息…" else "说点什么…",
                        style = ComposerPlaceholderStyle
                    )
                }
                innerTextField()
            }
        }
    )

    AnimatedVisibility(
        visible = text.isNotBlank(),
        modifier = Modifier.padding(start = 8.dp),
        enter = fadeIn(tween(120)) + scaleIn(
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
            initialScale = 0.82f
        ),
        exit = fadeOut(tween(90)) + scaleOut(
            animationSpec = tween(110),
            targetScale = 0.82f
        )
    ) {
        ComposerButton(onClick = onSend)
    }
}

@Composable
private fun ComposerButton(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Accent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = "发送" },
        contentAlignment = Alignment.Center
    ) {
        PinpinIcon(PinpinIcon.Send, Modifier.size(21.dp), Color.White)
    }
}

private enum class PinpinIcon {
    Menu,
    More,
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
        val w = size.width
        val h = size.height

        when (icon) {
            PinpinIcon.Menu -> {
                drawLine(
                    color, Offset(w * 0.22f, h * 0.28f), Offset(w * 0.78f, h * 0.28f),
                    strokeWidth, StrokeCap.Round
                )
                drawLine(
                    color, Offset(w * 0.22f, h * 0.5f), Offset(w * 0.78f, h * 0.5f),
                    strokeWidth, StrokeCap.Round
                )
                drawLine(
                    color, Offset(w * 0.22f, h * 0.72f), Offset(w * 0.78f, h * 0.72f),
                    strokeWidth, StrokeCap.Round
                )
            }

            PinpinIcon.More -> {
                val radius = w * 0.075f
                drawCircle(color, radius, Offset(w * 0.27f, h * 0.5f))
                drawCircle(color, radius, Offset(w * 0.5f, h * 0.5f))
                drawCircle(color, radius, Offset(w * 0.73f, h * 0.5f))
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
                    color = Accent,
                    start = Offset(w * 0.47f, h * 0.57f),
                    end = Offset(w * 0.76f, h * 0.23f),
                    strokeWidth = strokeWidth * 0.7f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

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
        color = AmbientShadowTint.copy(alpha = alpha)
    )
)
