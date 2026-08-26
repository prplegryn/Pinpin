# Pinpin

Pinpin 是一个 Android 原生单页视觉原型，使用 Jetpack Compose 与 [Backdrop / AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 实现液态玻璃控件。

## 当前界面

- 明亮的抽象流线简约壁纸，用柔和色块观察玻璃的折射、模糊与边缘高光。
- 顶部左侧为菜单按钮和按内容自适应宽度的标题胶囊，更多按钮独立贴右；三个控件使用高透明度液态玻璃。
- 顶部控件复用了 Backdrop 官方 `LiquidButton` 的按压高光、弹性缩放和拖拽方向形变，并保留玻璃边缘高光。
- 底部为纯白输入药丸，支持 1–4 行输入；整个文本区域拥有完整的 42dp 最小点击高度，文字起点始终位于左侧圆弧切线之外并额外保留 2px，不再保留加号和语音入口。
- 输入框根据 IME 动画目标立即切换边距、高度和圆角，避免键盘收起后的状态延迟；发送按钮仅在存在非空文字时以淡入缩放动画出现，并与输入药丸的右下圆弧保持同心等距。
- 点击空白处、手动收起输入法或应用进入后台时都会强制清除输入焦点，避免光标残留及返回前台自动弹出输入法。
- 标题胶囊先测量标题和副标题的实际像素宽度，再以弹簧动画显式改变胶囊宽度；内部文字固定按最终宽度布局并由外层玻璃逐步揭示，避免动画期间反复省略和重绘闪烁；文字与右侧圆弧之间保留“圆角半径 + 2px”的安全距离。
- 顶部玻璃控件和整个输入药丸使用零偏移、向四周扩散的低饱和冷蓝灰环境投影；输入药丸的投影位于尺寸动画裁剪层之外，不会再在四角留下黑块。
- 性能路径会复用 Backdrop 玻璃效果与轮廓缓存；标题宽度动画只触发布局而不逐帧重组文字，输入框只观察 IME 动画目标并用一个进度同步全部几何变化，编辑器内容不会跟随键盘的每一帧重复组合。
- 界面文字使用随应用打包的 Inter 4.1；Inter 未包含的中文字形由系统字体回退。
- 所有内容均在圆角裁剪和内部安全边距中，不会渗入药丸圆角区域。
- Android 12L / API 32 以下会由 Backdrop 自动跳过系统不支持的运行时着色器，保留可用的半透明表面。

## 构建

项目约定不在本地执行任何 Gradle 或 Android 编译。所有验证与产物都由 [GitHub Actions](https://github.com/prplegryn/Pinpin/actions) 完成：

1. `main` 分支的 push 会执行 release lint，并只构建固定签名 APK，不构建 AAB。
2. 工作流只把 APK 作为可下载产物上传，不附带校验包或 lint 报告。
3. 外部 fork 的 Pull Request 无法读取仓库签名 secrets，因此只构建 debug 作为安全回退。
4. 也可以在 Actions 页手动运行 `Android` 工作流。

Release 筿名与密钥通过四个 GitHub Actions secrets 注入：

- `PINPIN_KEYSTORE_BASE64`
- `PINPIN_STORE_PASSWORD`
- `PINPIN_KEY_ALIAS`
- `PINPIN_KEY_PASSWORD`

固定证书 SHA-256 指纹会记录在本文件的“签名”一节；密钥文件和口令不会进入 Git 历史。

## 技术栈

- Android Gradle Plugin 9.3.2 / Gradle 9.7.1
- Kotlin 2.4.10 / Compose 1.12.0
- Backdrop 2.0.1 / Kyant Shapes 1.2.1
- minSdk 23 / targetSdk 36

玻璃参数参考了 Backdrop 的 [Get started](https://kyant.gitbook.io/backdrop) 文档与官方 `LiquidButton` 示例；该库只提供底层效果，本仓库的高层控件与自适应输入行为均在项目内实现。

## 签名

Release 证书（SHA-256）：

`71:7E:4C:80:25:D2:88:33:1B:B3:23:80:0E:B7:59:D4:78:F9:E7:1B:C3:CA:9D:B5:CA:D6:13:88:10:81:11:6F`

## 背景资产

`app/src/main/res/drawable-nodpi/pinpin_minimal_flow.png` 由 OpenAI 内置 image generation 生成。提示目标是 9:16 竖屏、明亮常见的现代简约壁纸，以浅蓝、水绿、暖白、淡紫和少量柔和桃色组成大面积流线色块，顶部和底部保持安静留白；图中不含文字、标志、UI、实景或品牌化设计。

Inter 字体来自 [rsms/inter](https://github.com/rsms/inter) v4.1，使用 SIL Open Font License 1.1，许可文本位于 `licenses/Inter-OFL.txt`。

## License

Apache-2.0
