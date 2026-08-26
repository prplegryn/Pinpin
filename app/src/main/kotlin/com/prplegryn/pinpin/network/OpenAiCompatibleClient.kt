package com.prplegryn.pinpin.network

import com.prplegryn.pinpin.data.ApiSettings
import com.prplegryn.pinpin.data.MessageEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONArray
import org.json.JSONObject

class ApiClientException(
    override val message: String,
    val statusCode: Int? = null
) : Exception(message)

data class ConnectionProbe(
    val message: String,
    val models: List<String>
)

class OpenAiCompatibleClient {
    private val activeConnection = AtomicReference<HttpURLConnection?>(null)

    fun validate(settings: ApiSettings): String? {
        validateEndpoint(settings)?.let { return it }
        if (settings.model.isBlank()) return "请填写模型名称"
        return null
    }

    fun validateEndpoint(settings: ApiSettings): String? {
        if (settings.baseUrl.isBlank()) return "请填写 API 地址"
        return runCatching { validatedBaseUri(settings.baseUrl) }
            .exceptionOrNull()
            ?.message
    }

    fun cancel() {
        activeConnection.getAndSet(null)?.disconnect()
    }

    fun streamCompletion(
        settings: ApiSettings,
        systemPrompt: String,
        messages: List<MessageEntity>,
        onText: (String) -> Unit
    ): String {
        val endpoint = chatEndpoint(validatedBaseUri(settings.baseUrl))
        val payload = JSONObject().apply {
            put("model", settings.model)
            put("stream", settings.streamResponses)
            if (settings.includeTemperature) {
                put("temperature", settings.temperature.toDouble())
            }
            put("messages", JSONArray().apply {
                if (systemPrompt.isNotBlank()) {
                    put(
                        JSONObject()
                            .put("role", "system")
                            .put("content", systemPrompt)
                    )
                }
                messages.forEach { message ->
                    put(
                        JSONObject()
                            .put("role", message.role)
                            .put("content", message.content)
                    )
                }
            })
        }.toString().toByteArray(Charsets.UTF_8)

        val connection = openConnection(endpoint, settings, "POST")
        connection.setRequestProperty("Accept", "text/event-stream, application/json")
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.doOutput = true
        connection.setFixedLengthStreamingMode(payload.size)
        activeConnection.set(connection)

        return try {
            connection.outputStream.use { it.write(payload) }
            val status = connection.responseCode
            if (status !in 200..299) {
                throw responseError(connection, status)
            }
            val contentType = connection.contentType.orEmpty().lowercase()
            if ("text/event-stream" in contentType) {
                parseEventStream(connection.inputStream, onText)
            } else {
                parseCompatibleBody(readLimited(connection.inputStream, MAX_RESPONSE_CHARS), onText)
            }
        } finally {
            activeConnection.compareAndSet(connection, null)
            connection.disconnect()
        }
    }

    fun testConnection(settings: ApiSettings): ConnectionProbe {
        val endpoint = modelsEndpoint(validatedBaseUri(settings.baseUrl))
        val connection = openConnection(endpoint, settings, "GET")
        connection.setRequestProperty("Accept", "application/json")
        activeConnection.set(connection)
        return try {
            val status = connection.responseCode
            if (status == 404) {
                throw ApiClientException(
                    "服务未提供模型列表；仍可手动填写模型 ID 后保存",
                    status
                )
            }
            if (status !in 200..299) throw responseError(connection, status)
            val body = readLimited(connection.inputStream, MAX_RESPONSE_CHARS)
            val models = runCatching {
                val data = JSONObject(body).optJSONArray("data") ?: JSONArray()
                buildList {
                    repeat(data.length()) { index ->
                        data.optJSONObject(index)
                            ?.optString("id")
                            ?.takeIf { it.isNotBlank() }
                            ?.let(::add)
                    }
                }.distinct().sorted().take(MAX_DISCOVERED_MODELS)
            }.getOrDefault(emptyList())
            ConnectionProbe(
                message = if (models.isEmpty()) "连接正常" else "连接正常 · 找到 ${models.size} 个模型",
                models = models
            )
        } finally {
            activeConnection.compareAndSet(connection, null)
            connection.disconnect()
        }
    }

    private fun openConnection(
        endpoint: URL,
        settings: ApiSettings,
        method: String
    ): HttpURLConnection = (endpoint.openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 20_000
        readTimeout = settings.timeoutSeconds.coerceIn(15, 300) * 1_000
        useCaches = false
        instanceFollowRedirects = false
        if (settings.apiKey.isNotBlank()) {
            setRequestProperty("Authorization", "Bearer ${settings.apiKey.trim()}")
        }
    }

    private fun parseEventStream(input: InputStream, onText: (String) -> Unit): String {
        val accumulated = StringBuilder()
        var eventData = StringBuilder()
        var lastEmissionNanos = 0L

        fun emitIfDue(force: Boolean = false) {
            val now = System.nanoTime()
            if (force || now - lastEmissionNanos >= EMIT_INTERVAL_NANOS) {
                onText(accumulated.toString())
                lastEmissionNanos = now
            }
        }

        fun consumeEvent(): Boolean {
            if (eventData.isEmpty()) return false
            val raw = eventData.toString().trim()
            eventData = StringBuilder()
            if (raw == "[DONE]") return true
            val json = runCatching { JSONObject(raw) }.getOrElse {
                throw ApiClientException("服务返回了无法解析的流数据")
            }
            json.optJSONObject("error")?.let { error ->
                throw ApiClientException(error.optString("message", "请求失败"))
            }
            val text = extractChoiceText(json)
            if (text.isNotEmpty()) {
                accumulated.append(text)
                if (accumulated.length > MAX_COMPLETION_CHARS) {
                    throw ApiClientException("回复内容过长，已停止接收")
                }
                emitIfDue()
            }
            return false
        }

        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                when {
                    line.isEmpty() -> if (consumeEvent()) break
                    line.startsWith("data:") -> {
                        if (eventData.isNotEmpty()) eventData.append('\n')
                        eventData.append(line.substringAfter("data:").trimStart())
                    }
                    line.startsWith(":") -> Unit
                }
            }
            consumeEvent()
        }
        emitIfDue(force = true)
        return accumulated.toString()
    }

    private fun parseCompatibleBody(body: String, onText: (String) -> Unit): String {
        if (body.lineSequence().any { it.startsWith("data:") }) {
            val combined = StringBuilder()
            body.lineSequence().forEach { line ->
                val data = line.takeIf { it.startsWith("data:") }
                    ?.substringAfter("data:")
                    ?.trim()
                    ?: return@forEach
                if (data != "[DONE]") {
                    val text = runCatching { extractChoiceText(JSONObject(data)) }.getOrDefault("")
                    combined.append(text)
                }
            }
            return combined.toString().also(onText)
        }
        val json = runCatching { JSONObject(body) }.getOrElse {
            throw ApiClientException("服务没有返回可识别的 JSON 内容")
        }
        json.optJSONObject("error")?.let { error ->
            throw ApiClientException(error.optString("message", "请求失败"))
        }
        return extractChoiceText(json).also(onText)
    }

    private fun extractChoiceText(json: JSONObject): String {
        val choice = json.optJSONArray("choices")?.optJSONObject(0) ?: return ""
        val container = choice.optJSONObject("delta") ?: choice.optJSONObject("message")
        val content = container?.opt("content") ?: return ""
        return when (content) {
            is String -> content
            is JSONArray -> buildString {
                repeat(content.length()) { index ->
                    val part = content.optJSONObject(index)
                    append(part?.optString("text").orEmpty())
                }
            }
            else -> ""
        }
    }

    private fun responseError(connection: HttpURLConnection, status: Int): ApiClientException {
        val body = runCatching {
            readLimited(connection.errorStream ?: connection.inputStream, MAX_ERROR_CHARS)
        }.getOrDefault("")
        val serviceMessage = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() }
        val message = when (status) {
            400, 422 -> buildString {
                append(serviceMessage ?: "请求参数不被服务接受")
                append("；可尝试关闭流式显示或温度参数")
            }
            401, 403 -> "API 密钥无效或没有访问权限"
            404 -> "接口不存在，请检查 API 地址"
            408, 504 -> "服务响应超时"
            429 -> "请求过于频繁或额度不足"
            in 500..599 -> "服务暂时不可用（$status）"
            else -> serviceMessage ?: "请求失败（$status）"
        }
        return ApiClientException(message.take(300), status)
    }

    private fun readLimited(input: InputStream, limit: Int): String =
        InputStreamReader(input, Charsets.UTF_8).use { reader ->
            val result = StringBuilder()
            val buffer = CharArray(2048)
            while (result.length < limit) {
                val count = reader.read(buffer, 0, minOf(buffer.size, limit - result.length))
                if (count < 0) break
                result.append(buffer, 0, count)
            }
            result.toString()
        }

    private fun validatedBaseUri(value: String): URI {
        val uri = runCatching { URI(value.trim()) }.getOrElse {
            throw ApiClientException("API 地址格式不正确")
        }
        if (uri.userInfo != null || uri.query != null || uri.fragment != null || uri.host.isNullOrBlank()) {
            throw ApiClientException("API 地址格式不正确")
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme == "https") return uri
        if (scheme == "http" && uri.host.lowercase() in LOCAL_HTTP_HOSTS) return uri
        throw ApiClientException("请使用 HTTPS；本机调试仅支持 localhost、127.0.0.1 或 10.0.2.2")
    }

    private fun chatEndpoint(base: URI): URL {
        val value = base.toString().trimEnd('/')
        return if (value.endsWith("/chat/completions")) {
            URL(value)
        } else {
            URL("$value/chat/completions")
        }
    }

    private fun modelsEndpoint(base: URI): URL {
        var value = base.toString().trimEnd('/')
        if (value.endsWith("/chat/completions")) {
            value = value.removeSuffix("/chat/completions")
        }
        return URL("$value/models")
    }

    private companion object {
        val LOCAL_HTTP_HOSTS = setOf("localhost", "127.0.0.1", "10.0.2.2")
        const val MAX_ERROR_CHARS = 16_384
        const val MAX_RESPONSE_CHARS = 2_000_000
        const val MAX_COMPLETION_CHARS = 200_000
        const val EMIT_INTERVAL_NANOS = 33_000_000L
        const val MAX_DISCOVERED_MODELS = 60
    }
}
