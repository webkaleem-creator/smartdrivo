# Claim-First Migration Notes

Prior implementation (#448–450) used `/settings/global.specialOffer` ON time to grant automatic membership. The new version removes automatic grant in `PreferencesManager`: a `SpecialOccasionClaim` from an immutable, server-confirmed Firestore record is required.

Admin ON sets only a claim-availability window. Finite duration is counted from individual `claimedAt` server timestamp. Admin OFF revokes all promotional access. To prevent changing existing claims' semantics, admin duration cannot change during a live campaign.

Safety: No local client write is allowed to `/users/{uid}` membership expiry or payment records. For one per-user campaign record the Firestore rule checks user ownership, actual server time, campaign settings and eligibility. No rules deployment, Firebase live mutation, or installation is performed by a GitHub push alone. Existing older app builds still auto-grant and need upgrading. Firebase rules must be deployed and checked separately.
