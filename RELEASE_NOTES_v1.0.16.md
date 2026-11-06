# SmartDrivo v1.0.16 — Razorpay Test-Only Checkout Staging

- Base: main Build #467 green, v1.0.15/versionCode24.
- Added isolated Razorpay Checkout Activity, only for **TEST keys**, with Firebase
  ID token and Cloudflare Worker order creation + confirmation.
- User membership is never unlocked by Android callbacks; only server-verified
  Firestore updates grant membership, using existing live sync.
- Razorpay UI is hidden unless an explicitly configured PUBLIC Worker URL exists
  in build environment AND settings/global.razorpayEnabled == true.
- Default CI build has empty endpoint, so original UPI/UTR screen and workflow
  remain **unchanged** until user completes secure external setup.
- Existing manual payment and admin approval, membership, filters, Fastest,
  Pickup Guard, popups and ride processing unchanged.
- VersionCode25 prevents in-place Android downgrade; signed APK required.
- No Cloudflare deployment, Firebase billing upgrade or live payment attempted.
