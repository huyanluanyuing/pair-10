---
name: implement-task
description: Builds one task end to end - plan, tests, code, test run, self-review, docs and a proposed commit.
argument-hint: "[task number or name]"
disable-model-invocation: true
---
# Implement one task

The task is `$ARGUMENTS`. If it is unclear which task that is, ask.

1. **Read.** The task's section of `docs/requirements.md`, the assumptions it cites, and `docs/architecture.md`. Check `git status`: the working tree must be clean.
2. **Plan, then wait.** In at most 12 lines: the rules, the edge cases, the tests you will write, the files you will create or change, and any open decision with your recommendation. Wait for approval.
3. **Branch.** Create `<INITIALS>/feat/task-NN-short-name` from an up-to-date `main` (see `CLAUDE.md`).
4. **Tests first.** Write the tests from the task's test list. For every calculated expected value, show the arithmetic in chat so the human can check it by hand, or use the spec's own example. Run the tests and show that they fail for the right reason.
5. **Code.** The smallest code that passes, following the rules files. Nothing extra.
6. **Run the whole suite** and show the result. If something fails, fix the code. Change a test only after saying why the test is wrong.
7. **Review the diff** (`git diff`) as a hostile reviewer and report in chat: any broken rule, any edge case from step 2 without a test, debug output, unused code, secrets. Fix what you find and run the tests again.
8. **Docs.** New decisions go into `docs/assumptions.md`. Structure changes go into `docs/architecture.md`. Anything cut becomes a line in its known limitations.
9. **Commit.** Propose small commits, each saying one thing, such as "Task 3: return 404 for an unknown id". Don't commit or merge unless asked.

Finish with: what was built, the test result, what was cut, and what the human should check by hand.
