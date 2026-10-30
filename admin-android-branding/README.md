# SmartDrivo Admin Android branding and screenshot fix

This is a **public, limited change backup**, not the full Admin Android app source.
It contains only the approved orange shield / white gear launcher resources and a
minimal description of the screenshot toggle change. No credentials, customer
records, Firebase configuration, signing keys or full AdminActivity are published.

To use the icon assets in the separate `SmartDrivoAdminAndroid/admin-android`
project, copy `res/` into `app/src/main/res/`, and in the app's AndroidManifest
`<application>` specify:

```xml
android:icon="@mipmap/sd_admin_shield"
android:roundIcon="@mipmap/sd_admin_shield_round"
```

For the Admin dashboard screenshot issue, `AdminActivity.kt` was changed from:

```kotlin
window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
```

to:

```kotlin
window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
```

Warning: this change permits screenshots of the Admin Activity, including
sensitive data shown there. Add separate protection for credential screens
as needed. Driver app V19 and its auto-accept engine are unrelated.

This repository is public; do not commit full Admin source without a separate
security review or use a private repository for full backups.