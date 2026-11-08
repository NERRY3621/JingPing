package com.jingping.adshield.core;

import android.content.Context;
import android.os.SystemClock;

/** 选择当前可用的特权通道：优先 Root，其次 Shizuku。 */
public final class Shells {

    private static Shell cached;
    private static long cachedAt;

    private Shells() {
    }

    public static synchronized void setRootShell(Shell shell) {
        cached = shell;
        cachedAt = SystemClock.elapsedRealtime();
    }

    public static synchronized Shell get(Context c) {
        long now = SystemClock.elapsedRealtime();
        if (cached != null && now - cachedAt < 60000L) {
            return cached;
        }
        RootShell root = RootShell.create();
        if (root != null) {
            cached = root;
            cachedAt = now;
            return root;
        }
        if (ShizukuClient.binderAlive() && ShizukuClient.granted()) {
            cached = new ShizukuShell();
            cachedAt = now;
            return cached;
        }
        cached = null;
        return null;
    }

    public static synchronized void invalidate() {
        cached = null;
        cachedAt = 0L;
    }
}
