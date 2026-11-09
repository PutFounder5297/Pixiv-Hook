package com.dsh.pixivadclean;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Small helpers: logging + tolerant hook installation. */
final class Util {

    static final String TAG = "PixivAdClean";

    private Util() {
    }

    static void log(String msg) {
        try {
            XposedBridge.log(TAG + ": " + msg);
        } catch (Throwable ignored) {
        }
    }

    static void logError(String where, Throwable t) {
        try {
            XposedBridge.log(TAG + " [" + where + "] " + t);
        } catch (Throwable ignored) {
        }
    }

    static Class<?> findClass(ClassLoader cl, String name) throws ClassNotFoundException {
        try {
            return Class.forName(name, false, cl);
        } catch (Throwable t) {
            return XposedHelpers.findClass(name, cl);
        }
    }

    interface Task {
        void run() throws Throwable;
    }

    /** Runs one hook group; one failure never breaks the others. */
    static void group(String name, Task task) {
        try {
            task.run();
            log("hook group ok: " + name);
        } catch (Throwable t) {
            logError("group " + name, t);
        }
    }

    private static final java.util.Map<String, Integer> HITS =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Logs first few hits per hook so the user can see the module actually working. */
    static void hit(String key) {
        Integer old = HITS.get(key);
        int n = old == null ? 0 : old;
        if (n < 3) {
            log("hit " + key);
        }
        HITS.put(key, n + 1);
    }

    static void tryHook(String label, Task task) {
        try {
            task.run();
        } catch (Throwable t) {
            logError("hook " + label, t);
        }
    }
}
