package com.dsh.pixivadclean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Entry point of the pixiv ad / popup cleaner.
 */
public class HookEntry implements IXposedHookLoadPackage {

    private static final String TARGET = "jp.pxv.android";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) {
            return;
        }
        Util.log("injected into " + lpparam.processName);
        AdHooks.install(lpparam.classLoader);
        DialogHooks.install(lpparam.classLoader);
        PremiumHooks.install(lpparam.classLoader);
        Util.log("all hook groups installed");
    }
}
