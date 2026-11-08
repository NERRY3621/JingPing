package com.jingping.adshield.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.jingping.adshield.App;
import com.jingping.adshield.MainActivity;
import com.jingping.adshield.R;
import com.jingping.adshield.core.Logs;
import com.jingping.adshield.core.Prefs;
import com.jingping.adshield.core.SettingsHelper;
import com.jingping.adshield.core.Shell;
import com.jingping.adshield.core.Shells;
import com.jingping.adshield.core.Status;

/**
 * 可选的前台守护服务：
 * - 常驻通知栏，避免进程被系统/厂商回收；
 * - 开机、应用更新后由 BootReceiver 自动拉起；
 * - 无障碍服务掉线时，在 Root/Shizuku 可用的情况下尝试自动恢复。
 *
 * 用户不开启「后台守护」时，这个服务不会被启动，App 也不会常驻后台。
 */
public class GuardService extends Service {

    public static final String CHANNEL = "guard";
    public static final int NOTIF_ID = 0x4A11;

    public static final String ACTION_TOGGLE = "com.jingping.adshield.GUARD_TOGGLE";
    public static final String ACTION_STOP = "com.jingping.adshield.GUARD_STOP";

    public static volatile boolean running;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean accessibilityTried;

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        createChannel();
        Logs.add("后台守护服务已创建");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            Prefs.setGuardEnabled(this, false);
            running = false;
            stopForeground(true);
            stopSelf();
            Logs.add("后台守护已停止");
            return START_NOT_STICKY;
        }
        if (ACTION_TOGGLE.equals(action)) {
            boolean next = !Prefs.isEnabled(this);
            Prefs.setEnabled(this, next);
            Logs.add(next ? "通知栏恢复防护" : "通知栏暂停防护");
            updateNotification();
            return START_STICKY;
        }

        Prefs.setGuardEnabled(this, true);
        startForegroundCompat();
        scheduleAccessibilityCheck();
        return START_STICKY;
    }

    private void startForegroundCompat() {
        Notification n = buildNotification();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF_ID, n);
            }
        } catch (Throwable t) {
            Logs.add("后台守护 startForeground 失败：" + t.getClass().getSimpleName());
            try {
                startForeground(NOTIF_ID, n);
            } catch (Throwable ignored) {
            }
        }
    }

    private void updateNotification() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(NOTIF_ID, buildNotification());
            }
        } catch (Throwable ignored) {
        }
    }

    private Notification buildNotification() {
        boolean enabled = Prefs.isEnabled(this);
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(this, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent toggle = new Intent(this, GuardService.class).setAction(ACTION_TOGGLE);
        PendingIntent togglePi = PendingIntent.getService(this, 2, toggle,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stop = new Intent(this, GuardService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 3, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(getString(R.string.guard_notif_title))
                .setContentText(getString(enabled
                        ? R.string.guard_notif_text_on : R.string.guard_notif_text_off))
                .setContentIntent(openPi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setColor(0xFF5B8DF6);

        Icon icon = Icon.createWithResource(this, R.drawable.ic_notify);
        b.addAction(new Notification.Action.Builder(icon,
                getString(enabled ? R.string.guard_action_pause : R.string.guard_action_resume),
                togglePi).build());
        b.addAction(new Notification.Action.Builder(icon,
                getString(R.string.guard_action_stop), stopPi).build());
        return b.build();
    }

    private void createChannel() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm == null) {
                return;
            }
            NotificationChannel ch = new NotificationChannel(CHANNEL,
                    getString(R.string.guard_channel), NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            ch.setSound(null, null);
            ch.enableVibration(false);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            nm.createNotificationChannel(ch);
        } catch (Throwable ignored) {
        }
    }

    private void scheduleAccessibilityCheck() {
        handler.postDelayed(this::checkAccessibility, 8000);
        handler.postDelayed(this::checkAccessibility, 60000);
        handler.postDelayed(this::checkAccessibility, 300000);
    }

    private void checkAccessibility() {
        if (!Prefs.guardEnabled(this) || SkipService.isRunning() || accessibilityTried) {
            return;
        }
        try {
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null && km.isKeyguardLocked()) {
                // 锁屏时不弹 Root 授权框，等下次检查
                return;
            }
        } catch (Throwable ignored) {
        }
        if (Boolean.FALSE.equals(Status.rootAvailable) && !Status.shizukuGranted) {
            // 明确没有特权通道时不打扰用户；通知栏仍会提示无障碍是否在线
            return;
        }
        accessibilityTried = true;
        App.IO.execute(() -> {
            if (!Prefs.guardEnabled(this) || SkipService.isRunning()) {
                return;
            }
            Shell sh = Shells.get(this);
            if (sh == null) {
                return;
            }
            boolean ok = SettingsHelper.setAccessibility(this, true, sh);
            Logs.add(ok ? "后台守护：已尝试恢复无障碍服务" : "后台守护：无障碍恢复失败");
        });
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        Logs.add("后台守护服务已销毁");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
