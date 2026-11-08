package com.jingping.adshield.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import java.util.HashSet;
import java.util.Set;

/** 配置存储。 */
public final class Prefs {

    private static final String FILE = "config";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_CLICK_SKIP = "click_skip";
    private static final String KEY_BACK_AD = "back_ad";
    private static final String KEY_INTERCEPT_JUMP = "intercept_jump";
    private static final String KEY_GLOBAL_SCOPE = "global_scope";
    private static final String KEY_SCOPE = "scope_apps";
    private static final String KEY_DISABLED = "disabled_components";
    private static final String KEY_FIRST_RUN = "first_run";
    private static final String KEY_GUARD = "guard_enabled";

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context c) {
        return sp(c).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_ENABLED, v).apply();
    }

    public static boolean clickSkip(Context c) {
        return sp(c).getBoolean(KEY_CLICK_SKIP, true);
    }

    public static void setClickSkip(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_CLICK_SKIP, v).apply();
    }

    public static boolean backAd(Context c) {
        return sp(c).getBoolean(KEY_BACK_AD, true);
    }

    public static void setBackAd(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_BACK_AD, v).apply();
    }

    public static boolean interceptJump(Context c) {
        return sp(c).getBoolean(KEY_INTERCEPT_JUMP, true);
    }

    public static void setInterceptJump(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_INTERCEPT_JUMP, v).apply();
    }

    public static boolean globalScope(Context c) {
        return sp(c).getBoolean(KEY_GLOBAL_SCOPE, true);
    }

    public static void setGlobalScope(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_GLOBAL_SCOPE, v).apply();
    }

    public static Set<String> scope(Context c) {
        SharedPreferences p = sp(c);
        Set<String> s = p.getStringSet(KEY_SCOPE, null);
        if (s == null) {
            s = defaultScope(c);
            p.edit().putStringSet(KEY_SCOPE, s).apply();
        }
        return new HashSet<>(s);
    }

    public static void setScope(Context c, Set<String> s) {
        sp(c).edit().putStringSet(KEY_SCOPE, new HashSet<>(s)).apply();
    }

    private static Set<String> defaultScope(Context c) {
        Set<String> out = new HashSet<>();
        PackageManager pm = c.getPackageManager();
        for (String pkg : Rules.DEFAULT_SCOPE) {
            try {
                pm.getPackageInfo(pkg, 0);
                out.add(pkg);
            } catch (Throwable ignored) {
            }
        }
        return out;
    }

    public static Set<String> disabledComponents(Context c) {
        Set<String> s = sp(c).getStringSet(KEY_DISABLED, null);
        return s == null ? new HashSet<String>() : new HashSet<>(s);
    }

    public static void setDisabledComponents(Context c, Set<String> s) {
        sp(c).edit().putStringSet(KEY_DISABLED, new HashSet<>(s)).apply();
    }

    public static boolean guardEnabled(Context c) {
        return sp(c).getBoolean(KEY_GUARD, false);
    }

    public static void setGuardEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_GUARD, v).apply();
    }

    public static boolean firstRun(Context c) {
        return sp(c).getBoolean(KEY_FIRST_RUN, true);
    }

    public static void setFirstRunDone(Context c) {
        sp(c).edit().putBoolean(KEY_FIRST_RUN, false).apply();
    }
}
