## 1. Notification access

- [ ] 1.1 Add `NotificationAccess` in `:app` (alerts package) reporting `Allowed`, `PermissionMissing(canAsk)`, `AppNotificationsOff`, `ChannelOff`, with the stored `permanentlyDenied` rule from design.md decision 2; expose it from `AppContainer`
- [ ] 1.2 Unit tests (Robolectric) for every state, including API 26 (no runtime permission), API 33+ never asked, denied once, permanently denied, dismissed, channel off, and the flag cleared when granted

## 2. Product list: banner and request

- [ ] 2.1 Extend `ProductListViewModel` and its state with the banner state (shown only with at least one product), re-checked on `ON_RESUME` and after each permission result; run the existing reconcile when the state turns `Allowed`
- [ ] 2.2 Add the disabled-alerts banner and its texts (spec) above the list, not dismissible; "Turn on" asks or opens the app or channel notification settings per the spec
- [ ] 2.3 Add the "Allow alerts?" explanation dialog and the permission launcher on the list; ask after a save signalled by the form (new and edit), never at launch
- [ ] 2.4 Robolectric Compose tests for every spec scenario that does not need a system dialog; screenshot references for the banner in light, dark and largest font size, and for the explanation dialog

## 3. End to end

- [ ] 3.1 Instrumented tests on `cn-api37` with UiAutomator for the system dialog: ask after the first save, grant (no banner), deny (banner, "Turn on" asks again), dismiss (asked again on the next save), permanent denial ("Turn on" opens the app notification settings), and a due alert shown after allowing from the banner
- [ ] 3.2 Run `scripts/e2e.sh` on `cn-api37` and check the video; boot `cn-api26`, install and verify by hand that no dialog appears on save and that turning the channel off shows the banner; shut the emulators down

## 4. Archive

- [ ] 4.1 Run `openspec archive add-notification-permission`, sync the delta spec into `openspec/specs/notification-permission/spec.md`, and update the Project state in `AGENTS.md` (#45 done, next #46)
