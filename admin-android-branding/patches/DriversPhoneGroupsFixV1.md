# Native Admin phone grouping UI fix

This public backup stores ONLY a scoped patch to `AdminActivity.kt` from the separate Android Admin project. It is NOT a backup of that entire project.

- Group visually by a normalized self-reported phone (fallback: UID), keep actual account count visible.
- Display all Firebase UID accounts on demand; never merge, delete, or change Firestore records.
- Disable account status actions while a phone group containing multiple UIDs is collapsed.
- Keep Admin accounts protected, expose UID during individual account status confirmation.
- Driver V20, signing keys, Firestore rules, authentication and payment logic unchanged.

The full native Admin source remains on the user's local computer, not in this public repository.