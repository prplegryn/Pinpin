package com.prplegryn.pinpin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prplegryn.pinpin.data.ApiSettings
import com.prplegryn.pinpin.data.ChatRepository
import com.prplegryn.pinpin.data.ConversationEntity
import com.prplegryn.pinpin.data.MessageEntity
import com.prplegryn.pinpin.data.PinpinDatabase
import com.prplegryn.pinpin.data.RoleProfile
import com.prplegryn.pinpin.data.SettingsStore
import com.prplegryn.pinpin.network.ApiClientException
import com.prplegryn.pinpin.network.OpenAiCompatibleClient
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StreamingReply(
    val conversationId: Long? = null,
    val text: String = "",
    val active: Boolean = false
)

data class ConnectionTestState(
    val running: Boolean = false,
    val result: String? = null,
    val successful: Boolean = false,
    val availableModels: List<String> = emptyList()
)

data class HistorySearchState(
    val query: String = "",
    val results: List<ConversationEntity> = emptyList()
)

@OptIn(
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
    kotlinx.coroutines.FlowPreview::class
)
class PinpinViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = ChatRepository(PinpinDatabase.get(application).pinpinDao())
    private val settingsStore = SettingsStore(application)
    private val apiClient = OpenAiCompatibleClient()
    private val connectionTestClient = OpenAiCompatibleClient()

    private val mutableConversationId = MutableStateFlow<Long?>(
        savedStateHandle[CURRENT_CONVERSATION_KEY]
    )
    val currentConversationId: StateFlow<Long?> = mutableConversationId.asStateFlow()

    val settings: StateFlow<ApiSettings> = settingsStore.settings

    val composerDraft: StateFlow<String> = savedStateHandle.getStateFlow(COMPOSER_DRAFT_KEY, "")

    val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val mutableHistoryQuery = MutableStateFlow("")
    val historyQuery: StateFlow<String> = mutableHistoryQuery.asStateFlow()
    val historySearch: StateFlow<HistorySearchState> = mutableHistoryQuery
        .debounce(HISTORY_SEARCH_DEBOUNCE_MILLIS)
        .flatMapLatest { query ->
            val source = if (query.isBlank()) repository.conversations
            else repository.searchConversations(query)
            source.map { HistorySearchState(query = query, results = it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorySearchState())

    val messages: StateFlow<List<MessageEntity>> = mutableConversationId
        .flatMapLatest { conversationId ->
            if (conversationId == null) flowOf(emptyList())
            else repository.messages(conversationId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentConversation: StateFlow<ConversationEntity?> = combine(
        conversations,
        mutableConversationId
    ) { all, id -> all.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val roleOverride = MutableStateFlow<Pair<Long?, String>?>(null)

    val currentRoleId: StateFlow<String> = combine(
        currentConversation,
        settings,
        roleOverride
    ) { conversation, currentSettings, override ->
        if (override != null && override.first == conversation?.id) {
            override.second
        } else {
            conversation?.roleId ?: currentSettings.activeRoleId
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        settings.value.activeRoleId
    )

    private val mutableStreamingReply = MutableStateFlow(StreamingReply())
    val streamingReply: StateFlow<StreamingReply> = mutableStreamingReply.asStateFlow()
    val isStreaming: StateFlow<Boolean> = mutableStreamingReply
        .map { it.active }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val mutableNotice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = mutableNotice.asStateFlow()

    private val mutableCanRetry = MutableStateFlow(false)
    val canRetry: StateFlow<Boolean> = mutableCanRetry.asStateFlow()

    private val mutableNeedsSettings = MutableStateFlow(false)
    val needsSettings: StateFlow<Boolean> = mutableNeedsSettings.asStateFlow()

    private val mutableConnectionTest = MutableStateFlow(ConnectionTestState())
    val connectionTest: StateFlow<ConnectionTestState> = mutableConnectionTest.asStateFlow()

    private enum class CancelReason { None, User, Navigation, Delete }

    private class RequestControl {
        @Volatile
        var cancelReason: CancelReason = CancelReason.None
    }

    @Volatile
    private var activeRequest: RequestControl? = null
    private var connectionTestGeneration = 0L
    private var selectionTouched = false
    private var lastFailedConversationId: Long? = null

    init {
        viewModelScope.launch {
            val restoredId = mutableConversationId.value
            if (restoredId != null) {
                val restoredExists = repository.conversation(restoredId) != null
                if (selectionTouched) return@launch
                if (restoredExists) return@launch
                setConversationId(null)
            }
            val initialId = repository.firstConversationId()
            if (!selectionTouched && mutableConversationId.value == null) {
                setConversationId(initialId)
            }
        }
        viewModelScope.launch {
            combine(mutableConversationId, messages) { conversationId, currentMessages ->
                conversationId to currentMessages.lastOrNull { it.conversationId == conversationId }
            }.collectLatest { (conversationId, lastMessage) ->
                val replyCanBeRetried = lastMessage?.status == MessageEntity.STATUS_ERROR ||
                    lastMessage?.role == MessageEntity.ROLE_USER
                if (activeRequest == null && replyCanBeRetried) {
                    lastFailedConversationId = conversationId
                    mutableCanRetry.value = true
                    mutableNeedsSettings.value = lastMessage.errorCode
                        ?.let { it in SETTINGS_ERROR_CODES } == true
                    if (mutableNotice.value == null) {
                        mutableNotice.value = lastMessage.error ?: "上次回复未完成"
                    }
                }
            }
        }
    }

    fun clearNotice() {
        mutableNotice.value = null
        mutableNeedsSettings.value = false
    }

    fun updateComposerDraft(value: String) {
        savedStateHandle[COMPOSER_DRAFT_KEY] = value.take(MAX_DRAFT_CHARS)
    }

    fun updateHistoryQuery(value: String) {
        mutableHistoryQuery.value = value.replace('\n', ' ').take(MAX_HISTORY_QUERY_CHARS)
    }

    fun newConversation() {
        cancelActiveRequest(CancelReason.Navigation)
        selectionTouched = true
        setConversationId(null)
        updateComposerDraft("")
        roleOverride.value = null
        mutableStreamingReply.value = StreamingReply()
        lastFailedConversationId = null
        mutableNotice.value = null
        mutableCanRetry.value = false
        mutableNeedsSettings.value = false
    }

    fun selectConversation(conversationId: Long) {
        if (conversationId == mutableConversationId.value) return
        cancelActiveRequest(CancelReason.Navigation)
        selectionTouched = true
        setConversationId(conversationId)
        updateComposerDraft("")
        roleOverride.value = null
        mutableStreamingReply.value = StreamingReply()
        lastFailedConversationId = null
        mutableNotice.value = null
        mutableCanRetry.value = false
        mutableNeedsSettings.value = false
    }

    fun setPinned(conversation: ConversationEntity) {
        viewModelScope.launch {
            runCatching { repository.setPinned(conversation.id, !conversation.isPinned) }
                .onFailure { mutableNotice.value = "置顶状态保存失败，请重试" }
        }
    }

    fun renameConversation(conversationId: Long, title: String): Boolean {
        val normalized = title.replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty()) {
            mutableNotice.value = "对话名称不能为空"
            return false
        }
        viewModelScope.launch {
            runCatching { repository.rename(conversationId, normalized) }
                .onFailure { mutableNotice.value = "对话名称保存失败，请重试" }
        }
        return true
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            val deletingCurrent = mutableConversationId.value == conversationId
            if (deletingCurrent) {
                cancelActiveRequest(CancelReason.Delete)
                selectionTouched = true
                setConversationId(null)
                updateComposerDraft("")
                roleOverride.value = null
            }
            val deleted = runCatching { repository.delete(conversationId) }.isSuccess
            if (!deleted) {
                if (deletingCurrent && repository.conversation(conversationId) != null) {
                    setConversationId(conversationId)
                }
                mutableNotice.value = "删除对话失败，请重试"
                return@launch
            }
            if (mutableConversationId.value == null) {
                setConversationId(repository.firstConversationId())
            }
            if (deletingCurrent || lastFailedConversationId == conversationId) {
                lastFailedConversationId = null
                mutableCanRetry.value = false
                mutableNeedsSettings.value = false
            }
        }
    }

    fun clearAllConversations() {
        val previousConversationId = mutableConversationId.value
        cancelActiveRequest(CancelReason.Delete)
        selectionTouched = true
        setConversationId(null)
        updateComposerDraft("")
        roleOverride.value = null
        mutableStreamingReply.value = StreamingReply()
        lastFailedConversationId = null
        mutableNotice.value = null
        mutableCanRetry.value = false
        mutableNeedsSettings.value = false
        viewModelScope.launch {
            val cleared = runCatching { repository.deleteAll() }.isSuccess
            if (!cleared) {
                if (
                    previousConversationId != null &&
                    repository.conversation(previousConversationId) != null
                ) {
                    setConversationId(previousConversationId)
                }
                mutableNotice.value = "清除对话失败，请重试"
            }
        }
    }

    fun selectRole(roleId: String) {
        val conversationId = mutableConversationId.value
        roleOverride.value = conversationId to roleId
        runCatching { settingsStore.updateActiveRole(roleId) }
            .onFailure { mutableNotice.value = "角色保存失败，请重试" }
        conversationId?.let { id ->
            viewModelScope.launch {
                runCatching { repository.setRole(id, roleId) }
                    .onFailure { mutableNotice.value = "会话角色保存失败，请重试" }
            }
        }
    }

    fun saveSettings(value: ApiSettings): String? {
        val error = apiClient.validate(value)
        if (error != null) {
            return error
        }
        runCatching { settingsStore.save(value) }.onFailure {
            return "设置无法安全保存，请重新打开应用后重试"
        }
        mutableNeedsSettings.value = false
        return null
    }

    fun clearConnectionTest() {
        connectionTestGeneration += 1
        connectionTestClient.cancel()
        mutableConnectionTest.value = ConnectionTestState()
    }

    fun testSettings(value: ApiSettings) {
        val error = apiClient.validateEndpoint(value)
        if (error != null) {
            mutableConnectionTest.value = ConnectionTestState(result = error)
            return
        }
        if (mutableConnectionTest.value.running) return
        val generation = ++connectionTestGeneration
        mutableConnectionTest.value = ConnectionTestState(running = true)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { connectionTestClient.testConnection(value) }
            }
            if (generation == connectionTestGeneration) {
                mutableConnectionTest.value = result.fold(
                    onSuccess = {
                        ConnectionTestState(
                            result = it.message,
                            successful = true,
                            availableModels = it.models
                        )
                    },
                    onFailure = {
                        ConnectionTestState(result = readableError(it), successful = false)
                    }
                )
            }
        }
    }

    fun send(text: String): Boolean {
        val normalizedText = text.trim()
        if (normalizedText.isEmpty() || activeRequest != null) return false
        val currentSettings = settings.value
        apiClient.validate(currentSettings)?.let { error ->
            mutableNotice.value = error
            mutableCanRetry.value = false
            mutableNeedsSettings.value = true
            return false
        }
        selectionTouched = true
        val control = RequestControl()
        activeRequest = control
        mutableStreamingReply.value = StreamingReply(
            conversationId = mutableConversationId.value,
            active = true
        )
        val selectedRoleId = selectedRoleIdForCurrentConversation()
        val targetConversationId = mutableConversationId.value
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val firstMessage = MessageEntity(
                    conversationId = targetConversationId ?: 0,
                    role = MessageEntity.ROLE_USER,
                    content = normalizedText,
                    createdAt = now
                )
                val conversationId = targetConversationId ?: repository.create(
                    conversation = ConversationEntity(
                        title = titleFrom(normalizedText),
                        preview = normalizedText.take(PREVIEW_LIMIT),
                        createdAt = now,
                        updatedAt = now,
                        roleId = selectedRoleId
                    ),
                    firstMessage = firstMessage
                ).also {
                    if (activeRequest === control) {
                        setConversationId(it)
                        roleOverride.value = it to selectedRoleId
                    }
                }
                if (targetConversationId != null) {
                    repository.append(
                        message = firstMessage,
                        preview = normalizedText.take(PREVIEW_LIMIT),
                        updatedAt = now
                    )
                }
                if (control.cancelReason == CancelReason.Delete) return@launch
                completeConversation(conversationId, currentSettings, selectedRoleId, control)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (activeRequest === control) {
                    updateComposerDraft(normalizedText)
                    mutableNotice.value = "消息无法保存，请稍后重试"
                }
            } finally {
                if (activeRequest === control) {
                    activeRequest = null
                    mutableStreamingReply.value = StreamingReply()
                }
            }
        }
        return true
    }

    fun retryLastReply() {
        val conversationId = mutableConversationId.value ?: return
        if (activeRequest != null || lastFailedConversationId != conversationId) return
        val currentSettings = settings.value
        apiClient.validate(currentSettings)?.let { error ->
            mutableNotice.value = error
            mutableNeedsSettings.value = true
            return
        }
        val control = RequestControl()
        activeRequest = control
        mutableStreamingReply.value = StreamingReply(conversationId = conversationId, active = true)
        val selectedRoleId = selectedRoleIdForCurrentConversation()
        viewModelScope.launch {
            try {
                repository.deleteFailedReplies(conversationId)
                completeConversation(conversationId, currentSettings, selectedRoleId, control)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (activeRequest === control) {
                    mutableNotice.value = "本地记录无法更新，请稍后重试"
                }
            } finally {
                if (activeRequest === control) {
                    activeRequest = null
                    mutableStreamingReply.value = StreamingReply()
                }
            }
        }
    }

    fun regenerateReply(messageId: Long) {
        val conversationId = mutableConversationId.value ?: return
        if (activeRequest != null) return
        val currentSettings = settings.value
        apiClient.validate(currentSettings)?.let { error ->
            mutableNotice.value = error
            mutableNeedsSettings.value = true
            return
        }
        val control = RequestControl()
        activeRequest = control
        mutableStreamingReply.value = StreamingReply(conversationId = conversationId, active = true)
        val selectedRoleId = selectedRoleIdForCurrentConversation()
        viewModelScope.launch {
            try {
                if (!repository.removeLastAssistantReply(conversationId, messageId)) {
                    mutableNotice.value = "只能重新生成当前对话的最后一条回复"
                    mutableCanRetry.value = false
                    return@launch
                }
                completeConversation(conversationId, currentSettings, selectedRoleId, control)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (activeRequest === control) {
                    mutableNotice.value = "无法重新生成回复，请稍后重试"
                    mutableCanRetry.value = true
                }
            } finally {
                if (activeRequest === control) {
                    activeRequest = null
                    mutableStreamingReply.value = StreamingReply()
                }
            }
        }
    }

    fun stopReply() {
        val control = activeRequest ?: return
        control.cancelReason = CancelReason.User
        apiClient.cancel()
    }

    private suspend fun completeConversation(
        conversationId: Long,
        currentSettings: ApiSettings,
        selectedRoleId: String,
        control: RequestControl
    ) {
        if (activeRequest === control) {
            lastFailedConversationId = null
            mutableNotice.value = null
            mutableCanRetry.value = false
            mutableNeedsSettings.value = false
            mutableStreamingReply.value = StreamingReply(conversationId = conversationId, active = true)
        }

        if (repository.conversation(conversationId) == null || control.cancelReason == CancelReason.Delete) {
            if (activeRequest === control) {
                activeRequest = null
                mutableStreamingReply.value = StreamingReply()
            }
            return
        }
        val role = RoleProfile.all(currentSettings)
            .firstOrNull { it.id == selectedRoleId }
            ?: RoleProfile.all(currentSettings).first()
        val context = trimContext(
            repository.context(
                conversationId,
                currentSettings.contextMessageLimit
            )
        )
        var partialText = ""

        try {
            if (control.cancelReason != CancelReason.None) {
                throw IOException("request cancelled before connection")
            }
            val answer = withContext(Dispatchers.IO) {
                apiClient.streamCompletion(
                    settings = currentSettings,
                    systemPrompt = role.prompt,
                    messages = context,
                    onText = { partial ->
                        partialText = partial.take(MAX_ASSISTANT_CHARS)
                        if (activeRequest === control) {
                            mutableStreamingReply.value = StreamingReply(
                                conversationId = conversationId,
                                text = partialText,
                                active = true
                            )
                        }
                    }
                )
            }.trim().take(MAX_ASSISTANT_CHARS)
            if (control.cancelReason != CancelReason.None) {
                throw IOException("request cancelled")
            }
            if (answer.isEmpty()) throw ApiClientException("服务没有返回文字内容")
            val completedAt = System.currentTimeMillis()
            repository.append(
                message = MessageEntity(
                    conversationId = conversationId,
                    role = MessageEntity.ROLE_ASSISTANT,
                    content = answer,
                    createdAt = completedAt
                ),
                preview = answer.take(PREVIEW_LIMIT),
                updatedAt = completedAt
            )
            if (activeRequest === control) mutableCanRetry.value = false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val partial = partialText.trim()
            val cancelledByUser = control.cancelReason == CancelReason.User
            val navigatedAway = control.cancelReason == CancelReason.Navigation
            val deletingConversation = control.cancelReason == CancelReason.Delete
            if (cancelledByUser || navigatedAway || deletingConversation) {
                if (partial.isNotEmpty() && !deletingConversation) {
                    val stoppedAt = System.currentTimeMillis()
                    repository.append(
                        message = MessageEntity(
                            conversationId = conversationId,
                            role = MessageEntity.ROLE_ASSISTANT,
                            content = partial,
                            createdAt = stoppedAt,
                            status = MessageEntity.STATUS_STOPPED
                        ),
                        preview = partial.take(PREVIEW_LIMIT),
                        updatedAt = stoppedAt
                    )
                }
                if (cancelledByUser && activeRequest === control) {
                    mutableNotice.value = null
                    mutableCanRetry.value = false
                    mutableNeedsSettings.value = false
                }
            } else {
                repository.insert(
                    MessageEntity(
                        conversationId = conversationId,
                        role = MessageEntity.ROLE_ASSISTANT,
                        content = partial.ifEmpty { "没有收到回复" },
                        createdAt = System.currentTimeMillis(),
                        status = MessageEntity.STATUS_ERROR,
                        error = readableError(error),
                        errorCode = (error as? ApiClientException)?.statusCode
                    )
                )
                if (activeRequest === control) {
                    lastFailedConversationId = conversationId
                    mutableNotice.value = readableError(error)
                    mutableCanRetry.value = true
                    mutableNeedsSettings.value = (error as? ApiClientException)?.statusCode
                        ?.let { it in SETTINGS_ERROR_CODES } == true
                }
            }
        } finally {
            if (activeRequest === control) {
                activeRequest = null
                mutableStreamingReply.value = StreamingReply()
            }
        }
    }

    private fun cancelActiveRequest(reason: CancelReason) {
        val control = activeRequest ?: return
        control.cancelReason = reason
        activeRequest = null
        mutableStreamingReply.value = StreamingReply()
        apiClient.cancel()
    }

    private fun setConversationId(value: Long?) {
        mutableConversationId.value = value
        savedStateHandle[CURRENT_CONVERSATION_KEY] = value
    }

    private fun selectedRoleIdForCurrentConversation(): String {
        val conversationId = mutableConversationId.value
        return roleOverride.value
            ?.takeIf { it.first == conversationId }
            ?.second
            ?: currentRoleId.value
    }

    private fun trimContext(messages: List<MessageEntity>): List<MessageEntity> {
        var remaining = MAX_CONTEXT_CHARS
        val kept = ArrayDeque<MessageEntity>()
        for (message in messages.asReversed()) {
            if (remaining <= 0) break
            if (message.content.length <= remaining) {
                kept.addFirst(message)
                remaining -= message.content.length
            } else if (kept.isEmpty()) {
                kept.addFirst(message.copy(content = message.content.takeLast(remaining)))
                remaining = 0
            }
        }
        return kept.toList()
    }

    private fun readableError(error: Throwable): String = when (error) {
        is ApiClientException -> error.message
        is java.net.SocketTimeoutException -> "连接超时，请检查网络或调高超时时间"
        is java.net.UnknownHostException -> "无法解析服务地址，请检查网络和 API 地址"
        is javax.net.ssl.SSLException -> "安全连接失败，请检查服务证书"
        is IOException -> "连接中断，请稍后重试"
        else -> error.message?.takeIf { it.isNotBlank() }?.take(200) ?: "请求失败"
    }

    private fun titleFrom(message: String): String = message
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(TITLE_LIMIT)

    override fun onCleared() {
        apiClient.cancel()
        connectionTestClient.cancel()
        super.onCleared()
    }

    private companion object {
        const val TITLE_LIMIT = 28
        const val PREVIEW_LIMIT = 80
        const val MAX_CONTEXT_CHARS = 120_000
        const val MAX_ASSISTANT_CHARS = 200_000
        const val MAX_DRAFT_CHARS = 8_000
        const val MAX_HISTORY_QUERY_CHARS = 80
        const val HISTORY_SEARCH_DEBOUNCE_MILLIS = 180L
        const val COMPOSER_DRAFT_KEY = "composer_draft"
        const val CURRENT_CONVERSATION_KEY = "current_conversation"
        val SETTINGS_ERROR_CODES = setOf(400, 401, 403, 404, 422)
    }
}
