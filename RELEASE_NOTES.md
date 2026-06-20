# YieldBrowser v0.9.90

## Professional tab spaces

- Added explicit **Umum | Privat** profile selector in the tab switcher.
- Normal and private tabs remain separate; an existing tab is never converted between profiles.
- The `+` button creates a tab in the selected profile space.
- Added a persistent private-profile banner and quick action to return to normal browsing.
- Added private home actions for opening a normal tab or another private tab.
- Closing the final private tab automatically returns to the normal browser task.
- Profile-switch intents bring the existing task forward instead of duplicating activities.
- Updated profile actions in Quick Menu and Settings.

## Existing improvements retained

- Isolated incognito WebView process/profile.
- Smooth RecyclerView-based download manager.
- Validated multipart download engine and background download notification behavior.
- AndroidX build configuration.
