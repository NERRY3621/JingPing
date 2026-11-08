package com.jingping.adshield.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.Map;

/** 应用名称缓存。 */
public final class Names {

    private static final Map<String, String> CACHE = new HashMap<>();

    private Names() {
    }

    public static String label(Context c, String pkg) {
        if (pkg == null) {
            return "";
        }
        synchronized (CACHE) {
            String v = CACHE.get(pkg);
            if (v != null) {
                return v;
            }
        }
        String label = pkg;
        try {
            PackageManager pm = c.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            label = pm.getApplicationLabel(ai).toString();
        } catch (Throwable ignored) {
        }
        synchronized (CACHE) {
            CACHE.put(pkg, label);
        }
        return label;
    }
}
