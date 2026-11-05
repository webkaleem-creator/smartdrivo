# SmartDrivo v1.0.14 — Assistant Power Button Style 1

Built from main v1.0.13 (versionCode 22).

## SmartDrivo Assistant UI
- Replace the two Home screen sliders with prominent **Power ⏻** icon buttons.
- **Smart Assistance**: active button is bright blue with **ON**; inactive button is grey with **OFF**.
- **Auto Reject — No Condition Match**: active button is green with **ON**; inactive button is grey with **OFF**.
- Existing full-row tapping and separate saved-state handlers are preserved.
- Existing permissions dialog, Auto Accept broadcast behavior, Auto Reject snackbar, paused/running labels and preferences are untouched.
- Fastest Mode, ride processing, pickup and fare filters, history, memberships, Firebase, and overlay behavior are unchanged.

## Regression and release safety
- Add a Compose/Robolectric test covering ON/OFF state presentation.
- Package: `com.smartdrivo.app`; versionCode: **23**; versionName: **1.0.14**.
- Require passing unit tests, a signed release APK, release certificate verification and a successful in-place phone installation before updating main.
- Never uninstall or clear app data. Roll back to v1.0.13 if verification fails.
