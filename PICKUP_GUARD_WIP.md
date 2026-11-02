# SmartDrivo owner-only Pickup Guard — WIP

Do not merge, release, or install this branch for live driving before CI and
stationary supervised tests.

## Authorization

Requires Firebase Auth custom claim `pickupGuardOwner: true` on **only the
owner's UID**, plus an active admin profile. A privileged backend using Firebase
Admin SDK must assign or revoke the claim; the app never grants it.
Do not add hardcoded UIDs, user-editable flags, shared PINs or credentials.
Expired/failed auth is fail-closed. Guard defaults OFF; saved per signed-in UID.

## Intended behavior

- More → Admin → Pickup Guard; 0.6 km default, editable and saved.
- Active Guard has priority over Fastest/Global/+Amount/Unlimited/Bundle/Go-To.
- At or below pickup maximum, destination No-Go clear => manual choice; no
  auto-accept and no automatic interaction with the offer.
- Outside pickup maximum or No-Go destination match => verified same-platform
  Reject/X action, no blind coordinate tap, gesture or ancestor substitution.
- Pickup/destination unreadable => no action; History explains why.
- No-Go uses existing enabled destination keywords, never pickup addresses.
- No safe Reject control, or an action without UI confirmation => do not
  misrepresent the result as a confirmed rejection.

## Known limitations before device verification

Uber may expose an icon-only X with no recognizable accessible label. The safe
selector deliberately does nothing in that case. Ola's X may also be
non-clickable. Platform confirmation after rejecting is not yet implemented;
History uses Ignored with a reason stating reject action was sent but unverified.

The preexisting per-platform genuine-offer gates still apply. Some partial
offers may not be logged until full offer data appears.

## Mandatory validation

Run focused and full JVM tests, release build/signing, and manual verification
of owner authorization/revocation, normal-user invisibility, no-go + limit
boundaries, missing data, real X buttons, duplicate events, offer replacement,
Rapido speed, Ola V3 repairs and Uber behavior. Main remains untouched.
