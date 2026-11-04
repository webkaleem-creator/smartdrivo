# Ola-only Shizuku preparation (owner preview)

## Scope
- Optional official Shizuku API: `dev.rikka.shizuku:api:13.1.5` and `provider:13.1.5`.
- Administrative More -> Ola • Shizuku Setup.
- Shows binder running status, permission state, manual permission request, official installation link.
- No Ola automation/click engine activated, no extra Accessibility scans and no calls from Rapido/Uber.
- Never toggles Developer Options, disables security checks, or controls the Shizuku manager on behalf of the user.
- A future Ola-only privileged input bridge must require permission, master ON, Ola enabled, exact verified foreground `com.olacabs.oladriver` and live-offer identity recheck immediately before clicks.

## Uploaded APK observations (static inspection only)
- RideClicker v1.1.11: SHA256 `456d9240a650a5623c7a226d8432ab5d76ec704ac07d97fe0e54a00d31579cfe`.
  Manifest package `com.triplogger.app`. Strings include `OLA:FastTap[Shizuku]`, `OLA:Method1[Shizuku]` and `com.olacabs.oladriver:id/btn_accept`.
- shizuku-rideclicker v13.7.1: SHA256 `e61ca8f8ced388c437773deabb291ab0cebab91f6645fbac9d263c75703059de`.
  Manifest package `com.zola.control`; this is not the normal official Shizuku package. No claim of safety or compatibility without further evaluation.
- APK static strings do NOT establish success rate, runtime safety, authentic signing provenance or capability on current Ola app UI.

## Required next Ola development
1. User installs official Shizuku and manually approves SmartDrivo permission.
2. Capture actual Ola offer UI hierarchy on consenting developer device, with fare, pickup/drop and accept target verified.
3. Build isolated Ola offer parser + filter decision (no pickup-address reading if unnecessary).
4. Build Shizuku user-service input adapter that targets only a freshly verified Ola offer; no blind coordinates or repeated taps.
5. Test service loss, permission denial, app reboot, altered offer, duplicate offers and Android/Ola UI updates.
6. Keep Ola master switch OFF and production auto-action disabled until phone testing passes.

## Release status
- Foundation only: not an Ola auto-accept release.
- Current app version remains unchanged. Do not distribute as a complete Ola solution.
