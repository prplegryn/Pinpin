# Pinpin 0.2 设计与工程研究底稿

更新时间：2026-08-26

这份文件是侧边栏、会话管理、设置和自定义 API 聊天功能的研究与决策底稿。它记录采用的官方依据、产品假设、状态模型、边界条件和迭代检查结果；实现代码才是最终行为的唯一事实来源。

## 范围与假设

- 单设备、本地优先，不提供账号、云同步或多端合并。
- 用户自行填写 OpenAI-compatible Chat Completions 服务地址、模型和可选 bearer key。
- API key 属于用户自带凭据。应用负责传输安全、静态加密和不记录密钥，但移动客户端必须在请求时解密，无法替代服务端网关。
- 会话与消息保存在应用私有 Room 数据库；聊天正文当前不做额外数据库加密，因此设备已解锁且应用沙箱被突破时不视为安全边界。
- 只由 GitHub Actions 构建固定签名 APK，本地不执行 Gradle、Kotlin 或 Android 编译。

## 证据矩阵

| 问题 | 采用结论 | 主要依据 |
| --- | --- | --- |
| 侧边栏行为 | 菜单按钮打开模态抽屉；遮罩点击和返回键关闭；抽屉覆盖而非挤压聊天页 | [Compose navigation drawer](https://developer.android.com/develop/ui/compose/components/drawer)、[Predictive back](https://developer.android.com/develop/ui/compose/system/predictive-back-setup) |
| 长按管理 | 整行可点击，长按提供明确语义和触觉反馈；删除必须二次确认 | [Compose gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures)、[`combinedClickable`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/package-summary) |
| 触控与无障碍 | 所有主要操作目标至少 48dp，并设置按钮角色、描述和长按标签 | [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)、[Semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics) |
| 本地数据 | Room 是会话和消息的单一事实源；UI 通过 Flow/ViewModel 观察；网络只写回结果 | [Offline-first data layer](https://developer.android.com/topic/architecture/data-layer/offline-first)、[Room 2.8.4](https://developer.android.com/jetpack/androidx/releases/room) |
| 状态所有权 | 业务状态进入 ViewModel；消息正文留在数据库；只将当前会话 ID 和未发送草稿放进 SavedStateHandle | [Compose state saving](https://developer.android.com/develop/ui/compose/state-saving)、[Compose architecture](https://developer.android.com/develop/ui/compose/architecture) |
| 密钥保存 | API key 用 Android Keystore 生成的 AES-GCM 密钥加密；偏好设置只保存 IV 与密文；关闭系统备份 | [Android cryptography](https://developer.android.com/privacy-and-security/cryptography)、[`KeyGenParameterSpec`](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec)、[OWASP mobile storage](https://mas.owasp.org/MASTG/knowledge/android/MASVS-STORAGE/MASTG-KNOW-0047/) |
| 传输安全 | 默认拒绝明文流量；仅为 localhost、127.0.0.1、10.0.2.2 放行 HTTP；其他地址必须 HTTPS | [Network security configuration](https://developer.android.com/privacy-and-security/security-config)、[Cleartext risk](https://developer.android.com/privacy-and-security/risks/cleartext-communications) |
| 自带密钥边界 | 不内置厂商密钥、不输出到日志；高权限密钥建议放在用户自己的 HTTPS 网关后 | [OpenAI API key safety](https://help.openai.com/en/articles/5112595-best-practices-for-api-key-safety)、[OWASP mobile security](https://cheatsheetseries.owasp.org/cheatsheets/Mobile_Application_Security_Cheat_Sheet.html) |
| API 兼容 | POST 到 `/chat/completions`，传 `model/messages/stream/temperature`；处理 SSE `data:` 与 `[DONE]`，并兼容普通 JSON 回退 | [OpenAI Chat Completions reference](https://developers.openai.com/api/reference/resources/chat/subresources/completions) |
| 列表与流性能 | LazyColumn 使用数据库稳定 ID；搜索仅基于已加载摘要；流式文本按约 33ms 合并；高频状态不参与玻璃布局计算 | [Compose performance best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices)、[Compose lists](https://developer.android.com/develop/ui/compose/lists)、[Compose phases](https://developer.android.com/develop/ui/compose/performance/phases) |
| 玻璃效果 | 只有顶部小控件继续使用 Backdrop 的 blur/lens/vibrancy 和官方 LiquidButton 派生交互；大面积抽屉与表单使用稳定不透明表面 | [Backdrop 文档](https://kyant.gitbook.io/backdrop)、[AndroidLiquidGlass 源码](https://github.com/Kyant0/AndroidLiquidGlass) |
| 构建依赖 | AGP 9 使用 built-in Kotlin；KSP 需 2.3.6+，本项目使用 2.3.10；Room 使用当前稳定 2.8.4 | [AGP built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)、[KSP quickstart](https://kotlinlang.org/docs/ksp-quickstart.html)、[Room releases](https://developer.android.com/jetpack/androidx/releases/room) |

## 信息架构与视觉规则

主聊天页保留原来的明亮壁纸、顶部三个液态玻璃控件和纯白输入药丸。菜单按钮打开左侧抽屉；标题胶囊点击打开角色面板；更多菜单只放低频的新对话、失败重试、设置与删除当前对话。

抽屉使用接近白色的不透明表面，宽度为屏幕的 88%，上限 372dp。只有右侧有 34dp 圆角，右侧内容安全边距为圆角半径再加 2px。顶部依次是标题/新建按钮和 52dp 搜索框；中间历史列表；底部是等宽角色、设置按钮。历史默认按“已置顶优先，其余按更新时间倒序”排列。

所有药丸和圆角卡片遵循同一条安全规则：文字起点不进入圆弧半径，靠近端帽时至少再留 2px。阴影均为零方向、低饱和冷蓝灰环境投影，不使用 Material 风格的单向黑色 elevation。消息气泡不做实时玻璃，避免长列表中的离屏模糊和重复着色器成本。

## 数据模型

`ConversationEntity` 保存标题、摘要、创建/更新时间、置顶状态和角色 ID。`MessageEntity` 保存会话外键、角色、正文、时间、完成状态和可选错误。外键使用级联删除，避免删除会话后留下孤立消息。

数据库写入的原子边界：

- 新会话与第一条用户消息在同一事务中创建。
- 后续消息与会话摘要/更新时间在同一事务中提交。
- 失败重试先移除该会话旧的失败回复，再发起同一条用户消息的补充请求，不重复插入用户消息。

## 请求状态机

一次请求只有一个 `RequestControl`。发送时先同步占用请求槽，防止双击；随后落盘用户消息，再读取上下文并联网。状态转换如下：

1. `Idle → Preparing`：校验 URL/模型，占用请求槽，保存用户消息。
2. `Preparing → Streaming`：构造系统角色和上下文，发出 HTTPS 请求。
3. `Streaming → Complete`：节流更新内存中的临时气泡；完整结果与会话摘要事务写入。
4. `Streaming → User stopped`：断开连接；有部分内容则以 `stopped` 状态保存，不进入之后的模型上下文。
5. `Streaming → Failed`：保存部分内容或“没有收到回复”占位及规范化错误；允许幂等重试。
6. `Streaming → Navigation`：旧请求被静默断开，不能再覆盖新会话 UI；已有部分内容作为停止结果保存。
7. `Streaming → Delete`：断开并丢弃待写的回复；数据库级联删除负责清理。

每个请求捕获目标会话 ID 和角色 ID，不能在协程真正运行时再读取“当前会话”，从而避免“发送后立刻切换会话”把消息写错位置。聊天请求与“测试连接”使用独立客户端，彼此不能误取消。

## 上下文、输出和恢复边界

- 设置允许 8–120 条上下文消息；发送前再施加 120,000 字符上限，从最新消息向前保留。
- `error` 与 `stopped` 的助手片段只用于界面回看，不再次发给模型。
- 单次助手回复上限 200,000 字符；超限转为可重试错误，避免无界内存增长。
- SSE 累积结果最多约每 33ms发布一次，避免按 token 重组整个聊天页面。
- 当前会话 ID 和未发送草稿由 SavedStateHandle 恢复；完整列表和消息始终从 Room 重建。
- 进程中断后如果最后一条是用户消息，重新进入该会话会提示“上次回复未完成”并允许重试。

## 迭代检查记录

第一轮交互检查修正：搜索框和设置输入的 BasicTextField 覆盖完整容器高度；所有列表操作至少 48dp；长按有触觉反馈和可访问性标签；删除有明确对象名与不可撤销说明。

第一轮业务检查修正：发送槽在启动协程前占用；连接测试改用独立客户端；角色选择增加即时覆盖，避免数据库更新延迟；失败重试不重复用户消息；设置保存失败不会假装成功。

第二轮竞态检查修正：请求捕获目标会话，避免切换后串写；旧请求只在仍为活动请求时更新提示和流式状态；删除、导航和用户停止采用不同取消原因；数据库关键写入改为事务；初始化恢复不会覆盖用户刚做出的选择。

第二轮性能检查修正：角色列表按设置缓存；搜索结果按查询和摘要列表缓存；Room 列表使用稳定主键；大面积面板不使用 Backdrop；流式结果、上下文和总输出均有明确上限。

第三轮恢复检查修正：失败消息保存 HTTP 状态类型；401/403/404 与配置缺失优先引导用户修改设置，限流、超时和普通断线提供幂等重试；连接测试按代次作废，旧结果不能覆盖已经编辑过的新配置；长按使用 `combinedClickable` 自带的反馈，避免重复震动。

## 尚未扩展的边界

- 不做云同步、账号系统、附件、工具调用、语音、图片或 Markdown 富文本。
- 不接受任意局域网明文 HTTP；需要局域网模型时应配置 HTTPS 反向代理，模拟器调试可使用 `10.0.2.2`。
- 不尝试把用户 API key 伪装成移动端不可提取的秘密；需要更强隔离时应使用用户自己的服务端代理。
