# SmartDrivo v1.0.16 — Rapido false booking ID / History collision patch

- Based exactly on main Build #467 GREEN commit da865375; user-approved
  phone app baseline v1.0.15/versionCode24.
- 2026-11-06 17:23:46 on-device logs showed genuine Rapido ₹77,
  pickup 0.3 km, trip 2.4 km detected, inserted and finalized IGNORED,
  but booking ID parsed as the single character `e`.
- ROOT CAUSE: Rapido parser's unanchored `ID` alternative matched
  the middle of the common word `Ride`, creating the Room key `e`.
  Subsequent real offers could reuse that primary key; `@Insert(REPLACE)`
  could overwrite previous History records.
- Both Rapido parsers now share the strict pure `RapidoBookingIdExtractor`.
  Only explicitly labeled alphanumeric >=5 characters containing a digit
  are accepted; unlabeled text and one-letter false IDs are ignored.
  Genuine offer with no visible booking ID falls back to existing
  fingerprint+timestamp History identity. No pickup address reading added.
- Legacy Rapido Auto Priority candidate and Fast snapshot guard are
  consistent. Other platforms unchanged.
- Pickup Guard cutoff decisions, Skip/Accept click flow, Filters, pricing,
  popups, UI and Firebase/Razorpay work unchanged.
- New regression tests include actual `Ride -> e` bug, invalid labels
  and real explicit IDs. CI signed release and mobile test must pass
  before merging to main. Existing overwritten rows cannot be restored
  without a backup.
