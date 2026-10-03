# Live demo steps

About 5 minutes. Every request and expected result below was run on 2026-10-03 against this code.
Use three PowerShell terminals. Terminal 3 is the one the audience watches.

## Before the audience arrives

Terminal 1, from the repository root (`pair-10`): generate the history data and start the mock CRM.

```powershell
node backend/fixtures/generate-history.mjs
node backend/mock-crm.mjs
```

Terminal 2, from `backend/solution`: start the API on port 3000.

```powershell
.\mvnw.cmd spring-boot:run
```

Terminal 3: set up three shortcuts. `Mode` switches how the mock CRM behaves.

```powershell
$auth = "Authorization: Bearer superday-demo-token"
$api  = "http://localhost:3000"
function Mode($m) { Invoke-RestMethod -Method Post -Uri http://localhost:4002/__control -ContentType application/json -Body "{`"mode`":`"$m`"}" }
Mode ok
```

The history file must be generated on the demo day: its dates end on the day it is generated. Without it the history endpoint returns `[]`.

## The demo

| # | Say | Run in terminal 3 | Expect |
|---|---|---|---|
| 1 | "Every route needs a token." (Task 4) | `curl.exe -s -i "$api/portfolios/P-9001"` | `401`, `{"error":"unauthorized", ...}` |
| 2 | "Task 1 maps the CRM's legacy shape into ours." | `curl.exe -s -H $auth "$api/portfolios/P-9001"` | `totalMarketValue 48930.00`, `dayChangePercent 0.000613`, `label "Taxable Brokerage"` |
| 3 | "The CRM returns all of a client's accounts; we pick by id, not by position." | `curl.exe -s -H $auth "$api/portfolios/P-9002"` | `label "Retirement Account"`, `totalMarketValue 500.00` |
| 4 | "A CRM outage is a clear 502, not a crash." | `Mode error` then `curl.exe -s -i -H $auth "$api/portfolios/P-9001"` | `502`, `{"error":"crm_unavailable", ...}` |
| 5 | "The CRM hangs for 10 seconds; we give up after 2." | `Mode timeout` then `Measure-Command { curl.exe -s -H $auth "$api/portfolios/P-9001" }` | about 2 seconds, not 10 |
| 6 | "Missing CRM fields become null, not zero." | `Mode missing` then `curl.exe -s -H $auth "$api/portfolios/P-9001"` | `"label":null`, `"totalMarketValue":null`, still `200` |
| 7 | "Not found is different from failed." | `Mode ok` then `curl.exe -s -i -H $auth "$api/portfolios/UNKNOWN"` | `404`, `{"error":"not_found", ...}` |
| 8 | "Holdings: every calculated field is computed at request time. Quantity 120 and cost 200 come from replaying three transactions." (Tasks 2 and 10) | `curl.exe -s -H $auth "$api/portfolios/P-9001/holdings"` | `AAPL`: `quantity 120`, `costBasisPerShare 200.00`, `marketValue 27300.00`, `weightPercent 0.557940`. `ZERO`: `costBasisPerShare null` |
| 9 | "A previous close of zero gives null, not a divide-by-zero and not a made-up 0." | `curl.exe -s -H $auth "$api/portfolios/P-9002/holdings"` | `"dayChangePercent":null` |
| 10 | "Allocation groups the same values by asset class." (Task 5) | `curl.exe -s -H $auth "$api/portfolios/P-9001/allocation"` | `Equity 27300.00 0.557940`, `Fixed Income 21630.00 0.442060` |
| 11 | "History filters by range from today's date." (Task 3) | `curl.exe -s -H $auth "$api/portfolios/P-9001/performance-history?range=1D"` | two rows: yesterday and today, today is `48930.00` |
| 12 | "An invalid range is a 400, not a silent default." | `curl.exe -s -i -H $auth "$api/portfolios/P-9001/performance-history?range=invalid"` | `400`, `{"error":"bad_request", ...}` |
| 13 | "The household view sums a client's portfolios." (Task 6) | `curl.exe -s -H $auth "$api/clients/abc123/household-summary"` | `totalMarketValue 49430.00`, `portfolioCount 3`, `dayChangeAmount 530.00`, `dayChangePercent 0.010838` |
| 14 | "The tests prove the numbers." | In terminal 2's folder, a fourth terminal: `.\mvnw.cmd test` | `BUILD SUCCESS`, 105 tests |

## The numbers to have ready

- **Step 8, `AAPL`:** bought 100 at 190 and 50 at 220, sold 30. Quantity 150 − 30 = 120. Cost (19000 + 11000) ÷ 150 = 200. Market value 120 × 227.5 = 27300.
- **Step 13, household percent:** day change 30 + 500 + 0 = 530. Value at the previous close 48900 + 0 + 0 = 48900. 530 ÷ 48900 = 0.010838. This is weighted by value; a plain average of the portfolios' percentages would give a different, wrong number.

## If something goes wrong

| Symptom | Cause | Fix |
|---|---|---|
| Every call returns 401 | The header is missing or `$auth` is not set in this terminal | Re-run the shortcut lines |
| `GET /portfolios/P-9001` returns 502 | The mock CRM is not running, or its mode is `error` or `auto` | Start it in terminal 1, then `Mode ok` |
| History returns `[]` | The history file was not generated, or the API was not started from `backend/solution` | Run the generator, restart the API |
| Port 3000 is in use | An earlier run is still alive | `Get-NetTCPConnection -LocalPort 3000 \| Select OwningProcess`, then `Stop-Process -Id <id>` |
| `Mode` seems to do nothing | The CRM was restarted, which resets it to `auto` | Run `Mode ok` again |

In the CRM's default `auto` mode every fifth call fails on purpose. Keep it on `ok` during the demo except for steps 4 to 6.

Not in the demo, because they are not built: currency conversion (Task 7), holding detail (Task 8), the CRM cache and stale fallback (Task 9).
