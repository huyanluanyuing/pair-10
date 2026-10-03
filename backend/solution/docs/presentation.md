# Walkthrough notes

For the two of us to present from. About 7 minutes, then the live demo in `demo-steps.md`.

## 1. What we built

Seven of the ten tasks, with 105 passing tests. Java 21 and Spring Boot 4.1.1, no database, no extra dependencies.

| Task | What it does | Built by |
|---|---|---|
| 1 | `GET /portfolios/{id}`: portfolio metadata mapped from the mock CRM | Wenxuan |
| 2 | `GET /portfolios/{id}/holdings`: positions with market value, weight, gain/loss, day change | Mahyar |
| 3 | `GET /portfolios/{id}/performance-history?range=`: daily snapshots filtered by range | Wenxuan |
| 4 | Bearer-token check on every route | Mahyar |
| 5 | `GET /portfolios/{id}/allocation`: value and percent by asset class | Wenxuan |
| 6 | `GET /clients/{id}/portfolios` and `/household-summary` | Mahyar |
| 10 | Data model, and ledger replay of transactions into quantity and average cost | Mahyar |

Not built: Task 7 (currency), Task 8 (holding detail), Task 9 (CRM cache and stale fallback).

## 2. How we worked

1. **Questions before code.** We read the spec and wrote down every decision it leaves open: 30 questions, each with a default we would build on (`questions.md`, `assumptions.md`). Our own restatement of the spec, with worked examples, is in `requirements.md`.
2. **A design both of us built to.** `architecture.md` fixed the package layout, the one error format, how time is handled and how the CRM is called, before we split up.
3. **Tests first, numbers by hand.** Each task started with failing tests. Expected values are literals worked out by hand from the seed data, never copied from the program's output.
4. **One branch per task, small commits, `main` always green.** Later tasks went through pull requests; we reviewed each other's before merging.
5. **A plan, an approval, then code.** Each task had a short plan (rules, edge cases, tests, files) that one of us approved first.

## 3. The design in one minute

- **Packages by feature:** `portfolio`, `crm`, `holding`, `history`, `client`, `ledger`, plus `common` for what they share.
- **Three roles:** controllers only translate HTTP; services load data and call calculators; calculators are plain classes with no framework, so their tests run in milliseconds.
- **One of each shared thing:** one error body `{error, message}`, one exception handler, one rounding rule, one auth filter.
- **The CRM is behind an interface.** Its field names appear only in the `crm` package. Connect timeout 1 s, read timeout 2 s, no retry.
- **Money is `BigDecimal`**, computed at full precision and rounded once, at output: 2 decimals for money, 6 for ratios.

## 4. Decisions we can defend

| Decision | Why |
|---|---|
| Quantity and cost basis come from replaying transactions, not from stored fields (A17) | It is what Task 10 asks for, and the two values cannot drift from the ledger. A closed position then has no cost basis, so we return `null`. |
| An unknown value is `null`, never 0 (A4) | A client must be able to tell "no change" from "cannot be computed". A previous close of 0 gives `dayChangePercent: null`. |
| CRM "not found" and "failed" are different (A9) | A 404 from the CRM is our 404. A timeout, a 5xx or a malformed body is a 502. Mixing them would hide outages. |
| Household percent = total day change ÷ total previous-close value (A24) | That is the value-weighted figure. On the seed data it is 0.010838; weighting the CRM's percentages by today's value would give 0.000607. |
| The auth filter covers every path, even unknown ones (A36) | An unauthenticated caller learns nothing about which routes exist. |
| A `securities` table added to the spec's four (architecture section 5) | Name, asset class and price belong to the ticker: `AAPL` is held in two portfolios. |
| Oversell rejects the whole replay (A35) | A partial result would be silently wrong. |

## 5. What we found in the spec

Worth mentioning, because we asked rather than guessed:

- Task 10's input table says transactions are "chronologically ordered"; its edge cases say do not assume it. We sort.
- Tasks 2 and 3 return arrays, but Task 7 adds `currency` and `exchangeRate` to "existing responses". An array has nowhere to put them.
- Task 2 says a zero quantity zeroes "both gain/loss fields", but `dayChangePercent` does not depend on quantity.
- Task 8's route is keyed by ticker, yet `purchaseDate` belongs to one position.
- The mock CRM reports a 500 day change with 0% for `P-9002`.

## 6. What is not done, and known limits

- **Tasks 7, 8 and 9 are not built.** Task 9 matters most: today a CRM failure is always a 502, with no stale fallback.
- **A missing history file is silent.** The history endpoint returns `[]` with no warning if the generated file is absent.
- **Data is in memory**, reloaded from the seed at startup.
- **One shared mock token** reads every client; there is no per-client authorization.
- **No CORS setup**, so a browser on another origin cannot call the API yet.

The full list, each with a next step, is in `architecture.md` section 8.

## 7. What we would do next

1. Task 9, the cache: it makes Task 1 resilient, and the design is already drawn in `architecture.md` section 4.
2. Task 7, currency: the conversion rule is decided (convert the full-precision value, then round), so totals stay consistent across endpoints.
3. Task 8, holding detail: small, reads the seed.
4. A startup warning when the history file is missing.

## 8. How we used AI

- **For:** reading the spec for open decisions, drafting the plan and tests for each task, writing code to the agreed rules, reviewing each diff and each other's pull request.
- **Guard rails:** a written working agreement (`CLAUDE.md`), rules files for the stack and for testing, and a plan we approved before any code.
- **Checks we did ourselves:** every expected number by hand; every decision recorded; `docs/ai-log.md` lists what we corrected.

## 9. Questions we may get

| Question | Answer |
|---|---|
| Why no database? | The spec allows in-memory. The schema is documented, and services read data through one class, so a database is a contained change. |
| Why no retry on the CRM? | A retry hides failures and changes the call counts the cache task verifies. The cache with stale fallback is the intended resilience. |
| How do you know the numbers are right? | Each expected value is worked by hand in `requirements.md` and asserted as a literal. The end-to-end tests compare the full JSON. |
| What happens when the CRM is slow? | The read times out after 2 seconds and the caller gets a 502 with a clear error body. A test proves a real timeout fires. |
| How did you avoid stepping on each other? | Packages by feature, one owner per shared file, and we reconciled the one overlap (error handling) into a single version. |
| What would break first at scale? | The in-memory data and the single-instance design. Both seams are listed in `architecture.md` section 7. |
