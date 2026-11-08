package com.jingping.adshield.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 枚举并禁用广告 SDK 的 Activity。 */
public final class AdComponents {

    public static final class Item {
        public final String pkg;
        public final String cls;
        public final String label;
        public final String flatten;

        public Item(String pkg, String cls, String label) {
            this.pkg = pkg;
            this.cls = cls;
            this.label = label;
            this.flatten = pkg + "/" + cls;
        }

        public String title() {
            return label + " · " + Rules.shortClass(cls);
        }
    }

    public static final class Op {
        public int ok;
        public final List<Item> done = new ArrayList<>();
        public final List<String> doneNames = new ArrayList<>();
        public String detail = "";
    }

    private AdComponents() {
    }

    public static List<Item> find(Context c, Set<String> pkgs) {
        List<Item> out = new ArrayList<>();
        PackageManager pm = c.getPackageManager();
        for (String pkg : pkgs) {
            if (pkg == null || pkg.equals(c.getPackageName())) {
                continue;
            }
            PackageInfo pi;
            try {
                pi = pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            } catch (Throwable t) {
                continue;
            }
            if (pi.activities == null) {
                continue;
            }
            String label = Names.label(c, pkg);
            for (ActivityInfo ai : pi.activities) {
                if (ai == null || ai.name == null || !ai.enabled) {
                    continue;
                }
                if (!Rules.isAdComponent(ai.name)) {
                    continue;
                }
                ComponentName cn = new ComponentName(pkg, ai.name);
                int state;
                try {
                    state = pm.getComponentEnabledSetting(cn);
                } catch (Throwable t) {
                    state = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
                }
                if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                        || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
                    continue;
                }
                out.add(new Item(pkg, ai.name, label));
            }
        }
        return out;
    }

    /** 找出当前已被禁用（包括手动 pm disable）的广告 Activity，用于恢复。 */
    public static List<String> findDisabled(Context c, Set<String> pkgs) {
        List<String> out = new ArrayList<>();
        PackageManager pm = c.getPackageManager();
        for (String pkg : pkgs) {
            if (pkg == null || pkg.equals(c.getPackageName())) {
                continue;
            }
            PackageInfo pi;
            try {
                pi = pm.getPackageInfo(pkg,
                        PackageManager.GET_ACTIVITIES | PackageManager.GET_DISABLED_COMPONENTS);
            } catch (Throwable t) {
                continue;
            }
            if (pi.activities == null) {
                continue;
            }
            for (ActivityInfo ai : pi.activities) {
                if (ai == null || ai.name == null || !Rules.isAdComponent(ai.name)) {
                    continue;
                }
                ComponentName cn = new ComponentName(pkg, ai.name);
                try {
                    int state = pm.getComponentEnabledSetting(cn);
                    if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                            || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
                        out.add(cn.flattenToString());
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return out;
    }

    /** 逐个执行，成功才记录；返回成功项。 */
    public static Op disable(Shell shell, List<Item> items) {
        Op op = new Op();
        StringBuilder detail = new StringBuilder();
        for (Item it : items) {
            // Android 15 上 pm disable-user 对组件可能只回到 default，必须用 pm disable
            Shell.Result r = shell.exec("pm disable --user 0 '" + it.flatten + "'");
            boolean ok = r.code == 0 && r.out.contains("new state: disabled")
                    && !looksError(r.all());
            detail.append(it.flatten).append(ok ? " → disabled\n" : " → 失败：" + r.all() + "\n");
            if (ok) {
                op.ok++;
                op.done.add(it);
            }
        }
        op.detail = detail.toString().trim();
        return op;
    }

    /** 恢复到系统默认状态（而不是强制 enable）。 */
    public static Op restore(Shell shell, Set<String> flattenNames) {
        Op op = new Op();
        StringBuilder detail = new StringBuilder();
        for (String f : flattenNames) {
            Shell.Result r = shell.exec("pm default-state --user 0 '" + f + "'");
            boolean ok = r.code == 0
                    && (r.out.contains("new state: default") || r.out.contains("new state: enabled"))
                    && !looksError(r.all());
            detail.append(f).append(ok ? " → default\n" : " → 失败：" + r.all() + "\n");
            if (ok) {
                op.ok++;
                op.doneNames.add(f);
            }
        }
        op.detail = detail.toString().trim();
        return op;
    }

    private static boolean looksError(String s) {
        if (s == null) {
            return false;
        }
        String l = s.toLowerCase();
        return l.contains("error") || l.contains("exception")
                || l.contains("unknown") || l.contains("not found")
                || l.contains("denied") || l.contains("failure")
                || l.contains("does not exist");
    }

    public static Set<String> flatten(List<Item> items) {
        Set<String> s = new HashSet<>();
        for (Item it : items) {
            s.add(it.flatten);
        }
        return s;
    }
}
