# Frontend Tasks: Wealth Management Portfolio Dashboard

You are building the frontend for a wealth management portfolio dashboard, from scratch. You may use any framework/tooling and any AI assistance. Use mock data wherever a backend isn't provided: document any assumptions you make.

---

## 1. Scaffold the Base App Shell

**Goal:** Establish the foundational layout and navigation structure that the rest of the dashboard will be built into.

**Inputs:** None required: this is structural.

**Expected Behaviour:**

- App loads to a placeholder "Portfolio Overview" page.
- A persistent layout element (e.g. header/sidebar) exists, even if navigation only has one working link.
- Layout is organized so future components have a clear place to live (e.g. a main content area, a summary region).

**Edge Cases / Constraints:**

- Should not hardcode content that later tasks will replace with dynamic data: structure it to be extensible.

**Definition of Done:**

- App runs locally and renders a shell with visible layout structure and a placeholder overview page.
- No console errors on load.

---

## 2. Portfolio Summary Card

**Goal:** Give the client an at-a-glance view of overall portfolio health.

**Inputs:** A mock JSON object representing a portfolio, e.g.:

```json
{
  "totalMarketValue": 482350.12,
  "dayChangeAmount": 1520.44,
  "dayChangePercent": 0.32,
  "totalReturnSinceInception": 0.187
}
```

**Expected Behaviour:**

- Displays total market value formatted as currency.
- Displays day change as both a dollar amount and a percentage.
- Displays total return since inception as a percentage.
- Positive values are visually distinguished from negative values (e.g. color and/or icon).

**Edge Cases / Constraints:**

- Zero day change should not be styled as "positive" or "negative": treat as neutral.
- Negative total market value is not a realistic case and does not need to be handled.
- Very large numbers should still be legible (e.g. thousands separators).

**Definition of Done:**

- Card renders correctly with the sample data above.
- Component re-renders correctly if the input data changes (e.g. via a mock update).
- Positive, negative, and zero day-change states have all been visually verified.

---

## 3. Holdings Table

**Goal:** Let the client see and compare all individual positions in their portfolio.

**Inputs:** A mock array of holdings, e.g.:

```json
[
  {
    "ticker": "AAPL",
    "name": "Apple Inc.",
    "quantity": 120,
    "price": 227.5,
    "marketValue": 27300.0,
    "weightPercent": 5.66,
    "gainLoss": 3200.0
  },
  {
    "ticker": "BND",
    "name": "Vanguard Total Bond ETF",
    "quantity": 300,
    "price": 72.1,
    "marketValue": 21630.0,
    "weightPercent": 4.48,
    "gainLoss": -410.5
  }
]
```

**Expected Behaviour:**

- Table displays all columns: ticker/name, quantity, price, market value, weight %, gain/loss.
- Columns are sortable (at minimum: market value, weight %, gain/loss) by clicking the column header.
- Sort direction toggles between ascending/descending on repeated clicks.
- Gain/loss is visually distinguished (positive vs. negative).

**Edge Cases / Constraints:**

- Handle an empty holdings array (no positions) without breaking the layout.
- Handle a large number of rows (50+) without significant UI lag: pagination or scrolling is acceptable.
- Weight % values across all holdings won't necessarily sum to exactly 100% due to rounding: do not attempt to correct this.

**Definition of Done:**

- Table renders correctly with sample data, including sorting on at least 2 columns.
- Empty state is handled gracefully (e.g. "No holdings to display").
- Verified with both a small (2-3 row) and larger (50+ row) mock dataset.

---

## 4. Portfolio Value Line Chart

**Goal:** Visualize how total portfolio market value has changed over time.

**Inputs:** A mock time-series array, e.g.:

```json
[
  { "date": "2025-01-01", "marketValue": 410000.0 },
  { "date": "2025-02-01", "marketValue": 423500.0 },
  { "date": "2025-03-01", "marketValue": 418200.0 }
]
```

**Expected Behaviour:**

- Renders a line chart with date on the x-axis and market value on the y-axis.
- Chart is legible at typical desktop screen widths (axis labels, gridlines/tooltips as appropriate).
- Hovering/tapping a point shows the exact date and value (tooltip or equivalent).

**Edge Cases / Constraints:**

- Handle a dataset with only 1-2 points without the chart breaking or looking degenerate.
- Handle gaps in the data (missing dates) without misleading interpolation.

**Definition of Done:**

- Chart renders correctly with the sample data.
- Tooltip or equivalent detail-on-hover is functional.
- Verified with a short (2-point) and longer (12+ point) dataset.

---

## 5. Asset Allocation Chart

**Goal:** Show the client how their portfolio is distributed across asset classes.

**Inputs:** A mock breakdown, e.g.:

```json
[
  { "assetClass": "Equity", "value": 289410.0 },
  { "assetClass": "Fixed Income", "value": 120500.0 },
  { "assetClass": "Cash", "value": 42340.12 },
  { "assetClass": "Alternatives", "value": 30100.0 }
]
```

**Expected Behaviour:**

- Renders a pie or donut chart with one segment per asset class.
- Each segment is labeled (directly or via legend) with asset class name and percentage of total.
- Segments are visually distinct (color-coded).

**Edge Cases / Constraints:**

- Handle a single-asset-class portfolio (100% in one category) without the chart breaking.
- Handle a very small allocation (e.g. <1%) remaining visible/labeled rather than disappearing entirely.

**Definition of Done:**

- Chart renders correctly with sample data, with all 4 categories visible and labeled.
- Verified with an edge-case dataset (single category, and one with a very small slice).

---

## 6. Date-Range Selector for Performance Chart

**Goal:** Let the client view portfolio performance over different time horizons.

**Inputs:** The same time-series dataset used in Task 4, plus a set of range options: `1D`, `1M`, `YTD`, `1Y`, `All`.

**Expected Behaviour:**

- Selector control (buttons, tabs, or dropdown) with the 5 range options.
- Selecting a range filters/updates the line chart from Task 4 to show only that window of data.
- The currently selected range is visually indicated.

**Edge Cases / Constraints:**

- If the underlying dataset doesn't contain enough history for a selected range (e.g. "1Y" selected but only 3 months of data exist), display what's available rather than erroring.
- "YTD" should calculate from January 1 of the current year, not from the dataset's start date.

**Definition of Done:**

- All 5 range options are selectable and correctly filter the chart.
- Verified with a dataset that has less history than one of the selectable ranges (confirms graceful fallback).

---

## 7. Currency Toggle (CAD ↔ USD)

**Goal:** Let the client view all dollar figures in the dashboard in either CAD or USD, since holdings and reporting may need to be viewed in either currency.

**Inputs:** A mock exchange rate, e.g.:

```json
{ "CADtoUSD": 0.73 }
```

Assume all underlying data (portfolio summary, holdings table, charts, widgets) is stored/native in CAD.

**Expected Behaviour:**

- A visible toggle/switch lets the client select CAD or USD.
- Switching the toggle converts and re-displays every dollar figure across the dashboard: summary card, holdings table, line chart, and any other component showing a currency value: using the exchange rate.
- The currently selected currency is clearly indicated (e.g. a label or currency symbol/code next to figures).
- Non-currency values (e.g. quantity, weight %, percentages) are unaffected by the toggle.

**Edge Cases / Constraints:**

- Conversion should be applied consistently: a value should never appear converted in one component and un-converted in another after toggling.
- Rounding after conversion should not introduce visible inconsistencies (e.g. table totals not matching a converted summary value due to independent rounding).
- Toggling currency should not require a full page reload or lose other UI state (e.g. selected date range, sort order, selected account).

**Definition of Done:**

- Toggling between CAD and USD updates every currency value on the dashboard correctly and consistently.
- Verified that non-currency values are unaffected.
- Verified that other UI state (sort order, selected range, selected account) persists across a currency toggle.

---

## 8. Multi-Portfolio/Account Selector

**Goal:** Support clients who hold more than one account (e.g. taxable + IRA) and let them switch between views.

**Inputs:** A mock list of accounts, each with its own summary/holdings/performance data, e.g.:

```json
[
  {
    "accountId": "P-9001",
    "label": "Taxable Brokerage",
    "totalMarketValue": 482350.12
  },
  {
    "accountId": "P-9002",
    "label": "Traditional IRA",
    "totalMarketValue": 215600.0
  }
]
```

**Expected Behaviour:**

- A selector (dropdown, tabs, etc.) lists all accounts by label.
- Selecting an account updates the summary card, holdings table, and charts to reflect that account's data only.
- The currently selected account is clearly indicated.

**Edge Cases / Constraints:**

- Handle a client with only one account (selector should still function, even if trivial).
- Switching accounts should not require a full page reload: the existing component structure should just update with new data.

**Definition of Done:**

- Selector renders with 2+ mock accounts and correctly updates dependent components on switch.
- Verified with a single-account edge case.

---

## 9. Detailed Holding View

**Goal:** Let the client dig into a single position and see meaningful detail that is not already visible in the holdings table: this view should add new information, not just repeat Task 3's row in a bigger box.

**Inputs:** A holding record (from Task 3) plus additional mock data not shown in the table, e.g.:

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
    { "date": "2025-01-01", "price": 195.2 },
    { "date": "2025-02-01", "price": 210.75 }
  ],
  "fiftyTwoWeekLow": 164.1,
  "fiftyTwoWeekHigh": 232.4
}
```

**Expected Behaviour:**

- Clicking a row in the holdings table (Task 3) opens a detail view (modal, side panel, or separate screen: your choice).
- Detail view surfaces information the table does not already show, such as: cost basis per share and purchase date, sector/asset class, dividend yield, 52-week high/low, and a price-history line chart for that ticker.
- Detail view may still reference core identifying info (ticker, name, current price) for context, but the emphasis should be on the additional detail.
- There is a clear way to return/close back to the holdings table.

**Edge Cases / Constraints:**

- Handle a holding with `null` optional fields (e.g. `dividendYield: null`, because it doesn't pay one) gracefully rather than showing "undefined" or a broken layout.
- Handle a holding with no available price history gracefully (e.g. show the rest of the detail without a broken chart).

**Definition of Done:**

- Clicking any row opens the detail view with correct, additional data for that specific holding not visible in the table.
- Closing/returning works without losing the state of the underlying holdings table (e.g. sort order is preserved).
- Verified with a holding missing at least one optional field (e.g. no dividend yield).

---

## 10. "Top Movers" Widget

**Goal:** Surface the day's most significant winners and losers in the portfolio at a glance.

**Inputs:** The same holdings dataset from Task 3, assuming each holding includes a day-change value, e.g.:

```json
[
  { "ticker": "AAPL", "dayChangePercent": 2.4 },
  { "ticker": "BND", "dayChangePercent": -0.6 },
  { "ticker": "TSLA", "dayChangePercent": -4.1 }
]
```

**Expected Behaviour:**

- Widget displays the top N (e.g. top 3) gainers and top N losers by day change %, ranked.
- Each entry shows ticker and day change %, visually distinguished by direction (gain vs. loss).

**Edge Cases / Constraints:**

- Handle a portfolio with fewer holdings than N (e.g. only 2 total holdings) without erroring or showing duplicate/empty slots.
- Handle a day where all holdings moved in the same direction (e.g. no losers at all): the "losers" section should indicate this rather than appear broken.

**Definition of Done:**

- Widget correctly ranks and displays gainers/losers with sample data.
- Verified with an edge-case dataset (fewer holdings than N, and a same-direction-only dataset).
