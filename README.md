# Pinpin

Pinpin 是一个 Android 原生单页视觉原型，使用 Jetpack Compose 与 [Backdrop / AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 实现液态玻璃控件。

## 当前界面

- 复杂的自然摄影背景，用来观察玻璃的折射、模糊、色彩增强与边缘高光。
- 顶部为菜单按钮、带头像的标题胶囊和更多按钮；三个控件使用高透明度液态玻璃。
- 底部为较高不透明度的磨砂输入药丸，支持 1–4 行输入。
- 输入框会根据键盘、焦点与文本状态平滑改变左右边距、底部间距、最小高度、提示文字和发送按钮。
- 所有内容均在圆角裁剪和内部安全边距中，不会渗入药丸圆角区域。
- Android 12L / API 32 以下会由 Backdrop 自动跳过系统不支持的运行时着色器，保留可用的半透明表面。

## 构建

项目约定不在本地执行任何 Gradle 或 Android 编译。所有验证与产物都由 [GitHub Actions](https://github.com/prplegryn/Pinpin/actions) 完成：

1. `main` 分支的 push 会执行 release lint，并生成固定签名的 APK 与 AAB。
2. 工作流会上传 APK、AAB、SHA-256 校验文件和 lint 报告。
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

`app/src/main/res/drawable-nodpi/pinpin_alpine_lake.png` 由 OpenAI 内置 image generation 生成，提示目标是竖屏、写实的雾中高山湖泊摄影，保留前中后景细节与顶部/底部较暗区域，以检验半透明玻璃效果。图中不含人物、文字、标志或 UI。

## License

Apache-2.0
