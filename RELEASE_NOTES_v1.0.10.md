# SmartDrivo v1.0.10 — Rapido Public Release

Release date: 2026-11-04

- Preserves the phone-tested Rapido Pickup Guard behavior from GitHub Actions Build #414.
- Faster, immediate History reason after Skip, with background verification that cannot block the UI.
- Preserves saved Rapido Fastest Mode, normal filters and Pickup Guard settings.
- Ola auto-order engine, Ola Admin Filter UI/settings and diagnostic reporter removed. Ola live order processing and clicks are disabled for everyone.
- Previously saved Ola History records remain readable; in-place updates never delete user data.
- Uber behavior is unchanged from the Build #414 source baseline.

Upgrade: app com.smartdrivo.app, versionCode 19, versionName 1.0.10. Signed by existing release signing certificate through GitHub CI.

Note: CI and static tests cannot replace testing new releases on a real phone.