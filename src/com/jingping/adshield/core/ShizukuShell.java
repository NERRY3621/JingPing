package com.jingping.adshield.core;

import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.InputStream;

import moe.shizuku.server.IRemoteProcess;
import moe.shizuku.server.IShizukuService;
import rikka.shizuku.Shizuku;

/** Shizuku 免 Root 通道（shell UID）。 */
public final class ShizukuShell extends Shell {

    @Override
    public Result exec(String command) {
        IRemoteProcess process = null;
        try {
            if (!Shizuku.pingBinder()) {
                return Result.error("Shizuku 未运行");
            }
            IShizukuService service = IShizukuService.Stub.asInterface(Shizuku.getBinder());
            if (service == null) {
                return Result.error("Shizuku 服务为空");
            }
            process = service.newProcess(
                    new String[]{"/system/bin/sh", "-c", command}, null, null);
            if (process == null) {
                return Result.error("Shizuku 创建进程失败");
            }
            final IRemoteProcess fp = process;
            final StringBuilder out = new StringBuilder();
            final StringBuilder err = new StringBuilder();
            Thread t1 = new Thread(() -> {
                synchronized (out) {
                    out.append(read(pfd(fp, true)));
                }
            });
            Thread t2 = new Thread(() -> {
                synchronized (err) {
                    err.append(read(pfd(fp, false)));
                }
            });
            t1.start();
            t2.start();
            int code;
            try {
                code = fp.waitFor();
            } catch (Throwable t) {
                code = -1;
            }
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
            return Result.error("Shizuku 执行失败：" + t.getMessage());
        } finally {
            try {
                if (process != null) {
                    process.destroy();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static ParcelFileDescriptor pfd(IRemoteProcess p, boolean input) {
        try {
            return input ? p.getInputStream() : p.getErrorStream();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String read(ParcelFileDescriptor fd) {
        if (fd == null) {
            return "";
        }
        try {
            InputStream in = new FileInputStream(fd.getFileDescriptor());
            return readAll(in);
        } catch (Throwable t) {
            return "";
        } finally {
            try {
                fd.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
