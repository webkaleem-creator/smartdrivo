# Ola-only read-only accessibility reader

## Behavior
- User: logged-in admin only, not blocked or in Banking Mode.
- Strict event package: \`com.olacabs.oladriver\`. The Accessibility Service adds only this package to monitored events.
- No Ola Accept, Reject, Shizuku input, touch, accessibility action, history write, network request or notification.
- Even with Auto Accept ON, Ola action paths remain disabled; the new route only records a process-memory preview.
- Does NOT read pickup address: the collector checks the view ID allowlist *before* node text.
- Rate-limited to 1 read every 500 ms and up to 190 nodes / 10 levels; no scanning of unrelated apps.
- Screenshots, APK strings and third-party resource IDs are hints, NOT evidence the current Ola app exposes them. Unknown UI IDs show "Not read" rather than guessed values.
- Snapshot expires after two minutes on the owner setup screen and disappears on process restart.
- Monitoring Android Accessibility is subject to OEM limitations. Existing Rapido and Uber click paths remain unchanged.

## Known tentative Ola UI resource IDs
- Fare: \`tv_compact_price\` (\`₹63 | ₹32/km\` -> ₹63).
- Pickup: \`tv_pickup_distance_time\` (\`1.2 km • 6 mins\`).
- Trip: \`tv_drop_distance_time\` (\`1.5 km • 7 mins\`).
- Drop: \`tv_detail_drop_address\` (string), NEVER \`tv_detail_pickup_address\`.

## Phone test
1. Keep SmartDrivo Accessibility enabled and owner account signed in.
2. Open an actual Ola incoming offer and *do not* accept it in SmartDrivo.
3. Return promptly to SmartDrivo → More → Ola • Shizuku Setup.
4. Report the read-only diagnostic fields or "expected resource IDs not found".
5. If fields remain missing, capture a redacted, consented UI hierarchy from the user's own device before adding fallback ids.
6. No auto-click implementation until the real offer, event ownership, stale-offer prevention and post-tap confirmation are separately verified.

## Release
This is a developer validation step, not a ready Ola auto-accept product; do not claim otherwise.
