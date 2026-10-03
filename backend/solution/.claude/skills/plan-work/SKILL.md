---
name: plan-work
description: Writes docs/work-plan.md - task dependencies, building blocks, rules for splitting work between people, a timeline, cuts and branch names.
argument-hint: "[minutes available] [number of people]"
disable-model-invocation: true
effort: medium
---
# Plan the work

`$ARGUMENTS` gives the minutes available and the number of people. If either is missing, ask. Read `docs/requirements.md` and `docs/assumptions.md`, and `docs/architecture.md` if it exists, so this can run while the design is still being written. Speed matters, so don't run a separate checking pass with tools afterwards.

Write `docs/work-plan.md`, at most 100 lines, with these sections:

1. **Goal.** Must have, should have and stretch, as a number of finished tasks per person. Size them from the planned minutes: the must-have fits in about 70% of each person's build minutes, the should-have in all of them, and anything more is stretch. A task is done when its tests pass, its decisions are recorded, it is merged, and its author can explain every line.
2. **Dependencies.** A table: `Task | Needs | How | Why`. "How" is **Merged** (it can't be finished until the other task's code is on `main`) or **Agreed** (it only needs a shared record or signature, so both can be built at once after that is pushed). Name the shared records.
3. **The split.** Don't assign tasks to people; the partner chooses too. Give:
   - building blocks: chains that are cheapest with one person, and singles that are free at any time, each with planned minutes;
   - the rules for any split: agree shared records before splitting a chain; the provider merges before the dependent wires in; one owner per shared file; balance the minutes;
   - an opening offer, and a table of other likely choices with what makes each one work.
4. **Timeline.** The parts done together are fixed. For 120 minutes: 0–15 understand, 15–25 foundation, 25–95 build, a check-in at 60, 95–100 buffer, 100–110 integrate, 110–120 prepare the walkthrough. Scale it for other lengths. No new code in the last 20 minutes.
5. **Cuts.** Cut inside a task first, using a `Task | Keep | Cut first` table, then each person's last task. Never cut calculation tests, the error format or the decision notes. When a task is 5 minutes past its box, stop and say so.
6. **Branches.** `<INITIALS>/feat/task-NN-short-name`, one per task, with each author's initials. To merge: pull `main` into the branch, run all tests, merge, push, tell the partner.
7. **Risks**, each with a response.
8. **Measured times**: a `Step | Planned | Actual` table to fill in.

All minutes are estimates; say so. When done, reply with the building blocks and the opening offer, then stop.
