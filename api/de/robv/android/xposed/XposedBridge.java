package de.robv.android.xposed;

import java.util.Set;

/** Compile-time stub of the Xposed API (never packaged into the module APK). */
public class XposedBridge {

    public static void log(String text) {
        throw new RuntimeException("stub");
    }

    public static void log(Throwable t) {
        throw new RuntimeException("stub");
    }

    public static Set hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        throw new RuntimeException("stub");
    }

    public static Set hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        throw new RuntimeException("stub");
    }

    public static XC_MethodHook.Unhook hookMethod(java.lang.reflect.Member hookMethod, XC_MethodHook callback) {
        throw new RuntimeException("stub");
    }

    public static Object invokeOriginalMethod(java.lang.reflect.Member method, Object thisObject, Object[] args) throws Throwable {
        throw new RuntimeException("stub");
    }
}
