package com.dsh.pixivadclean;

import android.os.Bundle;
import android.os.Parcelable;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Popup suppression for pixiv's Charcoal dialog system.
 *
 * <p>Pixiv builds every generic charcoal dialog with `defpackage.bg8.p(...)`,
 * which fills a Bundle on `defpackage.uz0` (extends DialogFragment). The event
 * objects stored in that bundle tell us what the dialog is for, so we can drop
 * only the promotional ones and leave functional dialogs intact.</p>
 */
final class DialogHooks {

    private static final String[] EVENT_KEYS = {
            "arguments_primary_button_event",
            "arguments_second_button_event",
            "arguments_tertiary_button_event",
            "arguments_positive_event",
            "arguments_negative_event",
    };

    /** Event families that are pure promotion: update / rating / feedback / pixiv news / notifications. */
    private static final String[] BLOCKED_PREFIXES = {
            "jp.pxv.android.feature.bottomnavigationroot.flux.TopLevelCharcoalDialogEvent",
            "jp.pxv.android.feature.androidnotification.ConfirmNotificationPermissionDialogEvent",
            "jp.pxv.android.feature.routing.main.RoutingDialogEvent",
    };

    private DialogHooks() {
    }

    static void install(ClassLoader cl) {
        Util.group("charcoalDialogs", new Util.Task() {
            @Override
            public void run() throws Throwable {
                // pixiv is R8-full-mode: androidx.fragment.app.DialogFragment is renamed to
                // androidx.fragment.app.i in this build (6.202.1, versionCode 69581).
                final Class<?> dialogFragment = Util.findClass(cl, "androidx.fragment.app.i");
                XC_MethodHook hook = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.args.length < 2 || !(param.args[1] instanceof String)) {
                            return;
                        }
                        try {
                            Object args = XposedHelpers.callMethod(param.thisObject, "getArguments");
                            if (!(args instanceof Bundle)) {
                                return;
                            }
                            if (isPromoDialog((Bundle) args)) {
                                Util.log("blocked dialog " + param.args[1]);
                                param.setResult(null);
                            }
                        } catch (Throwable t) {
                            Util.logError("dialogArgs", t);
                        }
                    }
                };
                XposedBridge.hookAllMethods(dialogFragment, "show", hook);
                XposedBridge.hookAllMethods(dialogFragment, "showNow", hook);
            }
        });
    }

    private static boolean isPromoDialog(Bundle bundle) {
        for (String key : EVENT_KEYS) {
            Object value;
            try {
                value = bundle.getParcelable(key);
            } catch (Throwable t) {
                continue;
            }
            if (!(value instanceof Parcelable)) {
                continue;
            }
            String name = value.getClass().getName();
            for (String prefix : BLOCKED_PREFIXES) {
                if (name.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }
}
