# 模块 hook 清单（v1，已实现）

包名 `com.dsh.pixivadclean`，入口 `com.dsh.pixivadclean.HookEntry`，作用域 `jp.pxv.android`。
所有 hook 都包在 `Util.group/tryHook` 里：**单条失败只打日志，不影响其它 hook，不会让 pixiv 崩溃**。

## 广告

| # | Hook | 行为 | 失败降级 |
|---|---|---|---|
| A1 | 17 个 `feature.advertisement.view.*` 构造器 | 构造完成后 `setVisibility(GONE)` | 类不存在就跳过 |
| A2 | `View.setVisibility(int)`（仅对 A1 类实例） | 把 `VISIBLE` 改成 `GONE`，防止父容器再显示 | 同上 |
| A3 | `MaxAdView.loadAd` / `startAutoRefresh` | 不执行 | 类缺失跳过 |
| A4 | `MaxInterstitialAd/MaxRewardedAd/MaxAppOpenAd.showAd*` | 不执行（全部重载） | 同上 |
| A5 | `ADG.start()` | 不执行 → 不发 Ad Generation 请求 | 同上 |
| A6 | `DTBAdRequest.loadAd` / `AdLayout.loadAd` | 不执行 → 不发 Amazon 请求 | 同上 |
| A7 | `AdView.loadAd` / `InterstitialAd.show` / `AppOpenAd.show` / `AdManagerAdView.loadAd` | 不执行 | 类缺失跳过 |
| A8 | `UserMessagingPlatform.loadAndShowConsentFormIfRequired` / `ConsentForm.show` | 不显示 UMP 同意弹窗 | 同上 |

## 弹窗

| # | Hook | 行为 |
|---|---|---|
| D1 | `DialogFragment.show` / `showNow`（全部重载） | 读 Bundle 的 5 个 `*_event` key，命中促销事件前缀 → `setResult(null)` |

命中前缀：`TopLevelCharcoalDialogEvent`、`ConfirmNotificationPermissionDialogEvent`、`RoutingDialogEvent`。
功能性弹窗（`GenericDialogFragment`）不拦截。

## Premium

| # | Hook | 行为 |
|---|---|---|
| P1 | `PremiumActivity.onCreate` | 进入后立即 `finish()`，会员页无法停留 |

## 风险与已知限制

1. 广告视图被置为 `GONE` 而非从列表移除：RecyclerView 里若容器固定高度，可能留下空白间距；
   如需彻底移除，需要继续 hook 列表数据层（把 ad item 从 adapter 数据里删掉），属于 v2 工作。
2. 只 hook 了广告**展示**入口，没有拦截底层 HTTP（广告 SDK 若被其它路径触发仍可能发少量请求，但不会显示）。
3. Charcoal 弹窗按“事件类型”过滤：若 pixiv 之后新增促销事件类，需要补充前缀。
4. Premium 入口按钮保留在界面上，只是点击无效；彻底隐藏入口需要改菜单模型。
