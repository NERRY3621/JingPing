<div align="center">

# JingPing · 净屏

### Block splash ads and shake-to-open ads on Android — without modifying any APK.

Accessibility auto-skip · Root / Shizuku component disabling · LSPosed sensor blocking · Fully local

[中文 README](README.md) · [Download APK](https://github.com/NERRY3621/JingPing/releases/latest) · [Landing page](https://nerry3621.github.io/JingPing/)

</div>

JingPing is a local Android ad-skipping tool for **splash ads** and **shake-to-open ads**.
It does not re-pack or modify other apps. Accessibility is enough for basic use; Root, Shizuku, and LSPosed are optional.

> ⭐ If it saves you a few ads, please [star the repo](https://github.com/NERRY3621/JingPing/stargazers). It is free, ad-free, and has no network permission.

## Features

| Feature | What it does | Needs |
| --- | --- | --- |
| Splash auto-skip | Finds and taps "Skip" / "跳过" in splash windows, including bottom-right buttons like "跳过 3" | Accessibility |
| Auto back | Presses Back on known splash ad activities when no skip button exists | Accessibility |
| Shake jump interception | Returns to the original app when an ad tries to jump to Taobao/JD/browser | Accessibility (experimental) |
| Deep clean | Disables ad SDK Activities with `pm disable`, e.g. Bilibili `HotSplashActivity` | Root or Shizuku |
| LSPosed module | Blocks ad SDK accelerometer registration and external intents inside the target process | LSPosed |
| Background guard | Foreground service, boot restart, process recovery, accessibility re-bind | Optional |
| In-app tutorial | 10-step guide with one-tap actions | None |

Tested on **Nubia NX769J / Android 15**; Android 8.0+ is supported.

## Quick start

1. Download `JingPing-1.3.1.apk` from [Releases](https://github.com/NERRY3621/JingPing/releases/latest).
2. Open the app → **使用教程** (Tutorial) → enable Accessibility.
3. Optional: choose apps → **深度净化** (Deep clean) to disable ad Activities.
4. Optional: enable the **净屏** module in LSPosed and select its scope. Force-stop and reopen the target app.
5. Want it to stay alive? Turn on **后台守护** (Background guard). If you only use LSPosed, leave it off — the app does not run in the background.

## Bilibili example

The splash ad page is `tv.danmaku.bili.splash.ad.page.HotSplashActivity`.
Deep clean runs:

```bash
pm disable --user 0 'tv.danmaku.bili/tv.danmaku.bili.splash.ad.page.HotSplashActivity'
```

Restore with:

```bash
pm default-state --user 0 'tv.danmaku.bili/tv.danmaku.bili.splash.ad.page.HotSplashActivity'
```

This is device-local; it is not bundled into the APK.

## Build

No Gradle. Requires Android SDK (`aapt`, `d8.jar`, `apksigner.jar`, `zipalign`), API 34 `android.jar`, and JDK 17.

```bash
git clone https://github.com/NERRY3621/JingPing.git
cd JingPing
bash build.sh
# dist/JingPing-1.3.1.apk
```

The repo does not contain a signing keystore; `build.sh` creates a local test keystore on first run.

## Privacy

- No `INTERNET` permission; no telemetry, no upload.
- Accessibility only inspects window nodes to find "Skip"; it does not read accounts or chats.
- Deep clean changes system component state and can be restored in-app.

## Limitations

- Without LSPosed, shake behavior can only be intercepted after the jump, not prevented at the sensor.
- Skip buttons drawn on a Canvas / SurfaceView may be invisible to accessibility.
- Ads that load later than 12 seconds may be missed.
- LSPosed requires force-stopping the target app after enabling the module.

## Credits

- [RhineLabUI](https://github.com/LBEILC/RhineLabUI) (MIT, LBEILC) — brand mark, warm-gray/ink/amber palette, archive card language.
- [Shizuku API](https://github.com/RikkaApps/Shizuku-API) (MIT, RikkaApps).
- AndroidX Annotation (Apache-2.0). Xposed API is compile-time stubs only.

Rhine Lab / Arknights names and marks belong to Hypergryph. This is an unofficial, non-commercial fan project, not affiliated with Hypergryph, Bilibili, or Xiaomi.

## License

No project-wide open-source license has been declared yet (all rights reserved by default).
Third-party components keep their own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
