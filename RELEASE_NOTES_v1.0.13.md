# SmartDrivo v1.0.13 — Quick Controls Hide Button + 5-Second Reason Popup Reliability

Built from main Build #456 (v1.0.11 / versionCode 20).

## Owner Quick Controls UI
- A clearly labelled **Hide** button sits immediately to the right of the one-line "SmartDrivo Quick Controls" heading, alongside the existing **×**.
- **Hide** and **×** call only `hideMenu()`, which removes the large overlay window.
- Neither changes Main Auto Accept, Pickup Guard state, or the small PG bubble. Long-press PG again to reopen the menu.
- Existing Pause, Emergency Stop, pickup preset 0.4/0.6/0.8 km and green/red master indicator remain unchanged.

## 5-Second Short Order Status Popup Reliability
- Timestamp-variant detections of the same Rapido offer are treated as one banner identity, preventing false dismissal.
- Update duplicate-display ownership only after the Android overlay is added successfully; retain 5000 ms display lifetime.
- Log a clear reason if overlay permission missing, OS popup creation fails, or the More > Notifications option is OFF.
- Existing status priorities prevent stale uncertain updates from downgrading verified accepted rides.

## Safety / Release
- Package: `com.smartdrivo.app` ; versionCode: **22**; versionName **1.0.13**.
- No modifications to Rapido/Ola/Uber parsers, accept/reject click target logic, filters, membership or Firebase rules.
- Full JVM debug tests, signed release build, certificate integrity, and real-phone tests are required before merging to main or public distribution.
- Safe in-place installation only. Never uninstall or clear data.
