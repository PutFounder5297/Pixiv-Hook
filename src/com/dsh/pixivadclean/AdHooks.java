package com.dsh.pixivadclean;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Ad suppression.
 *
 * <p>Two layers: (1) keep every pixiv ad container GONE so nothing is visible,
 * (2) neuter the ad SDK entry points so no request is made at all.</p>
 */
final class AdHooks {

    private static final List<Class<?>> HIDDEN = new ArrayList<>();

    private AdHooks() {
    }

    /** Pixiv's own ad slot views (all of them live under feature.advertisement.view). */
    private static final String[] AD_VIEW_CLASSES = {
            "jp.pxv.android.feature.advertisement.view.RectangleAdSwitchView",
            "jp.pxv.android.feature.advertisement.view.RectangleAppLovinView",
            "jp.pxv.android.feature.advertisement.view.RectangleADGAutoRotationView",
            "jp.pxv.android.feature.advertisement.view.RectangleAdgTamView",
            "jp.pxv.android.feature.advertisement.view.OverlayAppLovinView",
            "jp.pxv.android.feature.advertisement.view.OverlayADGAutoRotationView",
            "jp.pxv.android.feature.advertisement.view.OverlayAdgTamView",
            "jp.pxv.android.feature.advertisement.view.MangaGridAdSwitchView",
            "jp.pxv.android.feature.advertisement.view.MangaGridADGView",
            "jp.pxv.android.feature.advertisement.view.MangaGridPlaceholderView",
            "jp.pxv.android.feature.advertisement.view.NovelNativeAdSwitchView",
            "jp.pxv.android.feature.advertisement.view.NovelNativeADGView",
            "jp.pxv.android.feature.advertisement.view.YufulightRectangleAdView",
            "jp.pxv.android.feature.advertisement.view.YufulightOverlayAdView",
            "jp.pxv.android.feature.advertisement.view.GridSelfServeView",
            "jp.pxv.android.feature.advertisement.view.SelfServeRelatedWorksView",
            "jp.pxv.android.feature.advertisement.view.RightTopEdgeAdsBackgroundView",
    };

    static void install(ClassLoader cl) {
        Util.group("adViews", new Util.Task() {
            @Override
            public void run() {
                for (String name : AD_VIEW_CLASSES) {
                    hideAdView(cl, name);
                }
                keepHidden();
            }
        });

        Util.group("applovin", new Util.Task() {
            @Override
            public void run() {
                neuter(cl, "com.applovin.mediation.ads.MaxAdView", "loadAd", "startAutoRefresh");
                neuter(cl, "com.applovin.mediation.ads.MaxInterstitialAd", "showAd");
                neuter(cl, "com.applovin.mediation.ads.MaxRewardedAd", "showAd");
                neuter(cl, "com.applovin.mediation.ads.MaxAppOpenAd", "showAd");
                neuter(cl, "com.applovin.mediation.ads.MaxNativeAdLoader", "loadAd");
            }
        });

        Util.group("adgeneration", new Util.Task() {
            @Override
            public void run() {
                neuter(cl, "com.socdm.d.adgeneration.ADG", "start");
            }
        });

        Util.group("amazonAps", new Util.Task() {
            @Override
            public void run() {
                neuter(cl, "com.amazon.device.ads.DTBAdRequest", "loadAd");
                neuter(cl, "com.amazon.device.ads.AdLayout", "loadAd");
                neuter(cl, "com.amazon.aps.ads.AdLayout", "loadAd");
            }
        });

        Util.group("googleAds", new Util.Task() {
            @Override
            public void run() {
                neuter(cl, "com.google.android.gms.ads.AdView", "loadAd");
                neuter(cl, "com.google.android.gms.ads.interstitial.InterstitialAd", "show");
                neuter(cl, "com.google.android.gms.ads.appopen.AppOpenAd", "show");
                neuter(cl, "com.google.android.gms.ads.admanager.AdManagerAdView", "loadAd");
            }
        });

        Util.group("umpConsent", new Util.Task() {
            @Override
            public void run() {
                // Do not show the GDPR/consent form (pixiv uses it as a popup on first run).
                neuter(cl, "com.google.android.ump.UserMessagingPlatform",
                        "loadAndShowConsentFormIfRequired");
                neuter(cl, "com.google.android.ump.ConsentForm", "show");
            }
        });
    }

    /** Hooks every method with one of the given names and turns it into a no-op. */
    private static void neuter(ClassLoader cl, String className, String... methods) {
        final Class<?> clazz;
        try {
            clazz = Util.findClass(cl, className);
        } catch (Throwable t) {
            Util.log("skip missing " + className);
            return;
        }
        for (final String method : methods) {
            final String key = className + "#" + method;
            Util.tryHook(key, new Util.Task() {
                @Override
                public void run() {
                    XposedBridge.hookAllMethods(clazz, method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Util.hit(key);
                            param.setResult(null);
                        }
                    });
                }
            });
        }
    }

    private static void hideAdView(ClassLoader cl, String className) {
        final Class<?> clazz;
        try {
            clazz = Util.findClass(cl, className);
        } catch (Throwable t) {
            Util.log("skip missing view " + className);
            return;
        }
        if (!View.class.isAssignableFrom(clazz)) {
            return;
        }
        HIDDEN.add(clazz);
        Util.tryHook("ctor " + className, new Util.Task() {
            @Override
            public void run() {
                XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            Util.hit("hide " + className);
                            collapse((View) param.thisObject);
                        } catch (Throwable ignored) {
                        }
                    }
                });
            }
        });
    }

    /** Someone may call setVisibility(VISIBLE) later - swallow that for ad views only. */
    private static void keepHidden() {
        Util.tryHook("View#setVisibility", new Util.Task() {
            @Override
            public void run() {
                XposedHelpers.findAndHookMethod(View.class, "setVisibility", int.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                if (param.args.length != 1) {
                                    return;
                                }
                                Object arg = param.args[0];
                                if (!(arg instanceof Integer) || (Integer) arg != View.VISIBLE) {
                                    return;
                                }
                                for (Class<?> c : HIDDEN) {
                                    if (c.isInstance(param.thisObject)) {
                                        param.args[0] = View.GONE;
                                        collapse((View) param.thisObject);
                                        return;
                                    }
                                }
                            }
                        });
            }
        });
    }

    /**
     * Hides the ad view and, crucially, sets its height (and the height of trivial single-child
     * wrappers, e.g. the RecyclerView item root) to 0. A GONE view with a fixed LayoutParams
     * height still leaves a blank block because RecyclerView measures children directly.
     */
    private static void collapse(final View view) {
        view.setVisibility(View.GONE);
        try {
            view.setMinimumHeight(0);
        } catch (Throwable ignored) {
        }
        view.post(new Runnable() {
            @Override
            public void run() {
                try {
                    zeroHeight(view);
                    ViewParent parent = view.getParent();
                    int depth = 0;
                    while (parent instanceof ViewGroup && depth < 4) {
                        ViewGroup group = (ViewGroup) parent;
                        String name = group.getClass().getName();
                        if (name.startsWith("androidx.recyclerview")
                                || name.startsWith("androidx.viewpager")
                                || name.startsWith("androidx.core.widget")) {
                            break;
                        }
                        if (group.getChildCount() != 1) {
                            break;
                        }
                        zeroHeight(group);
                        parent = group.getParent();
                        depth++;
                    }
                    Util.hit("collapse " + view.getClass().getSimpleName());
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private static void zeroHeight(View v) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp == null) {
            return;
        }
        if (lp.height != 0) {
            lp.height = 0;
            v.setLayoutParams(lp);
        }
    }
}
