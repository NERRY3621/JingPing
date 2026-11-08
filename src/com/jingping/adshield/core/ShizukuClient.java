package com.jingping.adshield.core;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import rikka.shizuku.Shizuku;

/** Shizuku 状态与授权。 */
public final class ShizukuClient {

    public static final String PACKAGE = "moe.shizuku.privileged.api";
    public static final int REQUEST_CODE = 1001;

    private ShizukuClient() {
    }

    public static boolean installed(Context c) {
        try {
            c.getPackageManager().getPackageInfo(PACKAGE, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean binderAlive() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean granted() {
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void requestPermission() {
        Shizuku.requestPermission(REQUEST_CODE);
    }

    public static void openManager(Context c) {
        try {
            Intent i = c.getPackageManager().getLaunchIntentForPackage(PACKAGE);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
            }
        } catch (Throwable ignored) {
        }
    }
}
