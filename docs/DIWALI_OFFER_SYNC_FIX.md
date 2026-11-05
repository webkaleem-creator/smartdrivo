# Build #448 Diwali offer sync — metadata transition fix

## Root cause
An initial Firestore snapshot could come from cache, which was intentionally not trusted for free entitlement. The global settings listener was attached without `MetadataChanges.INCLUDE`. When server data matched cached data, only `isFromCache` metadata changed, and by default Firestore delivered no second callback. The valid ON special campaign therefore remained OFF in app memory.

## Fix
Only the global document listener in `FirebaseRepository.fetchGlobalSettings()` now uses `addSnapshotListener(MetadataChanges.INCLUDE)` so the server confirmation arrives even when no data fields change. Offer still fails closed for cached/missing/error snapshots; only a server-verified LIVE campaign grants temporary access. An explicit per-session sync status is displayed in Membership if no LIVE offer banner is available. Logout clears cached in-memory promotional access.

## Coverage
`SpecialOfferSnapshotPolicyTest` reproduces cache ON then identical server ON (granted only on server event), server OFF, and absent offer. No Firestore rules, paid-plan expiry, order engine, auto-accept filters, Ola or Shizuku source changed.

## Manual test
1. Install newer signed APK in-place, keep current login.
2. Verify Web Admin shows LIVE and valid 3-day offer.
3. Open Membership while online; observe a Diwali banner after server sync.
4. If no banner, note the visible sync status (`CHECKING`, `CACHE_ONLY`, `SERVER_NO_OFFER`, or error) to identify remaining problems.
5. Turn OFF in Admin and verify user re-lock; paid user remains paid.

Note: Android client cannot prove the web deployment or live Firestore value without an authenticated connection. No existing users or live campaign settings are changed by code push.
