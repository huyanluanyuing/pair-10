---
name: analyze-spec
description: Turns a requirements document into docs/requirements.md, docs/assumptions.md and docs/questions.md, before any design or code.
argument-hint: "[path to the spec]"
disable-model-invocation: true
effort: medium
---
# Analyze a spec

Read the spec at `$ARGUMENTS` in full. If no path was given, ask for it. Write no design and no code in this step. Speed matters: the humans are waiting for the questions, so write `docs/questions.md` first, and don't run a separate checking pass with tools afterwards.

## Write three files, in this order

**`docs/questions.md`**: questions for the client, at most 30. Start with "Ask these first": at most six, the ones whose answer changes the design or more than one task. Then one table per task: `# | Question | Why it matters | Default (A#)`. Leave out questions whose default is obvious and changes no code or test.

**`docs/assumptions.md`**: every decision the spec leaves open, at most 35, as `**A#. Decision.** Why. *(Answers Q#.)*` Group by task, with a "Cross-cutting" group first. Numbers are permanent IDs: a new one takes the next free number.

**`docs/requirements.md`**: our restatement of the spec, one section per task, at most about 20 lines each:

- Route or deliverable, inputs and outputs.
- Rules, with every formula written out.
- Edge cases in two groups: "From the spec" and "Found by us".
- Tests: one line per behaviour, including every example the spec gives, with its exact numbers.

## Where open decisions hide

Check every task for these. Each gap becomes a question plus a default assumption.

- Words like "decide", "document your choice", "e.g.", "gracefully", "should".
- Any response that could show one user's data to another user.
- For each external call, which answers mean "not found" and which mean "failed". They need different handling, and malformed data counts as a failure.
- Two values that must agree but come from different sources or different formulas.
- Division by zero, empty lists, zero values, missing or `null` fields: is the answer `null`, 0 or an error?
- Time: what "today" is, which time zone, whether range ends are included, month ends and leap years.
- Numbers: the rounding rule and when it is applied, units, and whether a percentage is 0.05 or 5.
- List order, and how ties are broken.
- External systems: their real response shape and failure behaviour. Ask for a real sample, or a mock if there is one, before trusting the spec's example.
- Sample data: whether its dates suit any rule that counts from today, and whether ids match across sources.
- Responses that a later task extends: shape them now so the later task doesn't break clients.
- Which failures are 400, 401, 404 or 5xx, and the error body.

## Rules

- Cite only the spec. Label anything else as an inference.
- Short sentences. Tables where they help. No class names or design.

When done, reply in chat with: the number of tasks, assumptions and questions; the "Ask these first" list; and anything in the spec that contradicts itself. Then stop, so the human can review the three files.
