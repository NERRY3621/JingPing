package com.jingping.adshield.core;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.jingping.adshield.service.SkipService;

import java.util.ArrayList;
import java.util.List;

/** 无障碍服务的开关（Root / Shizuku 可免手动跳设置）。 */
public final class SettingsHelper {

    private SettingsHelper() {
    }

    private static String serviceFull(Context c) {
        return c.getPackageName() + "/" + SkipService.class.getName();
    }

    private static String serviceShort(Context c) {
        return c.getPackageName() + "/.service.SkipService";
    }

    private static List<String> readServices(Context c) {
        List<String> list = new ArrayList<>();
        try {
            String v = Settings.Secure.getString(c.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (v != null) {
                for (String s : v.split(":")) {
                    if (s != null && !s.trim().isEmpty()) {
                        list.add(s.trim());
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    public static boolean isAccessibilityEnabled(Context c) {
        try {
            int enabled = Settings.Secure.getInt(c.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            if (enabled != 1) {
                return false;
            }
            String full = serviceFull(c);
            String shrt = serviceShort(c);
            for (String s : readServices(c)) {
                if (s.equalsIgnoreCase(full) || s.equalsIgnoreCase(shrt)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** 通过 shell 写入安全设置；返回命令是否成功发送。 */
    public static boolean setAccessibility(Context c, boolean enable, Shell shell) {
        if (shell == null) {
            return false;
        }
        List<String> list = readServices(c);
        list.removeIf(s -> s.equalsIgnoreCase(serviceFull(c)) || s.equalsIgnoreCase(serviceShort(c)));
        if (enable) {
            list.add(serviceFull(c));
        }

        Shell.Result r;
        if (list.isEmpty()) {
            r = shell.exec("settings delete secure enabled_accessibility_services");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(':');
                }
                sb.append(list.get(i));
            }
            r = shell.exec("settings put secure enabled_accessibility_services '" + sb + "'");
        }
        Shell.Result r2 = shell.exec("settings put secure accessibility_enabled "
                + (list.isEmpty() ? "0" : "1"));
        return r.code == 0 && r2.code == 0;
    }

    public static void openAccessibilitySettings(Context c) {
        try {
            Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Throwable ignored) {
        }
    }
}
