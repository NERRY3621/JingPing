package de.robv.android.xposed;

public final class XposedHelpers {
    private XposedHelpers() {
    }

    public static Class<?> findClass(String className, ClassLoader cl) throws ClassNotFoundException {
        return Class.forName(className, false, cl);
    }

    public static Class<?> findClassIfExists(String className, ClassLoader cl) {
        try {
            return findClass(className, cl);
        } catch (Throwable t) {
            return null;
        }
    }
}
