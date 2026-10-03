---
name: design-architecture
description: Writes docs/architecture.md from the analyzed requirements - decisions with alternatives, structure, diagrams, test strategy, growth and limitations.
disable-model-invocation: true
effort: medium
---
# Design the architecture

Read `docs/requirements.md`, `docs/assumptions.md` and `docs/questions.md`. If the stack hasn't been agreed, ask before going further. Speed matters, so don't run a separate checking pass with tools afterwards.

First, propose the key decisions in chat as a table: decision, choice, main alternative, why. Cover at least the framework, storage, project layout, error handling, how time is handled, and how each external system is called. Prefer the simplest option that meets the requirement, and say what would make the alternative worth it. Wait for approval.

Then write `docs/architecture.md`, at most 150 lines not counting the diagrams, with these sections. Keep diagrams to the boxes and arrows a reader needs; detail goes in the text.

1. **Decisions at a glance**: the approved table.
2. **Project structure**: the folder tree, packaged by feature, with one line per folder saying what it holds. Add the rules: handlers only translate HTTP, services touch data and external systems, calculations are pure.
3. **How a request flows**: a sequence diagram for one typical request, with numbered steps explained below it.
4. **External calls**: for each external system, a sequence diagram with its failure paths (timeout, error, fallback) and the timeout values.
5. **Data model**: an ER diagram with each table's keys and only the columns that matter, and a table of what is stored and what is computed at request time.
6. **Test strategy**: the levels (unit, web slice, full app), how time and external systems are faked, and the priority order under time pressure.
7. **Growth**: a table of "when this grows, change, where the seam already is".
8. **Known limitations**: each one as "the limit, why it's acceptable here, the next step".

## Mermaid rules

These keep diagrams readable in simple Markdown viewers.

- Use `participant`, never `actor`.
- Write step numbers in the arrow text ("1. GET /items"). Don't use `autonumber`.
- Keep each label on one line, with no `<br/>`.
- Don't use `Note over`.
- In an ER diagram, put each attribute on its own line.

When done, reply with a five-line summary and the decisions that still depend on unanswered questions. Then stop.
