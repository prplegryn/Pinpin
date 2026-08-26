package com.prplegryn.pinpin.data

import kotlinx.coroutines.flow.Flow

/**
 * Local conversation boundary. UI state never needs to know which writes must be atomic,
 * and future sync support can replace this implementation without rewriting the screen.
 */
class ChatRepository(private val dao: PinpinDao) {
    val conversations: Flow<List<ConversationEntity>> = dao.observeConversations()

    fun messages(conversationId: Long): Flow<List<MessageEntity>> =
        dao.observeMessages(conversationId)

    suspend fun conversation(conversationId: Long): ConversationEntity? =
        dao.getConversation(conversationId)

    suspend fun firstConversationId(): Long? = dao.getFirstConversationId()

    suspend fun context(conversationId: Long, limit: Int): List<MessageEntity> =
        dao.getContextMessages(conversationId, limit)

    suspend fun create(
        conversation: ConversationEntity,
        firstMessage: MessageEntity
    ): Long = dao.createConversationWithFirstMessage(conversation, firstMessage)

    suspend fun append(message: MessageEntity, preview: String, updatedAt: Long) {
        dao.appendMessageAndTouch(message, preview, updatedAt)
    }

    suspend fun insert(message: MessageEntity) = dao.insertMessage(message)

    suspend fun setPinned(conversationId: Long, pinned: Boolean) =
        dao.setPinned(conversationId, pinned)

    suspend fun setRole(conversationId: Long, roleId: String) =
        dao.setRole(conversationId, roleId)

    suspend fun rename(conversationId: Long, title: String) {
        dao.renameConversation(
            conversationId = conversationId,
            title = title.replace(Regex("\\s+"), " ").trim().take(MAX_TITLE_CHARS),
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun delete(conversationId: Long) = dao.deleteConversation(conversationId)

    suspend fun deleteAll() = dao.deleteAllConversations()

    suspend fun deleteFailedReplies(conversationId: Long) =
        dao.deleteFailedReplies(conversationId)

    suspend fun removeLastAssistantReply(conversationId: Long, messageId: Long): Boolean =
        dao.removeLastAssistantReply(conversationId, messageId)

    private companion object {
        const val MAX_TITLE_CHARS = 80
    }
}
