# SmartDrivo UI filter freeze — only presentation

User-confirmed correction: **do not modify +Amount acceptance logic**. It remains a separately managed filter, accessible even when GO TO is active. No order engine classes, +Amount decision logic, service or database logic changed.

- Fastest ON: Home Global Filter, Area Rules Engine access, Smart Filter Order Unlimited, +Amount and Bundle, and Area Preferences edits are greyed and tap-blocked. Fastest pickup field remains usable.
- Fastest OFF with active GO TO: Global Filter, Order Unlimited, Bundle and Fix Area UI edits are greyed and tap-blocked. GO TO, NO GO and +Amount remain available.
- Both OFF: cards are fully interactive with their prior saved values. A paused card is **not** a stored disabled setting; flags and values remain unchanged.
- Rapido/other platform enable controls are not order filter settings and are not paused.
- Scope: HomeScreen, SettingsScreen, AreaManagerScreen, and new UI-only policy/wrapper/tests.

**Important behavior note:** This freezes *editing UI*, not runtime engine rules. The existing +Amount/GO TO, Bundle, and Fastest decision semantics are preserved exactly, including any current gate order. No claim is made that a greyed setting has been turned OFF in the engine.

Manual smoke checks:
1. With GO TO Home1 ON and Fastest OFF, verify Home Global is paused; GO TO, NO GO and +Amount can be edited. Fix Area and Bundle UI grey.
2. Enable Fastest; only Fastest pickup and ON/OFF usable in filter screens.
3. Turn Fastest OFF; Home1, GO TO, NO GO and +Amount values remain unchanged; pause behavior returns to GO TO.
4. Turn all GO TO groups OFF; prior Global, Unlimited, Bundle and Fix Area editing returns.
5. Confirm order acceptance logic unchanged using existing engine regression tests.
