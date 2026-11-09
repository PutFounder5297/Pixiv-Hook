package de.robv.android.xposed;

/** Compile-time stub of the Xposed API. */
public final class XposedHelpers {

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        throw new RuntimeException("stub");
    }

    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        throw new RuntimeException("stub");
    }

    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        throw new RuntimeException("stub");
    }

    public static Object getObjectField(Object obj, String fieldName) {
        throw new RuntimeException("stub");
    }

    public static void setObjectField(Object obj, String fieldName, Object value) {
        throw new RuntimeException("stub");
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        throw new RuntimeException("stub");
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        throw new RuntimeException("stub");
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        throw new RuntimeException("stub");
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        throw new RuntimeException("stub");
    }
}
