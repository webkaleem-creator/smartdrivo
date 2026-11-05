# SmartDrivo v1.0.11 — Owner Pickup Guard Quick Controls

This is an owner-only UX update, based on v1.0.10 / Build #452.

- Single tap on the PG bubble retains the existing Pickup Guard ON/OFF function.
- Hold for 500 ms opens a compact, draggable-overlay-adjacent Quick Controls menu.
- Main Auto Accept uses the existing master setting; it is NOT a separate acceptance path. With Pickup Guard ON, eligible orders stay manual choices.
- Quick pickup presets 0.3 / 0.6 / 1.0 km reuse verified owner-only storage; custom max remains in Pickup Guard settings.
- Temporary 5 / 10 / 15 minute pause uses the existing master OFF switch, with fail-closed resume if owner authorization, accessibility service, overlay permission or Banking Mode is not suitable.
- Last reason shows recent saved local history when available.
- Per-owner popup position lock blocks dragging, but long press still opens the menu.
- Emergency Stop disables master Auto Accept and Pickup Guard and clears any pending timed resume.
- The owner bubble remains visible when main Auto Accept is OFF, provided service, auth, permissions and Banking Mode gates remain valid.
- No changes to Rapido/Ola/Uber order parsers, filters or click-finding strategies.

**Caution:** Owner-only controls should be tested on a stationary phone before any live-driving use. No functionality is considered verified until tests and phone validation pass.

Upgrade package: com.smartdrivo.app; versionCode 20; versionName 1.0.11. In-place installation must use matching original signing certificate and never uninstall or clear data.
