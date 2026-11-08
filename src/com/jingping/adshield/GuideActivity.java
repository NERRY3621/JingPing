package com.jingping.adshield;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jingping.adshield.core.Prefs;
import com.jingping.adshield.core.SettingsHelper;
import com.jingping.adshield.core.Shell;
import com.jingping.adshield.core.Shells;
import com.jingping.adshield.core.ShizukuClient;
import com.jingping.adshield.core.Status;
import com.jingping.adshield.service.GuardService;
import com.jingping.adshield.service.SkipService;
import com.jingping.adshield.ui.AppPicker;

/** 内置图文教程 + 一键操作。 */
public class GuideActivity extends Activity {

    private LinearLayout sections;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        sections = findViewById(R.id.guideSections);
        tvStatus = findViewById(R.id.tvGuideStatus);
        findViewById(R.id.btnGuideBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuideDone).setOnClickListener(v -> finish());

        buildSections();
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildSections() {
        addSection("1", "先选一种用法", "方案 A：App 模式（大多数人）\n"
                + "无障碍自动点「跳过」+ Root/Shizuku 深度净化 + 可选后台守护。\n\n"
                + "方案 B：LSPosed 模式（不想后台）\n"
                + "仅在 LSPosed 启用本模块并勾选应用，拦截广告 SDK 的摇一摇传感器与外部跳转。"
                + "开屏广告页可用「深度净化」禁用。App 不需要常驻后台。", null, null);

        addSection("2", "第 1 步：开启无障碍", "这是 App 模式的基础。开启后状态卡应显示绿色「运行中」。\n\n"
                + "有 Root / Shizuku：点下面的按钮自动写入系统设置。\n"
                + "没有：按钮会跳到系统无障碍页，手动找到「净屏 · 开屏广告跳过」打开。\n\n"
                + "掉线时：回到本页点一次「一键开启」，或开启后台守护让它自动恢复。",
                "一键开启无障碍", v -> enableAccessibility());

        addSection("3", "第 2 步：Root / Shizuku（可选）", "二选一即可；深度净化和自动开无障碍需要它。\n\n"
                + "Shizuku：安装并启动 Shizuku 服务 → 回净屏点「授权」→ 允许。\n"
                + "Root：在 Magisk 弹窗里允许 su。\n\n"
                + "两者都有时优先 Root。都没有也能用无障碍跳过，只是不能深度净化。",
                "授权 Shizuku", v -> requestShizuku());

        addSection("4", "第 3 步：选择生效应用", "默认自动跳过对全部应用生效；深度净化只处理「已勾选」的应用。\n\n"
                + "点「管理」勾选常用 App：B 站、知乎、微博、小红书、抖音、快手、电商类等。"
                + "不确定时先勾 B 站和常用视频 App。",
                "管理应用", v -> AppPicker.show(this, this::refreshStatus));

        addSection("5", "第 4 步：深度净化（B 站示例）", "勾选 B 站 → 扫描组件 → 一键净化。\n\n"
                + "会禁用 tv.danmaku.bili.splash.ad.page.HotSplashActivity，B 站冷启动直接进首页，"
                + "京东 11.11 开屏和「扭一扭」都不会出现。\n\n"
                + "不修改安装包，可随时恢复。B 站更新后如果开屏复活，重新扫描净化一次。",
                "去深度净化", v -> openMainForClean());

        addSection("6", "第 5 步：后台守护（可选）", "不想每次手动打开 App，就开启后台守护：\n"
                + "前台服务 + 常驻通知栏、开机自启、进程被杀后自动恢复、无障碍掉线自动尝试恢复。\n\n"
                + "开启后按弹窗把电池优化设为「不限制」，并在系统设置允许自启动。"
                + "通知栏出现「净屏后台守护中」即成功。\n\n"
                + "只想用 LSPosed 模块：保持关闭，App 不会常驻后台。",
                "开启后台守护", v -> startGuard());

        addSection("7", "LSPosed 模式（可选）", "在 LSPosed 模块页启用「净屏」，进入作用域勾选目标 App，"
                + "然后强行停止并重新打开目标 App。\n\n"
                + "模块会拦截广告 SDK 的加速度传感器注册（摇一摇）和外部跳转；"
                + "开屏 Activity 仍建议配合深度净化。\n\n"
                + "模块更新后也要重新停止并打开作用域 App。LSPosed 管理器可能寄生在 com.android.shell，"
                + "下面按钮打不开时，从通知栏点 LSPosed 通知进入。",
                "打开 LSPosed", v -> openLsposed());

        addSection("8", "验证与测试", "点「打开测试页」，右下角会有「跳过 5」。"
                + "无障碍正常时 1～2 秒内自动变成「已跳过」。\n\n"
                + "也可以用 B 站冷启动验证：没有京东开屏即生效。",
                "打开测试页", v -> startActivity(new Intent(this, TestAdActivity.class)));

        addSection("9", "恢复与卸载", "深度净化改的是系统组件状态，卸载净屏不会自动恢复已禁用的组件。\n\n"
                + "卸载前：进「深度净化 → 恢复」。它会扫描所选应用里所有已禁用的广告 Activity，"
                + "包括手动用 pm disable 禁用的组件。",
                "去恢复", v -> openMainForClean());

        addSection("10", "常见问题", "• 通知栏没有守护通知：允许通知权限，重新开关后台守护。\n"
                + "• 无障碍显示已启用但不跳过：点「一键开启」重连；确认总开关和「自动点击跳过」都开着。\n"
                + "• 某个 App 不跳：跳过按钮可能画在 Canvas 上，无障碍树里没有节点；"
                + "改用深度净化禁用广告 Activity，或启用 LSPosed 模块。\n"
                + "• 深度净化扫描超时：系统包管理器繁忙，稍后重试。\n"
                + "• LSPosed 不生效：确认模块已启用、作用域已勾选、目标 App 已强行停止并重开。", null, null);
    }

    private void addSection(String number, String title, String body,
                            String action, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int p = dp(16);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        card.setLayoutParams(lp);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView num = new TextView(this);
        num.setText(number);
        num.setTextSize(12);
        num.setTextColor(getColor(R.color.text));
        num.setGravity(android.view.Gravity.CENTER);
        num.setBackgroundResource(R.drawable.bg_step);
        head.addView(num, new LinearLayout.LayoutParams(dp(26), dp(26)));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(15);
        t.setTextColor(getColor(R.color.text));
        t.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(10);
        head.addView(t, tlp);
        card.addView(head);

        TextView b = new TextView(this);
        b.setText(body);
        b.setTextSize(12);
        b.setTextColor(getColor(R.color.text_muted));
        b.setLineSpacing(dp(3), 1f);
        b.setPadding(0, dp(8), 0, 0);
        card.addView(b);

        if (action != null && listener != null) {
            Button btn = new Button(this);
            btn.setText(action);
            btn.setAllCaps(false);
            btn.setTextSize(12);
            btn.setTextColor(getColor(R.color.accent));
            btn.setBackgroundResource(R.drawable.bg_button_ghost);
            btn.setMinWidth(0);
            btn.setMinimumWidth(0);
            btn.setMinHeight(0);
            btn.setMinimumHeight(0);
            btn.setPadding(dp(14), dp(9), dp(14), dp(9));
            btn.setStateListAnimator(null);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            blp.topMargin = dp(10);
            card.addView(btn, blp);
            btn.setOnClickListener(listener);
        }

        sections.addView(card);
    }

    private void refreshStatus() {
        if (tvStatus == null) {
            return;
        }
        tvStatus.setText("正在检测当前状态…");
        Status.refreshAsync(this, this::renderStatus);
    }

    private void renderStatus() {
        if (tvStatus == null) {
            return;
        }
        boolean accessRunning = SkipService.isRunning();
        String access = accessRunning ? "运行中"
                : Status.accessibilityEnabled ? "已启用，等待连接" : "未开启";
        String shizuku = !Status.shizukuInstalled ? "未安装"
                : Status.shizukuGranted ? "已授权"
                : Status.shizukuAlive ? "已连接，待授权" : "未运行";
        String root = Status.rootAvailable == null ? "检测中"
                : Status.rootAvailable ? "已授权" : "未授权";
        String guard = GuardService.running ? "运行中"
                : Prefs.guardEnabled(this) ? "已开启，等待启动" : "关闭";
        tvStatus.setText("无障碍：" + access + "\n"
                + "Shizuku：" + shizuku + "    Root：" + root + "\n"
                + "后台守护：" + guard);
    }

    private void enableAccessibility() {
        toast("正在尝试开启无障碍…");
        App.IO.execute(() -> {
            Shell sh = Shells.get(this);
            boolean sent = sh != null && SettingsHelper.setAccessibility(this, true, sh);
            App.MAIN.post(() -> {
                if (sent) {
                    toast("已尝试开启，等待系统连接");
                    App.MAIN.postDelayed(this::refreshStatus, 1200);
                } else {
                    toast("需要手动开启");
                    SettingsHelper.openAccessibilitySettings(this);
                }
            });
        });
    }

    private void requestShizuku() {
        if (!ShizukuClient.installed(this)) {
            toast("未安装 Shizuku");
            return;
        }
        if (!ShizukuClient.binderAlive()) {
            ShizukuClient.openManager(this);
            toast("请先启动 Shizuku 服务");
            return;
        }
        if (!ShizukuClient.granted()) {
            try {
                ShizukuClient.requestPermission();
                toast("已发送授权请求，请允许");
                App.MAIN.postDelayed(this::refreshStatus, 1600);
            } catch (Throwable t) {
                toast("授权请求失败：" + t.getMessage());
            }
            return;
        }
        toast("Shizuku 已授权");
    }

    private void startGuard() {
        Prefs.setGuardEnabled(this, true);
        try {
            Intent i = new Intent(this, GuardService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(i);
            } else {
                startService(i);
            }
            toast("后台守护已开启");
        } catch (Throwable t) {
            toast("开启失败：" + t.getMessage());
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2101);
        }
        App.MAIN.postDelayed(this::refreshStatus, 900);
    }

    private void openLsposed() {
        toast("正在打开 LSPosed…");
        App.IO.execute(() -> {
            Shell sh = Shells.get(this);
            String out = "";
            if (sh != null) {
                out = sh.exec("am start -n com.android.shell/org.lsposed.manager.ui.activity.MainActivity").all();
            }
            final String result = out;
            App.MAIN.post(() -> {
                if (result.contains("Starting")) {
                    return;
                }
                try {
                    Intent i = new Intent();
                    i.setClassName("com.android.shell", "org.lsposed.manager.ui.activity.MainActivity");
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    return;
                } catch (Throwable ignored) {
                }
                if (sh != null) {
                    sh.exec("cmd statusbar expand-notifications");
                }
                toast("请从通知栏点开 LSPosed");
            });
        });
    }

    private void openMainForClean() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_SCROLL_TO_CLEAN, true);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2101) {
            refreshStatus();
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
