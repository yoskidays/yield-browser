# Compatibility Ad Shield — v0.9.97

## Root causes in v0.9.96

1. `applyPlainCompatibilitySettings()` forced JavaScript popup opening back on.
2. Suspicious navigations carrying `hasGesture=true` were allowed before the compatibility guard.
3. Compatibility mode bypassed all request interception, including known third-party ad resources.
4. Blocked redirects could be represented as temporary tabs and later auto-closed, producing visible tab churn.
5. Recovery used browser history first, which could return to an intermediate blank/ad entry.

## Fix

- Respect the popup-blocker setting in compatibility mode.
- Block suspicious cross-site main-frame navigation before it is marked trusted.
- Preserve same-site reader traffic and content assets.
- Apply selective third-party ad filtering instead of all-or-nothing interception.
- Add a minimal compatibility-safe JavaScript guard.
- Suppress auto-closed ad tabs before they are inserted into the tab model.
- Restore to the tab's last safe reader URL.
