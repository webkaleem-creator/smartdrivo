# SmartDrivo v1.0.14 — Home Style #3 Outline Toggles

Selected screenshot: Style 3 "Outline Toggle".

- Replace **only** the Home screen switch appearance for **Smart Assistance** and **Auto Reject — No Condition Match**.
- Blue outline with blue circular thumb and ON label in enabled state; grey outline with grey circular thumb and OFF label when disabled.
- The existing Smart Assistance blue lightning icon and Auto Reject green shield icon remain unchanged.
- Reuse the existing Home handlers `handleToggleAutoAccept` and `changeReject`, including the existing permission checks, persistence, snackbar feedback and broadcasts.
- **Do not change** the Fastest Mode toggle, Pickup Guard floating bubble, settings screens, membership, Rapido/Ola/Uber order parsers, filter conditions or click paths.
- New outlined toggle uses Compose `toggleable` with `Role.Switch` and external, persisted state.
- Release upgrade: `com.smartdrivo.app`; versionCode 23; versionName 1.0.14.

Build verification: run complete `:app:testDebugUnitTest`, signed release `assembleRelease` and APK certificate SHA-256 match. Real-phone UI confirmation required.
