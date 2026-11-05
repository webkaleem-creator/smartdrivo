# Ola Device Diagnostics — read-only, admin-only

## How to access
SmartDrivo > More > Ola • Shizuku Setup > Device diagnostics • READ ONLY.

## What it shows
- Existing Shizuku binder running status and SmartDrivo authorization.
- Developer Options `development_settings_enabled`: app-reported value explicitly **masked, not proof of OFF**.
- USB Debugging `adb_enabled`: app-reported value explicitly **masked, not proof of OFF**.
- Wireless Debugging `adb_wifi_enabled`: app-reported indicator only, **not a connection test**.
- Refresh button; screen also rechecks every 3 seconds while open.
- Android may restrict or omit some values; show "Unavailable" rather than fabricate.
- The official Android Settings.Global API documents Developer/USB return 0 for third-party apps; commands run by adb shell can differ from app-visible values.

## Strict exclusions
- No setting mutation, Shizuku shell command, spoofing, or automatic hide action.
- No Ola Accept/Reject, Rapido/Uber processing changes, Accessibility event changes.
- No saved diagnostics, history data, background polling, Firebase writes or new permissions.

## Owner test checklist
1. Open Ola Shizuku Setup and verify running + authorized status.
2. Confirm Developer Options/USB are described as masked when reported 0, not "OFF".
3. Compare Wireless Debugging indicator with system Settings; distinguish app-reported status from live ADB connection.
4. Toggle nothing automatically. Confirm Read-Only Ola capture is unchanged.
5. Inspect Android diagnostics on Realme device, including cases where values are unavailable.
