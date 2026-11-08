package com.jingping.adshield;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import com.jingping.adshield.core.Logs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public class App extends Application {

    public static ExecutorService IO;
    public static Handler MAIN;

    @Override
    public void onCreate() {
        super.onCreate();
        IO = Executors.newCachedThreadPool();
        MAIN = new Handler(Looper.getMainLooper());

        // Shizuku 只在安装并运行时才会真正回调；这里注册不会影响无 Shizuku 的设备。
        try {
            Shizuku.addBinderReceivedListenerSticky(() -> Logs.add("Shizuku 服务已连接"));
            Shizuku.addBinderDeadListener(() -> Logs.add("Shizuku 服务已断开"));
            Shizuku.addRequestPermissionResultListener((requestCode, grantResult) ->
                    Logs.add(grantResult == PackageManager.PERMISSION_GRANTED
                            ? "Shizuku 授权成功" : "Shizuku 授权被拒绝"));
        } catch (Throwable t) {
            Logs.add("Shizuku 初始化跳过：" + t.getClass().getSimpleName());
        }
    }
}
