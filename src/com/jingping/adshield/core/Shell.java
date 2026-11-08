package com.jingping.adshield.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/** 命令执行抽象：RootShell 或 ShizukuShell。 */
public abstract class Shell {

    public static final class Result {
        public final int code;
        public final String out;
        public final String err;

        public Result(int code, String out, String err) {
            this.code = code;
            this.out = out == null ? "" : out;
            this.err = err == null ? "" : err;
        }

        public boolean ok() {
            return code == 0;
        }

        public String all() {
            return (out + "\n" + err).trim();
        }

        public static Result error(String msg) {
            return new Result(-1, "", msg);
        }
    }

    public abstract Result exec(String command);

    protected static String readAll(InputStream in) {
        if (in == null) {
            return "";
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
            return bos.toString("UTF-8");
        } catch (Throwable t) {
            return "";
        } finally {
            try {
                in.close();
            } catch (Throwable ignored) {
            }
        }
    }

    protected static int waitFor(Process p, long timeoutMs) {
        try {
            if (p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                return p.exitValue();
            }
        } catch (Throwable ignored) {
        }
        try {
            p.destroyForcibly();
        } catch (Throwable ignored) {
        }
        return -1;
    }
}
