# Bottom Ad Banner Positioning Below Navigation Bar

Position the advertisement banner (`AdBannerCard`) directly below the main bottom navigation bar (`NavigationBar` / `ReadoverBottomBar`), fixed to the bottom of the screen across all app tabs and screens for non-premium users.

## User Decisions & Key Requirements

> [!IMPORTANT]
> - **Confirmed Placement**: The Ad Banner (`AdBannerCard`) will be displayed attached directly beneath the Bottom Navigation Bar (`ReadoverBottomBar`) at the very bottom of the screen across all screens and tabs.
> - **Premium Support**: If the user is a premium user (`AdManager.isPremiumUser`), the ad banner is hidden.

---

## Proposed Implementation Plan

### 1. Update `ReadoverBottomBar` Composable
- Wrap the `NavigationBar` and `AdBannerCard` inside a vertical `Column`.
- Place `AdBannerCard` directly below `NavigationBar`.
- Apply bottom window insets / padding appropriately so the banner fits seamlessly at the edge of the screen.

### 2. Clean Up Duplicate Ad Banners
- Remove floating or standalone `AdBannerCard` calls inside `LibraryScreen` FAB column or individual screen content areas to avoid duplicate banners.
- Ensure all screens (`LibraryScreen`, `AudiobookPlayerScreen`, `SettingsScreen`, etc.) that render `ReadoverBottomBar` automatically show the attached bottom ad banner.

### 3. Verification & Testing
- Compile the applet using `compile_applet` to verify layout syntax and UI integrity.
- Run unit tests to ensure no layout regressions.

---

## User Review Required

> [!NOTE]
> Please review the plan above and click **Proceed** to begin implementation.
