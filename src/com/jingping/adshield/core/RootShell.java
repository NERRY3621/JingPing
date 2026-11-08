package com.jingping.adshield.core;

import java.io.InputStream;

/** Magisk / SuperSU 等 su 通道。 */
public final class RootShell extends Shell {

    private static final String[] SU_PATHS = new String[]{
            "/system/bin/su", "/debug_ramdisk/su", "/sbin/su", "su"
    };

    private final String suPath;

    private RootShell(String suPath) {
        this.suPath = suPath;
    }

    /** 探测 su；会触发 Magisk 授权弹窗（仅首次）。 */
    public static RootShell create() {
        for (String path : SU_PATHS) {
            try {
                Result r = runWith(path, "id", 15000);
                if (r.code == 0 && (r.out.contains("uid=0") || r.out.contains("(root)"))) {
                    return new RootShell(path);
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Result runWith(String su, String command, long timeoutMs) {
        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(su, "-c", command);
            pb.environment().put("PATH", "/system/bin:/system/xbin:/sbin:/debug_ramdisk");
            p = pb.redirectErrorStream(false).start();
            final Process fp = p;
            final StringBuilder out = new StringBuilder();
            final StringBuilder err = new StringBuilder();
            Thread t1 = new Thread(() -> {
                synchronized (out) {
                    out.append(readAll(fp.getInputStream()));
                }
            });
            Thread t2 = new Thread(() -> {
                synchronized (err) {
                    err.append(readAll(fp.getErrorStream()));
                }
            });
            t1.start();
            t2.start();
            int code = waitFor(p, timeoutMs);
            t1.join(1500);
            t2.join(1500);
            String o;
            String e;
            synchronized (out) {
                o = out.toString();
            }
            synchronized (err) {
                e = err.toString();
            }
            return new Result(code, o, e);
        } catch (Throwable t) {
            return Result.error("su 不可用：" + t.getMessage());
        } finally {
            try {
                if (p != null) {
                    p.destroy();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public Result exec(String command) {
        return runWith(suPath, command, 30000);
    }
}
