# Mobile Tasks: Wealth Management Portfolio Dashboard

You are building a mobile app (native iOS/Android, or React Native/Expo) for a wealth management portfolio dashboard, from scratch. You may use any AI assistance. Mock data and mock/local APIs are acceptable wherever a live backend isn't provided — document any assumptions you make.

---

## 1. Scaffold the Base App Shell

**Goal:** Establish the foundational navigation and screen structure the rest of the app will be built into.

**Inputs:** None required: this is structural.

**Expected Behaviour:**
- App launches to a placeholder "Portfolio Overview" screen.
- Basic navigation structure exists (tab bar, stack navigator, or equivalent), even if only one screen is functional so far.
- Structure is organized so future screens/components have a clear place to live.

**Edge Cases / Constraints:**
- Should not hardcode content that later tasks will replace with dynamic data: structure it to be extensible.

**Definition of Done:**
- App builds and runs on a simulator/emulator (or device), rendering the shell with a placeholder overview screen.
- No crashes or console errors on launch.

---

## 2. Core Portfolio Screen

**Goal:** Give the client an at-a-glance view of their portfolio's overall health and holdings in one primary screen.

**Inputs:** Mock JSON, e.g.:
```json
{
  "portfolio": {
    "totalMarketValue": 482350.12,
    "dayChangeAmount": 1520.44,
    "dayChangePercent": 0.32,
    "totalReturnSinceInception": 0.187
  },
  "holdings": [
    { "ticker": "AAPL", "name": "Apple Inc.", "quantity": 120, "price": 227.50, "marketValue": 27300.00, "weightPercent": 5.66, "gainLoss": 3200.00 },
    { "ticker": "BND", "name": "Vanguard Total Bond ETF", "quantity": 300, "price": 72.10, "marketValue": 21630.00, "weightPercent": 4.48, "gainLoss": -410.50 }
  ]
}
```

**Expected Behaviour:**
- Displays a summary section (total market value, day change $ and %, total return since inception) at the top of the screen.
- Displays a scrollable list of holdings below it, showing ticker/name, market value, and gain/loss per position.
- Pull-to-refresh re-fetches and updates both the summary and holdings list.
- Positive/negative values are visually distinguished (color and/or icon).

**Edge Cases / Constraints:**
- Handle an empty holdings list gracefully (e.g. "No holdings to display") without breaking the summary section.
- Handle a large number of holdings (50+) without significant scroll/render lag.
- Zero day change should be styled neutrally, not as positive or negative.

**Definition of Done:**
- Screen renders correctly with the sample data, including a working pull-to-refresh.
- Verified with an empty-holdings dataset and a large (50+) holdings dataset.

---

## 3. Local Persistent Storage

**Goal:** Persist portfolio and holdings data in an embedded local store so it's available instantly on app relaunch, not just held in memory for the current session.

**Inputs:** The same portfolio/holdings data shape as Task 2.

**Expected Behaviour:**
- Portfolio and holdings data is written to a real embedded store (SQLite, Realm, or platform equivalent) — not just an in-memory variable or non-persistent state.
- A defined schema/table structure exists for this data (documented, even briefly).
- On app relaunch (cold start), the Core Portfolio Screen renders from the local store immediately, before/without waiting on a network call.
- Once fresh data is fetched, the local store is updated to reflect it.

**Edge Cases / Constraints:**
- First-ever app launch (no data has ever been stored) should not crash — handle the empty-store case explicitly (e.g. show a loading state until first fetch completes).
- Storage writes should not block the UI thread in a way that causes visible jank.

**Definition of Done:**
- Verified that data persists and renders correctly after fully closing and relaunching the app (not just backgrounding it).
- Verified the first-launch/empty-store case is handled without crashing.

---

## 4. Offline State Handling & Reconciliation

**Goal:** Keep the app usable and honest about data freshness when the device loses connectivity, and correctly resync when it returns.

**Inputs:** The same portfolio/holdings data shape as Task 2, plus a way to simulate connectivity loss/return (e.g. airplane mode toggle, or a mock network-status flag in your dev environment).

**Expected Behaviour:**
- When offline, a visible banner/indicator communicates "offline — showing cached data."
- Cached data displayed while offline includes a staleness indicator (e.g. "prices as of 4 minutes ago").
- When connectivity returns, the app automatically re-fetches and updates the displayed data, replacing/merging the cached state cleanly (no duplicate holdings, no leftover stale values mixed with fresh ones).
- The offline banner disappears once connectivity and fresh data are confirmed.

**Edge Cases / Constraints:**
- Connectivity flapping (drops and returns rapidly) should not trigger duplicate/overlapping fetch requests or a flickering UI.
- If the app has never successfully fetched data and goes offline immediately, handle this distinctly from the "has cached data" case (e.g. an empty/first-load state rather than a broken banner).

**Definition of Done:**
- Verified the offline banner and staleness indicator appear correctly when connectivity is dropped.
- Verified that reconnecting triggers a clean refresh with no duplicated or stale-mixed-with-fresh data.
- Verified the rapid-flapping edge case does not cause duplicate requests or UI flicker.

---

## 5. Secure Access

**Goal:** Protect access to the portfolio screen with a biometric/PIN gate, and ensure the session credential behind that gate is actually stored securely — not just visually hidden.

**Inputs:** A mock session/auth token (any string) to be protected.

**Expected Behaviour:**
- On app launch (or resume from background, your choice — document it), the user must authenticate via biometric (Face ID/Touch ID/fingerprint) or a PIN fallback before the portfolio screen is accessible.
- The session/auth token is stored using the platform's secure storage API (Keychain on iOS, Keystore/EncryptedSharedPreferences on Android, or the equivalent secure storage API if using a cross-platform framework) — not in plain local storage, not in memory-only state that defeats the point of persistence.
- Failed authentication keeps the user locked out of the portfolio screen (does not fall through to showing data anyway).

**Edge Cases / Constraints:**
- Device/simulator without biometric hardware available should fall back to the PIN path, not crash or skip the gate entirely.
- Token should not be readable via basic inspection of app storage (e.g. it shouldn't show up in plain text if you dump local storage/plist/shared prefs directly).

**Definition of Done:**
- Verified the portfolio screen is inaccessible without successful authentication.
- Verified the fallback path works on a device/simulator without biometric hardware.
- Verified (by inspecting where the token is stored) that it is in secure storage, not plain local storage.

---

## 6. Push Notification & Deep Link Handling

**Goal:** Route the user directly to the relevant content when they tap a push notification, rather than dropping them at a generic home screen.

**Inputs:** A mock push notification payload, e.g.:
```json
{
  "title": "Portfolio Alert",
  "body": "Your portfolio is up 2.1% today",
  "data": { "portfolioId": "P-9001", "type": "portfolio_alert" }
}
```

**Expected Behaviour:**
- A mock/local mechanism triggers a notification (real push infrastructure is not required — a local notification simulating a server push is acceptable).
- Tapping the notification opens the app directly to the relevant portfolio's overview screen (using `data.portfolioId`), not just to the app's default launch screen.
- If the app is already open in the background, tapping the notification navigates to the correct screen without restarting the app state.

**Edge Cases / Constraints:**
- Tapping a notification referencing a `portfolioId` that doesn't exist (or isn't the currently loaded one) should be handled gracefully (e.g. fall back to the default portfolio) rather than crashing on a bad navigation target.
- Notification tap handling should work whether the app was fully closed, backgrounded, or already in the foreground.

**Definition of Done:**
- Verified that tapping the mock notification navigates to the correct portfolio screen from a cold start (app fully closed).
- Verified the same from a backgrounded state.
- Verified the invalid-`portfolioId` edge case falls back gracefully.

---

## 7. Native Share Sheet

**Goal:** Let the client share a snapshot of their portfolio summary through the OS-native share sheet.

**Inputs:** The same portfolio summary data shape as Task 2 (`totalMarketValue`, `dayChangeAmount`, `dayChangePercent`, `totalReturnSinceInception`).

**Expected Behaviour:**
- A share action (button/icon) on the Core Portfolio Screen opens the native OS share sheet.
- The shared content is a readable text summary (e.g. "My portfolio: $482,350.12, up 0.32% today") or an image snapshot of the summary card — your choice, document it.
- The share sheet shows the device's normal share targets (Messages, Mail, etc. — whatever the simulator/emulator/device supports).

**Edge Cases / Constraints:**
- Should not crash if the user dismisses the share sheet without selecting a target.
- If sharing an image snapshot, ensure it doesn't include any hidden/unintended data (e.g. accidentally capturing full-screen content beyond the summary card).

**Definition of Done:**
- Verified the share sheet opens with correctly formatted portfolio data.
- Verified dismissing without sharing does not crash or leave the UI in a broken state.

---

## 8. Native Multi-Portfolio Selector

**Goal:** Let clients with multiple accounts (e.g. taxable + IRA) switch between them using a mobile-native interaction pattern.

**Inputs:** A mock list of accounts, e.g.:
```json
[
  { "accountId": "P-9001", "label": "Taxable Brokerage", "totalMarketValue": 482350.12 },
  { "accountId": "P-9002", "label": "Traditional IRA", "totalMarketValue": 215600.00 }
]
```

**Expected Behaviour:**
- Accounts are switchable via a mobile-native pattern — swipeable tabs, a bottom sheet, or a segmented control (not a plain web-style dropdown).
- Selecting/swiping to a different account updates the Core Portfolio Screen (summary + holdings) to reflect that account's data.
- The currently selected account is clearly indicated.

**Edge Cases / Constraints:**
- Handle a client with only one account (the selector should still render sensibly, even if trivial/hidden).
- Switching accounts should feel immediate — avoid a full-screen reload/flash between accounts.

**Definition of Done:**
- Verified switching between 2+ mock accounts correctly updates the displayed data.
- Verified the single-account edge case does not look broken or show an empty selector.

---

## 9. Detailed Holding View

**Goal:** Let the client dig into a single position and see meaningful detail not already visible in the holdings list — mirroring the frontend's Detailed Holding View.

**Inputs:** A holding record (from Task 2) plus additional mock data, e.g.:
```json
{
  "ticker": "AAPL",
  "name": "Apple Inc.",
  "quantity": 120,
  "costBasisPerShare": 158.75,
  "purchaseDate": "2022-03-14",
  "sector": "Technology",
  "assetClass": "Equity",
  "dividendYield": 0.005,
  "priceHistory": [
    { "date": "2025-01-01", "price": 195.20 },
    { "date": "2025-02-01", "price": 210.75 }
  ],
  "fiftyTwoWeekLow": 164.10,
  "fiftyTwoWeekHigh": 232.40
}
```

**Expected Behaviour:**
- Tapping a holding in the list (Task 2) opens a detail view using a mobile-native transition (push navigation or a swipeable modal — your choice).
- Detail view surfaces info not already in the holdings list: cost basis per share, purchase date, sector, dividend yield, 52-week high/low, and a price-history chart for that ticker.
- There is a clear, native-feeling way to return (back gesture/button) to the holdings list.

**Edge Cases / Constraints:**
- Handle `null` optional fields (e.g. `dividendYield: null`) gracefully rather than showing "undefined" or a broken layout.
- Handle a holding with no available price history gracefully (show the rest of the detail without a broken chart).

**Definition of Done:**
- Verified tapping any holding opens the detail view with correct, additional data for that specific holding.
- Verified returning to the list preserves its prior state (e.g. scroll position).
- Verified with a holding missing at least one optional field.

---

## 10. Portfolio Value Line Chart

**Goal:** Visualize how total portfolio market value has changed over time, touch-responsive on mobile.

**Inputs:** A mock time-series array, e.g.:
```json
[
  { "date": "2025-01-01", "marketValue": 410000.00 },
  { "date": "2025-02-01", "marketValue": 423500.00 },
  { "date": "2025-03-01", "marketValue": 418200.00 }
]
```

**Expected Behaviour:**
- Renders a line chart with date on the x-axis and market value on the y-axis, using a mobile charting library appropriate to your stack.
- Chart is legible on a typical phone screen width.
- Tapping or dragging along the chart shows the exact date and value at that point (e.g. a moving tooltip/crosshair).

**Edge Cases / Constraints:**
- Handle a dataset with only 1-2 points without the chart breaking or looking degenerate.
- Handle gaps in the data (missing dates) without misleading interpolation.
- Touch interaction should remain responsive/smooth, not laggy, even with a longer dataset (12+ points).

**Definition of Done:**
- Chart renders correctly with the sample data.
- Touch-driven detail display (tap/drag) is functional.
- Verified with a short (2-point) and longer (12+ point) dataset.
