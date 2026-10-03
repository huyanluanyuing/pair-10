# Assumptions

Every decision the spec (`backend/REQUIREMENTS.md`) leaves open. Numbers are permanent: a new assumption takes the next free number.
"Inference" marks anything that comes from the supplied files or our own reading, not from the spec text.

## Cross-cutting

**A1. Every route needs the token; one token reads all clients.** The spec gates "portfolio endpoints" and accepts a hardcoded token. We protect `/portfolios`, `/clients` and `/holdings` alike and do no per-client ownership check (inference). *(Answers Q6.)*

**A2. One error body everywhere: `{ "error": code, "message": text }`.** Task 4 defines it for 401; we reuse it. Codes: `bad_request` (400), `unauthorized` (401), `not_found` (404), `crm_unavailable` (502). No stack traces. *(Answers Q9.)*

**A3. Money is rounded to 2 decimals, ratios to 6, half-up, once, at output.** Calculations run at full precision. All percentages are decimals (0.05 means 5%), as the spec's examples show. Quantities are returned as stored. *(Answers Q7.)*

**A4. An unknown value is `null`, never 0.** The field is always present in the JSON. This applies to every division by zero we compute. *(Answers Q5.)*

**A5. "Today" is the current date in UTC, read from an injected clock.** The history generator builds its dates in UTC (inference from `generate-history.mjs`). *(Answers Q8.)*

**A6. Ids, tickers and query values match exactly.** `?currency=usd` and `?range=all` are 400; ticker `aapl` is 404. An empty value (`?range=`) is 400. *(Answers Q10.)*

**A7. Our own data is held in memory, loaded from the supplied `fixtures/seed.json`.** The spec allows in-memory storage. Whether an id exists for Tasks 2, 3, 5, 6 and 8 is decided by this data; those endpoints never call the CRM. Only Task 1 calls the CRM. *(Answers Q3.)*

## Task 1: Portfolio metadata via CRM

**A8. The account is picked by `acct_ref`, never by position.** The CRM returns all of the client's accounts (inference from `CRM.md`). We look in `client_record.accounts`, then in `client_record.relationships.accounts` (the mock's `nested` mode).

**A9. Not found and failed are different.** Not found (our 404): the CRM answers 404, or answers 200 without the requested `acct_ref`. Failed: timeout, connection error, any other non-2xx status, a body that is not JSON, or no `client_record` or account list. *(Answers Q11.)*

**A10. A missing or `null` CRM field becomes `null` in our response, with status 200.** In the mock's `missing` mode, `label` and `totalMarketValue` are `null`. *(Answers Q12.)*

**A11. CRM calls time out after 1 s to connect and 2 s to read. No retry.** The mock's slow answer takes 10 s. A retry would change the call counts Task 9 verifies. *(Answers Q13.)*

**A12. CRM values are passed through, not recomputed.** `asOf` is `meta.retrieved_at`. `dayChangePercent` for `P-9002` stays the CRM's `0`, because we cannot tell a CRM 0 from "unknown". *(Answers Q5.)*

## Task 2: Holdings

**A13. `previousClosePrice` of 0 gives `dayChangePercent: null`.** The spec allows 0 or `null`; A4 picks `null`. *(Answers Q5.)*

**A14. `quantity: 0` gives 0 for `marketValue`, `weightPercent`, `unrealizedGainLoss` and `dayChangeAmount`.** `dayChangePercent` does not use quantity and stays price-based (`ZERO`: 0.2). `weightPercent` is 0 whenever `marketValue` is 0, which also covers a portfolio whose total is 0. Quantities and prices are assumed not negative. *(Answers Q15.)*

**A15. Holdings are returned by ticker, ascending.** *(Answers Q16.)*

**A16. The portfolio total used for `weightPercent` is the sum of our holdings' market values.** It is not the CRM figure. In the seed the two agree (48930). *(Answers Q3.)*

**A17. Task 2's `quantity` and `costBasisPerShare` come from replaying the transactions, not from the seed's fields.** Decided 2026-10-03 (MGK), with Task 10 built first. `costBasisPerShare` is `null` for a closed position (`ZERO`), where the seed says 10. With a `null` cost basis and zero quantity, `unrealizedGainLoss` is 0. *(Answers Q4.)*

## Task 3: Performance history

**A18. A range keeps snapshots with start ≤ date ≤ today.** Starts: `1D` today minus 1 day; `1M` today minus 1 calendar month; `1Y` today minus 1 year; `YTD` January 1 of today's year; `All` no start. A day that does not exist is clamped to the month's last day (31 March minus 1 month is 28 or 29 February). *(Answers Q17, Q18.)*

**A19. Snapshots are sorted by date ascending.** A missing `range` means `All`. Any other value is 400.

**A20. History comes from the supplied generated file. We never pad, shift or invent snapshots.** If the file is old, short ranges return few or no rows; that is correct (inference from `START-HERE.md`). *(Answers Q19.)*

## Task 4: Auth

**A21. The valid header is exactly `Bearer superday-demo-token`.** The token is from `requests.http` (inference) and is configurable. Missing header, wrong scheme, wrong token or empty token are all 401. Auth is checked first: 401 wins over 400 and 404. *(Answers Q20.)*

**A36. The auth check covers every path, including routes that do not exist.** Without a valid token an unknown route is 401, not 404, so the API reveals nothing to an unauthenticated caller. Decided 2026-10-03 (MGK). The token is read from `auth.token`.

## Task 5: Allocation

**A22. One entry per asset class that has at least one holding, even if its value is 0.** `percent` is 0 when `value` is 0. Order: `value` descending, then `assetClass` ascending. Unknown id is 404 (the spec does not say; inference from the other tasks). *(Answers Q21.)*

## Task 6: Clients and household

**A23. Client, portfolio list and all totals come from our own data.** `totalMarketValue` and `dayChangeAmount` per portfolio are sums over its holdings. No CRM call. *(Answers Q3.)*

**A24. Household `dayChangePercent` = total `dayChangeAmount` ÷ total previous-close value.** Previous-close value is the sum of `quantity × previousClosePrice`. This equals weighting each portfolio's percent by its previous value. `null` when the divisor is 0. *(Answers Q2.)*

**A25. `portfolioCount` counts empty portfolios. The list is ordered by `portfolioId` ascending.** A known client with no portfolios gets `[]`, and a summary of zeros with `dayChangePercent: null`. *(Answers Q22.)*

## Task 7: Currency

**A26. The rate is fixed: 1 CAD = 0.73 USD (seed `CADtoUSD`), configurable.** For CAD the rate is 1. Every portfolio's native currency is CAD. *(Answers Q23.)*

**A27. Array responses stay arrays; every element carries `currency` and `exchangeRate`.** `GET /portfolios/:id` carries both at the top level. Both fields are always present, also without the query param. An empty array carries neither. *(Answers Q1.)*

**A28. Convert the full-precision CAD value, then round.** A converted amount is never recomputed from a rounded converted price. No balancing of cents. History is converted at today's rate. *(Answers Q25.)*

**A29. `currency` works only on the three endpoints Task 7 names.** Allocation, client list, household summary and holding detail ignore it and return CAD. *(Answers Q24.)*

## Task 8: Holding detail

**A30. Detail is one record per ticker, taken from the seed.** `purchaseDate` and the 52-week values are returned as stored, not derived. `priceHistory` is sorted by date ascending. Prices are CAD. *(Answers Q26.)*

## Task 9: Cache

**A31. TTL is 60 s, configurable. The cache is in memory, keyed by the requested portfolio id.** It holds the mapped CAD value, before currency conversion. Only the requested account is cached, not its siblings. A 200 with `null` fields is cached like any success. *(Answers Q27, Q28.)*

**A32. A successful CRM call after expiry replaces the entry: `stale: false`, new `cachedAt`.** `cachedAt` is our clock at the moment of the fetch. A stale entry has no maximum age. *(Answers Q27.)*

**A33. CRM failure with nothing cached is 502 `crm_unavailable`.** Same code for an error and a timeout. A CRM 404 is never served from cache: we return 404 and drop the entry. *(Answers Q14, Q28.)*

## Task 10: Ledger replay

**A34. At zero quantity `averageCostBasisPerShare` is `null`.** A later BUY starts a new average. An empty list gives quantity 0 and `null`. *(Answers Q4.)*

**A35. Replay sorts by `date` itself; equal dates keep their input order. Invalid input rejects the whole replay with an error, with no partial result.** Invalid: a SELL for more than is held, quantity ≤ 0, price < 0, an unknown type, a date that is not `YYYY-MM-DD`. The function is internal: no public endpoint. *(Answers Q29, Q30.)*
