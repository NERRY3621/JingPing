package com.jingping.adshield.xposed;

import android.content.Intent;
import android.hardware.Sensor;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_LoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * LSPosed 模块：
 * 1) 广告 SDK 注册加速度传感器时直接拒绝（摇一摇没有数据可用）；
 * 2) 广告 SDK 启动外部 Activity（淘宝/京东/浏览器等）时直接拦截；
 * 3) 只有调用栈里出现广告 SDK / 广告代码时才动作，避免影响正常功能。
 *
 * 作用范围由 LSPosed 的模块作用域控制。
 */
public class AdShieldHook implements IXposedHookLoadPackage {

    private static final String TAG = "JingPingX";
    private static final String SELF = "com.jingping.adshield";

    private static final String[] AD_PREFIXES = new String[]{
            "com.bytedance.sdk.openadsdk",
            "com.bytedance.msdk",
            "com.bytedance.pangle",
            "com.qq.e",
            "com.kwad",
            "com.mbridge",
            "com.unity3d.ads",
            "com.unity3d.services.ads",
            "com.applovin",
            "com.vungle",
            "com.ironsource",
            "com.chartboost",
            "com.inmobi",
            "com.sigmob",
            "com.huawei.hms.ads",
            "com.baidu.mobads",
            "com.tencent.klevin",
            "com.bilibili.ad",
            "tv.danmaku.bili.splash.ad"
    };

    private static volatile boolean sensorLogged;
    private static volatile int startBlocked;
    private static volatile int sensorBlocked;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || lpparam.packageName == null) {
            return;
        }
        if (SELF.equals(lpparam.packageName)) {
            return;
        }
        final String pkg = lpparam.packageName;
        try {
            XposedBridge.log("JingPing module loaded in " + pkg);
            Log.i(TAG, "loaded in " + pkg);
        } catch (Throwable ignored) {
        }

        try {
            hookSensors(pkg);
        } catch (Throwable t) {
            log("sensor hook failed", t);
        }
        try {
            hookActivityStarts(pkg);
        } catch (Throwable t) {
            log("activity hook failed", t);
        }
    }

    private void hookSensors(final String hostPkg) {
        XC_MethodHook sensorHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    Sensor sensor = findSensor(param.args);
                    if (sensor == null || !isShakeSensor(sensor.getType())) {
                        return;
                    }
                    if (!isAdCaller()) {
                        return;
                    }
                    block(param);
                    sensorBlocked++;
                    if (!sensorLogged) {
                        sensorLogged = true;
                        String msg = "blocked sensor type=" + sensor.getType() + " in " + hostPkg;
                        XposedBridge.log("JingPing " + msg);
                        Log.i(TAG, msg);
                    }
                } catch (Throwable t) {
                    log("sensor before hook", t);
                }
            }
        };

        // 公开 API 与系统实现各 hook 一次，覆盖不同调用路径。
        hookAllSafe("android.hardware.SensorManager", "registerListener", sensorHook);
        hookAllSafe("android.hardware.SystemSensorManager", "registerListenerImpl", sensorHook);
    }

    private void hookActivityStarts(final String hostPkg) {
        XC_MethodHook startHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    Intent intent = findIntent(param.args);
                    if (intent == null) {
                        return;
                    }
                    if (!isExternal(intent, hostPkg)) {
                        return;
                    }
                    if (!isAdCaller()) {
                        return;
                    }
                    block(param);
                    startBlocked++;
                    if (startBlocked <= 12) {
                        String msg = "blocked ad launch -> " + safeIntent(intent) + " in " + hostPkg;
                        XposedBridge.log("JingPing " + msg);
                        Log.i(TAG, msg);
                    }
                } catch (Throwable t) {
                    log("start before hook", t);
                }
            }
        };

        // Activity / Context 两条常见路径：最终都会经过其中之一。
        hookAllSafe("android.app.Instrumentation", "execStartActivity", startHook);
        hookAllSafe("android.app.Instrumentation", "execStartActivities", startHook);
        hookAllSafe("android.app.ContextImpl", "startActivity", startHook);
    }

    private static void hookAllSafe(String className, String methodName, XC_MethodHook hook) {
        try {
            Class<?> cls = Class.forName(className);
            XposedBridge.hookAllMethods(cls, methodName, hook);
        } catch (Throwable t) {
            log("hook " + className + "#" + methodName + " failed", t);
        }
    }

    private static Sensor findSensor(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object a : args) {
            if (a instanceof Sensor) {
                return (Sensor) a;
            }
        }
        return null;
    }

    private static Intent findIntent(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object a : args) {
            if (a instanceof Intent) {
                return (Intent) a;
            }
        }
        return null;
    }

    private static boolean isShakeSensor(int type) {
        return type == Sensor.TYPE_ACCELEROMETER
                || type == Sensor.TYPE_LINEAR_ACCELERATION
                || type == Sensor.TYPE_GRAVITY;
    }

    private static boolean isExternal(Intent intent, String hostPkg) {
        try {
            if (intent.getComponent() != null) {
                String target = intent.getComponent().getPackageName();
                return target != null && !target.equals(hostPkg);
            }
            String target = intent.getPackage();
            if (target != null) {
                return !target.equals(hostPkg);
            }
            // 隐式 Intent（浏览器 / deeplink）视为外部跳转
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 调用栈里是否有广告 SDK / 广告代码。 */
    private static boolean isAdCaller() {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        for (StackTraceElement e : stack) {
            String cn = e.getClassName();
            if (cn == null || cn.startsWith(SELF)) {
                continue;
            }
            for (String p : AD_PREFIXES) {
                if (cn.startsWith(p)) {
                    return true;
                }
            }
            String l = cn.toLowerCase(Locale.ROOT);
            if (l.contains("openadsdk") || l.contains("adsdk")
                    || l.contains("splashad") || l.contains("rewardad")
                    || l.contains("advert")) {
                return true;
            }
        }
        return false;
    }

    private static void block(XC_MethodHook.MethodHookParam param) {
        try {
            Method m = (Method) param.method;
            Class<?> rt = m.getReturnType();
            if (rt == boolean.class) {
                param.setResult(Boolean.FALSE);
            } else if (rt == int.class) {
                param.setResult(0);
            } else if (rt == long.class) {
                param.setResult(0L);
            } else {
                param.setResult(null);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String safeIntent(Intent i) {
        try {
            String s = i.toString();
            return s.length() > 180 ? s.substring(0, 180) : s;
        } catch (Throwable t) {
            return "intent";
        }
    }

    private static void log(String msg, Throwable t) {
        try {
            XposedBridge.log("JingPing " + msg + ": " + t);
        } catch (Throwable ignored) {
        }
        Log.e(TAG, msg, t);
    }
}
