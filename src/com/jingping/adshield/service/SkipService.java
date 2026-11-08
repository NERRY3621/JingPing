package com.jingping.adshield.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.jingping.adshield.core.Logs;
import com.jingping.adshield.core.Names;
import com.jingping.adshield.core.Prefs;
import com.jingping.adshield.core.Rules;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 开屏广告跳过引擎：
 * 1) 窗口变化时识别广告 Activity / 启动前 7 秒；
 * 2) 查找「跳过 / skip」文字或控件 ID；
 * 3) 点击优先，手势点击兜底；
 * 4) 纯开屏广告页可按返回键；
 * 5) 广告页跳转淘宝/京东等外部 App 时可立即返回。
 */
public class SkipService extends AccessibilityService {

    private static volatile SkipService INSTANCE;

    public static boolean isRunning() {
        return INSTANCE != null;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable scanTask = this::scan;

    private boolean scanScheduled;
    private String curPkg;
    private String curCls;
    private long curSince;
    private long lastAdAt;
    private long lastScanAt;
    private long lastClickAt;
    private long lastInterceptAt;
    private boolean backTried;
    private String lastClickKey;

    private Set<String> scopeCache;
    private long scopeAt;

    private final Map<String, String> labelCache = new HashMap<>();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        INSTANCE = this;
        Logs.add("无障碍服务已连接");
    }

    @Override
    public void onDestroy() {
        INSTANCE = null;
        handler.removeCallbacksAndMessages(null);
        Logs.add("无障碍服务已断开");
        super.onDestroy();
    }

    @Override
    public void onInterrupt() {
        Logs.add("无障碍服务被中断");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || !Prefs.isEnabled(this)) {
            return;
        }
        CharSequence p = event.getPackageName();
        if (p == null) {
            return;
        }
        String pkg = p.toString();
        int type = event.getEventType();
        long now = System.currentTimeMillis();

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String cls = event.getClassName() == null ? "" : event.getClassName().toString();
            boolean pkgChanged = !pkg.equals(curPkg);
            boolean clsChanged = !cls.equals(curCls);
            if (pkgChanged) {
                handleForegroundChange(pkg, cls, now);
                curPkg = pkg;
                curSince = now;
                lastScanAt = 0;
                lastClickKey = null;
                backTried = false;
            }
            if (pkgChanged || clsChanged) {
                curCls = cls;
                backTried = false;
                lastScanAt = 0;
                lastClickKey = null;
                if (isAdContext(cls)) {
                    lastAdAt = now;
                }
            }
            if (allowed(pkg, cls)) {
                schedule(pkgChanged || clsChanged ? 120 : 260);
            }
        } else if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (pkg.equals(curPkg) && allowed(curPkg, curCls)) {
                boolean ad = isAdContext(curCls)
                        || now - lastAdAt < 12000
                        || now - curSince < 12000;
                if (ad) {
                    schedule(140);
                }
            }
        } else if (type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            if (pkg.equals(curPkg) && allowed(curPkg, curCls)
                    && (isAdContext(curCls) || now - lastAdAt < 12000)) {
                schedule(160);
            }
        }
    }

    private void handleForegroundChange(String newPkg, String newCls, long now) {
        if (!Prefs.interceptJump(this) || curPkg == null || newPkg.equals(curPkg)) {
            return;
        }
        if (!Rules.isJumpTarget(newPkg)) {
            return;
        }
        if (!allowed(curPkg, curCls)) {
            return;
        }
        boolean recentAd = now - lastAdAt < 8000;
        boolean quickSwitch = now - curSince < 1200;
        boolean notTooLate = now - curSince < 3500 || recentAd;
        if (!notTooLate || !(recentAd || quickSwitch)) {
            return;
        }
        if (now - lastInterceptAt < 2500) {
            return;
        }
        lastInterceptAt = now;
        boolean ok = performGlobalAction(GLOBAL_ACTION_BACK);
        String name = name(newPkg);
        Logs.add("拦截广告跳转 → " + name + (ok ? "" : "（返回失败）"));
        lastAdAt = now;
    }

    private void schedule(long delayMs) {
        if (scanScheduled) {
            return;
        }
        scanScheduled = true;
        handler.postDelayed(() -> {
            scanScheduled = false;
            scan();
        }, Math.max(50, delayMs));
    }

    private void scan() {
        if (!Prefs.isEnabled(this)) {
            return;
        }
        String pkg = curPkg;
        String cls = curCls;
        if (pkg == null || !allowed(pkg, cls)) {
            return;
        }
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean ad = isAdContext(cls);
        boolean splash = Rules.isSplashLike(cls);
        long adWindow = splash ? 12000 : 7000;
        if (!ad && now - curSince > 12000 && now - lastAdAt > 12000) {
            return;
        }
        if (ad && now - lastAdAt > adWindow) {
            return;
        }
        if (now - lastScanAt < 180) {
            schedule(180);
            return;
        }
        lastScanAt = now;

        boolean allowGesture = ad;
        if (Prefs.clickSkip(this)) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                Candidate c = findCandidate(root, ad, allowGesture);
                if (c != null && click(c, pkg, now, allowGesture)) {
                    return;
                }
            }
        }

        if (Prefs.backAd(this) && Rules.isBackableAd(cls) && !backTried
                && now - lastAdAt > 1000 && now - lastAdAt < 7000
                && now - lastClickAt > 2000) {
            backTried = true;
            boolean ok = performGlobalAction(GLOBAL_ACTION_BACK);
            Logs.add("广告页返回 · " + Names.label(this, pkg) + (ok ? "" : "（失败）"));
            return;
        }

        long elapsed = now - lastAdAt;
        if (ad || elapsed < 12000) {
            schedule(elapsed < 2500 ? 300 : 700);
        }
    }

    private boolean isAdContext(String cls) {
        return Rules.isAdActivity(cls) || Rules.isSplashLike(cls);
    }

    /** BFS 查找跳过按钮；allowGesture=false 时只接受真正可点击的控件，避免误点。 */
    private Candidate findCandidate(AccessibilityNodeInfo root, boolean adActivity, boolean allowGesture) {
        ArrayDeque<NodeDepth> q = new ArrayDeque<>();
        q.add(new NodeDepth(root, 0));
        int visited = 0;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        while (!q.isEmpty() && visited < 600) {
            NodeDepth nd = q.poll();
            AccessibilityNodeInfo n = nd.node;
            visited++;
            if (n == null) {
                continue;
            }
            if (n.isVisibleToUser()) {
                String text = n.getText() == null ? "" : n.getText().toString();
                String desc = n.getContentDescription() == null ? "" : n.getContentDescription().toString();
                String id = n.getViewIdResourceName();
                boolean textHit = Rules.isSkipText(text) || Rules.isSkipText(desc);
                boolean idHit = Rules.isSkipId(id);
                if (textHit || idHit) {
                    Rect r = new Rect();
                    n.getBoundsInScreen(r);
                    boolean onScreen = r.width() > 0 && r.height() > 0
                            && r.bottom > 0 && r.top < screenH;
                    // 文字命中的「跳过」不限制在屏幕上半部：B 站等开屏按钮常在右下角。
                    // 控件 ID 命中仍要求上半部，避免把普通列表里的 skip 控件点掉。
                    boolean topArea = textHit || adActivity || r.top < screenH * 0.50;
                    if (onScreen && topArea) {
                        AccessibilityNodeInfo clickable = findClickable(n);
                        if (clickable == null && !allowGesture) {
                            // 普通应用里只点得动的按钮，绝不盲点文字/坐标
                            continue;
                        }
                        return new Candidate(n, clickable, textHit ? text : desc,
                                id == null ? "" : id, r);
                    }
                }
            }
            if (nd.depth < 20) {
                int count = n.getChildCount();
                for (int i = 0; i < count; i++) {
                    AccessibilityNodeInfo ch = n.getChild(i);
                    if (ch != null) {
                        q.add(new NodeDepth(ch, nd.depth + 1));
                    }
                }
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findClickable(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo cur = n;
        for (int i = 0; i < 4 && cur != null; i++) {
            if (cur.isClickable()) {
                return cur;
            }
            cur = cur.getParent();
        }
        return null;
    }

    private boolean click(Candidate c, String pkg, long now, boolean allowGesture) {
        String key = pkg + "|" + curCls + "|" + c.label + "|" + c.id + "|" + c.bounds.toShortString();
        if (key.equals(lastClickKey) && now - lastClickAt < 2200) {
            return false;
        }
        boolean ok = false;
        try {
            if (c.clickable != null) {
                ok = c.clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            if (!ok) {
                ok = c.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        } catch (Throwable ignored) {
        }
        if (!ok && allowGesture) {
            int x = c.bounds.centerX();
            int y = c.bounds.centerY();
            ok = tap(x, y);
        }
        if (ok) {
            lastClickAt = now;
            lastClickKey = key;
            lastAdAt = now;
            String label = c.label != null && !c.label.trim().isEmpty() ? c.label.trim() : "跳过按钮";
            Logs.add("已跳过 · " + Names.label(this, pkg) + " · " + label);
        }
        return ok;
    }

    private boolean tap(int x, int y) {
        try {
            Path path = new Path();
            path.moveTo(Math.max(1, x), Math.max(1, y));
            GestureDescription gd = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0, 55))
                    .build();
            return dispatchGesture(gd, null, null);
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean allowed(String pkg, String cls) {
        if (pkg == null || Rules.isSystemUi(pkg) || Rules.isLauncher(pkg)) {
            return false;
        }
        if (pkg.equals(getPackageName())) {
            return cls != null && cls.contains("TestAdActivity");
        }
        if (Prefs.globalScope(this)) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (scopeCache == null || now - scopeAt > 5000) {
            scopeCache = Prefs.scope(this);
            scopeAt = now;
        }
        return scopeCache.contains(pkg);
    }

    private String name(String pkg) {
        String v = labelCache.get(pkg);
        if (v == null) {
            v = Names.label(this, pkg);
            labelCache.put(pkg, v);
        }
        return v;
    }

    private static final class NodeDepth {
        final AccessibilityNodeInfo node;
        final int depth;

        NodeDepth(AccessibilityNodeInfo node, int depth) {
            this.node = node;
            this.depth = depth;
        }
    }

    private static final class Candidate {
        final AccessibilityNodeInfo node;
        final AccessibilityNodeInfo clickable;
        final String label;
        final String id;
        final Rect bounds;

        Candidate(AccessibilityNodeInfo node, AccessibilityNodeInfo clickable,
                  String label, String id, Rect bounds) {
            this.node = node;
            this.clickable = clickable;
            this.label = label;
            this.id = id;
            this.bounds = bounds;
        }
    }
}
