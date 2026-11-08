package com.jingping.adshield;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jingping.adshield.core.AdComponents;
import com.jingping.adshield.core.Logs;
import com.jingping.adshield.core.Names;
import com.jingping.adshield.core.Prefs;
import com.jingping.adshield.core.RootShell;
import com.jingping.adshield.core.SettingsHelper;
import com.jingping.adshield.core.Shell;
import com.jingping.adshield.core.Shells;
import com.jingping.adshield.core.ShizukuClient;
import com.jingping.adshield.core.Status;
import com.jingping.adshield.service.GuardService;
import com.jingping.adshield.service.SkipService;
import com.jingping.adshield.ui.AppPicker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    public static final String EXTRA_SCROLL_TO_CLEAN = "scroll_to_clean";

    private Switch swEnabled;
    private Switch swGuard;
    private Switch swClickSkip;
    private Switch swBackAd;
    private Switch swIntercept;

    private View dotAccess;
    private View dotShizuku;
    private View dotRoot;
    private View dotLsposed;

    private TextView tvGuardDetail;
    private TextView tvAccessDetail;
    private TextView tvShizukuDetail;
    private TextView tvRootDetail;
    private TextView tvLsposedDetail;

    private Button btnAccess;
    private Button btnShizuku;
    private Button btnRoot;
    private Button btnLsposed;

    private TextView tvScopeSummary;
    private TextView tvScopeDetail;
    private TextView tvCleanResult;
    private TextView tvLog;

    private Button btnScan;
    private Button btnClean;
    private Button btnRestore;

    private boolean busy;
    private boolean resumed;
    private boolean guardPromptShown;
    private ScrollView scrollMain;
    private View cardClean;
    private View btnGuide;
    private int taskToken;
    private long busyAt;

    private final Logs.Listener logListener = line -> {
        if (App.MAIN != null) {
            App.MAIN.post(this::updateLog);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        loadPrefs();
        setupListeners();
        updateScopeUi();
        updateLog();
        updateGuardUi();
        refreshStatus();
        if (Prefs.guardEnabled(this)) {
            startGuardService(false);
        }

        if (Prefs.firstRun(this)) {
            showWelcome();
        }
        handleGuideIntent(getIntent());
    }

    private void bindViews() {
        scrollMain = findViewById(R.id.scrollMain);
        cardClean = findViewById(R.id.cardClean);
        btnGuide = findViewById(R.id.btnGuide);
        swEnabled = findViewById(R.id.swEnabled);
        swGuard = findViewById(R.id.swGuard);
        swClickSkip = findViewById(R.id.swClickSkip);
        swBackAd = findViewById(R.id.swBackAd);
        swIntercept = findViewById(R.id.swIntercept);

        dotAccess = findViewById(R.id.dotAccess);
        dotShizuku = findViewById(R.id.dotShizuku);
        dotRoot = findViewById(R.id.dotRoot);
        dotLsposed = findViewById(R.id.dotLsposed);

        tvGuardDetail = findViewById(R.id.tvGuardDetail);
        tvAccessDetail = findViewById(R.id.tvAccessDetail);
        tvShizukuDetail = findViewById(R.id.tvShizukuDetail);
        tvRootDetail = findViewById(R.id.tvRootDetail);
        tvLsposedDetail = findViewById(R.id.tvLsposedDetail);

        btnAccess = findViewById(R.id.btnAccess);
        btnShizuku = findViewById(R.id.btnShizuku);
        btnRoot = findViewById(R.id.btnRoot);
        btnLsposed = findViewById(R.id.btnLsposed);

        tvScopeSummary = findViewById(R.id.tvScopeSummary);
        tvScopeDetail = findViewById(R.id.tvScopeDetail);
        tvCleanResult = findViewById(R.id.tvCleanResult);
        tvLog = findViewById(R.id.tvLog);

        btnScan = findViewById(R.id.btnScan);
        btnClean = findViewById(R.id.btnClean);
        btnRestore = findViewById(R.id.btnRestore);
    }

    private void loadPrefs() {
        bindSwitch(swEnabled, Prefs.isEnabled(this), (b, v) -> {
            Prefs.setEnabled(this, v);
            Logs.add(v ? "防护总开关：已启用" : "防护总开关：已暂停");
            if (v && !SkipService.isRunning()) {
                toast("请先开启无障碍服务");
            }
        });
        bindSwitch(swGuard, Prefs.guardEnabled(this), (b, v) -> onGuardChanged(v));
        bindSwitch(swClickSkip, Prefs.clickSkip(this), (b, v) -> {
            Prefs.setClickSkip(this, v);
            Logs.add(v ? "自动点击跳过：开" : "自动点击跳过：关");
        });
        bindSwitch(swBackAd, Prefs.backAd(this), (b, v) -> {
            Prefs.setBackAd(this, v);
            Logs.add(v ? "广告页自动返回：开" : "广告页自动返回：关");
        });
        bindSwitch(swIntercept, Prefs.interceptJump(this), (b, v) -> {
            Prefs.setInterceptJump(this, v);
            Logs.add(v ? "摇一摇跳转拦截：开" : "摇一摇跳转拦截：关");
            if (v) {
                toast("实验性功能：广告页跳转外部 App 时按返回");
            }
        });
    }

    private void bindSwitch(Switch sw, boolean checked, CompoundButton.OnCheckedChangeListener l) {
        sw.setOnCheckedChangeListener(null);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener(l);
    }

    private void setupListeners() {
        btnAccess.setOnClickListener(v -> onAccessClick());
        btnAccess.setOnLongClickListener(v -> {
            SettingsHelper.openAccessibilitySettings(this);
            return true;
        });
        btnShizuku.setOnClickListener(v -> onShizukuClick());
        btnRoot.setOnClickListener(v -> {
            Shells.invalidate();
            toast("正在重新检测 su…");
            refreshStatus();
        });
        btnLsposed.setOnClickListener(v -> openLsposed());

        findViewById(R.id.btnScope).setOnClickListener(v ->
                AppPicker.show(this, this::updateScopeUi));
        findViewById(R.id.btnTest).setOnClickListener(v ->
                startActivity(new Intent(this, TestAdActivity.class)));

        btnGuide.setOnClickListener(v -> startActivity(new Intent(this, GuideActivity.class)));

        btnScan.setOnClickListener(v -> scanClean(true));
        btnClean.setOnClickListener(v -> scanClean(false));
        btnRestore.setOnClickListener(v -> askRestore());

        findViewById(R.id.btnClearLog).setOnClickListener(v -> {
            Logs.clear();
            updateLog();
        });

        findViewById(R.id.tvVersion).setOnClickListener(v -> showAbout());
    }

    private void refreshStatus() {
        setDot(dotAccess, R.drawable.dot_amber);
        setDot(dotShizuku, R.drawable.dot_amber);
        setDot(dotRoot, R.drawable.dot_amber);
        setDot(dotLsposed, R.drawable.dot_amber);
        tvAccessDetail.setText("正在检测…");
        tvShizukuDetail.setText("正在检测…");
        tvRootDetail.setText(R.string.root_detail_checking);
        tvLsposedDetail.setText("正在检测…");
        Status.refreshAsync(this, this::applyStatus);
    }

    private void applyStatus() {
        // 无障碍
        boolean running = SkipService.isRunning();
        if (running) {
            setDot(dotAccess, R.drawable.dot_green);
            tvAccessDetail.setText("运行中：正在监视前台窗口，识别开屏广告");
            btnAccess.setText("已运行");
            btnAccess.setEnabled(false);
        } else if (Status.accessibilityEnabled) {
            setDot(dotAccess, R.drawable.dot_amber);
            tvAccessDetail.setText("已勾选，等待系统连接服务");
            btnAccess.setText("刷新");
            btnAccess.setEnabled(true);
        } else {
            setDot(dotAccess, R.drawable.dot_grey);
            tvAccessDetail.setText(R.string.access_detail_off);
            btnAccess.setText("开启");
            btnAccess.setEnabled(true);
        }

        // Shizuku
        if (!Status.shizukuInstalled) {
            setDot(dotShizuku, R.drawable.dot_grey);
            tvShizukuDetail.setText(R.string.shizuku_detail_missing);
            btnShizuku.setText("未安装");
            btnShizuku.setEnabled(false);
        } else if (Status.shizukuAlive && Status.shizukuGranted) {
            setDot(dotShizuku, R.drawable.dot_green);
            tvShizukuDetail.setText(R.string.shizuku_detail_ok);
            btnShizuku.setText("已授权");
            btnShizuku.setEnabled(false);
        } else if (Status.shizukuAlive) {
            setDot(dotShizuku, R.drawable.dot_amber);
            tvShizukuDetail.setText(R.string.shizuku_detail_no_perm);
            btnShizuku.setText("授权");
            btnShizuku.setEnabled(true);
        } else {
            setDot(dotShizuku, R.drawable.dot_grey);
            tvShizukuDetail.setText(R.string.shizuku_detail_off);
            btnShizuku.setText("启动");
            btnShizuku.setEnabled(true);
        }

        // Root
        if (Status.rootAvailable == null) {
            setDot(dotRoot, R.drawable.dot_amber);
            tvRootDetail.setText(R.string.root_detail_checking);
            btnRoot.setText("检测");
        } else if (Status.rootAvailable) {
            setDot(dotRoot, R.drawable.dot_green);
            tvRootDetail.setText(R.string.root_detail_ok);
            btnRoot.setText("重新检测");
        } else {
            setDot(dotRoot, R.drawable.dot_grey);
            tvRootDetail.setText(R.string.root_detail_no);
            btnRoot.setText("检测");
        }

        // LSPosed
        if (Status.lsposedInstalled) {
            setDot(dotLsposed, R.drawable.dot_green);
            tvLsposedDetail.setText("已安装。点右侧打开，在模块页启用「净屏」并勾选应用");
            btnLsposed.setText("打开");
            btnLsposed.setEnabled(true);
        } else {
            setDot(dotLsposed, R.drawable.dot_grey);
            tvLsposedDetail.setText(R.string.lsposed_detail_missing);
            btnLsposed.setText("未安装");
            btnLsposed.setEnabled(false);
        }
    }

    private void onAccessClick() {
        if (SkipService.isRunning()) {
            toast("无障碍服务正在运行");
            return;
        }
        App.IO.execute(() -> {
            Shell sh = Shells.get(this);
            boolean sent = sh != null && SettingsHelper.setAccessibility(this, true, sh);
            String via = sh == null ? "" : (sh instanceof RootShell ? "Root" : "Shizuku");
            App.MAIN.post(() -> {
                if (sent) {
                    Logs.add("已通过 " + via + " 请求开启无障碍服务");
                    toast("已尝试开启，等待系统连接…");
                    App.MAIN.postDelayed(this::refreshStatus, 1000);
                    App.MAIN.postDelayed(this::refreshStatus, 2800);
                } else {
                    toast("需要 Root / Shizuku，或手动开启");
                    SettingsHelper.openAccessibilitySettings(this);
                }
            });
        });
    }

    private void onShizukuClick() {
        if (!Status.shizukuInstalled) {
            toast(getString(R.string.toast_shizuku_missing));
            return;
        }
        if (!ShizukuClient.binderAlive()) {
            ShizukuClient.openManager(this);
            toast(getString(R.string.toast_shizuku_not_running));
            return;
        }
        if (!ShizukuClient.granted()) {
            try {
                ShizukuClient.requestPermission();
                toast(getString(R.string.toast_request_sent));
                App.MAIN.postDelayed(this::refreshStatus, 1600);
            } catch (Throwable t) {
                toast("授权请求失败：" + t.getMessage());
            }
            return;
        }
        toast("Shizuku 已授权");
    }

    private void openLsposed() {
        if (!Status.lsposedInstalled) {
            toast(getString(R.string.toast_lsposed_missing));
            return;
        }
        toast("正在打开 LSPosed…");
        App.IO.execute(() -> {
            Shell sh = Shells.get(this);
            String out = "";
            if (sh != null) {
                out = sh.exec("am start -n com.android.shell/org.lsposed.manager.ui.activity.MainActivity").all();
            }
            final String result = out;
            App.MAIN.post(() -> {
                String lower = result.toLowerCase();
                if (result.contains("Starting") && !lower.contains("error")) {
                    return;
                }
                // 退路 1：直接以组件方式启动（部分 LSPosed 版本可由普通 App 拉起）
                try {
                    Intent i = new Intent();
                    i.setClassName("com.android.shell", "org.lsposed.manager.ui.activity.MainActivity");
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    return;
                } catch (Throwable ignored) {
                }
                // 退路 2：展开通知栏，让用户点 LSPosed 通知
                if (sh != null) {
                    sh.exec("cmd statusbar expand-notifications");
                }
                toast("请在通知栏点开 LSPosed，或在 LSPosed 模块页启用「净屏」");
            });
        });
    }

    private void onGuardChanged(boolean enabled) {
        Prefs.setGuardEnabled(this, enabled);
        if (enabled) {
            startGuardService(true);
            ensureNotificationPermission();
            maybeWhitelistBattery();
            App.MAIN.postDelayed(this::showGuardDialogIfNeeded, 700);
            Logs.add("后台守护：已开启");
        } else {
            stopService(new Intent(this, GuardService.class));
            toast(getString(R.string.guard_stopped));
            Logs.add("后台守护：已关闭");
        }
        updateGuardUi();
    }

    private void startGuardService(boolean showToast) {
        try {
            Intent i = new Intent(this, GuardService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(i);
            } else {
                startService(i);
            }
            if (showToast) {
                toast(getString(R.string.guard_started));
            }
        } catch (Throwable t) {
            toast(getString(R.string.guard_start_failed) + "：" + t.getClass().getSimpleName());
            Logs.add("后台守护启动失败：" + t);
        }
    }

    private void updateGuardUi() {
        boolean enabled = Prefs.guardEnabled(this);
        bindSwitch(swGuard, enabled, (b, v) -> onGuardChanged(v));
        if (enabled && GuardService.running) {
            tvGuardDetail.setText(R.string.guard_desc_running);
        } else if (enabled) {
            tvGuardDetail.setText(R.string.guard_desc_on);
        } else {
            tvGuardDetail.setText(R.string.guard_desc_off);
        }
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2001);
        }
    }

    private void maybeWhitelistBattery() {
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                return;
            }
        } catch (Throwable ignored) {
        }
        App.IO.execute(() -> {
            Shell sh = Shells.get(this);
            if (sh == null) {
                return;
            }
            Shell.Result r = sh.exec("dumpsys deviceidle whitelist +" + getPackageName());
            if (r.code == 0) {
                App.MAIN.post(() -> Logs.add(getString(R.string.guard_whitelist_ok)));
            }
        });
    }

    private void showGuardDialogIfNeeded() {
        if (!Prefs.guardEnabled(this) || guardPromptShown || isFinishing()) {
            return;
        }
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                return;
            }
        } catch (Throwable ignored) {
        }
        guardPromptShown = true;
        new AlertDialog.Builder(this)
                .setTitle(R.string.guard_dialog_title)
                .setMessage(R.string.guard_dialog_msg)
                .setPositiveButton(R.string.guard_battery, (d, w) -> requestIgnoreBattery())
                .setNeutralButton(R.string.guard_app_settings, (d, w) -> openAppSettings())
                .setNegativeButton(R.string.guard_later, null)
                .show();
    }

    private void requestIgnoreBattery() {
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Throwable t) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Throwable ignored) {
            }
        }
    }

    private void openAppSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Throwable ignored) {
        }
    }

    private void updateScopeUi() {
        Set<String> s = Prefs.scope(this);
        boolean global = Prefs.globalScope(this);
        if (global) {
            tvScopeSummary.setText(R.string.scope_all);
            if (s.isEmpty()) {
                tvScopeDetail.setText("自动跳过对全部应用生效；深度净化需先点「管理」勾选应用");
            } else {
                tvScopeDetail.setText("自动跳过对全部应用生效；深度净化只处理已勾选的 "
                        + s.size() + " 个应用");
            }
        } else if (s.isEmpty()) {
            tvScopeSummary.setText(R.string.scope_none);
            tvScopeDetail.setText("点击「管理」勾选需要防护的应用");
        } else {
            tvScopeSummary.setText("已选 " + s.size() + " 个应用");
            tvScopeDetail.setText("仅对勾选应用自动跳过；深度净化同样只处理这些应用");
        }
    }

    private void scanClean(final boolean previewOnly) {
        if (busy) {
            return;
        }
        final Set<String> scope = Prefs.scope(this);
        if (scope.isEmpty()) {
            toast(getString(R.string.toast_scope_empty));
            AppPicker.show(this, this::updateScopeUi);
            return;
        }
        final int token = ++taskToken;
        setBusy(true);
        tvCleanResult.setText("正在扫描所选应用内的广告组件…");
        Logs.add("深度净化：开始扫描 " + scope.size() + " 个应用");
        App.IO.execute(() -> {
            List<AdComponents.Item> found = null;
            Throwable error = null;
            try {
                found = AdComponents.find(this, scope);
            } catch (Throwable t) {
                error = t;
            }
            final List<AdComponents.Item> items = found;
            final Throwable err = error;
            App.MAIN.post(() -> {
                if (token != taskToken) {
                    return;
                }
                setBusy(false);
                if (err != null) {
                    tvCleanResult.setText("扫描失败：" + err.getClass().getSimpleName());
                    toast("扫描失败，请重试");
                    return;
                }
                if (items == null || items.isEmpty()) {
                    tvCleanResult.setText("未发现可禁用的广告 Activity");
                    Logs.add("深度净化：没有发现可禁用的广告 Activity");
                    toast(getString(R.string.toast_nothing));
                    return;
                }
                Logs.add("深度净化：发现 " + items.size() + " 个广告 Activity");
                if (previewOnly) {
                    showPreview(items);
                } else {
                    confirmClean(items);
                }
            });
        });
        App.MAIN.postDelayed(() -> {
            if (token == taskToken && busy) {
                taskToken++;
                setBusy(false);
                tvCleanResult.setText("扫描超时，请重试（可能是系统包管理器繁忙）");
                toast("扫描超时，请重试");
            }
        }, 15000);
    }

    private void showPreview(List<AdComponents.Item> items) {
        StringBuilder sb = new StringBuilder();
        int max = Math.min(items.size(), 18);
        for (int i = 0; i < max; i++) {
            sb.append("• ").append(items.get(i).title()).append('\n');
        }
        if (items.size() > max) {
            sb.append("… 另有 ").append(items.size() - max).append(" 个\n");
        }
        sb.append("\n禁用后这些广告页将无法启动，可随时「恢复」。");
        String msg = sb.toString().trim();
        tvCleanResult.setText("发现 " + items.size() + " 个广告 Activity");
        new AlertDialog.Builder(this)
                .setTitle("发现 " + items.size() + " 个广告 Activity")
                .setMessage(msg)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void confirmClean(final List<AdComponents.Item> items) {
        new AlertDialog.Builder(this)
                .setTitle("一键净化")
                .setMessage("将禁用 " + items.size() + " 个广告 Activity（需要 Root 或 Shizuku）。\n\n"
                        + "这不会修改应用安装包；可能影响应用内的广告 / 奖励功能。之后可点「恢复」还原。")
                .setPositiveButton("禁用", (d, w) -> disableItems(items))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void disableItems(final List<AdComponents.Item> items) {
        setBusy(true);
        tvCleanResult.setText("正在禁用 " + items.size() + " 个组件…");
        App.IO.execute(() -> {
            Shell sh = Shells.get(this);
            if (sh == null) {
                App.MAIN.post(() -> {
                    setBusy(false);
                    tvCleanResult.setText(R.string.clean_need_priv);
                    toast(getString(R.string.toast_no_priv));
                });
                return;
            }
            final AdComponents.Op op = AdComponents.disable(sh, items);
            App.MAIN.post(() -> {
                setBusy(false);
                Set<String> cur = Prefs.disabledComponents(this);
                cur.addAll(AdComponents.flatten(op.done));
                Prefs.setDisabledComponents(this, cur);
                tvCleanResult.setText("已禁用 " + op.ok + " / " + items.size() + " 个广告 Activity");
                Logs.add("深度净化：成功禁用 " + op.ok + " 个广告组件");
                if (op.ok < items.size()) {
                    Logs.add("部分组件禁用失败，请查看 Root / Shizuku 权限：" + op.detail.replace('\n', ' '));
                }
                toast(getString(R.string.toast_cleaned, op.ok));
            });
        });
    }

    private void askRestore() {
        final Set<String> disabled = Prefs.disabledComponents(this);
        new AlertDialog.Builder(this)
                .setTitle("恢复组件")
                .setMessage("将检查所选应用中已禁用的广告 Activity，并把它们恢复为系统默认状态。\n\n"
                        + "包括手动用 pm disable 禁用的组件。继续？")
                .setPositiveButton("恢复", (d, w) -> restore(disabled))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void restore(final Set<String> disabled) {
        setBusy(true);
        tvCleanResult.setText("正在检查已禁用的广告组件…");
        App.IO.execute(() -> {
            Shell sh = Shells.get(this);
            if (sh == null) {
                App.MAIN.post(() -> {
                    setBusy(false);
                    tvCleanResult.setText(R.string.clean_need_priv);
                    toast(getString(R.string.toast_no_priv));
                });
                return;
            }
            Set<String> targets = new HashSet<>(disabled);
            try {
                targets.addAll(AdComponents.findDisabled(this, Prefs.scope(this)));
            } catch (Throwable ignored) {
            }
            if (targets.isEmpty()) {
                App.MAIN.post(() -> {
                    setBusy(false);
                    tvCleanResult.setText("没有需要恢复的组件");
                    toast("没有需要恢复的组件");
                });
                return;
            }
            final AdComponents.Op op = AdComponents.restore(sh, targets);
            App.MAIN.post(() -> {
                setBusy(false);
                Set<String> cur = Prefs.disabledComponents(this);
                cur.removeAll(op.doneNames);
                Prefs.setDisabledComponents(this, cur);
                tvCleanResult.setText("已恢复 " + op.ok + " / " + targets.size() + " 个组件");
                Logs.add("恢复组件：成功 " + op.ok + " 个");
                toast(getString(R.string.toast_restored, op.ok));
            });
        });
    }

    private void setBusy(boolean v) {
        busy = v;
        if (v) {
            busyAt = SystemClock.elapsedRealtime();
        }
        btnScan.setEnabled(!v);
        btnClean.setEnabled(!v);
        btnRestore.setEnabled(!v);
    }

    private void updateLog() {
        List<String> lines = Logs.snapshot();
        if (lines.isEmpty()) {
            tvLog.setText(R.string.log_empty);
            return;
        }
        int from = Math.max(0, lines.size() - 12);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < lines.size(); i++) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        tvLog.setText(sb.toString());
    }

    private void setDot(View dot, int res) {
        if (dot != null) {
            dot.setBackgroundResource(res);
        }
    }

    private void showWelcome() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_welcome_title)
                .setMessage(R.string.dialog_welcome_msg)
                .setPositiveButton("查看教程", (d, w) -> {
                    Prefs.setFirstRunDone(this);
                    startActivity(new Intent(this, GuideActivity.class));
                })
                .setNegativeButton("直接使用", (d, w) -> Prefs.setFirstRunDone(this))
                .setCancelable(false)
                .show();
    }

    private void handleGuideIntent(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_SCROLL_TO_CLEAN, false)) {
            return;
        }
        intent.removeExtra(EXTRA_SCROLL_TO_CLEAN);
        if (scrollMain == null || cardClean == null) {
            return;
        }
        scrollMain.post(() -> scrollMain.smoothScrollTo(0, cardClean.getTop()));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleGuideIntent(intent);
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("关于净屏")
                .setMessage("净屏 1.3.0\n\n"
                        + "工作原理：\n"
                        + "• 无障碍服务在前台窗口变化时查找「跳过 / skip」控件并点击；\n"
                        + "• Root / Shizuku 用于免手动开启无障碍，以及禁用所选应用内的广告 SDK Activity；\n"
                        + "• LSPosed 模块在目标应用进程内拦截广告 SDK 的加速度传感器注册与外部跳转，这是屏蔽「摇一摇」最彻底的方式。\n\n"
                        + "限制：\n"
                        + "• 没有 LSPosed 时，无法真正阻止 App 读取传感器，只能拦截跳转结果；\n"
                        + "• 部分应用把跳过按钮绘制在 Canvas 上，无障碍树里没有节点，可能无法识别；\n"
                        + "• 深度净化只禁用 Activity，不改安装包，应用升级后可能需要重新扫描。\n\n"
                        + "本应用不申请网络权限，不上传任何数据。\n\n"
                        + "视觉语言参考 RhineLabUI（MIT，LBEILC）；Rhine Lab / 明日方舟相关权利归鹰角网络，本项目为非官方粉丝作品。")
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }

    private void toast(String s) {
        if (!isFinishing()) {
            Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (busy && SystemClock.elapsedRealtime() - busyAt > 15000) {
            taskToken++;
            setBusy(false);
            tvCleanResult.setText("上次操作超时，请重试");
        }
        Logs.addListener(logListener);
        refreshStatus();
        updateScopeUi();
        updateLog();
        updateGuardUi();
        if (Prefs.guardEnabled(this) && !GuardService.running) {
            startGuardService(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2001) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                if (Prefs.guardEnabled(this)) {
                    startGuardService(false);
                }
            } else {
                toast("通知权限未授予：守护仍会运行，但通知栏可能不显示");
            }
        }
    }

    @Override
    protected void onPause() {
        resumed = false;
        Logs.removeListener(logListener);
        super.onPause();
    }
}
