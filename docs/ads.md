# pixiv 6.202.1 广告链路分析

APK: `jp.pxv.android` 6.202.1 (versionCode 69581)，targetSdk 36，4 个 dex。

## 接入的广告 SDK（全部为中文/日本常见聚合）

| SDK | 包名 | 证据 |
|---|---|---|
| Google Mobile Ads + UMP 同意弹窗 | `com.google.android.gms.ads.*`, `com.google.android.ump.*` | manifest 的 `MobileAdsInitProvider`、`AdActivity`；`defpackage.tm3` 调 `loadAndShowConsentFormIfRequired` |
| AppLovin MAX | `com.applovin.*` | `libapplovin-native-crash-reporter.so`；pixiv 直接 new `MaxAdView` |
| Ad Generation (Supership) | `com.socdm.d.adgeneration.*`, `jp.supership.*` | `RectangleADGAutoRotationView` 内 `new ADG(ctx)` + `start()` |
| Amazon APS / TAM | `com.amazon.device.ads.*`, `com.amazon.aps.*` | `RectangleAdgTamView` 里 `new DTBAdRequest(...).loadAd(...)` |
| Prebid Mobile | `org.prebid.mobile.*` | `RectangleAppLovinView` 的 `MediationBannerAdUnit`（MAX 适配器） |
| Pangle 穿山甲 | `com.bytedance.sdk.openadsdk.*` | manifest 里 20+ `TT*Activity`、`libtt_ugen_layout.so` |
| InMobi / Mintegral / five | `com.inmobi.*`, `com.mbridge.*`, `com.five_corp.ad.*` | manifest、assets 模板 |

## pixiv 自己的广告槽位

服务端下发广告配置，DTO 都叫 `UnSafe*`（`data/advertisement/remote/dto`）：

- `UnSafeYufulightShowResponse`：总配置，含 `type / adgData / adgTamData / applovinData / yflData / fallback / rotationInterval`
- `UnSafeAdgData(location_id)`、`UnSafeAdgTamData(location_id, slot_id)`、`UnSafeAppLovinData(ad_unit_id)`、`UnSafeYflData(image, link)`
- 映射成 sealed 类（defpackage）：`ebb`=ADG、`fbb`=AdgTam、`gbb`=AppLovin、`ibb`=YFL，打包进 `jbb`（YufulightAdvertisementInformation）

视图层（`feature/advertisement/view/`，共 17 个）：

- 矩形广告：`RectangleAdSwitchView` 在 ADG / Yufulight / AdgTam / AppLovin 之间切换
- 漫画网格：`MangaGridAdSwitchView` / `MangaGridADGView` / `MangaGridPlaceholderView`
- 小说原生：`NovelNativeAdSwitchView` / `NovelNativeADGView`
- 浮层：`OverlayAppLovinView` / `OverlayADGAutoRotationView` / `OverlayAdgTamView`
- 自营：`GridSelfServeView`（`SelfServeAdvertisementApiModel`：click_url/imp_url/view_url/ad_image_url/ad_title/ad_text）、`SelfServeRelatedWorksView`
- 其它：`RightTopEdgeAdsBackgroundView`

## 实际下发的广告请求入口（已 hook）

| 类 | 方法 | 说明 |
|---|---|---|
| `com.applovin.mediation.ads.MaxAdView` | `loadAd()` / `startAutoRefresh()` | MAX banner |
| `MaxInterstitialAd` / `MaxRewardedAd` / `MaxAppOpenAd` | `showAd(...)` 全部重载 | MAX 插屏/激励/开屏 |
| `com.socdm.d.adgeneration.ADG` | `start()` | Ad Generation 开始请求/轮播 |
| `com.amazon.device.ads.DTBAdRequest` / `AdLayout` | `loadAd(...)` | Amazon TAM |
| `com.google.android.gms.ads.AdView` / `InterstitialAd` / `AppOpenAd` | `loadAd` / `show` | GMA 直投 |
| `com.google.android.ump.UserMessagingPlatform` | `loadAndShowConsentFormIfRequired` | UMP 同意弹窗 |
| `com.google.android.ump.ConsentForm` | `show` | 同意表单 |
