# SmartDrivo v1.0.17 — Razorpay TEST-ONLY staging based on Build #470

- Base: main Build #470, v1.0.16/versionCode25, with Rapido History booking-ID collision fix retained.
- Candidate version: v1.0.17/versionCode26; intended for staging only, not users.
- Cloudflare Workers Free (PAYMENTS_ENABLED=false) verifier, HMAC and captured-payment checks, Firestore atomic idempotent grants.
- Android Razorpay Checkout is TEST-key only, remains hidden without explicit PUBLIC HTTPS Worker URL **and** remote `settings/global.razorpayEnabled=true`.
- Default CI build has blank Worker endpoint. Manual UPI/UTR, membership, filters, Pickup Guard, Rapido and existing features are preserved.
- No live Razorpay charge, Cloudflare deployment, Firebase billing upgrade, APK installation, or main merge.
- Do not activate or public-release until merchant sandbox, real device, webhook replay and backend recovery tests pass.
