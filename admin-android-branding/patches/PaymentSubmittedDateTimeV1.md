# Native Admin Payment submission date/time display

This public backup stores ONLY the scoped UI diff to `AdminActivity.kt` from the separate Android Admin project. It is NOT a full backup of the project.

- Show actual `Payment.submittedAt` date/time on every card in Pending and All Payments, formatted in India Standard Time (IST).
- If `Payment.approvedAt` is declared and populated, show that timestamp on Approved cards too.
- Historical entries without a usable timestamp display `Not recorded` (no fabricated date).
- Do not modify /payments documents, UTRs, amounts, approvals, driver accounts, AdminStore or Firebase config.
- Driver V20, phone groupings and admin auth are unchanged.

The full native Admin project remains only on the user's local computer.