# SmartDrivo Special Occasion Free Plan — Claim-First

## Required behavior
- Admin ON publishes the offer for eligible users; it does NOT start any user's free days or unlock membership.
- Membership page displays `Claim 3 Days FREE` (or selected 1/7/30/Unlimited). Pressing it explicitly creates the claim.
- Each eligible user receives a separate 72 hours from the claim's Firebase **server timestamp**, so claiming today versus tomorrow gives independently counted periods.
- One immutable `/users/{uid}/occasionClaims/{campaignId}` document per campaign. Claim cannot be repeated, reset or transferred to another account.
- Once OFF, all promotional access stops; paid membership and trial fields remain unchanged. A future OFF->ON creates a new campaign ID. Duration locks while ON.
- Free accounts must be online and see a server-confirmed claim before tools unlock. Cached offers/claims, pending writes, failed verification, or a missing claim never activate membership.
- Paid/trial accounts keep their existing plan and cannot claim until it expires. Popup/notification invite the user to Claim; they do not activate anything.
- Unlimited lasts while the campaign remains ON, but still requires one explicit Claim.

## Server-side rules deployment is required
`firestore.rules` includes an owner-only GET/CREATE rule validating the campaign ID, ON state, duration, eligible user profile and `claimedAt == request.time`; UPDATE, DELETE and LIST are denied. The claim document has exactly `uid`, `campaignId`, `days`, `claimedAt` fields.

**GitHub push / APK installation alone do NOT deploy Firestore rules.** Before claims can succeed, deploy reviewed rules in the correct Firebase project from the repository root:

```powershell
firebase deploy --only firestore:rules --project YOUR_VERIFIED_FIREBASE_PROJECT_ID
```

Use your real project ID. Verify the new `/users/{uid}/occasionClaims/{campaignId}` match in Firebase Console Rules. The deployment cannot be performed merely by GitHub source changes.

## Rollout and old-client limitation
Older builds (#450 and earlier) still automatically grant the ON offer locally, even after new rules are deployed. They MUST be upgraded (or covered by separately planned minimum-version enforcement) before claiming this is universal. Do not reset paid user data, uninstall the app, or change UTR/payment security.

## Phone regression checklist
1. Open a new eligible account with the new APK while the offer is ON: Home must remain locked BEFORE Claim.
2. Tap Membership `Claim 3 Days FREE`; Firebase must confirm first. Home then shows Active / offer title / 3 days left.
3. Reopen: same claim expires at original timestamp; re-claim is denied; reinstall doesn't reset.
4. Another eligible account claims a day later: gets its own 72h, not the first user's end time.
5. Admin OFF: promotional access stops; paid account's membership remains as before.
6. Offline/cache-only claim cannot create new access; paid plans, Rapido, Ola and GO TO / +Amount filters stay unchanged.
