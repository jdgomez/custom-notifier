## Context

See proposal.md - Why. `SettingsViewModel` collects `CalendarChannelSettings.observe()` and recomputes the state asynchronously (`refresh` then `recompute`, which checks access and the calendar). Until the first recompute ends, `SettingsState` holds its defaults.

## Decisions

### 1. Explicit loading state
`SettingsState.channelOn: Boolean = false` becomes `channel: ChannelSwitch = ChannelSwitch.Loading` with `enum ChannelSwitch { Loading, Off, On }`. `recompute` sets `Off` or `On`; nothing sets `Loading` again, so a later refresh never disables a loaded switch. A read-only `channelOn` stays on the state for callers that only need the loaded value.

### 2. Screen
The `toggleable` row and its `Switch` get `enabled = channel != Loading`. A disabled `toggleable` ignores taps and exposes the disabled semantics. The loaded UI is unchanged, so the screenshot references stay.

### 3. Other controls
The "Allow" button and the "Calendar" row only appear after `recompute` sets them, so they cannot be tapped before load. Dialog buttons act on dialog state, not on defaults.

### 4. Failure to load
If `recompute` throws (a gateway failure), `relaunch` swallows it and the state stays `Loading`; the next resume refresh retries. The switch stays disabled rather than acting on a guess.

### 5. Tests
A view model test (loading until the setting is emitted), a Compose test (disabled and ignoring taps while loading), and the instrumented tests wait with `awaitSettingsSwitchEnabled()` before tapping.

## Risks

None known. The state of the switch shows disabled for a few frames at most on a normal device.
