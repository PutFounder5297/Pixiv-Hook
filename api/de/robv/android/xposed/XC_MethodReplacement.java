package de.robv.android.xposed;

/** Compile-time stub of the Xposed API. */
public abstract class XC_MethodReplacement extends XC_MethodHook {

    protected abstract Object replaceHookedMethod(MethodHookParam param) throws Throwable;

    public static XC_MethodReplacement returnConstant(final Object result) {
        return new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return result;
            }
        };
    }

    public static XC_MethodReplacement DO_NOTHING = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) {
            return null;
        }
    };
}
