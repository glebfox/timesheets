---
name: issue-flow
description: Use when the user asks to start work on a GitHub issue of this project, to open the pull request for the current branch, or to merge it.
argument-hint: start <issue> | pr [<issue>] | close [<pr>]
---

# Issue flow

The steps of the implementation cycle in `CLAUDE.md` that touch GitHub. Work is tracked on the Timesheets board (user project #3, owner `glebfox`); this script moves an issue's Status there:

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

## `close [<pr>]`

Merges the pull request and leaves `main`, up to date, as the only branch locally and on origin.

1. Take the pull request from the argument or from this session's `pr`; otherwise the one of the current branch: `gh pr view --json number`.
2. Check it: `gh pr view <pr> --json state,isDraft,mergeable,title,headRefName,headRefOid,statusCheckRollup,closingIssuesReferences`. Merge nothing, and report why, when a check is pending or failed, the pull request is a draft, or `mergeable` is `CONFLICTING`; `UNKNOWN` only means GitHub is still computing it, so ask again. A pending check is not waited for: the user runs `close` again once it passes. When this checkout is on `headRefName`, ask first if it holds work the pull request lacks: `git status --porcelain` lists changes, or `git rev-parse HEAD` is not `headRefOid`.
3. Squash-merge it, with the title and number as the subject and no body or trailers: `gh pr merge <pr> --squash --subject "<title> (#<pr>)" --body "" --match-head-commit <headRefOid> --delete-branch`. The repository allows only squash merges and deletes the branch on origin after the merge; `--delete-branch` deletes the local branch and switches this checkout to `main`. In a worktree the app made, `main` is checked out in another worktree and `--delete-branch` fails after the merge: leave it out.
4. Update `main`: `git switch main && git pull --ff-only && git fetch --prune`. In a worktree the app made, update it where it is checked out, the first path of `git worktree list`: `git -C <path> pull --ff-only && git fetch --prune`; the worktree keeps its branch until the app removes the worktree. If the pull fails, as on a local change that the update touches, leave that checkout as it is and report it.
5. Outside a worktree the app made, a local branch that `gh` left behind goes only when it holds nothing more: `[ "$(git rev-parse <branch>)" = <headRefOid> ] && git branch -D <branch>`; `-D`, because a squash merge leaves the branch's own commits unmerged.
6. `Closes #<issue>` closes the issue, and the board moves it to `Done`. Check: `gh issue view <issue> --json state,projectItems`. If its Status is not `Done`, set it.
7. Stop what this session started for a live check: the server on :8081, its copy of the working tree, its entry in `.claude/launch.json`. Never the user's app on :8080.
8. Report the merge commit, the issue's state, and what is left: `git branch -a` and `git worktree list`.
