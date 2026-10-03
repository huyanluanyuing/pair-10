# Live Currency Display Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add live daily CAD-to-USD display conversion to portfolio, holdings, and performance-history responses.

**Architecture:** Controllers parse the display currency and obtain a conversion context from a `CurrencyRateClient`. A Frankfurter HTTP client retrieves the latest CAD/USD reference rate, while response mappers convert only monetary values at full precision before existing output rounding.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring `RestClient`, Jackson, JUnit 5, MockMvc.

**Spec:** `docs/superpowers/specs/2026-10-03-live-currency-design.md`

## Global Constraints

- Support exact `CAD` and `USD` only; omitted `currency` means CAD.
- Use Frankfurter `GET /v2/rate/cad/usd`; no new dependency or API key.
- CAD uses exchange rate `1`; USD uses the current daily reference rate.
- Convert full-precision monetary values, then round money half-up to 2 decimals; preserve `null`.
- Never convert quantities, ratios, dates, identifiers, labels, or names.
- Invalid currency returns existing structured 400; a bad rate response or failure returns structured 502 `currency_rate_unavailable`.
- Task 7 affects only portfolio metadata, holdings, and performance history; allocation stays CAD-only.

## Review Focus

- Empty and lowercase `currency` values must return 400 rather than silently defaulting.
- A malformed, non-positive, or failed Frankfurter response must produce 502 without exposing upstream details.
- A USD conversion must use unrounded source values, especially `227.5 × rate` before money rounding.
- `null` monetary fields from CRM responses must remain `null` after conversion.
- `?currency=CAD` and an omitted parameter must have the same CAD values and exchange rate 1.

---

### Task 1: Currency context and conversion rules

**Files:**
- Create: `src/main/java/ca/en/solution/currency/CurrencyRateClient.java`
- Create: `src/main/java/ca/en/solution/currency/DisplayCurrency.java`
- Create: `src/main/java/ca/en/solution/currency/CurrencyContext.java`
- Create: `src/main/java/ca/en/solution/currency/CurrencyConverter.java`
- Create: `src/test/java/ca/en/solution/currency/CurrencyConverterTest.java`

**Interfaces:**
- Produces `CurrencyContext resolve(String requestedCurrency)` with `currency(): String`, `exchangeRate(): BigDecimal`, and `convertMoney(BigDecimal): BigDecimal`.
- Consumes `CurrencyRateClient#cadToUsdRate(): BigDecimal` only for USD.
- Produces `CurrencyConverter#contextFor(String requestedCurrency): CurrencyContext` for every endpoint mapper.

- [ ] **Step 1: Write failing converter tests**

Assert an omitted/CAD request yields `(CAD, 1)`, a USD request using fake rate `0.73` converts `227.5` to `166.08`, a `null` amount remains null, and `usd`, `EUR`, and empty values throw `BadRequestException`.

- [ ] **Step 2: Run the converter test to verify it fails**

Run: `mvn -o -Dtest=CurrencyConverterTest test`

Expected: FAIL because the currency types do not exist.

- [ ] **Step 3: Implement the currency interfaces and converter**

Use exact `CAD`/`USD` matching. Create the rate context only once per request; use `Rounding.money` after multiplying a non-null input by the context rate.

- [ ] **Step 4: Run the converter test to verify it passes**

Run: `mvn -o -Dtest=CurrencyConverterTest test`

Expected: PASS.

### Task 2: Frankfurter HTTP rate client and error mapping

**Files:**
- Create: `src/main/java/ca/en/solution/currency/FrankfurterCurrencyRateClient.java`
- Create: `src/main/java/ca/en/solution/currency/CurrencyRateUnavailableException.java`
- Modify: `src/main/java/ca/en/solution/common/ApiExceptionHandler.java`
- Modify: `src/main/resources/application.properties`
- Create: `src/test/java/ca/en/solution/currency/FrankfurterCurrencyRateClientTest.java`

**Interfaces:**
- Consumes `currency-rate.base-url`, `currency-rate.connect-timeout`, and `currency-rate.read-timeout` properties.
- Implements `CurrencyRateClient#cadToUsdRate()`.
- Throws `CurrencyRateUnavailableException` for non-2xx, malformed, missing, or non-positive rates.

- [ ] **Step 1: Write failing HTTP client tests**

With a local `HttpServer`, assert the client requests `/v2/rate/cad/usd`, reads a JSON `rate` decimal, and rejects non-2xx, malformed JSON, missing rate, and zero/negative rate.

- [ ] **Step 2: Run the client test to verify it fails**

Run: `mvn -o -Dtest=FrankfurterCurrencyRateClientTest test`

Expected: FAIL because the HTTP client does not exist.

- [ ] **Step 3: Implement the client and shared 502 handler**

Configure `RestClient` with the existing bounded timeout pattern. Parse only the `rate` field; expose the fixed 502 body `{ "error": "currency_rate_unavailable", "message": "Currency rate service is unavailable." }` through `ApiExceptionHandler`.

- [ ] **Step 4: Run focused client tests to verify they pass**

Run: `mvn -o -Dtest=FrankfurterCurrencyRateClientTest test`

Expected: PASS.

### Task 3: Convert portfolio metadata response

**Files:**
- Create: `src/main/java/ca/en/solution/portfolio/CurrencyPortfolioResponse.java`
- Modify: `src/main/java/ca/en/solution/portfolio/PortfolioController.java`
- Modify: `src/test/java/ca/en/solution/portfolio/PortfolioEndpointTests.java`

**Interfaces:**
- Consumes `CurrencyConverter#contextFor(String)` and `PortfolioService#getPortfolio(String)`.
- Produces the existing portfolio fields plus `currency` and `exchangeRate` in the top-level JSON object.

- [ ] **Step 1: Write failing portfolio endpoint tests**

Use a deterministic fake rate `0.73`. Assert USD converts `totalMarketValue 48930` to `35718.90` and `dayChangeAmount 30` to `21.90`, preserves `dayChangePercent`, passes through CRM nulls, and returns `currency: "USD"`, `exchangeRate: 0.73`. Assert CAD/default return rate 1 and invalid currency gives 400.

- [ ] **Step 2: Run the portfolio endpoint tests to verify they fail**

Run: `mvn -o -Dtest=PortfolioEndpointTests test`

Expected: FAIL because the response has no conversion context or new schema fields.

- [ ] **Step 3: Implement the portfolio response mapper and controller query handling**

Request the conversion context before the CRM call so rate availability is established for the complete response; map only `totalMarketValue` and `dayChangeAmount` as monetary fields.

- [ ] **Step 4: Run the portfolio endpoint tests to verify they pass**

Run: `mvn -o -Dtest=PortfolioEndpointTests test`

Expected: PASS.

### Task 4: Convert holdings and performance-history responses

**Files:**
- Create: `src/main/java/ca/en/solution/holding/CurrencyHoldingResponse.java`
- Create: `src/main/java/ca/en/solution/history/CurrencyPerformanceSnapshot.java`
- Modify: `src/main/java/ca/en/solution/holding/HoldingController.java`
- Modify: `src/main/java/ca/en/solution/history/PerformanceHistoryController.java`
- Modify: `src/test/java/ca/en/solution/holding/HoldingsEndToEndTest.java`
- Modify: `src/test/java/ca/en/solution/history/PerformanceHistoryEndToEndTest.java`

**Interfaces:**
- Consumes the CAD `HoldingResponse` and `PerformanceSnapshot` records plus a `CurrencyContext`.
- Produces one converted response record per source element, each carrying `currency` and `exchangeRate`.

- [ ] **Step 1: Write failing holdings and history endpoint tests**

Inject fake `0.73` rate. For holdings, assert AAPL's price `166.08`, market value `19929.00`, unchanged quantity/weight, and per-element currency metadata; assert CAD/default parity, invalid currency 400, and unavailable rate 502. For history, assert `48930` converts to `35718.90`, retains dates/range filtering, and includes the same metadata/error behavior.

- [ ] **Step 2: Run the focused endpoint tests to verify they fail**

Run: `mvn -o -Dtest=HoldingsEndToEndTest,PerformanceHistoryEndToEndTest test`

Expected: FAIL because the controllers return the CAD records directly.

- [ ] **Step 3: Implement holdings and history response mapping**

Resolve one context per request, convert only each record's monetary fields, retain the existing service calculations/range behavior, and keep `null` values untouched.

- [ ] **Step 4: Run focused endpoint tests to verify they pass**

Run: `mvn -o -Dtest=HoldingsEndToEndTest,PerformanceHistoryEndToEndTest test`

Expected: PASS.

### Task 5: Document live-rate behavior and complete verification

**Files:**
- Modify: `docs/assumptions.md`
- Modify: `docs/architecture.md`
- Modify: `README.md`
- Modify: `docs/requirements.md`

**Interfaces:**
- Documents the current daily reference-rate source, configuration, exact response additions, and structured 502 behavior.

- [ ] **Step 1: Update documentation**

Replace the static CAD-to-USD assumption with Frankfurter's daily rate, record no historical-rate lookup or rate cache, and show the `currency-rate.*` settings and bearer-authenticated request examples.

- [ ] **Step 2: Run the complete verification suite**

Run: `mvn -o clean verify`

Expected: PASS with all tests, including the new live-currency coverage.

- [ ] **Step 3: Inspect the final change**

Run: `git diff --check` and `git status --short`

Expected: no whitespace errors; only intentional Task 7 files are changed.
