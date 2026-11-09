package com.dsh.pixivadclean;

import android.app.Activity;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/** Keeps the Premium landing page from opening (entry stays visible but inert). */
final class PremiumHooks {

    private PremiumHooks() {
    }

    static void install(ClassLoader cl) {
        Util.group("premium", new Util.Task() {
            @Override
            public void run() {
                XposedHelpers.findAndHookMethod(
                        "jp.pxv.android.feature.premium.lp.PremiumActivity", cl,
                        "onCreate", Bundle.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                try {
                                    Util.log("blocked PremiumActivity");
                                    ((Activity) param.thisObject).finish();
                                } catch (Throwable t) {
                                    Util.logError("premiumFinish", t);
                                }
                            }
                        });
            }
        });
    }
}
