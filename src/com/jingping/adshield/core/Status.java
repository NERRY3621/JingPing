package com.jingping.adshield.core;

import android.content.Context;

import com.jingping.adshield.App;

/** 全局状态（在后台线程刷新，主线程读取）。 */
public final class Status {

    public static volatile Boolean rootAvailable = null;
    public static volatile String rootDetail = "正在检测 su…";

    public static volatile boolean shizukuInstalled;
    public static volatile boolean shizukuAlive;
    public static volatile boolean shizukuGranted;

    public static volatile boolean lsposedInstalled;
    public static volatile boolean accessibilityEnabled;

    private Status() {
    }

    public static void refreshAsync(Context c, Runnable done) {
        final Context app = c.getApplicationContext();
        App.IO.execute(() -> {
            RootShell root = RootShell.create();
            if (root != null) {
                rootAvailable = Boolean.TRUE;
                rootDetail = "su 可用，可执行系统命令";
                Shells.setRootShell(root);
            } else {
                rootAvailable = Boolean.FALSE;
                rootDetail = "未获得 Root 授权（可改用 Shizuku）";
            }

            shizukuInstalled = ShizukuClient.installed(app);
            shizukuAlive = ShizukuClient.binderAlive();
            shizukuGranted = shizukuAlive && ShizukuClient.granted();
            lsposedInstalled = isInstalled(app, "org.lsposed.manager")
                    || isInstalled(app, "org.lsposed.manager.debug");
            if (!lsposedInstalled) {
                // LSPosed 管理器可能是寄生在 com.android.shell 里的，PackageManager 查不到；
                // 有 Root/Shizuku 时直接看 /data/adb/lspd 是否存在。
                Shell sh = root;
                if (sh == null && shizukuAlive && shizukuGranted) {
                    sh = new ShizukuShell();
                }
                if (sh != null) {
                    Shell.Result r = sh.exec("test -d /data/adb/lspd && echo LSPOSED_OK");
                    lsposedInstalled = r.out.contains("LSPOSED_OK");
                }
            }
            accessibilityEnabled = SettingsHelper.isAccessibilityEnabled(app);

            if (done != null) {
                App.MAIN.post(done);
            }
        });
    }

    public static boolean isInstalled(Context c, String pkg) {
        try {
            c.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
