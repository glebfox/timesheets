# Timesheets — Guidance

Map of this repository's knowledge base: pointers, not content. The docs hold only what the code cannot tell; read the code for everything else. Where the code has moved past a doc, the code wins and the doc is fixed in the same change.

## Documentation Map

- **README.md** — the public face of the repository: features, screenshots (`docs/images/`), and getting started.
- **ARCHITECTURE.md** — what is built: where things live, profiles, non-obvious commands. Written from the code.
- **docs/conventions.md** — rules for writing and committing code that tooling does not enforce and the code does not consistently show.
- **docs/guidelines/** — non-obvious mechanics of this project's platform and test harness. Read when implementing. Jmix framework mechanics live in the project skills (start with the `jmix` skill), not here.
  - `testing.md` — how the tests run, the harness specifics, and what only a live check can verify.
  - `database.md` — changelog layout, demo data, and how soft delete shapes uniqueness.
  - `design-system.md` — the Aura theme, color, typography, and spacing tokens, and the project's own colors. Read when building or reviewing UI.
- **docs/decisions/** — append-only log of significant decisions and the alternatives they beat; only choices that are hard to reverse, counterintuitive, or keep getting re-litigated get a file. The filenames are the index. Not created yet: created with the first decision that clears the bar.
- **docs/reference/** — dated external research; frozen. Never a source for what the product does. Not created yet: created at first need.
- **docs/specs/data-model/*.md** — what each entity and value type is: its meaning, rules, and lifecycle, and what no test covers. `README.md` gives the overview and the rules that hold for every entity.
- **docs/specs/roles/*.md** — what each security role grants and who holds it. `README.md` gives the overview.
- **docs/specs/views/*.md** — what each screen does as its users see it, one file per functional area, and what no test covers.
- Each spec opens with `Source:`, the code paths it describes, relative to the base package `com/glebfox/timesheets/` in `src/main/java` and `src/main/resources`.

## Reading order

- **Always:** this map and `ARCHITECTURE.md`.
- **Starting on a unit:** its spec, and the `docs/decisions/` filenames for the area.
- **Implementing:** the `jmix` skill, which routes each artifact to the skill that governs it, and the relevant `docs/guidelines/`.

## Workflow

- Implementation cycle: GitHub issue → branch → design and plan → execute → verify → consolidate → pull request → merge.
- The user runs the cycle's GitHub steps with the `issue-flow` skill (`/issue-flow start <issue>`, `/issue-flow pr`): the branch, the issue's Status on the Timesheets board, consolidation, local CI, and the pull request. Only the user invokes it; asked for those steps without it, follow `.skills/issue-flow/SKILL.md`. A new issue is filed with the `create-issue` skill.
- When execution changes scope, interfaces, approach, or task order, update the design or plan at once.
- Consolidate before the merge: specs for changed behavior, including their **Not covered**; `ARCHITECTURE.md` for changed layout or commands; `docs/conventions.md` for a rule adopted; `docs/guidelines/` for a non-obvious mechanic; `docs/decisions/` for decisions that clear the bar. The design and plan are not committed, so consolidation is the only way anything in them outlives the work.
- Bug fixes, refactors, and chores skip the design and plan, but not consolidation.

## Documentation Hygiene

- A spec states what a unit does, not how: a statement belongs only if it survives a behavior-preserving refactor. Platform mechanics go to guidelines; when unsure, leave it in the spec.
- "Spec" means the per-unit docs in `docs/specs/`; a design or a plan is not one.
- `ARCHITECTURE.md` maps packages and their roles, not classes: a statement belongs only if it stays true when a class is added, renamed, or removed. Specific classes are named in the specs' `Source:` and in the guidelines that need them.
- Living docs describe what is built, never what is planned. A planned change lives in its issue until it lands.
- Rationale lives in `docs/decisions/`, and living docs link to it. A decision is superseded, never edited, and never a source for current state.
- Rules that tooling enforces are not restated in prose.

## Problems with a jmix-* skill

While following a `jmix-*` skill, when an instruction is unclear, wrong, or silent on something that mattered, record it in `.jmix/skill-feedback.md` at that moment — not at the end of the task — then continue the work. Do not wait to feel that a skill failed you; watch for the action instead. You went outside the skill to settle its own subject — grepped a framework jar, a theme stylesheet, an upstream template, or the source — or a gate caught something the skill's own rules would not have prevented. Working around the gap yourself is not a reason to skip recording it: a silent workaround is the main way these gaps survive. Do NOT edit the skill files. Read `/jmix-skill-feedback:record` for the entry format, and `/jmix-skill-feedback:submit` to file findings upstream. When you dispatch a subagent that will run a `jmix-*` skill, put this instruction in its prompt and ask it to return what it recorded: its findings reach you no other way.
