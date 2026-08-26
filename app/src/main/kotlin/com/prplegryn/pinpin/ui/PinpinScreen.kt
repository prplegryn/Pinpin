package com.prplegryn.pinpin.ui

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.compose.ui.graphics.shadow.Shadow as ComposeShadow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.prplegryn.pinpin.R
import com.prplegryn.pinpin.data.ApiSettings
import com.prplegryn.pinpin.data.ConversationEntity
import com.prplegryn.pinpin.data.MessageEntity
import com.prplegryn.pinpin.data.RoleProfile
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.math.PI

private enum class AppPage { Chat, Settings }

private data class MessageActionTarget(
    val message: MessageEntity,
    val canRegenerate: Boolean
)

private data class HistorySection(
    val key: String,
    val label: String,
    val conversations: List<ConversationEntity>
)

private data class ChatTextBlock(
    val text: String,
    val code: Boolean,
    val language: String = ""
)

@Composable
fun PinpinApp(viewModel: PinpinViewModel = viewModel()) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val historySearch by viewModel.historySearch.collectAsStateWithLifecycle()
    val historyQuery by viewModel.historyQuery.collectAsStateWithLifecycle()
    val currentConversation by viewModel.currentConversation.collectAsStateWithLifecycle()
    val currentConversationId by viewModel.currentConversationId.collectAsStateWithLifecycle()
    val currentRoleId by viewModel.currentRoleId.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val canRetry by viewModel.canRetry.collectAsStateWithLifecycle()
    val needsSettings by viewModel.needsSettings.collectAsStateWithLifecycle()
    val connectionTest by viewModel.connectionTest.collectAsStateWithLifecycle()
    val composerDraft by viewModel.composerDraft.collectAsStateWithLifecycle()

    PinpinScreen(
        viewModel = viewModel,
        conversations = conversations,
        historyResults = if (historyQuery.isBlank()) conversations else historySearch.results,
        historySearching = historyQuery.isNotBlank() && historySearch.query != historyQuery,
        historyQuery = historyQuery,
        currentConversation = currentConversation,
        currentConversationId = currentConversationId,
        currentRoleId = currentRoleId,
        settings = settings,
        isStreaming = isStreaming,
        notice = notice,
        canRetry = canRetry,
        needsSettings = needsSettings,
        connectionTest = connectionTest,
        composerDraft = composerDraft
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinpinScreen(
    viewModel: PinpinViewModel,
    conversations: List<ConversationEntity>,
    historyResults: List<ConversationEntity>,
    historyQuery: String,
    historySearching: Boolean,
    currentConversation: ConversationEntity?,
    currentConversationId: Long?,
    currentRoleId: String,
    settings: ApiSettings,
    isStreaming: Boolean,
    notice: String?,
    canRetry: Boolean,
    needsSettings: Boolean,
    connectionTest: ConnectionTestState,
    composerDraft: String
) {
    val backdrop = rememberLayerBackdrop()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboard = LocalClipboardManager.current
    var page by rememberSaveable { mutableStateOf(AppPage.Chat) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var rolePickerOpen by rememberSaveable { mutableStateOf(false) }
    var moreMenuOpen by rememberSaveable { mutableStateOf(false) }
    var historyAction by remember { mutableStateOf<ConversationEntity?>(null) }
    var messageAction by remember { mutableStateOf<MessageActionTarget?>(null) }
    var pendingDelete by remember { mutableStateOf<ConversationEntity?>(null) }
    var pendingRename by remember { mutableStateOf<ConversationEntity?>(null) }
    var pendingClearAll by rememberSaveable { mutableStateOf(false) }
    var copiedFeedback by remember { mutableStateOf(false) }
    val roles = remember(settings) { RoleProfile.all(settings) }
    val retainedHistoryAction = retainForExit(historyAction)
    val retainedMessageAction = retainForExit(messageAction)
    val retainedRename = retainForExit(pendingRename)
    val retainedDelete = retainForExit(pendingDelete)

    val dismissInput = remember(keyboardController, focusManager) {
        {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        dismissInput()
    }

    LaunchedEffect(copiedFeedback) {
        if (copiedFeedback) {
            delay(1_500)
            copiedFeedback = false
        }
    }

    BackHandler(
        enabled = pendingClearAll || pendingRename != null || pendingDelete != null ||
            messageAction != null || historyAction != null || rolePickerOpen ||
            moreMenuOpen || drawerOpen || page == AppPage.Settings
    ) {
        when {
            pendingClearAll -> pendingClearAll = false
            pendingRename != null -> pendingRename = null
            pendingDelete != null -> pendingDelete = null
            messageAction != null -> messageAction = null
            historyAction != null -> historyAction = null
            rolePickerOpen -> rolePickerOpen = false
            moreMenuOpen -> moreMenuOpen = false
            drawerOpen -> {
                dismissInput()
                drawerOpen = false
            }
            page == AppPage.Settings -> {
                dismissInput()
                page = AppPage.Chat
            }
        }
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

        AnimatedContent(
            targetState = page,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (targetState == AppPage.Settings) {
                    (slideInHorizontally(PinpinMotion.emphasizedTween()) { it / 4 } +
                        fadeIn(PinpinMotion.standardTween())) togetherWith
                        (slideOutHorizontally(PinpinMotion.standardTween()) { -it / 8 } +
                            fadeOut(PinpinMotion.quickTween()))
                } else {
                    (slideInHorizontally(PinpinMotion.emphasizedTween()) { -it / 5 } +
                        fadeIn(PinpinMotion.standardTween())) togetherWith
                        (slideOutHorizontally(PinpinMotion.standardTween()) { it / 5 } +
                            fadeOut(PinpinMotion.quickTween()))
                }
            },
            label = "chat settings navigation"
        ) { targetPage ->
            if (targetPage == AppPage.Settings) {
                SettingsPage(
                    backdrop = backdrop,
                    settings = settings,
                    connectionTest = connectionTest,
                    onBack = {
                        dismissInput()
                        page = AppPage.Chat
                    },
                    onSave = viewModel::saveSettings,
                    onTest = viewModel::testSettings,
                    onClearTest = viewModel::clearConnectionTest,
                    onClearHistory = { pendingClearAll = true }
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { dismissInput() })
                        }
                ) {
                    MessageListHost(
                        viewModel = viewModel,
                        currentConversationId = currentConversationId,
                        currentRole = roles
                            .firstOrNull { it.id == currentRoleId }
                            ?: roles.first(),
                        onMessageLongPress = { message, canRegenerate ->
                            dismissInput()
                            messageAction = MessageActionTarget(message, canRegenerate)
                        }
                    )

                    TopBar(
                        backdrop = backdrop,
                        subtitle = currentConversation?.title ?: "新的对话",
                        onMenuClick = {
                            dismissInput()
                            drawerOpen = true
                        },
                        onTitleClick = {
                            dismissInput()
                            rolePickerOpen = true
                        },
                        onMoreClick = {
                            dismissInput()
                            moreMenuOpen = true
                        }
                    )

                    AdaptiveComposer(
                        text = composerDraft,
                        isStreaming = isStreaming,
                        onTextChange = viewModel::updateComposerDraft,
                        onDismissInput = dismissInput,
                        onSend = viewModel::send,
                        onStop = viewModel::stopReply
                    )

                    NoticeBanner(
                        text = notice,
                        canRetry = canRetry,
                        needsSettings = needsSettings,
                        onRetry = viewModel::retryLastReply,
                        onOpenSettings = {
                            viewModel.clearNotice()
                            dismissInput()
                            page = AppPage.Settings
                        },
                        onDismiss = viewModel::clearNotice
                    )

                    MoreMenu(
                        visible = moreMenuOpen,
                        canManage = currentConversation != null,
                        canRetry = canRetry,
                        onDismiss = { moreMenuOpen = false },
                        onNewConversation = {
                            moreMenuOpen = false
                            viewModel.newConversation()
                        },
                        onOpenSettings = {
                            moreMenuOpen = false
                            page = AppPage.Settings
                        },
                        onRetry = {
                            moreMenuOpen = false
                            viewModel.retryLastReply()
                        },
                        onRename = {
                            moreMenuOpen = false
                            pendingRename = currentConversation
                        },
                        onDelete = {
                            moreMenuOpen = false
                            pendingDelete = currentConversation
                        }
                    )

                    ConversationDrawer(
                        visible = drawerOpen,
                        conversations = historyResults,
                        query = historyQuery,
                        searching = historySearching,
                        onQueryChange = viewModel::updateHistoryQuery,
                        currentConversationId = currentConversationId,
                        onDismiss = {
                            dismissInput()
                            drawerOpen = false
                        },
                        onNewConversation = {
                            dismissInput()
                            drawerOpen = false
                            viewModel.newConversation()
                        },
                        onSelectConversation = { conversation ->
                            dismissInput()
                            drawerOpen = false
                            viewModel.selectConversation(conversation.id)
                        },
                        onLongPressConversation = {
                            dismissInput()
                            historyAction = it
                        },
                        onOpenRoles = {
                            dismissInput()
                            drawerOpen = false
                            rolePickerOpen = true
                        },
                        onOpenSettings = {
                            dismissInput()
                            drawerOpen = false
                            page = AppPage.Settings
                        }
                    )
                }
            }
        }

        RolePickerSheet(
            visible = rolePickerOpen,
            roles = roles,
            selectedRoleId = currentRoleId,
            onSelect = { role ->
                viewModel.selectRole(role.id)
                rolePickerOpen = false
            },
            onDismiss = { rolePickerOpen = false },
            onEditCustomRole = {
                rolePickerOpen = false
                page = AppPage.Settings
            }
        )

        retainedHistoryAction?.let { conversation ->
            HistoryActionSheet(
                visible = historyAction != null,
                conversation = conversation,
                onDismiss = { historyAction = null },
                onTogglePin = {
                    viewModel.setPinned(conversation)
                    historyAction = null
                },
                onRename = {
                    historyAction = null
                    pendingRename = conversation
                },
                onDelete = {
                    historyAction = null
                    pendingDelete = conversation
                }
            )
        }

        retainedMessageAction?.let { target ->
            MessageActionSheet(
                visible = messageAction != null,
                target = target,
                onDismiss = { messageAction = null },
                onCopy = {
                    clipboard.setText(AnnotatedString(target.message.content))
                    messageAction = null
                    copiedFeedback = true
                },
                onRegenerate = {
                    messageAction = null
                    viewModel.regenerateReply(target.message.id)
                }
            )
        }

        retainedRename?.let { conversation ->
            RenameConversationDialog(
                visible = pendingRename != null,
                conversation = conversation,
                onDismiss = { pendingRename = null },
                onConfirm = { title ->
                    if (viewModel.renameConversation(conversation.id, title)) {
                        pendingRename = null
                    }
                }
            )
        }

        retainedDelete?.let { conversation ->
            DeleteConfirmation(
                visible = pendingDelete != null,
                title = conversation.title,
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    pendingDelete = null
                    viewModel.deleteConversation(conversation.id)
                }
            )
        }
        ClearHistoryConfirmation(
            visible = pendingClearAll,
            count = conversations.size,
            onDismiss = { pendingClearAll = false },
            onConfirm = {
                pendingClearAll = false
                viewModel.clearAllConversations()
                page = AppPage.Chat
            }
        )

        TransientToast(visible = copiedFeedback, text = "已复制")
    }
}

@Composable
private fun <T> retainForExit(value: T?): T? {
    var retained by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value != null) {
            retained = value
        } else {
            delay(PinpinMotion.Standard.toLong())
            retained = null
        }
    }
    return value ?: retained
}

@Composable
private fun BoxScope.MessageListHost(
    viewModel: PinpinViewModel,
    currentConversationId: Long?,
    currentRole: RoleProfile,
    onMessageLongPress: (MessageEntity, Boolean) -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
    MessageList(
        messages = messages,
        streamingReply = streamingReply.takeIf {
            it.active && it.conversationId == currentConversationId
        },
        currentRole = currentRole,
        onMessageLongPress = { message ->
            onMessageLongPress(
                message,
                message.role == MessageEntity.ROLE_ASSISTANT &&
                    !streamingReply.active && messages.lastOrNull()?.id == message.id
            )
        }
    )
}

@Composable
private fun BoxScope.MessageList(
    messages: List<MessageEntity>,
    streamingReply: StreamingReply?,
    currentRole: RoleProfile,
    onMessageLongPress: (MessageEntity) -> Unit
) {
    val empty = messages.isEmpty() && streamingReply == null
    AnimatedVisibility(
        visible = empty,
        modifier = Modifier.align(Alignment.Center),
        enter = fadeIn(PinpinMotion.emphasizedTween()) +
            scaleIn(PinpinMotion.expressiveSpring(), initialScale = 0.92f),
        exit = fadeOut(PinpinMotion.quickTween()) +
            scaleOut(PinpinMotion.quickTween(), targetScale = 0.96f)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .appleAmbientShadow(CircleShape, 18.dp, 0.09f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                PinpinIcon(PinpinIcon.Chat, Modifier.size(23.dp), PrimaryText)
            }
            Spacer(Modifier.height(16.dp))
            BasicText(
                text = "从这里开始",
                style = TitleTextStyle.copy(fontSize = 18.sp)
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = "${currentRole.name} · ${currentRole.description}",
                modifier = Modifier.widthIn(max = 280.dp),
                style = SmallTextStyle
            )
        }
    }

    AnimatedVisibility(
        visible = !empty,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(PinpinMotion.standardTween()),
        exit = fadeOut(PinpinMotion.quickTween())
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 100.dp,
                bottom = 108.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            streamingReply?.let { reply ->
                item(key = "streaming-${reply.conversationId}") {
                    MessageBubble(
                        content = reply.text,
                        fromUser = false,
                        isStreaming = true,
                        waiting = reply.text.isEmpty(),
                        modifier = Modifier.animateItem(
                            fadeInSpec = PinpinMotion.standardTween(),
                            fadeOutSpec = PinpinMotion.quickTween(),
                            placementSpec = PinpinMotion.settledSpring()
                        )
                    )
                }
            }
            items(
                items = messages.asReversed(),
                key = { it.id },
                contentType = { it.role }
            ) { message ->
                MessageBubble(
                    content = message.content,
                    fromUser = message.role == MessageEntity.ROLE_USER,
                    status = message.status,
                    modifier = Modifier.animateItem(
                        fadeInSpec = PinpinMotion.standardTween(),
                        fadeOutSpec = PinpinMotion.quickTween(),
                        placementSpec = PinpinMotion.settledSpring()
                    ),
                    onLongPress = { onMessageLongPress(message) }
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    content: String,
    fromUser: Boolean,
    status: String = MessageEntity.STATUS_COMPLETE,
    isStreaming: Boolean = false,
    waiting: Boolean = false,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val radius = 22.dp
    val safeHorizontalInset = radius + with(density) { 2.toDp() }
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .appleAmbientShadow(RoundedCornerShape(radius), 16.dp, 0.075f)
                .then(
                    if (onLongPress != null) {
                        Modifier
                            .pinpinPressFeedback(interactionSource, pressedScale = 0.985f)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Button,
                                onClickLabel = "消息操作",
                                onLongClickLabel = "消息操作",
                                onLongClick = onLongPress,
                                onClick = onLongPress
                            )
                    } else {
                        Modifier
                    }
                )
                .clip(RoundedCornerShape(radius))
                .background(
                    if (fromUser) Color(0xFF1678E8).copy(alpha = 0.94f)
                    else Color.White.copy(alpha = 0.94f)
                )
                .border(
                    0.6.dp,
                    if (fromUser) Color.White.copy(alpha = 0.2f) else ThinBorder,
                    RoundedCornerShape(radius)
                )
                .padding(horizontal = safeHorizontalInset, vertical = 14.dp)
        ) {
            MessageContent(
                content = content,
                color = if (fromUser) Color.White else PrimaryText,
                fromUser = fromUser,
                streaming = isStreaming,
                waiting = waiting
            )
            if (status != MessageEntity.STATUS_COMPLETE || isStreaming) {
                Spacer(Modifier.height(6.dp))
                BasicText(
                    text = when {
                        waiting -> "正在连接"
                        isStreaming -> "接收中"
                        status == MessageEntity.STATUS_STOPPED -> "已停止"
                        else -> "回复中断"
                    },
                    style = SmallTextStyle.copy(
                        fontSize = 10.sp,
                        color = if (fromUser) Color.White.copy(alpha = 0.72f) else SecondaryText
                    )
                )
            }
        }
    }
}

@Composable
private fun MessageContent(
    content: String,
    color: Color,
    fromUser: Boolean,
    streaming: Boolean,
    waiting: Boolean
) {
    if (waiting) {
        TypingIndicator(color)
        return
    }
    if (streaming || "```" !in content) {
        SelectionContainer {
            BasicText(
                text = content,
                style = BodyTextStyle.copy(
                    color = color,
                    textMotion = if (streaming) TextMotion.Animated else TextMotion.Static
                )
            )
        }
        return
    }

    val blocks = remember(content) { parseChatTextBlocks(content) }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        blocks.forEach { block ->
            if (block.code) {
                val shape = RoundedCornerShape(14.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(
                            if (fromUser) Color.Black.copy(alpha = 0.16f)
                            else Color(0xFFF0F3F7)
                        )
                        .border(
                            0.6.dp,
                            if (fromUser) Color.White.copy(alpha = 0.16f) else ThinBorder,
                            shape
                        )
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    if (block.language.isNotBlank()) {
                        BasicText(
                            block.language,
                            style = SmallTextStyle.copy(
                                fontSize = 10.sp,
                                color = color.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(Modifier.height(7.dp))
                    }
                    SelectionContainer {
                        BasicText(
                            block.text,
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            softWrap = false,
                            style = BodyTextStyle.copy(
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        )
                    }
                }
            } else if (block.text.isNotBlank()) {
                SelectionContainer {
                    BasicText(block.text, style = BodyTextStyle.copy(color = color))
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator(color: Color) {
    val transition = rememberInfiniteTransition(label = "waiting reply")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = LinearEasing)
        ),
        label = "waiting phase"
    )
    Canvas(Modifier.width(34.dp).height(12.dp)) {
        repeat(3) { index ->
            val wave = (sin((phase - index * 0.16f) * (PI * 2f)).toFloat() + 1f) / 2f
            drawCircle(
                color = color.copy(alpha = 0.28f + wave * 0.64f),
                radius = 2.5.dp.toPx(),
                center = Offset(4.dp.toPx() + index * 12.dp.toPx(), size.height / 2f)
            )
        }
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
                contentDescription = "打开聊天历史",
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
                contentDescription = "更多操作",
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
        .appleAmbientShadow(shape = shadowShape, radius = 18.dp, alpha = 0.12f)
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
    val interactiveHighlight = remember(animationScope) { InteractiveHighlight(animationScope) }
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
    var displayedSubtitle by remember { mutableStateOf(subtitle) }
    var previousTargetWidth by remember { mutableStateOf(targetWidth) }
    LaunchedEffect(subtitle, targetWidth) {
        if (targetWidth <= previousTargetWidth) displayedSubtitle = subtitle
        previousTargetWidth = targetWidth
    }
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = PinpinMotion.expressiveSpring(),
        label = "title capsule width",
        finishedListener = { displayedSubtitle = subtitle }
    )
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) { InteractiveHighlight(animationScope) }
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
            .semantics { contentDescription = "切换角色" },
        content = {
            Row(
                modifier = Modifier
                    .height(52.dp)
                    .clip(shadowShape)
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
                    BasicText("P", style = AvatarTextStyle)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = "Pinpin",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = TitleTextStyle
                    )
                    AnimatedContent(
                        targetState = displayedSubtitle,
                        modifier = Modifier.fillMaxWidth(),
                        transitionSpec = {
                            fadeIn(PinpinMotion.standardTween()) togetherWith
                                fadeOut(PinpinMotion.quickTween())
                        },
                        label = "title text"
                    ) { title ->
                        BasicText(
                            text = title,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = SubtitleTextStyle.copy(textMotion = TextMotion.Animated)
                        )
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val content = measurables.single().measure(
            Constraints.fixed(targetWidth.roundToPx(), 52.dp.roundToPx())
        )
        val layoutWidth = animatedWidth.roundToPx()
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = 52.dp.roundToPx()
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(layoutWidth, layoutHeight) { content.placeRelative(0, 0) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.AdaptiveComposer(
    text: String,
    isStreaming: Boolean,
    onTextChange: (String) -> Unit,
    onDismissInput: () -> Unit,
    onSend: (String) -> Boolean,
    onStop: () -> Unit
) {
    var imeHasOpened by remember { mutableStateOf(false) }
    var composerFocused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val imeTargetBottom = WindowInsets.imeAnimationTarget.getBottom(density)
    val keyboardExpanded = imeTargetBottom > 0

    LaunchedEffect(keyboardExpanded) {
        if (keyboardExpanded) {
            imeHasOpened = true
        } else if (imeHasOpened) {
            imeHasOpened = false
            onDismissInput()
        }
    }

    val textStartPadding = 21.dp + with(density) { 2.toDp() }
    val send = {
        val message = text.trim()
        if (message.isNotEmpty() && onSend(message)) onTextChange("")
    }

    ComposerSurface(keyboardExpanded = keyboardExpanded, focused = composerFocused) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { composerFocused = it.isFocused }
                .heightIn(min = 42.dp, max = 102.dp),
            textStyle = ComposerInputStyle,
            cursorBrush = ComposerCursorBrush,
            minLines = 1,
            maxLines = 4,
            keyboardOptions = ComposerKeyboardOptions,
            keyboardActions = KeyboardActions(onSend = { if (!isStreaming) send() }),
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
            visible = text.isNotBlank() || isStreaming,
            modifier = Modifier.padding(start = 8.dp),
            enter = fadeIn(PinpinMotion.quickTween()) + scaleIn(
                animationSpec = PinpinMotion.expressiveSpring(),
                initialScale = 0.82f
            ),
            exit = fadeOut(PinpinMotion.quickTween()) +
                scaleOut(PinpinMotion.quickTween(), targetScale = 0.82f)
        ) {
            ComposerButton(
                stopping = isStreaming,
                onClick = if (isStreaming) onStop else send
            )
        }
    }
}

@Composable
private fun BoxScope.ComposerSurface(
    keyboardExpanded: Boolean,
    focused: Boolean,
    content: @Composable RowScope.() -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = if (keyboardExpanded) 1f else 0f,
        animationSpec = PinpinMotion.quickTween(),
        label = "composer IME geometry"
    )
    val horizontalMargin = 18.dp + (8.dp - 18.dp) * progress
    val bottomMargin = 12.dp + (7.dp - 12.dp) * progress
    val minimumHeight = 64.dp + (56.dp - 64.dp) * progress
    val cornerRadius = 32.dp + (28.dp - 32.dp) * progress
    val edgePadding = 11.dp + (7.dp - 11.dp) * progress
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor by animateColorAsState(
        targetValue = if (focused) Accent.copy(alpha = 0.32f) else ThinBorder,
        animationSpec = PinpinMotion.standardTween(),
        label = "composer focus border"
    )

    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(start = horizontalMargin, end = horizontalMargin, bottom = bottomMargin)
            .fillMaxWidth()
            .appleAmbientShadow(shape = shape, radius = 22.dp, alpha = 0.105f)
            .heightIn(min = minimumHeight, max = 132.dp)
            .animateContentSize(animationSpec = PinpinMotion.quickTween())
            .clip(shape)
            .background(Color.White)
            .border(if (focused) 1.dp else 0.75.dp, borderColor, shape)
            .padding(edgePadding),
        verticalAlignment = Alignment.Bottom,
        content = content
    )
}

@Composable
private fun ComposerButton(stopping: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        targetValue = if (stopping) Color(0xFF26343E) else Accent,
        animationSpec = PinpinMotion.standardTween(),
        label = "send stop color"
    )
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(background)
            .pinpinPressFeedback(interactionSource, pressedScale = 0.88f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = if (stopping) "停止回复" else "发送" },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = stopping,
            transitionSpec = {
                (fadeIn(PinpinMotion.quickTween()) +
                    scaleIn(PinpinMotion.expressiveSpring(), initialScale = 0.65f)) togetherWith
                    (fadeOut(PinpinMotion.quickTween()) +
                        scaleOut(PinpinMotion.quickTween(), targetScale = 0.7f))
            },
            label = "send stop icon"
        ) { isStopping ->
            PinpinIcon(
                if (isStopping) PinpinIcon.Stop else PinpinIcon.Send,
                Modifier.size(21.dp),
                Color.White
            )
        }
    }
}

@Composable
private fun BoxScope.NoticeBanner(
    text: String?,
    canRetry: Boolean,
    needsSettings: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = text != null,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(start = 22.dp, end = 22.dp, bottom = 86.dp),
        enter = fadeIn(PinpinMotion.quickTween()) +
            scaleIn(PinpinMotion.expressiveSpring(), initialScale = 0.96f),
        exit = fadeOut(PinpinMotion.quickTween())
    ) {
        val shape = RoundedCornerShape(22.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .appleAmbientShadow(shape, 18.dp, 0.1f)
                .clip(shape)
                .background(Color(0xFFF8FAFD).copy(alpha = 0.98f))
                .border(0.7.dp, ThinBorder, shape)
                .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = text.orEmpty(),
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = SmallTextStyle.copy(color = PrimaryText)
            )
            if (needsSettings) {
                CompactTextButton("设置", Accent, onOpenSettings)
            } else if (canRetry) {
                CompactTextButton("重试", Accent, onRetry)
            }
            IconTouchButton(PinpinIcon.Close, "关闭提示", onDismiss)
        }
    }
}

@Composable
private fun BoxScope.MoreMenu(
    visible: Boolean,
    canManage: Boolean,
    canRetry: Boolean,
    onDismiss: () -> Unit,
    onNewConversation: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(PinpinMotion.quickTween()),
        exit = fadeOut(PinpinMotion.quickTween())
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = null, indication = null, onClick = onDismiss)
        )
    }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(top = 72.dp, end = 18.dp),
        enter = fadeIn(PinpinMotion.quickTween()) +
            scaleIn(
                animationSpec = PinpinMotion.expressiveSpring(),
                initialScale = 0.88f,
                transformOrigin = TransformOrigin(1f, 0f)
            ) + slideInVertically(PinpinMotion.standardTween()) { -it / 8 },
        exit = fadeOut(PinpinMotion.quickTween()) +
            scaleOut(
                animationSpec = PinpinMotion.quickTween(),
                targetScale = 0.94f,
                transformOrigin = TransformOrigin(1f, 0f)
            )
    ) {
        Column(
            modifier = Modifier
            .width(206.dp)
            .appleAmbientShadow(RoundedCornerShape(24.dp), 22.dp, 0.12f)
            .clip(RoundedCornerShape(24.dp))
            .background(Panel.copy(alpha = 0.98f))
            .border(0.7.dp, ThinBorder, RoundedCornerShape(24.dp))
            .padding(vertical = 10.dp)
        ) {
            MenuAction(PinpinIcon.Add, "新对话", onNewConversation)
            if (canRetry) MenuAction(PinpinIcon.Retry, "重试上次回复", onRetry, Accent)
            if (canManage) MenuAction(PinpinIcon.Edit, "重命名", onRename)
            MenuAction(PinpinIcon.Settings, "设置", onOpenSettings)
            if (canManage) MenuAction(PinpinIcon.Trash, "删除当前对话", onDelete, Destructive)
        }
    }
}

@Composable
private fun MenuAction(
    icon: PinpinIcon,
    label: String,
    onClick: () -> Unit,
    color: Color = PrimaryText
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .pinpinPressFeedback(interactionSource, pressedScale = 0.98f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PinpinIcon(icon, Modifier.size(20.dp), color)
        Spacer(Modifier.width(13.dp))
        BasicText(label, style = BodyTextStyle.copy(color = color, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun BoxScope.ConversationDrawer(
    visible: Boolean,
    conversations: List<ConversationEntity>,
    query: String,
    searching: Boolean,
    onQueryChange: (String) -> Unit,
    currentConversationId: Long?,
    onDismiss: () -> Unit,
    onNewConversation: () -> Unit,
    onSelectConversation: (ConversationEntity) -> Unit,
    onLongPressConversation: (ConversationEntity) -> Unit,
    onOpenRoles: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(PinpinMotion.standardTween()),
        exit = fadeOut(PinpinMotion.quickTween())
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF16202A).copy(alpha = 0.25f))
                .clickable(interactionSource = null, indication = null, onClick = onDismiss)
        )
    }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.CenterStart),
        enter = slideInHorizontally(PinpinMotion.emphasizedTween()) { -it },
        exit = slideOutHorizontally(PinpinMotion.standardTween()) { -it }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val drawerWidth = minOf(maxWidth * 0.88f, 372.dp)
            val drawerShape = RoundedCornerShape(topEnd = 34.dp, bottomEnd = 34.dp)
            val drawerSafeEnd = 34.dp + with(LocalDensity.current) { 2.toDp() }
            val sections = remember(conversations, query) {
                historySections(conversations, query)
            }
            val resultCount = remember(sections) { sections.sumOf { it.conversations.size } }

            Column(
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .appleAmbientShadow(drawerShape, 28.dp, 0.15f)
                    .clip(drawerShape)
                    .background(Panel)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = drawerSafeEnd, top = 18.dp, bottom = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        "对话",
                        modifier = Modifier.weight(1f),
                        style = TitleTextStyle.copy(fontSize = 22.sp)
                    )
                    SolidIconButton(PinpinIcon.Add, "新对话", onNewConversation)
                }
                Spacer(Modifier.height(16.dp))
                SearchField(query = query, onQueryChange = onQueryChange)
                Spacer(Modifier.height(16.dp))
                if (searching) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BasicText("正在搜索…", style = BodyTextStyle.copy(color = SecondaryText))
                    }
                } else if (resultCount == 0) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BasicText(
                            if (query.isBlank()) "还没有对话" else "没有匹配的对话",
                            style = BodyTextStyle.copy(color = SecondaryText)
                        )
                        if (query.isBlank()) {
                            Spacer(Modifier.height(5.dp))
                            BasicText("发送第一条消息后会保存在这里", style = SmallTextStyle)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sections.forEach { section ->
                            item(key = "section-${section.key}", contentType = "section") {
                                BasicText(
                                    text = section.label,
                                    modifier = Modifier
                                        .animateItem(
                                            fadeInSpec = PinpinMotion.standardTween(),
                                            fadeOutSpec = PinpinMotion.quickTween(),
                                            placementSpec = PinpinMotion.settledSpring()
                                        )
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
                                    style = SmallTextStyle.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SecondaryText.copy(alpha = 0.82f)
                                    )
                                )
                            }
                            items(
                                items = section.conversations,
                                key = { it.id },
                                contentType = { "conversation" }
                            ) { conversation ->
                                HistoryRow(
                                    conversation = conversation,
                                    selected = conversation.id == currentConversationId,
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = PinpinMotion.standardTween(),
                                        fadeOutSpec = PinpinMotion.quickTween(),
                                        placementSpec = PinpinMotion.expressiveSpring()
                                    ),
                                    onClick = { onSelectConversation(conversation) },
                                    onLongClick = { onLongPressConversation(conversation) }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DrawerBottomButton(
                        modifier = Modifier.weight(1f),
                        icon = PinpinIcon.Role,
                        label = "角色",
                        onClick = onOpenRoles
                    )
                    DrawerBottomButton(
                        modifier = Modifier.weight(1f),
                        icon = PinpinIcon.Settings,
                        label = "设置",
                        onClick = onOpenSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(Color.White)
            .border(0.7.dp, ThinBorder, shape)
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PinpinIcon(PinpinIcon.Search, Modifier.size(20.dp), SecondaryText)
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            singleLine = true,
            textStyle = BodyTextStyle,
            cursorBrush = ComposerCursorBrush,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) BasicText("搜索对话", style = ComposerPlaceholderStyle)
                    inner()
                }
            }
        )
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(PinpinMotion.quickTween()) +
                scaleIn(PinpinMotion.expressiveSpring(), initialScale = 0.7f),
            exit = fadeOut(PinpinMotion.quickTween()) +
                scaleOut(PinpinMotion.quickTween(), targetScale = 0.7f)
        ) {
            IconTouchButton(PinpinIcon.Close, "清除搜索", onClick = { onQueryChange("") })
        }
    }
}

@Composable
private fun HistoryRow(
    conversation: ConversationEntity,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        targetValue = if (selected) Color(0xFFE5F0FD) else Color.Transparent,
        animationSpec = PinpinMotion.standardTween(),
        label = "history selection"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .pinpinPressFeedback(interactionSource, pressedScale = 0.985f)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onLongClickLabel = "管理对话",
                onLongClick = onLongClick,
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (conversation.isPinned) {
                    PinpinIcon(PinpinIcon.Pin, Modifier.size(13.dp), Accent)
                    Spacer(Modifier.width(6.dp))
                }
                BasicText(
                    conversation.title,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = BodyTextStyle.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(Modifier.height(3.dp))
            Row {
                BasicText(
                    conversation.preview,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = SmallTextStyle
                )
                Spacer(Modifier.width(8.dp))
                BasicText(relativeTime(conversation.updatedAt), style = SmallTextStyle.copy(fontSize = 10.sp))
            }
        }
    }
}

@Composable
private fun DrawerBottomButton(
    modifier: Modifier,
    icon: PinpinIcon,
    label: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .height(52.dp)
            .appleAmbientShadow(shape, 14.dp, 0.07f)
            .clip(shape)
            .background(Color.White)
            .border(0.7.dp, ThinBorder, shape)
            .pinpinPressFeedback(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        PinpinIcon(icon, Modifier.size(19.dp), PrimaryText)
        Spacer(Modifier.width(8.dp))
        BasicText(label, style = BodyTextStyle.copy(fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun BoxScope.RolePickerSheet(
    visible: Boolean,
    roles: List<RoleProfile>,
    selectedRoleId: String,
    onSelect: (RoleProfile) -> Unit,
    onDismiss: () -> Unit,
    onEditCustomRole: () -> Unit
) {
    ModalScrim(onDismiss, visible)
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(PinpinMotion.emphasizedTween()) { it } +
            fadeIn(PinpinMotion.standardTween()),
        exit = slideOutVertically(PinpinMotion.standardTween()) { it } +
            fadeOut(PinpinMotion.quickTween())
    ) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .appleAmbientShadow(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), 26.dp, 0.14f)
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(Panel)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 34.dp, end = 34.dp, top = 28.dp, bottom = 20.dp)
        ) {
            BasicText("切换角色", style = TitleTextStyle.copy(fontSize = 21.sp))
        Spacer(Modifier.height(6.dp))
        BasicText("角色只改变回答方式，不会改动历史消息", style = SmallTextStyle)
        Spacer(Modifier.height(20.dp))
        roles.forEach { role ->
            val selected = role.id == selectedRoleId
            val shape = RoundedCornerShape(22.dp)
            val interactionSource = remember(role.id) { MutableInteractionSource() }
            val roleBackground by animateColorAsState(
                targetValue = if (selected) Color(0xFFE4F0FE) else Color.White,
                animationSpec = PinpinMotion.standardTween(),
                label = "role selection"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(roleBackground)
                    .border(0.7.dp, if (selected) Accent.copy(alpha = 0.3f) else ThinBorder, shape)
                    .pinpinPressFeedback(interactionSource, pressedScale = 0.98f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.RadioButton,
                        onClick = { onSelect(role) }
                    )
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    BasicText(role.name, style = BodyTextStyle.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(2.dp))
                    BasicText(role.description, style = SmallTextStyle)
                }
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(PinpinMotion.quickTween()) +
                        scaleIn(PinpinMotion.expressiveSpring(), initialScale = 0.55f),
                    exit = fadeOut(PinpinMotion.quickTween()) +
                        scaleOut(PinpinMotion.quickTween(), targetScale = 0.65f)
                ) {
                    PinpinIcon(PinpinIcon.Check, Modifier.size(20.dp), Accent)
                }
            }
            Spacer(Modifier.height(9.dp))
        }
            CompactTextButton("编辑自定义角色", Accent, onEditCustomRole)
        }
    }
}

@Composable
private fun BoxScope.HistoryActionSheet(
    visible: Boolean,
    conversation: ConversationEntity,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    ModalScrim(onDismiss, visible)
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(PinpinMotion.emphasizedTween()) { it } +
            fadeIn(PinpinMotion.standardTween()),
        exit = slideOutVertically(PinpinMotion.standardTween()) { it } +
            fadeOut(PinpinMotion.quickTween())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appleAmbientShadow(
                    RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    26.dp,
                    0.14f
                )
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Panel)
                .navigationBarsPadding()
                .padding(start = 34.dp, end = 34.dp, top = 28.dp, bottom = 20.dp)
        ) {
            BasicText(
                conversation.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TitleTextStyle.copy(fontSize = 20.sp)
            )
            Spacer(Modifier.height(18.dp))
            SheetAction(
                icon = PinpinIcon.Pin,
                label = if (conversation.isPinned) "取消置顶" else "置顶对话",
                onClick = onTogglePin
            )
            Spacer(Modifier.height(8.dp))
            SheetAction(PinpinIcon.Edit, "重命名", onRename)
            Spacer(Modifier.height(8.dp))
            SheetAction(PinpinIcon.Trash, "删除对话", onDelete, Destructive)
        }
    }
}

@Composable
private fun BoxScope.MessageActionSheet(
    visible: Boolean,
    target: MessageActionTarget,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    ModalScrim(onDismiss, visible)
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(PinpinMotion.emphasizedTween()) { it } +
            fadeIn(PinpinMotion.standardTween()),
        exit = slideOutVertically(PinpinMotion.standardTween()) { it } +
            fadeOut(PinpinMotion.quickTween())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appleAmbientShadow(
                    RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    26.dp,
                    0.14f
                )
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Panel)
                .navigationBarsPadding()
                .padding(start = 34.dp, end = 34.dp, top = 28.dp, bottom = 20.dp)
        ) {
            BasicText(
                if (target.message.role == MessageEntity.ROLE_USER) "你的消息" else "回复操作",
                style = TitleTextStyle.copy(fontSize = 20.sp)
            )
            Spacer(Modifier.height(5.dp))
            BasicText(
                target.message.content.replace(Regex("\\s+"), " ").take(90),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = SmallTextStyle
            )
            Spacer(Modifier.height(18.dp))
            SheetAction(PinpinIcon.Copy, "复制全文", onCopy)
            if (target.canRegenerate) {
                Spacer(Modifier.height(8.dp))
                SheetAction(PinpinIcon.Retry, "重新生成回复", onRegenerate, Accent)
            }
        }
    }
}

@Composable
private fun BoxScope.RenameConversationDialog(
    visible: Boolean,
    conversation: ConversationEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(conversation.id) { mutableStateOf(conversation.title) }
    var error by remember(conversation.id) { mutableStateOf(false) }
    ModalScrim(onDismiss, visible)
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.Center),
        enter = fadeIn(PinpinMotion.standardTween()) +
            scaleIn(PinpinMotion.expressiveSpring(), initialScale = 0.9f),
        exit = fadeOut(PinpinMotion.quickTween()) +
            scaleOut(PinpinMotion.quickTween(), targetScale = 0.94f)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 30.dp)
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .appleAmbientShadow(RoundedCornerShape(30.dp), 26.dp, 0.15f)
                .clip(RoundedCornerShape(30.dp))
                .background(Panel)
                .padding(32.dp)
        ) {
            BasicText("重命名对话", style = TitleTextStyle.copy(fontSize = 21.sp))
            Spacer(Modifier.height(16.dp))
            val fieldShape = RoundedCornerShape(20.dp)
            BasicTextField(
                value = value,
                onValueChange = {
                    value = it.replace('\n', ' ').take(80)
                    error = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(fieldShape)
                    .background(Color.White)
                    .border(
                        0.8.dp,
                        if (error) Destructive.copy(alpha = 0.7f) else ThinBorder,
                        fieldShape
                    )
                    .padding(horizontal = 22.dp),
                singleLine = true,
                textStyle = BodyTextStyle,
                cursorBrush = ComposerCursorBrush,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (value.isBlank()) error = true else onConfirm(value)
                }),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() }
                }
            )
            AnimatedVisibility(
                visible = error,
                enter = fadeIn(PinpinMotion.quickTween()) +
                    slideInVertically(PinpinMotion.quickTween()) { -it / 3 },
                exit = fadeOut(PinpinMotion.quickTween())
            ) {
                BasicText(
                    "名称不能为空",
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    style = SmallTextStyle.copy(color = Destructive)
                )
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogButton(Modifier.weight(1f), "取消", PrimaryText, Color.White, onDismiss)
                DialogButton(
                    Modifier.weight(1f),
                    "保存",
                    Color.White,
                    Accent,
                    onClick = {
                        if (value.isBlank()) error = true else onConfirm(value)
                    }
                )
            }
        }
    }
}

@Composable
private fun BoxScope.ClearHistoryConfirmation(
    visible: Boolean,
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalScrim(onDismiss, visible)
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.Center),
        enter = fadeIn(PinpinMotion.standardTween()) +
            scaleIn(PinpinMotion.expressiveSpring(), initialScale = 0.9f),
        exit = fadeOut(PinpinMotion.quickTween()) +
            scaleOut(PinpinMotion.quickTween(), targetScale = 0.94f)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 30.dp)
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .appleAmbientShadow(RoundedCornerShape(30.dp), 26.dp, 0.15f)
                .clip(RoundedCornerShape(30.dp))
                .background(Panel)
                .padding(32.dp)
        ) {
            BasicText("清除全部对话？", style = TitleTextStyle.copy(fontSize = 21.sp))
            Spacer(Modifier.height(9.dp))
            BasicText(
                if (count == 0) "当前没有已保存的对话。" else "将永久移除这台设备上的 $count 段对话和其中全部消息。",
                style = BodyTextStyle.copy(color = SecondaryText)
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogButton(Modifier.weight(1f), "取消", PrimaryText, Color.White, onDismiss)
                DialogButton(
                    Modifier.weight(1f),
                    if (count == 0) "知道了" else "全部清除",
                    Color.White,
                    if (count == 0) Accent else Destructive,
                    if (count == 0) onDismiss else onConfirm
                )
            }
        }
    }
}

@Composable
private fun BoxScope.TransientToast(visible: Boolean, text: String) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 96.dp),
        enter = fadeIn(PinpinMotion.quickTween()) +
            slideInVertically(PinpinMotion.expressiveSpring()) { it / 2 },
        exit = fadeOut(PinpinMotion.quickTween()) +
            slideOutVertically(PinpinMotion.quickTween()) { it / 3 }
    ) {
        Row(
            modifier = Modifier
                .appleAmbientShadow(RoundedCornerShape(22.dp), 18.dp, 0.1f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF26343E).copy(alpha = 0.94f))
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PinpinIcon(PinpinIcon.Check, Modifier.size(17.dp), Color.White)
            Spacer(Modifier.width(8.dp))
            BasicText(text, style = SmallTextStyle.copy(color = Color.White, fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun SheetAction(
    icon: PinpinIcon,
    label: String,
    onClick: () -> Unit,
    color: Color = PrimaryText
) {
    val shape = RoundedCornerShape(24.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val safeInset = 24.dp + with(LocalDensity.current) { 2.toDp() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(Color.White)
            .border(0.7.dp, ThinBorder, shape)
            .pinpinPressFeedback(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = safeInset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PinpinIcon(icon, Modifier.size(21.dp), color)
        Spacer(Modifier.width(14.dp))
        BasicText(label, style = BodyTextStyle.copy(color = color, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun BoxScope.DeleteConfirmation(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalScrim(onDismiss, visible)
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.Center),
        enter = fadeIn(PinpinMotion.standardTween()) +
            scaleIn(PinpinMotion.expressiveSpring(), initialScale = 0.9f),
        exit = fadeOut(PinpinMotion.quickTween()) +
            scaleOut(PinpinMotion.quickTween(), targetScale = 0.94f)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 30.dp)
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .appleAmbientShadow(RoundedCornerShape(30.dp), 26.dp, 0.15f)
                .clip(RoundedCornerShape(30.dp))
                .background(Panel)
                .padding(32.dp)
        ) {
            BasicText("删除这段对话？", style = TitleTextStyle.copy(fontSize = 21.sp))
            Spacer(Modifier.height(9.dp))
            BasicText(
                "“${title.take(48)}”及其中的消息会从这台设备移除。",
                style = BodyTextStyle.copy(color = SecondaryText)
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogButton(Modifier.weight(1f), "取消", PrimaryText, Color.White, onDismiss)
                DialogButton(Modifier.weight(1f), "删除", Color.White, Destructive, onConfirm)
            }
        }
    }
}

@Composable
private fun BoxScope.ModalScrim(onDismiss: () -> Unit, visible: Boolean = true) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(PinpinMotion.standardTween()),
        exit = fadeOut(PinpinMotion.quickTween())
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF15202A).copy(alpha = 0.3f))
                .clickable(interactionSource = null, indication = null, onClick = onDismiss)
        )
    }
}

@Composable
private fun DialogButton(
    modifier: Modifier,
    label: String,
    textColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(backgroundColor)
            .border(0.7.dp, if (backgroundColor == Color.White) ThinBorder else Color.Transparent, RoundedCornerShape(26.dp))
            .pinpinPressFeedback(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicText(label, style = BodyTextStyle.copy(color = textColor, fontWeight = FontWeight.SemiBold))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.SettingsPage(
    backdrop: Backdrop,
    settings: ApiSettings,
    connectionTest: ConnectionTestState,
    onBack: () -> Unit,
    onSave: (ApiSettings) -> String?,
    onTest: (ApiSettings) -> Unit,
    onClearTest: () -> Unit,
    onClearHistory: () -> Unit
) {
    var baseUrl by remember(settings) { mutableStateOf(settings.baseUrl) }
    var apiKey by remember(settings) { mutableStateOf(settings.apiKey) }
    var model by remember(settings) { mutableStateOf(settings.model) }
    var temperature by remember(settings) { mutableStateOf(settings.temperature.toString()) }
    var includeTemperature by remember(settings) { mutableStateOf(settings.includeTemperature) }
    var streamResponses by remember(settings) { mutableStateOf(settings.streamResponses) }
    var timeout by remember(settings) { mutableStateOf(settings.timeoutSeconds.toString()) }
    var contextLimit by remember(settings) { mutableStateOf(settings.contextMessageLimit.toString()) }
    var customRoleName by remember(settings) { mutableStateOf(settings.customRoleName) }
    var customRolePrompt by remember(settings) { mutableStateOf(settings.customRolePrompt) }
    var showSecret by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var localSuccess by remember { mutableStateOf<String?>(null) }

    fun edited() {
        localError = null
        localSuccess = null
        onClearTest()
    }

    fun draftOrNull(requireModel: Boolean = true): ApiSettings? {
        localSuccess = null
        val parsedTemperature = temperature.toFloatOrNull()
        val parsedTimeout = timeout.toIntOrNull()
        val parsedContext = contextLimit.toIntOrNull()
        localError = when {
            baseUrl.isBlank() -> "请填写 API 地址"
            requireModel && model.isBlank() -> "请填写模型名称"
            parsedTemperature == null || parsedTemperature !in 0f..2f -> "温度需在 0–2 之间"
            parsedTimeout == null || parsedTimeout !in 15..300 -> "超时时间需在 15–300 秒之间"
            parsedContext == null || parsedContext !in 8..120 -> "上下文消息数需在 8–120 之间"
            else -> null
        }
        if (localError != null) return null
        return settings.copy(
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model,
            temperature = requireNotNull(parsedTemperature),
            includeTemperature = includeTemperature,
            streamResponses = streamResponses,
            timeoutSeconds = requireNotNull(parsedTimeout),
            contextMessageLimit = requireNotNull(parsedContext),
            customRoleName = customRoleName,
            customRolePrompt = customRolePrompt
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.7f))
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(backdrop, PinpinIcon.Back, "返回聊天", onBack)
            Spacer(Modifier.width(14.dp))
            Column {
                BasicText("设置", style = TitleTextStyle.copy(fontSize = 24.sp))
                BasicText("连接、回复与角色", style = SmallTextStyle)
            }
        }
        Spacer(Modifier.height(24.dp))
        SettingsSection(
            title = "连接",
            description = "支持 OpenAI-compatible Chat Completions 接口"
        ) {
            SettingField(
                label = "API 地址",
                value = baseUrl,
                placeholder = "https://example.com/v1",
                keyboardType = KeyboardType.Uri,
                onValueChange = { baseUrl = it.take(500); edited() }
            )
            SettingField(
                label = "API 密钥",
                value = apiKey,
                placeholder = "可留空，用于无需鉴权的本地服务",
                secret = !showSecret,
                keyboardType = KeyboardType.Password,
                trailingIcon = if (showSecret) PinpinIcon.EyeOff else PinpinIcon.Eye,
                trailingDescription = if (showSecret) "隐藏密钥" else "显示密钥",
                onTrailingClick = { showSecret = !showSecret },
                onValueChange = { apiKey = it.take(1000); edited() }
            )
            SettingField(
                label = "模型",
                value = model,
                placeholder = "填写服务提供的模型 ID",
                onValueChange = { model = it.take(160); edited() }
            )
            AnimatedVisibility(
                visible = connectionTest.availableModels.isNotEmpty(),
                enter = fadeIn(PinpinMotion.standardTween()) +
                    slideInVertically(PinpinMotion.standardTween()) { -it / 4 },
                exit = fadeOut(PinpinMotion.quickTween())
            ) {
                Column {
                    BasicText("服务返回的模型", style = SmallTextStyle.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(9.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        connectionTest.availableModels.take(12).forEach { discoveredModel ->
                            ModelChip(
                                label = discoveredModel,
                                selected = model == discoveredModel,
                                onClick = {
                                    model = discoveredModel
                                    localError = null
                                    localSuccess = null
                                }
                            )
                        }
                    }
                    if (connectionTest.availableModels.size > 12) {
                        Spacer(Modifier.height(8.dp))
                        BasicText(
                            "另有 ${connectionTest.availableModels.size - 12} 个模型，可直接输入 ID",
                            style = SmallTextStyle
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
            BasicText(
                "密钥由 Android Keystore 加密后保存在本机。移动端仍需在请求时解密；若密钥权限较高，建议使用你自己的 HTTPS 网关。",
                style = SmallTextStyle
            )
        }
        Spacer(Modifier.height(14.dp))
        SettingsSection(
            title = "回复",
            description = "只影响之后发出的请求"
        ) {
            SettingsToggle(
                title = "流式显示回复",
                description = "关闭后等待服务返回完整内容，兼容不支持 SSE 的服务",
                checked = streamResponses,
                onCheckedChange = { streamResponses = it; edited() }
            )
            Spacer(Modifier.height(12.dp))
            SettingsToggle(
                title = "发送温度参数",
                description = "部分推理模型不接受 temperature，可在这里省略",
                checked = includeTemperature,
                onCheckedChange = { includeTemperature = it; edited() }
            )
            AnimatedVisibility(
                visible = includeTemperature,
                enter = fadeIn(PinpinMotion.standardTween()) +
                    slideInVertically(PinpinMotion.standardTween()) { -it / 4 },
                exit = fadeOut(PinpinMotion.quickTween()) +
                    slideOutVertically(PinpinMotion.quickTween()) { -it / 4 }
            ) {
                Column {
                    Spacer(Modifier.height(18.dp))
                    SettingField(
                        label = "温度 · 0–2",
                        value = temperature,
                        placeholder = "0.7",
                        keyboardType = KeyboardType.Decimal,
                        onValueChange = { temperature = it.take(4); edited() }
                    )
                }
            }
            SettingField(
                label = "无数据超时 · 秒",
                value = timeout,
                placeholder = "90",
                keyboardType = KeyboardType.Number,
                onValueChange = { timeout = it.filter(Char::isDigit).take(3); edited() }
            )
            SettingField(
                label = "上下文消息数 · 8–120",
                value = contextLimit,
                placeholder = "40",
                keyboardType = KeyboardType.Number,
                onValueChange = { contextLimit = it.filter(Char::isDigit).take(3); edited() }
            )
        }
        Spacer(Modifier.height(14.dp))
        SettingsSection(
            title = "自定义角色",
            description = "角色名称和说明会用于自定义角色"
        ) {
            SettingField(
                label = "名称",
                value = customRoleName,
                placeholder = "自定义",
                onValueChange = { customRoleName = it.take(32); edited() }
            )
            SettingField(
                label = "角色说明",
                value = customRolePrompt,
                placeholder = "描述回答方式、语气和边界",
                minLines = 4,
                maxLines = 8,
                onValueChange = { customRolePrompt = it.take(8_000); edited() }
            )
        }
        Spacer(Modifier.height(14.dp))
        SettingsSection(
            title = "本地数据",
            description = "对话只保存在这台设备；清除后无法恢复"
        ) {
            SheetAction(
                icon = PinpinIcon.Trash,
                label = "清除全部对话",
                onClick = onClearHistory,
                color = Destructive
            )
        }
        val feedback = localError ?: localSuccess ?: connectionTest.result
        if (feedback != null) {
            Spacer(Modifier.height(14.dp))
            InlineFeedback(
                feedback,
                localSuccess != null || (localError == null && connectionTest.successful)
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsButton(
                modifier = Modifier.weight(1f),
                label = if (connectionTest.running) "连接中…" else "测试连接",
                filled = false,
                enabled = !connectionTest.running,
                onClick = {
                    localSuccess = null
                    draftOrNull(requireModel = false)?.let(onTest)
                }
            )
            SettingsButton(
                modifier = Modifier.weight(1f),
                label = "保存",
                filled = true,
                onClick = {
                    draftOrNull()?.let {
                        val saveError = onSave(it)
                        if (saveError == null) {
                            localError = null
                            localSuccess = "设置已保存"
                        } else {
                            localSuccess = null
                            localError = saveError
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(30.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appleAmbientShadow(shape, 20.dp, 0.075f)
            .clip(shape)
            .background(Panel.copy(alpha = 0.97f))
            .border(0.7.dp, ThinBorder, shape)
            .padding(32.dp)
    ) {
        BasicText(title, style = TitleTextStyle.copy(fontSize = 19.sp))
        Spacer(Modifier.height(4.dp))
        BasicText(description, style = SmallTextStyle)
        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val trackColor by animateColorAsState(
        targetValue = if (checked) Accent else Color(0xFFD8E0E8),
        animationSpec = PinpinMotion.standardTween(),
        label = "toggle track"
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = PinpinMotion.expressiveSpring(),
        label = "toggle knob"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .pinpinPressFeedback(interactionSource, pressedScale = 0.99f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = BodyTextStyle.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(2.dp))
            BasicText(description, style = SmallTextStyle)
        }
        Spacer(Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(trackColor)
                .semantics { contentDescription = title }
        ) {
            Box(
                Modifier
                    .offset(x = knobOffset, y = 2.dp)
                    .size(24.dp)
                    .appleAmbientShadow(CircleShape, 8.dp, 0.12f)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun ModelChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val safeInset = 20.dp + with(LocalDensity.current) { 2.toDp() }
    val background by animateColorAsState(
        targetValue = if (selected) Color(0xFFE4F0FE) else Color.White,
        animationSpec = PinpinMotion.standardTween(),
        label = "model chip selection"
    )
    Row(
        modifier = Modifier
            .height(40.dp)
            .widthIn(max = 250.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .border(
                0.7.dp,
                if (selected) Accent.copy(alpha = 0.32f) else ThinBorder,
                RoundedCornerShape(20.dp)
            )
            .pinpinPressFeedback(interactionSource, pressedScale = 0.96f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = safeInset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(PinpinMotion.quickTween()) +
                scaleIn(PinpinMotion.expressiveSpring(), initialScale = 0.5f),
            exit = fadeOut(PinpinMotion.quickTween())
        ) {
            Row {
                PinpinIcon(PinpinIcon.Check, Modifier.size(14.dp), Accent)
                Spacer(Modifier.width(6.dp))
            }
        }
        BasicText(
            label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = SmallTextStyle.copy(
                color = if (selected) Accent else PrimaryText,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun SettingField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    trailingIcon: PinpinIcon? = null,
    trailingDescription: String = "",
    onTrailingClick: () -> Unit = {},
    minLines: Int = 1,
    maxLines: Int = 1
) {
    BasicText(label, style = SmallTextStyle.copy(color = PrimaryText, fontWeight = FontWeight.SemiBold))
    Spacer(Modifier.height(8.dp))
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (minLines > 1) 112.dp else 56.dp)
            .clip(shape)
            .background(Color.White)
            .border(0.7.dp, ThinBorder, shape)
            .padding(start = 22.dp, end = if (trailingIcon == null) 22.dp else 4.dp),
        verticalAlignment = if (minLines > 1) Alignment.Top else Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = if (minLines > 1) 112.dp else 56.dp)
                .padding(vertical = if (minLines > 1) 17.dp else 0.dp),
            singleLine = maxLines == 1,
            minLines = minLines,
            maxLines = maxLines,
            textStyle = BodyTextStyle,
            cursorBrush = ComposerCursorBrush,
            visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = if (maxLines == 1) ImeAction.Next else ImeAction.Default),
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = if (minLines > 1) Alignment.TopStart else Alignment.CenterStart
                ) {
                    if (value.isEmpty()) BasicText(placeholder, style = SmallTextStyle.copy(fontSize = 14.sp))
                    inner()
                }
            }
        )
        trailingIcon?.let {
            IconTouchButton(it, trailingDescription, onTrailingClick)
        }
    }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun InlineFeedback(text: String, success: Boolean) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (success) Color(0xFFE7F6EE) else Color(0xFFFFF0EF))
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PinpinIcon(
            if (success) PinpinIcon.Check else PinpinIcon.Info,
            Modifier.size(19.dp),
            if (success) Color(0xFF23865C) else Destructive
        )
        Spacer(Modifier.width(10.dp))
        BasicText(text, modifier = Modifier.weight(1f), style = SmallTextStyle.copy(color = PrimaryText))
    }
}

@Composable
private fun SettingsButton(
    modifier: Modifier,
    label: String,
    filled: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val background = if (filled) Accent else Color.White
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(54.dp)
            .appleAmbientShadow(RoundedCornerShape(27.dp), 16.dp, if (enabled) 0.075f else 0f)
            .clip(RoundedCornerShape(27.dp))
            .background(background.copy(alpha = if (enabled) 1f else 0.55f))
            .border(0.7.dp, if (filled) Color.Transparent else ThinBorder, RoundedCornerShape(27.dp))
            .pinpinPressFeedback(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label,
            style = BodyTextStyle.copy(
                color = if (filled) Color.White else PrimaryText,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun SolidIconButton(icon: PinpinIcon, description: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(48.dp)
            .appleAmbientShadow(CircleShape, 14.dp, 0.07f)
            .clip(CircleShape)
            .background(Color.White)
            .border(0.7.dp, ThinBorder, CircleShape)
            .pinpinPressFeedback(interactionSource, pressedScale = 0.9f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        PinpinIcon(icon, Modifier.size(20.dp), PrimaryText)
    }
}

@Composable
private fun IconTouchButton(
    icon: PinpinIcon,
    description: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .pinpinPressFeedback(interactionSource, pressedScale = 0.88f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        PinpinIcon(icon, Modifier.size(19.dp), SecondaryText)
    }
}

@Composable
private fun CompactTextButton(label: String, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .pinpinPressFeedback(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(label, style = BodyTextStyle.copy(color = color, fontWeight = FontWeight.SemiBold))
    }
}

private fun historySections(
    conversations: List<ConversationEntity>,
    query: String
): List<HistorySection> {
    val needle = query.trim()
    val filtered = conversations
    if (filtered.isEmpty()) return emptyList()
    if (needle.isNotEmpty()) {
        return listOf(HistorySection("search", "搜索结果 · ${filtered.size}", filtered))
    }

    val startToday = Calendar.getInstance().run {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }
    val startYesterday = startToday - DateUtils.DAY_IN_MILLIS
    val startLastWeek = startToday - DateUtils.DAY_IN_MILLIS * 7
    val pinned = filtered.filter { it.isPinned }
    val regular = filtered.filterNot { it.isPinned }

    return buildList {
        if (pinned.isNotEmpty()) add(HistorySection("pinned", "置顶", pinned))
        regular.filter { it.updatedAt >= startToday }
            .takeIf { it.isNotEmpty() }
            ?.let { add(HistorySection("today", "今天", it)) }
        regular.filter { it.updatedAt in startYesterday until startToday }
            .takeIf { it.isNotEmpty() }
            ?.let { add(HistorySection("yesterday", "昨天", it)) }
        regular.filter { it.updatedAt in startLastWeek until startYesterday }
            .takeIf { it.isNotEmpty() }
            ?.let { add(HistorySection("week", "最近 7 天", it)) }
        regular.filter { it.updatedAt < startLastWeek }
            .takeIf { it.isNotEmpty() }
            ?.let { add(HistorySection("older", "更早", it)) }
    }
}

private fun parseChatTextBlocks(content: String): List<ChatTextBlock> {
    val blocks = mutableListOf<ChatTextBlock>()
    val buffer = StringBuilder()
    var inCode = false
    var language = ""

    fun flush() {
        if (buffer.isEmpty()) return
        blocks += ChatTextBlock(
            text = buffer.toString().trimEnd('\n'),
            code = inCode,
            language = if (inCode) language else ""
        )
        buffer.clear()
    }

    content.lineSequence().forEach { line ->
        if (line.startsWith("```")) {
            flush()
            if (inCode) {
                inCode = false
                language = ""
            } else {
                inCode = true
                language = line.removePrefix("```").trim().take(24)
            }
        } else {
            if (buffer.isNotEmpty()) buffer.append('\n')
            buffer.append(line)
        }
    }
    flush()
    return blocks.ifEmpty { listOf(ChatTextBlock(content, code = false)) }
}

private fun relativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    return when {
        now - timestamp < DateUtils.MINUTE_IN_MILLIS -> "刚刚"
        now - timestamp < DateUtils.HOUR_IN_MILLIS -> "${(now - timestamp) / DateUtils.MINUTE_IN_MILLIS} 分钟"
        now - timestamp < DateUtils.DAY_IN_MILLIS -> "${(now - timestamp) / DateUtils.HOUR_IN_MILLIS} 小时"
        else -> DateUtils.getRelativeTimeSpanString(
            timestamp,
            now,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
}

private enum class PinpinIcon {
    Menu,
    More,
    Send,
    Stop,
    Search,
    Add,
    Pin,
    Trash,
    Role,
    Settings,
    Back,
    Check,
    Close,
    Eye,
    EyeOff,
    Chat,
    Info,
    Retry,
    Edit,
    Copy
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
                listOf(0.28f, 0.5f, 0.72f).forEach { y ->
                    drawLine(color, Offset(w * 0.22f, h * y), Offset(w * 0.78f, h * y), strokeWidth, StrokeCap.Round)
                }
            }
            PinpinIcon.More -> {
                listOf(0.27f, 0.5f, 0.73f).forEach { x ->
                    drawCircle(color, w * 0.075f, Offset(w * x, h * 0.5f))
                }
            }
            PinpinIcon.Send -> {
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.51f)
                    lineTo(w * 0.79f, h * 0.2f)
                    lineTo(w * 0.58f, h * 0.81f)
                    lineTo(w * 0.47f, h * 0.57f)
                    close()
                }
                drawPath(path, color)
                drawLine(Accent, Offset(w * 0.47f, h * 0.57f), Offset(w * 0.76f, h * 0.23f), strokeWidth * 0.7f, StrokeCap.Round)
            }
            PinpinIcon.Stop -> drawRoundRect(
                color,
                topLeft = Offset(w * 0.31f, h * 0.31f),
                size = androidx.compose.ui.geometry.Size(w * 0.38f, h * 0.38f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f)
            )
            PinpinIcon.Search -> {
                drawCircle(color, w * 0.27f, Offset(w * 0.43f, h * 0.43f), style = Stroke(strokeWidth))
                drawLine(color, Offset(w * 0.62f, h * 0.62f), Offset(w * 0.81f, h * 0.81f), strokeWidth, StrokeCap.Round)
            }
            PinpinIcon.Add -> {
                drawLine(color, Offset(w * 0.5f, h * 0.22f), Offset(w * 0.5f, h * 0.78f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.22f, h * 0.5f), Offset(w * 0.78f, h * 0.5f), strokeWidth, StrokeCap.Round)
            }
            PinpinIcon.Pin -> {
                val path = Path().apply {
                    moveTo(w * 0.35f, h * 0.18f)
                    lineTo(w * 0.68f, h * 0.18f)
                    lineTo(w * 0.62f, h * 0.43f)
                    lineTo(w * 0.76f, h * 0.58f)
                    lineTo(w * 0.25f, h * 0.58f)
                    lineTo(w * 0.39f, h * 0.43f)
                    close()
                }
                drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                drawLine(color, Offset(w * 0.5f, h * 0.58f), Offset(w * 0.5f, h * 0.86f), strokeWidth, StrokeCap.Round)
            }
            PinpinIcon.Trash -> {
                drawRoundRect(color, Offset(w * 0.3f, h * 0.31f), androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.5f), androidx.compose.ui.geometry.CornerRadius(w * 0.05f), style = Stroke(strokeWidth))
                drawLine(color, Offset(w * 0.24f, h * 0.25f), Offset(w * 0.76f, h * 0.25f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.41f, h * 0.17f), Offset(w * 0.59f, h * 0.17f), strokeWidth, StrokeCap.Round)
            }
            PinpinIcon.Role -> {
                drawCircle(color, w * 0.16f, Offset(w * 0.5f, h * 0.34f), style = Stroke(strokeWidth))
                drawArc(color, 205f, 130f, false, Offset(w * 0.24f, h * 0.48f), androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.38f), style = Stroke(strokeWidth, cap = StrokeCap.Round))
            }
            PinpinIcon.Settings -> {
                listOf(0.3f, 0.5f, 0.7f).forEachIndexed { index, y ->
                    drawLine(color, Offset(w * 0.18f, h * y), Offset(w * 0.82f, h * y), strokeWidth, StrokeCap.Round)
                    val x = listOf(0.39f, 0.65f, 0.46f)[index]
                    drawCircle(color, w * 0.075f, Offset(w * x, h * y), style = Stroke(strokeWidth))
                }
            }
            PinpinIcon.Back -> {
                drawLine(color, Offset(w * 0.65f, h * 0.2f), Offset(w * 0.35f, h * 0.5f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.35f, h * 0.5f), Offset(w * 0.65f, h * 0.8f), strokeWidth, StrokeCap.Round)
            }
            PinpinIcon.Check -> {
                drawLine(color, Offset(w * 0.2f, h * 0.52f), Offset(w * 0.42f, h * 0.72f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.42f, h * 0.72f), Offset(w * 0.81f, h * 0.29f), strokeWidth, StrokeCap.Round)
            }
            PinpinIcon.Close -> {
                drawLine(color, Offset(w * 0.29f, h * 0.29f), Offset(w * 0.71f, h * 0.71f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.71f, h * 0.29f), Offset(w * 0.29f, h * 0.71f), strokeWidth, StrokeCap.Round)
            }
            PinpinIcon.Eye, PinpinIcon.EyeOff -> {
                val path = Path().apply {
                    moveTo(w * 0.14f, h * 0.5f)
                    quadraticBezierTo(w * 0.5f, h * 0.15f, w * 0.86f, h * 0.5f)
                    quadraticBezierTo(w * 0.5f, h * 0.85f, w * 0.14f, h * 0.5f)
                }
                drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                drawCircle(color, w * 0.1f, Offset(w * 0.5f, h * 0.5f), style = Stroke(strokeWidth))
                if (icon == PinpinIcon.EyeOff) {
                    drawLine(color, Offset(w * 0.2f, h * 0.2f), Offset(w * 0.8f, h * 0.8f), strokeWidth, StrokeCap.Round)
                }
            }
            PinpinIcon.Chat -> {
                drawRoundRect(color, Offset(w * 0.15f, h * 0.2f), androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.53f), androidx.compose.ui.geometry.CornerRadius(w * 0.16f), style = Stroke(strokeWidth))
                drawLine(color, Offset(w * 0.38f, h * 0.72f), Offset(w * 0.29f, h * 0.86f), strokeWidth, StrokeCap.Round)
            }
            PinpinIcon.Info -> {
                drawCircle(color, w * 0.35f, Offset(w * 0.5f, h * 0.5f), style = Stroke(strokeWidth))
                drawCircle(color, w * 0.045f, Offset(w * 0.5f, h * 0.35f))
                drawLine(color, Offset(w * 0.5f, h * 0.48f), Offset(w * 0.5f, h * 0.68f), strokeWidth, StrokeCap.Round)
            }
            PinpinIcon.Retry -> {
                drawArc(
                    color = color,
                    startAngle = 38f,
                    sweepAngle = 286f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.18f),
                    size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.64f),
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )
                val arrow = Path().apply {
                    moveTo(w * 0.72f, h * 0.18f)
                    lineTo(w * 0.84f, h * 0.37f)
                    lineTo(w * 0.62f, h * 0.36f)
                }
                drawPath(arrow, color)
            }
            PinpinIcon.Edit -> {
                drawLine(
                    color,
                    Offset(w * 0.25f, h * 0.73f),
                    Offset(w * 0.68f, h * 0.3f),
                    strokeWidth * 1.25f,
                    StrokeCap.Round
                )
                drawLine(
                    color,
                    Offset(w * 0.62f, h * 0.24f),
                    Offset(w * 0.75f, h * 0.37f),
                    strokeWidth * 1.25f,
                    StrokeCap.Round
                )
                drawLine(
                    color,
                    Offset(w * 0.23f, h * 0.77f),
                    Offset(w * 0.38f, h * 0.73f),
                    strokeWidth,
                    StrokeCap.Round
                )
            }
            PinpinIcon.Copy -> {
                drawRoundRect(
                    color,
                    Offset(w * 0.31f, h * 0.27f),
                    androidx.compose.ui.geometry.Size(w * 0.48f, h * 0.53f),
                    androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
                    style = Stroke(strokeWidth)
                )
                drawLine(color, Offset(w * 0.22f, h * 0.67f), Offset(w * 0.22f, h * 0.28f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.22f, h * 0.28f), Offset(w * 0.62f, h * 0.28f), strokeWidth, StrokeCap.Round)
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
