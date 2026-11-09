# pixiv 6.202.1 弹窗 / Premium 分析

## 两套对话框系统

1. **Charcoal 对话框**：`defpackage.uz0 extends androidx.fragment.app.DialogFragment`
   - 所有通用弹窗由 `defpackage.bg8.p(title, description, imageRes, buttons, requestKey, flags)` 构造 Bundle 后 show
   - Bundle key：`arguments_title`、`arguments_description`、`arguments_image_res`、`arguments_primary_button_text`、
     `arguments_primary_button_event`、`arguments_second_button_event`、`arguments_tertiary_button_event`、`arguments_request_key`、`arguments_is_cancelable`
   - 按钮模型 `qz0`（单键）/ `sz0`（双键）/ `rz0`（三键：PlayStore / Feedback / RateLater）

2. **GenericDialogFragment**（`feature/component/androidview/dialog/`）：用于**功能性**弹窗
   （删除作品确认、草稿恢复、Street 隐藏确认、邮件注册、用户限制重试）。**不要**整体屏蔽。

## 会被屏蔽的 Charcoal 弹窗事件族（Button event 类型）

| 事件类 | 用途 |
|---|---|
| `jp.pxv.android.feature.bottomnavigationroot.flux.TopLevelCharcoalDialogEvent$SelectRedirectPlayStore` | 更新 / 评分引导（去商店） |
| `...$SelectRedirectFeedback` | 反馈引导 |
| `...$SelectRateLater` | 评分稍后 |
| `...$SelectPixivInfoPrimaryButton` / `$SelectPixivInfoSecondaryButton` / `$OpenUrl` | pixiv 官方公告弹窗 |
| `jp.pxv.android.feature.androidnotification.ConfirmNotificationPermissionDialogEvent$*` | 通知权限请求弹窗 |
| `jp.pxv.android.feature.routing.main.RoutingDialogEvent$*` | 强制/可选更新对话框 |

## 实现方式

Hook `androidx.fragment.app.DialogFragment.show/showNow`，读 `thisObject.getArguments()`，
若三个 `arguments_*_button_event` 中任一类型命中上表前缀 → `param.setResult(null)` 直接不显示。
功能性对话框（GenericDialogFragment）不受影响。

## Premium

- `jp.pxv.android.feature.premium.lp.PremiumActivity` 是唯一 Premium 落地页（manifest 注册）。
- 入口散落在菜单/设置里，点击后都是启动该 Activity；模块在 `onCreate` 后立刻 `finish()`，
  因此入口仍然可见但点击不会进入会员页。
- 已在 `PremiumHooks` 中实现；若后续要彻底隐藏入口，需要再定位菜单项（`feature/bottomnavigationroot` 的 drawer 模型）。
