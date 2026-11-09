# Pixiv 净化（LSPosed 模块）

针对 **pixiv 6.202.1（jp.pxv.android，versionCode 69581）** 的广告 / 弹窗 / Premium 入口屏蔽模块。
纯离线构建：不需要 Gradle、不需要联网，只依赖本机 Android Studio JBR + Android SDK。

## 产物

- `dist/pixiv-adclean.apk` —— 已签名可安装（debug 证书，`debug.keystore`）
- 包名 `com.dsh.pixivadclean`，作用域 `jp.pxv.android`

## 目录

```
pixiv_lsp/
├─ api/                  # Xposed API 编译桩（只参与编译，不会打进 APK）
├─ src/                  # 模块源码
│  └─ com/dsh/pixivadclean/
│     ├─ HookEntry.java  # 入口
│     ├─ AdHooks.java    # 广告视图隐藏 + SDK 请求拦截
│     ├─ DialogHooks.java# Charcoal 促销弹窗拦截
│     ├─ PremiumHooks.java# PremiumActivity 拦截
│     └─ Util.java
├─ assets/xposed_init    # 入口类名
├─ res/values/strings.xml# 名称/描述/推荐作用域
├─ AndroidManifest.xml
└─ build.ps1             # 全流程构建
```

## 构建

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

步骤：javac（API 桩）→ javac（模块，source/target 8）→ d8 → aapt2 compile/link →
写入 classes.dex + assets → zipalign → apksigner（首次自动生成 debug.keystore）。
构建脚本会优先使用 `%LOCALAPPDATA%\Android\Sdk`，找不到时回退到 `C:\Users\junbo\AppData\Local\Android\Sdk`。

## 安装 / 启用

```bash
adb push dist/pixiv-adclean.apk /data/local/tmp/pa.apk
adb shell su -c "pm install -r -t /data/local/tmp/pa.apk"
```

然后在 LSPosed 管理器里启用模块，作用域勾选 `jp.pxv.android`。
MIUI 下直接 `adb install` 会报 `INSTALL_FAILED_USER_RESTRICTED`，用 root 的 `pm install` 绕过。

## 验证

```bash
adb shell su -c "tail -100 /data/adb/lspd/log/modules*.log" | grep PixivAdClean
```

正常会看到 `hook group ok: ...`，以及浏览时的 `hit <类#方法>` / `blocked dialog` / `blocked PremiumActivity`。

## 版本适配（重要）

pixiv 是 R8 full mode：**类名/包名会被重命名或合并**，例如 `androidx.fragment.app.DialogFragment`
在这个版本里叫 `androidx.fragment.app.i`，Charcoal 弹窗是 `defpackage.uz0`，广告数据类是 `ebb/fbb/gbb/ibb`
（反编译里显示成 `defpackage.*`，dex 里其实是默认包 `Lebb;`）。

pixiv 升级后：

1. 重新拉 APK、jadx 反编译；
2. 核对 `DialogHooks` 里的 `androidx.fragment.app.i` 是否改名（搜 `extends androidx.fragment.app.` + `showNow(`）；
3. 核对 `feature/advertisement/view/*` 是否有新增广告视图；
4. 核对促销弹窗事件类前缀是否变化（`TopLevelCharcoalDialogEvent`、`ConfirmNotificationPermissionDialogEvent`、`RoutingDialogEvent`）。

## 已知限制

- 广告视图用 `GONE` 隐藏，列表里若容器固定高度可能留空白；彻底移除需要改列表数据层。
- 只拦截广告 SDK 的加载/展示入口，不做网络层拦截。
- Premium 入口仍在界面上可见，点击后立即关闭（`PremiumActivity.onCreate` → `finish()`）。
- 只 hook 了 GMA 的静态 UMP 接口；`InterstitialAd.show` 是抽象方法，Xposed 无法 hook（pixiv 的插屏走 MAX，已覆盖）。
