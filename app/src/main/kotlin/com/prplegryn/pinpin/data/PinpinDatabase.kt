package com.prplegryn.pinpin.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val preview: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val roleId: String = RoleProfile.GENERAL_ID
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String,
    val content: String,
    val createdAt: Long,
    val status: String = STATUS_COMPLETE,
    val error: String? = null,
    val errorCode: Int? = null
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val STATUS_COMPLETE = "complete"
        const val STATUS_ERROR = "error"
        const val STATUS_STOPPED = "stopped"
    }
}

data class RoleProfile(
    val id: String,
    val name: String,
    val description: String,
    val prompt: String
) {
    companion object {
        const val GENERAL_ID = "general"
        const val WRITING_ID = "writing"
        const val ENGINEERING_ID = "engineering"
        const val CUSTOM_ID = "custom"

        fun all(settings: ApiSettings): List<RoleProfile> = listOf(
            RoleProfile(
                id = GENERAL_ID,
                name = "通用",
                description = "清晰、直接，适合日常问题",
                prompt = "请清晰、准确、直接地回答。信息不足时明确指出，不要编造事实。"
            ),
            RoleProfile(
                id = WRITING_ID,
                name = "写作",
                description = "重视语气、结构和文字质感",
                prompt = "你是一位克制的写作搭档。先理解语境和目标，再给出自然、具体、有节奏的文字，避免模板腔和空泛套话。"
            ),
            RoleProfile(
                id = ENGINEERING_ID,
                name = "技术",
                description = "偏重推理、边界和可执行方案",
                prompt = "你是一位严谨的技术搭档。优先给出可验证的结论，说明关键假设、边界条件和风险，代码应简洁且可维护。"
            ),
            RoleProfile(
                id = CUSTOM_ID,
                name = settings.customRoleName.ifBlank { "自定义" },
                description = if (settings.customRolePrompt.isBlank()) {
                    "可在设置中填写角色说明"
                } else {
                    "使用你的专属角色说明"
                },
                prompt = settings.customRolePrompt.trim()
            )
        )
    }
}

@Dao
interface PinpinDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, updatedAt DESC, id DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversation(conversationId: Long): ConversationEntity?

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessage(messageId: Long): MessageEntity?

    @Query(
        "SELECT * FROM messages WHERE conversationId = :conversationId " +
            "ORDER BY createdAt DESC, id DESC LIMIT 1"
    )
    suspend fun getLastMessage(conversationId: Long): MessageEntity?

    @Query("SELECT id FROM conversations ORDER BY isPinned DESC, updatedAt DESC, id DESC LIMIT 1")
    suspend fun getFirstConversationId(): Long?

    @Query(
        "SELECT * FROM (" +
            "SELECT * FROM messages WHERE conversationId = :conversationId " +
            "AND (role = 'user' OR status = 'complete') " +
            "ORDER BY createdAt DESC, id DESC LIMIT :limit" +
            ") ORDER BY createdAt ASC, id ASC"
    )
    suspend fun getContextMessages(conversationId: Long, limit: Int): List<MessageEntity>

    @Insert
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Transaction
    suspend fun createConversationWithFirstMessage(
        conversation: ConversationEntity,
        firstMessage: MessageEntity
    ): Long {
        val conversationId = insertConversation(conversation)
        insertMessage(firstMessage.copy(conversationId = conversationId))
        return conversationId
    }

    @Transaction
    suspend fun appendMessageAndTouch(
        message: MessageEntity,
        preview: String,
        updatedAt: Long
    ) {
        insertMessage(message)
        touchConversation(message.conversationId, preview, updatedAt)
    }

    @Query(
        "UPDATE conversations SET preview = :preview, updatedAt = :updatedAt " +
            "WHERE id = :conversationId"
    )
    suspend fun touchConversation(conversationId: Long, preview: String, updatedAt: Long)

    @Query("UPDATE conversations SET isPinned = :isPinned WHERE id = :conversationId")
    suspend fun setPinned(conversationId: Long, isPinned: Boolean)

    @Query("UPDATE conversations SET roleId = :roleId WHERE id = :conversationId")
    suspend fun setRole(conversationId: Long, roleId: String)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun renameConversation(conversationId: Long, title: String, updatedAt: Long)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: Long)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    @Query("DELETE FROM messages WHERE id = :messageId AND conversationId = :conversationId")
    suspend fun deleteMessage(conversationId: Long, messageId: Long)

    @Transaction
    suspend fun removeLastAssistantReply(conversationId: Long, messageId: Long): Boolean {
        val message = getMessage(messageId) ?: return false
        if (
            message.conversationId != conversationId ||
            message.role != MessageEntity.ROLE_ASSISTANT ||
            getLastMessage(conversationId)?.id != messageId
        ) {
            return false
        }
        deleteMessage(conversationId, messageId)
        val fallback = getLastMessage(conversationId)
        if (fallback != null) {
            touchConversation(
                conversationId = conversationId,
                preview = fallback.content.take(80),
                updatedAt = fallback.createdAt
            )
        }
        return true
    }

    @Query(
        "DELETE FROM messages WHERE conversationId = :conversationId " +
            "AND role = 'assistant' AND status = 'error'"
    )
    suspend fun deleteFailedReplies(conversationId: Long)
}

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PinpinDatabase : RoomDatabase() {
    abstract fun pinpinDao(): PinpinDao

    companion object {
        @Volatile
        private var instance: PinpinDatabase? = null

        fun get(context: Context): PinpinDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PinpinDatabase::class.java,
                "pinpin.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD COLUMN errorCode INTEGER")
            }
        }
    }
}
