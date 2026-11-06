# SmartDrivo Razorpay Auto-Membership — staging foundation only

**Not deployed, not enabled, and no APK checkout added yet.** The current production
manual UPI/UTR approval continues to work. Source alone cannot process payments.

## Why a server is required
Android success callbacks and UTRs are not proof that payment was captured.
Only the backend (Cloud Functions) can hold Razorpay Key Secret and Webhook Secret
and update users' protected \`plan\`, \`planExpireMillis\` and \`isApproved\`.

## Architecture (directly distributed APK; Play Store requires billing-policy review)

1. Firebase-authenticated Android client calls \`createSmartDrivoOrder\` with a plan ID.
2. Backend reads prices from \`settings/global.planPrices\` and verifies active user.
3. Backend calls Razorpay Orders API and stores \`razorpayOrders/<order_id>\`.
4. Android launches the official Razorpay Checkout SDK using the public Key ID
   returned with the server-created Order ID, amount and currency.
5. After payment, Android calls \`confirmSmartDrivoPayment\` with Razorpay
   payment ID, order ID and signature. Server verifies HMAC AND fetches captured
   payment via Razorpay API. **No client alone can grant a plan.**
6. The \`smartDrivoRazorpayWebhook\` endpoint independently processes signed
   \`payment.captured\` events even if the phone is closed or offline.
7. A Firestore transaction checks Order ID, amount, currency, UID, plan,
   captured state and payment ID uniqueness. It extends remaining expiry or
   starts from the server's current time. Duplicate webhook deliveries are
   idempotent; a duplicate never extends membership twice.
8. Existing \`startOwnMembershipSync()\` Firestore snapshot listener on Android
   updates active membership in near-real-time (network latency varies).

## Merchant prerequisites — NOT included in repository
- Razorpay account with full KYC and live payment activation.
- Razorpay live/test Key ID and Key Secret; configure as Firebase Secret Manager
  \`RAZORPAY_KEY_ID\`, \`RAZORPAY_KEY_SECRET\`, \`RAZORPAY_WEBHOOK_SECRET\`.
  **Never paste any secret in GitHub, app source, screenshots or chat.**
- Firebase Cloud Functions enabled (may require Firebase Blaze billing plan).
- Test and live webhook endpoint URLs separately; subscribe to \`payment.captured\`.
- Deploy Cloud Functions in a controlled step after test suite and emulator tests.
- Set \`settings/global.razorpayEnabled: true\` **only after deployment/QA**
  and plan prices verified; all functions fail closed if flag is false.
- Server-only Firestore rules should deny all client reads/writes to
  \`razorpayOrders\` and \`razorpayPayments\`. This repo's current production
  rules have NOT been deployed or changed in this branch.

## Go-live blockers
- Android Checkout UI + callback wiring and navigation must be implemented and
  instrumented using actual test-mode Razorpay account (no API keys provided).
- Test captured, declined, pending, refunded, webhook delayed, duplicate,
  replay, revoked/blocked account, manual renewal overlap and server errors.
- Add refund/chargeback and reconciliation policy before accepting real payments.
- Confirm distribution route: subscriptions to app functionality on Google Play
  generally require Google Play Billing unless eligible for/enrolled in India
  alternative billing programme; Razorpay direct-in-app is suitable for compliant
  direct APK distribution.
- Validate live production Firestore security rules before deploy; do not
  overwrite the current working rules via a default deploy.
- Admin transaction audit log and customer receipts/help/refund policy.

## Test locally
\`cd functions && npm test && npm run check\` — tests are Node built-in and
do not require a merchant account or network.
