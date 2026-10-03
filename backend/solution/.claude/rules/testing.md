# Testing rules

- Test in this order when time is short: calculations, then edge cases, then status codes and error bodies, then one happy path per endpoint end to end. Wrong numbers fail silently; HTTP mistakes are obvious.
- Calculations live in plain functions or classes with no framework, no I/O and no clock reads, and get plain unit tests with no server.
- Each task's test list comes from `docs/requirements.md`. Name each test after the behaviour it proves: "an unknown id returns 404", not "test2".
- Expected values are literals worked out by hand or copied from the spec's examples. Never compute an expected value with the code under test.
- For every endpoint, cover: empty input, zero, an unknown id (404), invalid input (400), and ordering when the output is sorted.
- Time is injected. Tests fix "now" with a fake clock, and move it forward to test anything time-based. Never sleep in a test.
- External services are reached through an interface. Tests use a fake that returns scripted results, including errors and timeouts.
- One test proves that a real timeout fires, against a local stub server.
- A failing test is fixed in the code. If the test itself is wrong, say why before changing it.
- Run the whole suite before every merge.
