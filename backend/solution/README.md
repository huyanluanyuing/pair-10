# Backend solution

Implemented: Task 1 portfolio metadata through the external CRM, Task 2 holdings,
Task 3 performance history, Task 4 authentication, Task 5 asset allocation, and
Task 10 ledger replay. Java 21,
Spring Boot 4.1.1, and Maven. No additional dependencies or database are needed.

## Run

From the repository root, start the supplied CRM in one terminal:

```powershell
node backend/mock-crm.mjs
```

In another terminal, open `backend/solution`, select Java 21, and run Maven:

```powershell
$env:JAVA_HOME = 'C:/Users/Lenovo/.jdks/ms-21.0.8'
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
# This machine already has Maven 3.9.16 cached by the wrapper.
$featureMaven = Get-ChildItem "$env:USERPROFILE/.m2/wrapper/dists/apache-maven-3.9.16" -Recurse -Filter mvn.cmd |
    Select-Object -First 1 -ExpandProperty FullName
& $featureMaven spring-boot:run
```

With a working wrapper, use `./mvnw spring-boot:run` on macOS/Linux or
`./mvnw.cmd spring-boot:run` on Windows instead. An installed Maven 3.6.3 or newer
also works with `mvn spring-boot:run`. This Windows environment's supplied wrapper
fails before Maven starts because it indexes a null directory `Target` property;
the cached Maven command above avoids that existing wrapper issue.

The API runs on port 3000. Override `server.port` if necessary.

Every request must send the mock auth token (Task 4):
`Authorization: Bearer superday-demo-token`. Anything else, including a missing
header, a missing `Bearer ` prefix or a wrong token, returns 401.

```powershell
$auth = @{ Authorization = 'Bearer superday-demo-token' }
Invoke-RestMethod http://localhost:3000/portfolios/P-9001 -Headers $auth
Invoke-RestMethod http://localhost:3000/portfolios/P-9002 -Headers $auth
Invoke-RestMethod http://localhost:3000/portfolios/P-9001/holdings -Headers $auth
```

Configuration:

| Property | Default | Purpose |
| --- | --- | --- |
| `server.port` | `3000` | API port |
| `crm.base-url` | `http://localhost:4002` | CRM URL; also settable with `CRM_BASE_URL` |
| `crm.connect-timeout` | `1s` | Connection timeout |
| `crm.read-timeout` | `2s` | Socket read timeout |
| `history.file` | `file:../fixtures/performance-history.json` | Generated performance-history fixture; also settable with `HISTORY_FILE` |
| `auth.token` | `superday-demo-token` | The one valid bearer token; also settable with `AUTH_TOKEN` |

## Test

In `backend/solution`, with Java 21 and the Maven selection above:

```powershell
& $featureMaven test
```

Or use `mvn test` / the working Maven wrapper. The tests start an isolated local
HTTP stub on a random port; they do not require the supplied CRM. They exercise
the real controller, service, CRM client, and JSON mapping, including matching
the second account, nested accounts, missing/null fields, empty/zero values,
decimal rounding, malformed responses, upstream status codes, redirects,
connection refusal, and real 2-second header/body timeouts. No tests sleep.

To test the supplied mocks from the repository root:

```powershell
npm test
```

## API contract and manual failure checks

`GET /portfolios/{id}` returns exactly the nine fields in Task 1. Each field is
present even when its CRM value is missing or null. Money is rounded half-up to
2 decimals and ratios to 6 (A3); values are sourced from the CRM, not calculated
from local holdings. The CRM timestamp is passed through.

Errors have exactly `{ "error": "...", "message": "..." }`:

| Condition | HTTP | Error code |
| --- | --- | --- |
| Missing or invalid `Authorization` header, on any path | 401 | `unauthorized` |
| CRM 404 or no matching account | 404 | `not_found` |
| Other non-2xx status, malformed structure/JSON, timeout or connection failure | 502 | `crm_unavailable` |

`GET /portfolios/{id}/holdings` calculates the position values from the replayed
ledger, sorted by ticker. `GET /portfolios/{id}/allocation` groups those
full-precision values by asset class, then rounds values to two decimals and
percentages to six decimals. Both return `[]` for `P-EMPTY`; unknown portfolios
return the structured 404.

`GET /portfolios/{id}/performance-history?range=1D|1M|YTD|1Y|All` reads the
generated history fixture, filters inclusively through today's UTC date, and
sorts ascending by date. Omitting `range` means `All`; an invalid value returns
the structured `400 bad_request`. Generate the runtime fixture from the repository
root before starting the service:

```powershell
node backend/fixtures/generate-history.mjs
```

Set the CRM's global mode; the backend deliberately sends no `mode` query:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:4002/__control -ContentType application/json -Body '{"mode":"ok"}'
Invoke-RestMethod http://localhost:3000/portfolios/P-9002 -Headers $auth
```

Repeat with `nested` (same mapping), `missing` (null label and value), `error`
(502), and `timeout` (502 after about 2 seconds instead of the CRM's 10 seconds).
In `ok` mode, request an unknown id for 404. Restore `auto` after checking.

## Scope

Task 1 has no retries or caching; HTTP redirects are rejected. Connect and read
timeouts bound stalled operations, rather than imposing a total download deadline.
No local data is persisted. Caching/stale fallback belongs to Task 9 and is not
implemented yet. Task 2's holdings read their quantity and cost basis from Task
10's ledger replay. Tasks 1–5 and 10 are implemented; Tasks 6–9 remain.

Decisions are recorded in `docs/assumptions.md`; structure and extension points
are recorded in `docs/architecture.md`.
