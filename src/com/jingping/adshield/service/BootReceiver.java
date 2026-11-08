package com.jingping.adshield.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.jingping.adshield.core.Logs;
import com.jingping.adshield.core.Prefs;

/** 开机 / 应用更新后自动恢复后台守护。 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || !Prefs.guardEnabled(context)) {
            return;
        }
        try {
            Intent service = new Intent(context, GuardService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
            Logs.add("后台守护：开机自启已触发");
        } catch (Throwable t) {
            Logs.add("后台守护：开机自启失败（" + t.getClass().getSimpleName() + "）");
        }
    }
}
