# Special Occasion Free Plan — SmartDrivo

## Admin actions
Both `admin/index.html` and `admin/dashboard.html` and the in-app `Admin Control Panel > Free Offer` tab edit the same Firestore `/settings/global.specialOffer` map. Firestore security rules allow only admins to write `/settings`.

Admin chooses title, message, duration (1/3/7/30 days or 0 = unlimited), then Save & Publish. OFF is the default when no campaign exists. OFF stops promotional access as soon as the logged-in app receives the live Firestore update. Toggling from OFF to ON starts a fresh campaign ID and countdown; editing a live campaign keeps its original start time.

## Driver experience
- An active offer appears at the very top of Membership, before paid choices.
- Eligible free accounts automatically get temporary access, with **no payment, UTR or self-grant transaction**.
- A one-time local notification and popup appear when an eligible signed-in app receives a new campaign. Tapping the popup opens Membership.
- Notifications require Android permission and an app process receiving Firestore updates. **This is not Firebase Cloud Messaging**; apps force-stopped or not running cannot receive instant pushes. That requires a separate authenticated FCM backend.
- Paid subscriptions and their expiry fields are not modified. Existing 1-day trial anti-abuse logic is unchanged.
- Only the admin can write the global config; a normal user cannot choose a duration or grant membership. The temporary entitlement is derived from the server-owned Firestore document and is not written to `/users`.
- Offer ends at earliest of configured duration expiry or admin OFF. `Unlimited` lasts only while ON, not a permanent paid entitlement. Offline devices may not receive an OFF event until reconnect; do not advertise instantaneous remote revocation to disconnected devices.
- Same existing Firebase global listener observes offer updates. No extra network traffic or scanning in any ride acceptance path.

## Rollout
- Set an offer title/message/duration but leave OFF to stage it.
- Install a signed APK built from this commit. Verify both web and mobile admin editors, then enable for a controlled live test.
- Confirm banner, notification, popup, after-OFF removal, expiry, and unchanged paid status on test accounts.
- The Render deployment and Firestore live data are not modified by pushing this repository alone unless hosting auto-deploy is configured. No Firebase rule deployment is necessary because the pre-existing `/settings` write rule is admin-only.
