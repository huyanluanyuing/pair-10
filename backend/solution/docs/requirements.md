# Requirements

Our restatement of `backend/REQUIREMENTS.md`. Decisions the spec leaves open are in `assumptions.md` (A-numbers).
Expected values below use the supplied seed and are worked by hand. Money is CAD, 2 decimals; ratios are decimals, 6 places (A3).

All routes need `Authorization: Bearer <token>` (Task 4). All errors use `{ error, message }` (A2).

## Task 1: Portfolio metadata via CRM

`GET /portfolios/:id` → one object, built from the mock CRM's `GET /crm/portfolios/:id`.

| Output | From CRM |
|---|---|
| `portfolioId` | `acct_ref` |
| `clientId` | `client_id` |
| `label` | `acct_nickname` |
| `currency` | `curr_val.ccy` |
| `totalMarketValue` | `curr_val.amt` |
| `dayChangeAmount`, `dayChangePercent` | `chg_1d.amt`, `chg_1d.pct` |
| `totalReturnSinceInception` | `since_inception_pct` |
| `asOf` | `meta.retrieved_at` |

Rules: pick the account whose `acct_ref` equals `:id` (A8). CRM field names appear nowhere in our output.

Edge cases, from the spec: CRM error or timeout → no crash, no hang. Unknown id → 404. Missing or `null` fields handled explicitly.
Found by us: the account is not always first; accounts may sit under `relationships.accounts`; a 200 without the account; a body that is not JSON (A9, A10).

Tests:
- `P-9001` maps to `clientId abc123`, `label "Taxable Brokerage"`, `currency "CAD"`, `totalMarketValue 48930`, `dayChangeAmount 30`, `dayChangePercent 0.000613` (30 ÷ 48900), `totalReturnSinceInception 0.187`.
- `P-9002` is found although it is the second account: `totalMarketValue 500`, `dayChangePercent 0`.
- The nested shape maps to the same result as the normal shape.
- Missing nickname and `null` amount → 200 with `label: null`, `totalMarketValue: null`.
- CRM 404 → 404 `not_found`. CRM 200 without the account → 404.
- CRM 503 → 502 `crm_unavailable`. Body not JSON → 502.
- A real timeout fires against a slow stub and returns 502 in about 2 s, not 10 s.

## Task 2: Holdings

`GET /portfolios/:id/holdings` → array of `ticker, name, assetClass, quantity, costBasisPerShare, price, previousClosePrice` plus:

- `marketValue = quantity × price`
- `weightPercent = marketValue ÷ sum of marketValue in the portfolio`; 0 when `marketValue` is 0 (A14)
- `unrealizedGainLoss = (price − costBasisPerShare) × quantity`
- `dayChangeAmount = (price − previousClosePrice) × quantity`
- `dayChangePercent = (price − previousClosePrice) ÷ previousClosePrice`; `null` when `previousClosePrice` is 0 (A13)

Computed at request time, never stored. Ordered by ticker (A15).

Edge cases, from the spec: no holdings → `[]`. `quantity: 0` → zeros, no error. `previousClosePrice: 0` → no divide by zero. Weights are not corrected to sum to 1. Unknown id → 404.
Found by us: portfolio total of 0; `costBasisPerShare: null` after Task 10 (A17).

Tests (`P-9001`, total 27300 + 21630 + 0 = 48930):
- `AAPL`: 120 × 227.5 = `27300`; weight 27300 ÷ 48930 = `0.557940`; gain (227.5 − 200) × 120 = `3300`; day (227.5 − 225) × 120 = `300`; 2.5 ÷ 225 = `0.011111`.
- `BND`: 300 × 72.1 = `21630`; weight `0.442060`; gain (72.1 − 74) × 300 = `-570`; day (72.1 − 73) × 300 = `-270`; −0.9 ÷ 73 = `-0.012329`.
- `ZERO` (quantity 0): `marketValue 0`, `weightPercent 0`, `unrealizedGainLoss 0`, `dayChangeAmount 0`, `dayChangePercent 0.2`.
- `P-9002` `NEW` (previous close 0): `marketValue 500`, `weightPercent 1`, gain `100`, `dayChangeAmount 500`, `dayChangePercent null`.
- `P-EMPTY` → `[]`. Unknown id → 404. Order is `AAPL, BND, ZERO`.

## Task 3: Performance history

`GET /portfolios/:id/performance-history?range=` → array of `{ date: YYYY-MM-DD, marketValue }`.

Rules: `range` is one of `1D, 1M, YTD, 1Y, All`; default `All`. Keep snapshots with start ≤ date ≤ today (A18):
`1D` today − 1 day; `1M` today − 1 month; `1Y` today − 1 year; `YTD` 1 January of this year; `All` everything. Sorted by date ascending.

Edge cases, from the spec: less history than the range → return what exists, no padding. Invalid range → 400. Unknown id → 404.
Found by us: no history → `[]`; month-end and 29 February starts; `?range=` empty; an old data file (A20).

Tests (clock fixed at 2026-10-03, snapshots daily and ending that day):
- 401 days of history: `All` → 401 rows; `1Y` → 366 (2025-10-03 to 2026-10-03); `YTD` → 276 (from 2026-01-01); `1M` → 31 (from 2026-09-03); `1D` → 2.
- 60 days of history with `range=1Y` → 60 rows.
- No `range` → same as `All`. `range=invalid` → 400. `range=all` → 400 (A6).
- Clock at 2026-03-31: `1M` starts 2026-02-28.
- Clock moved to 2027-01-01: `YTD` returns only that day's snapshot, if any.
- `P-EMPTY` → `[]`. Unknown id → 404. Rows come back in date order even if stored unsorted.

## Task 4: Auth

A check that runs before any route logic. Valid header: `Authorization: Bearer superday-demo-token` (A21).
Failure → 401 with `{ "error": "unauthorized", "message": ... }`.

Edge cases, from the spec: missing header → 401. Malformed header (no `Bearer ` prefix) → 401, not 500. Valid header → request proceeds.
Found by us: wrong token; `Bearer` with no token; lower-case `bearer`; 401 must win over 404 and 400.

Tests:
- No header → 401 and the error body.
- `Authorization: superday-demo-token` (no prefix) → 401.
- `Bearer wrong` → 401. `Bearer ` → 401. `bearer superday-demo-token` → 401.
- Valid header → 200 from the endpoint.
- No header on an unknown portfolio → 401, not 404.
- Every route is protected: `/portfolios`, `/clients`, `/holdings`.

## Task 5: Asset allocation

`GET /portfolios/:id/allocation` → array of `{ assetClass, value, percent }`.

Rules: `value` = sum of `marketValue` of the class. `percent = value ÷ portfolio total`; 0 when `value` is 0. One entry per class that has a holding. Ordered by `value` descending, then `assetClass` (A22).

Edge cases, from the spec: one class only → single entry with `percent: 1.0`. No holdings → `[]`.
Found by us: a class whose holdings are all zero value; unknown id → 404.

Tests:
- `P-9001` → `Equity 27300 0.557940`, then `Fixed Income 21630 0.442060`.
- `P-SINGLE` → one entry: `Equity 2275 1`.
- `P-9002` → `Equity 500 1`.
- `P-EMPTY` → `[]`. Unknown id → 404.
- A class with only a zero-quantity holding → listed with `value 0`, `percent 0`.

## Task 6: Clients and household

`GET /clients/:clientId/portfolios` → array of `{ portfolioId, label, totalMarketValue }`, ordered by `portfolioId`.
`GET /clients/:clientId/household-summary` → `{ clientId, totalMarketValue, portfolioCount, dayChangeAmount, dayChangePercent }`.

Rules, all from our holdings (A23):
- portfolio `totalMarketValue` = sum of `quantity × price`
- household `totalMarketValue` and `dayChangeAmount` = sums over the client's portfolios
- `dayChangePercent = total dayChangeAmount ÷ total (quantity × previousClosePrice)`; `null` when the divisor is 0 (A24)

Edge cases, from the spec: a client with one portfolio works on both routes. Unknown `clientId` → 404. The percent is value-weighted, not a plain average.
Found by us: an empty portfolio in the household; a portfolio whose previous value is 0; a client with no portfolios (A25).

Tests:
- `abc123` list → `P-9001 48930`, `P-9002 500`, `P-EMPTY 0`.
- `abc123` summary → total 48930 + 500 + 0 = `49430`; `portfolioCount 3`; change 30 + 500 + 0 = `530`; 530 ÷ 48900 = `0.010838`.
- `single-client` → list `P-SINGLE 2275`; summary `2275`, count `1`, change `25`, 25 ÷ 2250 = `0.011111`.
- Weighted, not averaged (our example): A was 1000 and gains 100 (10%); B was 9000 and loses 90 (−1%). Result 10 ÷ 10000 = `0.001`, not the average `0.045`.
- Unknown client → 404 on both routes.

## Task 7: Currency display

`?currency=CAD|USD` on `GET /portfolios/:id`, `/holdings` and `/performance-history`. Default CAD.

Rules: rate CAD→USD is 0.73; CAD→CAD is 1 (A26). Each response carries `currency` and `exchangeRate`: top level on the portfolio object, on every element of the arrays (A27).
Converted: `totalMarketValue`, `dayChangeAmount`, `marketValue`, `price`, `previousClosePrice`, `costBasisPerShare`, `unrealizedGainLoss`. Not converted: `quantity`, `weightPercent`, `dayChangePercent`, `totalReturnSinceInception`.
Convert the full-precision CAD value, then round (A28). `null` stays `null`.

Edge cases, from the spec: unsupported currency → 400, no fallback. The three endpoints agree. Rounding keeps summed holdings ≈ portfolio total.
Found by us: the cache holds CAD and conversion happens after it; history uses today's rate; `currency=usd` → 400 (A6).

Tests (`?currency=USD`):
- `P-9001` portfolio → `currency "USD"`, `exchangeRate 0.73`, `totalMarketValue` 48930 × 0.73 = `35718.90`, `dayChangeAmount` 30 × 0.73 = `21.90`, `dayChangePercent 0.000613` unchanged.
- `AAPL` → `price 166.08` (166.075 rounded), `costBasisPerShare 146.00`, `previousClosePrice 164.25`, `marketValue 19929.00`, gain `2409.00`, day `219.00`; `quantity 120` and `weightPercent 0.557940` unchanged.
- `BND` → `price 52.63`, `marketValue 15789.90`, gain `-416.10`, day `-197.10`.
- Holdings sum 19929.00 + 15789.90 = `35718.90` = the portfolio total.
- A history snapshot of 48930 → `35718.90`.
- No param and `?currency=CAD` → CAD values, `currency "CAD"`, `exchangeRate 1`.
- `?currency=EUR` → 400 on each of the three endpoints.

## Task 8: Holding detail

`GET /holdings/:ticker/detail` → `{ ticker, name, sector, assetClass, purchaseDate, dividendYield, fiftyTwoWeekLow, fiftyTwoWeekHigh, priceHistory: [{ date, price }] }`.

Rules: one record per ticker (A30). `priceHistory` sorted by date ascending. `dividendYield` is `null` when there is no dividend; the field is always present.

Edge cases, from the spec: unknown ticker → 404. No dividend → `null`, not 0 and not omitted. No price history → `[]`.
Found by us: a ticker held in two portfolios (`AAPL`); history stored out of order.

Tests:
- `AAPL` → `sector "Technology"`, `purchaseDate "2025-01-02"`, `dividendYield 0.005`, low `164.1`, high `232.4`, history `2025-01-02 200` then `2025-02-01 227.5`.
- `NEW` → `dividendYield: null` (field present), `priceHistory: []`.
- `ZERO` → `dividendYield: null`, `priceHistory: []`.
- Unknown ticker → 404. `aapl` → 404 (A6).
- History stored newest-first comes back oldest-first.

## Task 9: Cache for CRM responses

Internal cache: key = portfolio id, value = Task 1's mapped object, `ttlSeconds` (60, A31). Task 1's response gains `stale` (boolean) and `cachedAt` (ISO datetime).

Rules:
- Fresh entry (age < TTL) → serve it, no CRM call, `stale: false`.
- No entry or expired, CRM succeeds → store, serve, `stale: false`, `cachedAt` = now (A32).
- Expired, CRM fails → serve the old entry, `stale: true`, old `cachedAt`.
- No entry, CRM fails → 502 `crm_unavailable` (A33).
- CRM says 404 → 404, entry dropped.

Edge cases, from the spec: cold cache + failure is distinct from stale fallback. One id's entry is never served for another id. Document whether success after a stale period resets `stale` (it does, A32).
Found by us: 404 while an entry exists; currency conversion applied after the cache; restart empties the cache.

Tests (fake clock, fake CRM with a call counter):
- Two requests 10 s apart → CRM called once; second has `stale: false` and the first `cachedAt`.
- Clock + 61 s, CRM fails → 200, `stale: true`, original `cachedAt`, original values.
- Clock + 61 s, CRM succeeds → called again, `stale: false`, new `cachedAt`.
- After a stale answer, the next success → `stale: false`.
- Cold cache, CRM fails → 502. Cold cache, CRM times out → 502.
- `P-9001` cached, CRM failing, request `P-9002` → 502, never `P-9001`'s data.
- Cached id, CRM now 404 → 404; a later failure for that id → 502.

## Task 10: Data model and ledger replay

Deliverables: (1) a documented schema for `clients`, `portfolios`, `holdings`, `transactions` with their relationships; (2) a replay function.
Input: list of `{ type: BUY or SELL, quantity, price, date }`. Output: `{ currentQuantity, averageCostBasisPerShare }`.

Rules, applied after sorting by `date` (A35):
- BUY: `average = (quantity × average + buyQuantity × buyPrice) ÷ (quantity + buyQuantity)`, then `quantity += buyQuantity`
- SELL: `quantity −= sellQuantity`; average unchanged
- `quantity` 0 → `average` is `null` (A34)
- Nothing is stored; both values come only from the transactions.

Edge cases, from the spec: sell to exactly 0. Oversell is invalid; decide and document. Input may be out of order.
Found by us: empty list; buy again after closing; two transactions on one date; quantity ≤ 0 or negative price (A35).
Note: the spec's input table says "chronologically ordered"; its edge cases say do not assume it. We sort.

Tests:
- `h1`: BUY 100 @ 190, BUY 50 @ 220, SELL 30 @ 230 → quantity `120`; average (19000 + 11000) ÷ 150 = `200`.
- `h3`: BUY 5 @ 10, SELL 5 @ 12 → quantity `0`, average `null`.
- `oversell`: BUY 5, SELL 6 → rejected with an error, no result.
- `outOfOrder`: SELL 2 dated 01-03 listed before BUY 5 @ 10 dated 01-01 → quantity `3`, average `10`.
- Buy again after closing (ours): BUY 5 @ 10, SELL 5, BUY 4 @ 20 → quantity `4`, average `20`.
- Empty list → quantity `0`, average `null`.
- Quantity 0 or negative, negative price, unknown type → rejected.
