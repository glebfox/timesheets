---
name: issue-flow
description: Use when the user runs /issue-flow to start work on a GitHub issue of this project or to open the pull request for the current branch.
argument-hint: start <issue> | pr [<issue>]
disable-model-invocation: true
---

# Issue flow

The two steps of the implementation cycle in `CLAUDE.md` that touch GitHub. Work is tracked on the Timesheets board (user project #3, owner `glebfox`); this script moves an issue's Status there:

```bash
bash .skills/issue-flow/scripts/set-board-field.sh <issue> Status "<status>"
```

## `start <issue>`

1. Read the issue: `gh issue view <issue> --comments`.
2. Branch from an up-to-date `main`, named as `docs/conventions.md` says: `git switch main && git pull --ff-only && git switch -c <type>/<slug>`. If the working tree has changes, ask the user before switching. In a worktree the app created, keep its branch.
3. Set Status to `In progress`.
4. Follow the reading order in `CLAUDE.md` for the unit the issue touches: its spec and the `docs/decisions/` filenames. A feature goes on to design and plan; a bug fix, refactor, or chore skips them.

## `pr [<issue>]`

1. Take the issue from the argument or from this session's `start`; otherwise ask.
2. Consolidate: dispatch the `docs-consolidator` agent and apply what it reports. Tell the user about anything you left out and why.
3. Run the `local-ci` skill. Open the pull request only when it passes.
4. Commit what is left (Conventional Commits, `docs/conventions.md`) and push: `git push -u origin HEAD`.
5. `gh pr create` with the title and the opening line that `docs/conventions.md` describes. After `Closes #<issue>.` the body has `## Summary` (what changed for the user and why), then what a reviewer must check by hand, such as a live check that was not run.
6. Set Status to `In review`.
