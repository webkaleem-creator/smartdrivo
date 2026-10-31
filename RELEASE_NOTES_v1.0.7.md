# SmartDrivo v1.0.7 (Build 12)

**Release date:** 31 October 2026

## What's improved
- Rapido after-click confirmation recognizes more explicit accepted trip and pickup-screen states.
- Split accessibility labels can now be checked together to avoid missing genuine positive status text.
- Late Rapido confirmation observation increased to 20 seconds, without extra Accept clicks.
- History recovery timeout increased to 30 seconds to avoid premature status finalization.
- For unresolved attempts, History displays **ACCEPT SENT**, not **UNCONFIRMED**. Only positively verified rides count as **ACCEPTED**.

## Unchanged
Rapido V20 pickup/distance/fare filters, Auto Reject, No-Go, Ola and the separate Admin Panel.

## Installation
Download **SmartDrivo-v1.0.7-release.apk** from Assets below. Install it **over** the existing signed SmartDrivo Driver app rather than uninstalling first. Matching signing certificates preserve local settings and History on in-place updates.

Package: `com.smartdrivo.app` · versionName `1.0.7` · versionCode `12`.

**Note:** Automated tests and APK signing verification gate this release. Live verification across all Rapido UI variants is still ongoing. The app intentionally does not count an unverified Accept tap as a successful ride.
