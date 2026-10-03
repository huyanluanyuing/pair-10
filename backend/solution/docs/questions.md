# Questions for the client

Source: `backend/REQUIREMENTS.md` (the spec). Where a question rests on the supplied files (`START-HERE.md`, `CRM.md`, `crm-service.mjs`, `fixtures/seed.json`, `requests.http`) it says so.
Each default is recorded in `assumptions.md` under the A-number shown. We build on the default until told otherwise.

## Ask these first

| # | Question | Why it matters | Default (A#) |
|---|---|---|---|
| Q1 | Tasks 2 and 3 return a bare array. Task 7 adds `currency` and `exchangeRate` to "existing responses". Where do they go on an array: on every element, in a wrapper object, or in headers? | Changes the response shape of two endpoints. Deciding late breaks clients. | On every element; the array stays an array (A27) |
| Q2 | Task 6 says household `dayChangePercent` is "weighted by portfolio value". Which value: yesterday's or today's? For client `abc123`: total change ÷ total previous value = 530 ÷ 48900 = 0.010838. Weighting the CRM percents by today's value gives 0.000607. | The two readings differ by 18 times on the supplied data. | Total change ÷ total previous-close value (A24) |
| Q3 | Portfolio totals exist twice: in the CRM (`curr_val`, `chg_1d`) and as the sum of our holdings. Which one is the truth for Task 6's list and household summary, and for `weightPercent`? What if they disagree? | Tasks 1, 2, 5, 6 and 7 must show the same total. The CRM has no "by client" call and fails 1 in 5. | Task 1 shows the CRM figure. Everything else is computed from our holdings (A16, A23) |
| Q4 | Once Task 10 exists, must Task 2's `quantity` and `costBasisPerShare` come from replaying transactions? For the closed position `ZERO` replay gives no cost basis, but the seed says 10. | Decides whether `costBasisPerShare` can be `null` in Task 2's response from day one. | Seed values until Task 10 lands, replay after. Field is nullable now (A17, A34) |
| Q5 | When a percentage has a zero denominator, is the answer `null` or `0`? The spec allows either for `previousClosePrice: 0`. The mock CRM sends `0` for `P-9002`. | One rule for every division in Tasks 1, 2, 5, 6 and 10. Clients must know whether `0` means "no change" or "unknown". | `null` for what we compute. A CRM value is passed through unchanged (A4, A12, A13) |
| Q6 | Is one shared token that can read every client's data acceptable? Does auth cover `/clients/...` and `/holdings/...` as well as `/portfolios/...`? | With one token, any caller can read any client's portfolios. | One token, every route protected, no per-client check (A1) |

## Cross-cutting

| # | Question | Why it matters | Default (A#) |
|---|---|---|---|
| Q7 | What is the rounding rule: how many decimals for money and for ratios, and which rounding mode? | Every expected value in every test depends on it. | Money 2 decimals, ratios 6 decimals, half-up, rounded once at output (A3) |
| Q8 | Which time zone defines "today" for YTD and the other ranges? | Near midnight the range start moves by a day. The history generator uses UTC (inference from `generate-history.mjs`). | UTC (A5) |
| Q9 | Should 400, 404 and upstream errors use the same `{error, message}` body as Task 4's 401? Which `error` codes? | The spec asks for "a structured error" four times but defines the body only for 401. | Same body everywhere; codes `bad_request`, `unauthorized`, `not_found`, `crm_unavailable` (A2) |
| Q10 | Are ids, tickers and the `range` / `currency` values case-sensitive? Is `?currency=usd` or `?range=all` a 400? | Decides 400 and 404 tests. | Exact match. `usd` and `all` are 400; `aapl` is 404 (A6) |

## Task 1: Portfolio metadata via CRM

| # | Question | Why it matters | Default (A#) |
|---|---|---|---|
| Q11 | If the CRM answers 200 but the requested `acct_ref` is not in the account list, is that "not found" (404) or a CRM failure? | Not found and failed are handled differently, and only a failure may use the stale cache. | 404 (A9) |
| Q12 | When an account is found but a field is missing or `null` (the mock's `missing` mode: `curr_val.amt: null`, no `acct_nickname`), do we answer 200 with `null` fields or an error? | The spec says "handle explicitly" without saying how. | 200, with those fields `null` (A10) |
| Q13 | What timeout is acceptable, and may we retry a failed CRM call? | The mock waits 10 s. A retry hides most failures but changes the call counts Task 9 checks. | 1 s connect, 2 s read, no retry (A11) |
| Q14 | Which status when the CRM fails and nothing is cached: 502, 503 or 504? One code for errors and timeouts, or two? | The spec only says "an appropriate error". | 502 `crm_unavailable` for both (A33) |

## Task 2: Holdings

| # | Question | Why it matters | Default (A#) |
|---|---|---|---|
| Q15 | For `quantity: 0` the spec zeroes "both gain/loss fields". `dayChangePercent` does not use quantity. Does it stay price-based (`ZERO`: 0.2) or become 0? | Three fields could be meant by "both". | Stays price-based: 0.2 (A14) |
| Q16 | In what order are holdings returned? | Tests and clients need a fixed order. | Ticker ascending (A15) |

## Task 3: Performance history

| # | Question | Why it matters | Default (A#) |
|---|---|---|---|
| Q17 | What does `1D` return when snapshots are daily: today only, or yesterday and today? | One point cannot draw a line. | Yesterday and today (A18) |
| Q18 | Do ranges count back from today or from the latest snapshot? Is the start date included? How is `1M` handled at month end? | Decides every range test, and what an old data file returns. | From today, start and end included, month end clamped (A18) |
| Q19 | The history file is generated with dates ending on the day it is generated, and is not in Git. Do we read that file, or may we hold our own history? | If the file is old, `1D` and `1M` return little or nothing. | Read the supplied file; never pad or shift dates (A20) |

## Task 4: Auth

| # | Question | Why it matters | Default (A#) |
|---|---|---|---|
| Q20 | Is `Bearer superday-demo-token` (from `requests.http`) the token to use? Must the scheme be exactly `Bearer`? | Decides the malformed-header tests. | Yes; exact `Bearer <token>`, anything else 401 (A21) |

## Task 5: Allocation

| # | Question | Why it matters | Default (A#) |
|---|---|---|---|
| Q21 | If every holding in a class has zero value, is the class listed with `value: 0`, or left out? In what order are classes returned? | "One entry per class present" can be read both ways. | Listed with 0; order by value descending, then name (A22) |

## Task 6: Clients and household

| # | Question | Why it matters | Default (A#) |
|---|---|---|---|
| Q22 | Does `portfolioCount` include empty portfolios (`P-EMPTY`)? In what order is the list returned? What do we return for a known client with no portfolios? | `abc123` is 3 or 2 portfolios depending on the answer. | Empty ones count; order by `portfolioId`; `[]` and zero totals with `dayChangePercent: null` (A25) |

## Task 7: Currency

| # | Question | Why it matters | Default (A#) |
|---|---|---|---|
| Q23 | Is a fixed rate (seed `CADtoUSD: 0.73`) enough, or must the rate come from a live source? | The spec mentions "rate lookup/cache" but supplies one number. | Fixed 0.73, configurable (A26) |
| Q24 | Task 7 names three endpoints. What should allocation, the client list, the household summary and holding detail do with `?currency=USD`? | A client switching currency would see mixed currencies across screens. | They ignore it and return CAD (A29) |
| Q25 | Is past history converted at today's rate? | There are no historical rates in the supplied data. | Yes, one rate for all dates (A28) |

## Task 8: Holding detail

| # | Question | Why it matters | Default (A#) |
|---|---|---|---|
| Q26 | The route is keyed by ticker, but `purchaseDate` is "the date the position was opened", which belongs to one portfolio. `AAPL` is held in `P-9001` and `P-SINGLE`. Which date do we return? | The same ticker can have two purchase dates. | The seed's one record per ticker (A30) |

## Task 9: Cache

| # | Question | Why it matters | Default (A#) |
|---|---|---|---|
| Q27 | Which TTL, and is there a limit on how old a stale value may be? | The spec suggests 30 to 60 s and sets no stale limit. | 60 s, configurable; no limit (A31, A32) |
| Q28 | If the CRM now says 404 for an id we have cached, do we return 404 or the cached value? Do we cache a response that had `null` fields? | A deleted account must not live on in the cache. | 404 and drop the entry; yes, cache it (A31, A33) |

## Task 10: Ledger replay

| # | Question | Why it matters | Default (A#) |
|---|---|---|---|
| Q29 | Is an oversell rejected for the whole history, or is only that transaction skipped? | The spec asks us to decide. | Reject the whole replay with an error (A35) |
| Q30 | Dates have no time. When two transactions share a date, which goes first? | A same-day buy and sell can look like an oversell, and order changes the average cost. | Keep the order they were given in (A35) |
