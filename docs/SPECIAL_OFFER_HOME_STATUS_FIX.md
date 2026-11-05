# Home membership status for special occasion access

Build #449 confirmed the new account receives the Diwali popup and has an unlocked Home screen, but `MembershipDisplay.of` used only stored `plan`/`planExpireMillis`, displaying No Plan / Inactive / Not activated even with a live, in-memory occasion entitlement.

This patch makes the Home summary use the same live admin offer seen by the membership page, while requiring the user's already-derived, non-persistent `specialOfferUntilMillis` plus the live offer and an unblocked active account.

- Live paid/free-trial subscription still takes precedence.
- When paid/trial has expired but the admin occasion is valid, show the offer's title, Active, days left (ceil by 24h).
- Unlimited shows Unlimited, but disappears immediately when the admin OFF update arrives. No new access grant is made by this UI display code.
- When offer OFF/expired, show original paid/trial/No Plan state. Membership records and expiry fields are unchanged.
- Does not touch Firebase write paths, app/driver filters, Rapido, Ola, Shizuku or order acceptance service.

Regression tests cover free user + 3-day Diwali, paid priority, paid expiry fallback, unlimited, OFF revocation, blocked users, and original membership display cases.

Manual: Build after CI; confirm on test new ID the Home summary shows Diwali Active / 3 days left, Membership screen also shows promotional entitlement; confirm setting OFF reverts Home and lock; confirm paid 3-day account still displays its actual paid subscription.
