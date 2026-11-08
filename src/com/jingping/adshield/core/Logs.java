package com.jingping.adshield.core;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/** 轻量环形日志：界面实时刷新，同时写入 logcat（tag=JingPing）。 */
public final class Logs {

    public interface Listener {
        void onLog(String line);
    }

    private static final int MAX = 240;
    private static final ArrayDeque<String> LINES = new ArrayDeque<>();
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final SimpleDateFormat FMT = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);

    private Logs() {
    }

    public static void add(String msg) {
        if (msg == null) {
            return;
        }
        String time;
        synchronized (FMT) {
            time = FMT.format(new Date());
        }
        String line = time + "  " + msg;
        synchronized (LINES) {
            LINES.addLast(line);
            while (LINES.size() > MAX) {
                LINES.removeFirst();
            }
        }
        Log.i("JingPing", msg);
        for (Listener l : LISTENERS) {
            try {
                l.onLog(line);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void addListener(Listener l) {
        if (l != null && !LISTENERS.contains(l)) {
            LISTENERS.add(l);
        }
    }

    public static void removeListener(Listener l) {
        LISTENERS.remove(l);
    }

    public static List<String> snapshot() {
        synchronized (LINES) {
            return new ArrayList<>(LINES);
        }
    }

    public static void clear() {
        synchronized (LINES) {
            LINES.clear();
        }
        for (Listener l : LISTENERS) {
            try {
                l.onLog("");
            } catch (Throwable ignored) {
            }
        }
    }
}
