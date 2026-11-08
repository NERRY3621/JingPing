# 第三方组件声明

本项目在 `libs/` 中随 APK 一起打包了以下第三方库：

## Shizuku API 13.1.5

- 项目：https://github.com/RikkaApps/Shizuku-API
- 作者：RikkaApps / Rikka
- 许可证：MIT License
- 用途：在未 Root 的设备上通过 Shizuku 以 shell UID 执行 `pm` / `settings` 命令。
- 打包文件：`shizuku-api.jar`、`shizuku-provider.jar`、`shizuku-aidl.jar`、`shizuku-shared.jar`

MIT License 允许在保留版权与许可声明的前提下自由使用、修改和分发。

## AndroidX Annotation 1.3.0

- 项目：https://developer.android.com/jetpack/androidx
- 许可证：Apache License 2.0
- 用途：Shizuku API 的编译期注解依赖（运行时无实际逻辑）。
- 打包文件：`annotation.jar`

## Xposed API

LSPosed 模块编译时使用 `stubs/` 下的最小 API 桩，**不会打包进 APK**。
运行时由 LSPosed 提供真实的 `de.robv.android.xposed.*` 类。
Xposed API 的版权归 original Xposed / LSPosed 项目所有。

---

本项目自身代码由使用者在本机构建、使用；如对外分发，请自行补充作者信息与许可证。

## RhineLabUI（视觉素材与设计语言）

- 项目：https://github.com/LBEILC/RhineLabUI
- 作者：LBEILC
- 许可证：MIT License
- 使用内容：Rhine Lab 品牌标志路径（顶部标志与启动图标）、暖灰/墨黑/琥珀金配色、
  细线档案卡片与编号排版风格。
- 许可全文：`assets/licenses/RhineLabUI-LICENSE.txt`
- 署名说明：`assets/licenses/RhineLabUI-NOTICE.txt`

Rhine Lab / 明日方舟相关名称、标志与设定归鹰角网络（Hypergryph）所有。
本项目为非官方、非商业粉丝作品，与官方无关联。

MiSans 字体未随 APK 分发；界面使用系统 sans-serif / monospace。
