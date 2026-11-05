# SmartDrivo v1.0.12 — 5-second Short Order Popup Reliability

Built from verified main Build #456 (v1.0.11 / versionCode 20).

- Restores reliable visibility of the compact dark accepted/ignored/rejected reason overlay.
- Same real Rapido offer can have varying detection timestamps across Accessibility events; popup identity now uses its stable fingerprint, preventing redundant events from prematurely hiding or suppressing the final 5-second banner.
- Deduplication and terminal-status tracking are updated **only after WindowManager successfully adds the popup**; an overlay failure cannot silently consume the next chance to show it.
- Keeps exactly **5,000 ms** display time, replaces the banner on genuinely distinct offers, and prevents late uncertain statuses from overwriting a confirmed Accepted status.
- Logs useful diagnoses if "Display over other apps" is missing, OS overlay creation throws, or More > Notifications > Short Order Reason Popups is OFF.
- Preserves the user's saved popup setting; does not silently override an intentional OFF setting.
- Does not touch Rapido/Ola/Uber order parsing, click selectors, rate limits, owner Pickup Guard settings, Firebase membership, history retention, or payment logic.

Checks: full JVM regression suite, signed release build, signature match, stationary phone tests with legitimate live offer. Signature and test results must be verified before release. No install/uninstall/data clear or Firebase mutation in GitHub commits.

Package `com.smartdrivo.app`, versionCode **21**, versionName **1.0.12**.
