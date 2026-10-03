# Working agreement for AI assistants

How we work with AI assistants: general conventions that apply to any project. Project facts live in the docs imported at the end; stack rules are in `.claude/rules/`, and the workflow skills are in `.claude/skills/`.

## How we work

- Understand before building. Read the task in `docs/requirements.md` and its decisions in `docs/assumptions.md` before writing code.
- Never decide silently. When the spec leaves a choice open, name the options, recommend one with a reason, and wait. Record the decision in `docs/assumptions.md`.
- Plan before code: state the rules, edge cases, tests and files for the task, and wait for approval.
- Build the simplest thing that meets the requirement. No abstraction without a second use or a test fake. Growth ideas go in `docs/architecture.md`, not in code.
- No new dependency, framework or file that nobody asked for. Ask first.
- A missing or unknown value is `null`, never a made-up 0.
- Tests come first for calculations. Expected values come from the spec's examples or from hand arithmetic shown step by step, never from running the code under test.
- Run the tests after every change and show the result. Never weaken, skip or delete a failing test to make it pass.
- Keep docs and diagrams in step with the code, on the same branch. A diagram that doesn't match the code is worse than none.
- When the human corrects something you produced, add one line to `docs/ai-log.md`: what was wrong, why, and the fix.

## Git

- One branch per task, starting with the author's initials: `<INITIALS>/feat/task-NN-short-name`, for example `MGK/feat/task-02-search`. Ask for the initials if you don't know them. `main` always passes its tests.
- Small commits that each say one thing. A test goes in the same commit as the code it tests.
- Propose commit messages. The human commits and merges.

## Commands

Java 21, Spring Boot 4.1.1, Maven wrapper. Run from `backend/solution`. On Windows use `.\mvnw.cmd` in place of `./mvnw`.

- Test: `./mvnw test`
- Run: `./mvnw spring-boot:run`

## Project docs

Written on the day with the skills. Files that don't exist yet are skipped.

@docs/requirements.md
@docs/assumptions.md
@docs/architecture.md
@docs/work-plan.md
