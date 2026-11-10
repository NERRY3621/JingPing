<div align="center">

# 净屏 · JingPing

### 开屏广告 / 摇一摇广告拦截，不修改任何应用的安装包

无障碍自动跳过 · Root / Shizuku 深度净化 · LSPosed 传感器拦截 · 完全本地运行

[![Release](https://img.shields.io/github/v/release/NERRY3621/JingPing?style=flat-square&color=A67D48)](https://github.com/NERRY3621/JingPing/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://github.com/NERRY3621/JingPing)
[![Stars](https://img.shields.io/github/stars/NERRY3621/JingPing?style=flat-square&color=A67D48)](https://github.com/NERRY3621/JingPing/stargazers)
[![Last commit](https://img.shields.io/github/last-commit/NERRY3621/JingPing?style=flat-square&color=6F6A5E)](https://github.com/NERRY3621/JingPing/commits/main)

[下载 APK](https://github.com/NERRY3621/JingPing/releases/latest) · [使用教程](#-快速开始) · [工作原理](#-工作原理) · [常见问题](#-常见问题) · [介绍页](https://nerry3621.github.io/JingPing/)

</div>

<p align="center">
  <img src="docs/screenshots/main.png" width="300" alt="净屏主界面">
</p>

> **一句话**：用无障碍点掉开屏「跳过」，用 Root/Shizuku 直接禁用广告 Activity，用 LSPosed 拦截广告 SDK 的摇一摇传感器。三种能力可以单独用，也可以组合。

---

## 为什么是净屏

- **不改 APK**：所有操作都在系统层完成，不重打包、不签名替换。
- **没有网络权限**：不联网、不上传任何数据；规则和日志只在本机。
- **不强制 Root**：没有 Root 也能用无障碍跳过；有 Root / Shizuku 再解锁深度净化。
- **两种用法**：App 模式（无障碍 + 可选后台守护），或 LSPosed 模式（完全不跑后台）。
- **内置教程**：不是只给一份 README，App 里就有带一键按钮的图文教程。

---

## 功能

| 能力 | 说明 | 需要 |
| --- | --- | --- |
| **开屏自动跳过** | 识别 `SplashShellActivity`、`HotSplashActivity` 等开屏窗口，按文字/控件 ID 点「跳过」；右下角「跳过 3」也支持 | 无障碍 |
| **广告页自动返回** | 纯开屏广告页找不到按钮时，按一次返回键 | 无障碍 |
| **摇一摇跳转拦截** | 广告页把用户摇到淘宝/京东/浏览器时，立即返回原应用 | 无障碍（实验性） |
| **深度净化** | 通过系统 `pm disable` 禁用所选应用内的广告 SDK Activity，如 B 站 `HotSplashActivity` | Root 或 Shizuku |
| **LSPosed 模块** | 在目标应用进程内拦截广告 SDK 的加速度传感器注册与外部跳转，摇一摇拿不到数据 | LSPosed |
| **后台守护** | 前台服务 + 常驻通知栏、开机自启、进程被杀后恢复、无障碍掉线自动恢复 | 可选 |
| **内置图文教程** | 10 节步骤 + 一键开启无障碍/授权 Shizuku/管理应用/去净化/开守护/打开 LSPosed/测试 | 无 |

### 适配范围

内置通用规则，不依赖某个 App 的私有接口。已在 **Nubia NX769J / Android 15** 上实测：
开屏跳过、B 站京东 11.11 开屏禁用、`pm disable` 组件恢复、后台守护进程自愈。
其他 Android 8.0+ 设备可安装，但不同 ROM 的无障碍保活策略不同。

---

## 截图

| 主界面 | 使用教程 |
| --- | --- |
| ![主界面](docs/screenshots/main.png) | ![教程](docs/screenshots/guide.png) |

| 模拟开屏测试 | 莱茵生命风格 |
| --- | --- |
| ![测试页](docs/screenshots/test.png) | 暖灰纸面 / 墨黑字 / 琥珀金 / 细线档案卡片 |

---

## 快速开始

### 1. 安装

从 [Releases](https://github.com/NERRY3621/JingPing/releases/latest) 下载 `JingPing-1.3.0.apk`，或：

```bash
adb install -r JingPing-1.3.0.apk
```

> Release 里的 APK 使用本地测试签名。自己构建会生成新的签名；长期升级请固定使用同一个 keystore。

### 2. 开启无障碍

打开 App，点顶部「使用教程」→ 第 1 步「一键开启无障碍」。

- 有 Root / Shizuku：App 自动写入系统设置；
- 没有：按提示到系统无障碍页，手动打开「净屏 · 开屏广告跳过」。

状态卡显示绿色「运行中」即成功。掉线时点一次「一键开启」，或开启后台守护。

### 3. 按需解锁

- **只想去开屏**：到这里就够用了。打开 B 站、知乎、微博等 App 试试。
- **想彻底禁用 B 站开屏**：勾选 B 站 → 扫描组件 → 一键净化（需要 Root/Shizuku）。
- **想拦摇一摇**：在 LSPosed 里启用「净屏」并勾选作用域，强行停止目标 App 再打开。
- **不想每次手动打开 App**：打开「后台守护」，允许通知，按弹窗设置电池优化/自启动。

---

## B 站开屏示例

B 站的开屏广告页是：

```text
tv.danmaku.bili/tv.danmaku.bili.splash.ad.page.HotSplashActivity
```

在「深度净化」里勾选 B 站，扫描后一键净化，会执行：

```bash
pm disable --user 0 'tv.danmaku.bili/tv.danmaku.bili.splash.ad.page.HotSplashActivity'
```

之后 B 站冷启动直接进首页，京东 11.11 开屏和「扭一扭」都不会出现。

> 这是**每台设备本地**的组件状态，不会随 APK 分发。换设备或 B 站更新后，重新扫描/净化一次即可。
> 想恢复：应用内「深度净化 → 恢复」，或：

```bash
pm default-state --user 0 'tv.danmaku.bili/tv.danmaku.bili.splash.ad.page.HotSplashActivity'
```

---

## 两种模式

| | App 模式 | LSPosed 模式 |
| --- | --- | --- |
| 后台常驻 | 可选（后台守护） | **不需要** |
| 开屏跳过 | 无障碍点击 | 需配合深度净化禁用广告 Activity |
| 摇一摇 | 跳转拦截（结果拦截） | **传感器注册拦截（源头拦截）** |
| 需要权限 | 无障碍；深度净化需 Root/Shizuku | LSPosed；深度净化需 Root |
| 适合 | 想开箱即用、覆盖多个 App | 不想跑后台、只要摇一摇/跳转拦截 |

> 两者可以同时用。只使用 LSPosed 时，不要开启「后台守护」，App 不会驻留后台。

---

## 工作原理

```text
前台窗口变化
   └─ 无障碍服务 SkipService
        ├─ 判断广告上下文（Activity 名 / 启动后 12 秒内）
        ├─ BFS 查找「跳过」文字 / skip 控件 ID
        ├─ 点击 / 手势点击 / 返回键
        └─ 监控外部 App 跳转 → 返回键拦截

深度净化
   └─ PackageManager 枚举 Activity
        └─ Root / Shizuku：pm disable --user 0 pkg/component

LSPosed
   └─ 目标应用进程内 hook
        ├─ SensorManager.registerListener（广告调用栈）
        └─ Instrumentation.execStartActivity / ContextImpl.startActivity
```

- 普通应用启动后扫描 **12 秒**，覆盖晚加载的开屏；
- 文字命中的「跳过」不限制在屏幕上半部，所以右下角按钮也能点；
- 控件 ID 命中仍限制上半部，避免误点普通列表里的 `skip`；
- 桌面/系统界面被排除，不会在桌面上找「跳过」。

---

## 构建

不需要 Gradle。使用 Android SDK 的 `aapt`、`d8`、`apksigner`、`zipalign` 和 JDK 17。

依赖：

- `android.jar`（API 34）
- `aapt`、`d8.jar`、`apksigner.jar`、`zipalign`
- OpenJDK 17
- 仓库内已带 Shizuku API 13.1.5 与 androidx annotation 的 jar

```bash
git clone https://github.com/NERRY3621/JingPing.git
cd JingPing
bash build.sh
# 输出：dist/JingPing-1.3.0.apk
```

可通过环境变量覆盖工具路径：

```bash
SDK_JAR=/path/to/android.jar \
JAVA_HOME=/path/to/jdk17 \
D8_JAR=/path/to/d8.jar \
APKSIGNER_JAR=/path/to/apksigner.jar \
AAPT=/path/to/aapt \
ZIPALIGN=/path/to/zipalign \
bash build.sh
```

> 构建脚本首次运行会在本地生成测试 keystore；仓库不包含任何签名私钥。

---

## 权限与隐私

| 权限 | 用途 |
| --- | --- |
| 无障碍服务 | 读取窗口结构，查找并点击「跳过」；不读取账号、聊天内容 |
| Root / Shizuku | 深度净化、自动开启无障碍、省电白名单 |
| 通知 | 可选的后台守护常驻通知 |
| 网络 | **未申请** |

- 所有规则、日志、配置只在本机；
- 无障碍只处理窗口节点，不上传屏幕内容；
- 深度净化修改的是系统组件状态，可随时恢复；
- 卸载前请先在应用内恢复被禁用的组件。

---

## 已知限制

- 没有 LSPosed 时，无法阻止 App 读取加速度传感器，摇一摇只能做事后跳转拦截。
- 部分应用把「跳过」画在 Canvas / SurfaceView 上，无障碍树没有节点，可能识别不到；这类应用优先用深度净化禁用广告 Activity。
- 开屏广告如果 12 秒后才加载，可能错过扫描窗口。
- 深度净化只禁用 Activity，信息流里的 View / WebView 广告仍可能存在。
- 不同厂商 ROM 的无障碍保活策略不同；需要时把净屏设为「不限制」并允许自启动。
- LSPosed 模块需要目标应用强行停止后重开才生效；模块更新后也一样。

---

## 常见问题

<details>
<summary>为什么状态全绿，但某个 App 还是不跳过？</summary>

1. 确认总开关和「自动点击跳过」都开着；
2. 该 App 的跳过按钮可能是 Canvas 绘制，无障碍看不到；
3. 试试「深度净化」禁用它的广告 Activity；
4. 或者启用 LSPosed 模块拦截广告 SDK 的传感器/跳转。
</details>

<details>
<summary>B 站更新后开屏又回来了？</summary>

B 站更新可能重新启用组件。重新进「深度净化 → 扫描 → 一键净化」即可。
</details>

<details>
<summary>LSPosed 模块不生效？</summary>

确认模块已启用、作用域已勾选、目标 App 已强行停止并重开。LSPosed 管理器可能寄生在 `com.android.shell`，从通知栏进入更稳。
</details>

<details>
<summary>通知栏没有守护通知？</summary>

允许通知权限，然后关闭再打开「后台守护」。部分 ROM 还需要在系统设置里允许自启动。
</details>

<details>
<summary>卸载前要注意什么？</summary>

先在「深度净化 → 恢复」里还原所有已禁用的广告 Activity，再卸载。否则组件会保持禁用状态。
</details>

---

## 致谢

- [RhineLabUI](https://github.com/LBEILC/RhineLabUI)（MIT，LBEILC）—— 品牌标志路径、暖灰/墨黑/琥珀金配色、细线档案卡片与编号排版。
- [Shizuku API](https://github.com/RikkaApps/Shizuku-API)（MIT，RikkaApps）—— 免 Root 的 shell 通道。
- AndroidX Annotation（Apache-2.0）。
- Xposed API 仅使用编译期桩，不打包进 APK。

Rhine Lab / 明日方舟相关名称、标志与设定归鹰角网络（Hypergryph）所有。
本项目为非官方、非商业粉丝作品，与鹰角、哔哩哔哩、小米均无关联。

---

## 许可

本项目暂未指定开源许可证（默认保留所有权利）。第三方组件遵循各自许可证，
详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 与 APK 内 `assets/licenses/`。

> 仅供学习与个人使用。请遵守各应用的服务条款；深度净化可能影响应用内的广告/奖励功能，使用风险自负。
