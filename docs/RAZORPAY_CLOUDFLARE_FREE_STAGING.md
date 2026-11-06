# SmartDrivo Razorpay Auto-Membership — Cloudflare Workers Free staging

**STATUS: NOT DEPLOYED, NOT LIVE, NOT IN MAIN.**
Production SmartDrivo Build #467 v1.0.15/code24 and manual UPI/UTR remain intact.
Razorpay has TEST API credentials, but the user must approve website/app to obtain live API keys.

## Cost policy
- Firebase remains **Spark (Free)**; do not upgrade to Blaze or deploy Firebase Cloud Functions.
- Cloudflare must remain **Workers Free**; do not enable paid plan, domain, add-ons or extra products.
- Free Workers currently allow 100,000 requests/day, with 10ms CPU per request and bounded subrequests. Quota exhaustion must fail closed; webhook/activation can be delayed.
- Free tiers and platform pricing may change: **cannot guarantee zero costs forever**.
- Razorpay standard platform fee normally 2% of successful transaction **plus GST on that fee**; additional payment-method or settlement fees can exist. Disable optional services; restrict checkout to standard UPI via Razorpay account configuration before live rollout.

## Automatic activation
1. Driver chooses one-time paid plan in Android. Android pays through official Checkout **only once Worker URL and admin flag are enabled**.
2. Android sends Firebase Auth ID token to Worker. Identity Toolkit verifies the user identity.
3. Worker reads admin plan price from Firebase Firestore and server-creates a Razorpay order; **never trusts a client-supplied price or UID**.
4. Official Checkout returns payment IDs and signed payment response. Android sends them to Worker; only Worker verifies Razorpay signature and GETs confirmed `captured` payment from Razorpay API.
5. Independent Razorpay-signed `payment.captured` webhook handles payment even if app closes.
6. One atomic Firestore `:commit` credits eligible user and stores unique payment/order ledger, with updateTime/existence preconditions. Duplicate webhooks cannot give bonus days. A new paid renewal extends remaining days.
7. The existing Firestore user snapshot listener shows membership ACTIVE after backend grant.
8. If verification is delayed or Firestore quota is exhausted, do not locally activate; show pending status and require safe reconciliation rather than a second payment.

## Before deployment (manual account setup required)
1. Register official free Cloudflare account, choose Workers Free **without entering paid-plan billing**. Obtain a `*.workers.dev` endpoint.
2. In Google Cloud IAM for the same Firebase project create a dedicated service account with least privileges sufficient for Firestore reads/writes, e.g. `roles/datastore.user`. Protect service-account JSON private key as sensitive credential. If organization disables service-account keys, stop and choose an approved secure alternative.
3. On the Cloudflare Worker add encrypted secrets for `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`, `FIREBASE_WEB_API_KEY`, `FIREBASE_SERVICE_ACCOUNT_JSON`. Do **not** paste them in chat, APK, GitHub source, PR or screenshots.
4. Keep `worker/wrangler.toml` PAYMENTS_ENABLED = "false" and Firebase settings/global.razorpayEnabled absent/false until complete sandbox verification.
5. From a trusted local machine: `cd worker`, `npm install`, `npm test`, `npx wrangler login`, `npx wrangler deploy`. Deployment may require choosing the Worker account and verifying the Free plan.
6. Configure Razorpay **TEST** webhook URL `https://<worker>.<account>.workers.dev/v1/webhook` with event `payment.captured` and a separate webhook signing secret.
7. Configure test secrets in Cloudflare, then enable test mode under controlled test account only. Use Razorpay official fake test payments (never real money). Test success, payment cancel, unauthorized, duplicate webhook, expired plan, active renewal, blocked user, internet failure, account fraud, and cleanup.
8. Set Android build environment variable `SMARTDRIVO_RAZORPAY_WORKER_URL` to exact verified HTTPS workers.dev URL for a new signed build. This URL is public; **API secrets never go into Android**. Default without URL means Razorpay UI remains hidden.
9. Deploy only reviewed Firestore Rules if modifications are needed; current rules are unchanged and new collections have no client rules by default. Verify clients cannot grant themselves plan fields.
10. To go LIVE, obtain Razorpay LIVE API permission (website/app review), replace TEST credentials with LIVE secrets through encrypted Worker controls, implement refunds/dispute/reconciliation and customer support, and rerun tests before switching admin enabled flags.

## Security and limits
- Cloudflare Worker is not a guarantee of 100% uptime; free quota can interrupt processing and trigger Razorpay webhook retries. Monitor failures and reconcile paid customers.
- Razorpay Checkout success is not proof of captured money. Worker refuses mismatched amount, currency, order and signature.
- Do not run payment webhooks in fail-open mode. Do not re-enable paid Firebase Cloud Functions staging.
- No untested code is auto-merged to main, no phone uninstall/data clear.
- For Play Store digital membership billing, check Google Play billing policy and eligible India alternative billing schemes before enabling Razorpay.

## Verified CI results (code only, no live captures)
- Worker HMAC/order/amount/currency, renewal expiry, double callback idempotency and simulated Firestore ledger tests: GREEN.
- Android v1.0.16/versionCode25 branch Build: `:app:testDebugUnitTest`, signed release, signature match GREEN.
- **No Cloudflare account, live endpoint, test payment or phone installation has been verified yet.**
