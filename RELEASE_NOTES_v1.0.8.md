# SmartDrivo v1.0.8 — Two-day history retention

- Retain only Today and Yesterday based on Asia/Kolkata calendar days.
- Permanently purge expired local Room database records.
- Remove expired legacy SharedPreferences history records.
- Refresh History counts and date filters; purge again on midnight.
- Preserve Banking Mode and compact short status popup fixes.
- Existing app data is preserved when upgrading the signed APK.

This release deletes expired **on-device** order-history records, not any historic Firestore cloud copies.
