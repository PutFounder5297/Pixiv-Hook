package de.robv.android.xposed.callbacks;

/** Compile-time stub of the Xposed API. */
public final class XC_LoadPackage {

    public static final class LoadPackageParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public android.content.pm.ApplicationInfo appInfo;
        public boolean isFirstApplication;
        public Object classLoaderDeprecated;
    }

    public interface IXposedHookLoadPackage {
        void handleLoadPackage(LoadPackageParam lpparam) throws Throwable;
    }
}
