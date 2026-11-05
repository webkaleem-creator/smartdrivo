# SmartDrivo v1.0.15 — Restore compact Home switches

Requested rollback of the oversized Style #3 Outline Toggles from v1.0.14.

- Restore the **exact HomeScreen.kt from verified Build #462** at commit `53d16ea0`.
- Smart Assistance uses its original compact blue ON / grey OFF Material switch and original master handler.
- Auto Reject — No Condition Match uses its original compact green ON / grey OFF Material switch and original reject handler.
- Remove now-unused HomeOutlineToggle component and its Style #3 color regression test.
- Keep every other feature and backend/engine file untouched: Pickup Guard quick menu/Hide logic, five-second reason overlays, Fastest/Normal filters, Rapido/Ola/Uber handling, membership and Firebase.
- **VersionCode 24**, versionName `1.0.15`. The higher versionCode enables safe in-place installation over v1.0.14 (versionCode 23) without Android downgrade/data reset.
- No automatic installs, uninstalls or data clears. Only `adb install -r` after validating exact signed release APK/certificate and installed package.

Verification: JVM unit tests, signed release build and certificate SHA-256 must pass. Phone UI and in-place install require user testing.
