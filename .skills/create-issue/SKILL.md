---
name: create-issue
description: Use when filing a new GitHub issue in glebfox/timesheets — a bug report, a feature request, an upgrade, or a follow-up found during other work.
---

# Create an issue

## Write it

- If the `jmix-issue-guidelines` skill is available, write the title and body with it.
- Title: a plain sentence, no type prefix. A bug names what the user sees go wrong ("Deleting a time entry deletes its tags"); a feature names what it adds ("Customizable business hours").
- Label: `bug` for a defect, `enhancement` for a new capability. A refactor, chore, or upgrade has no label: leave out `--label`. Add `documentation` or `accessibility` when the issue is about that.

## File it

```bash
gh issue create --repo glebfox/timesheets --title "<title>" --body-file <file> [--label <label>] --project "Timesheets"
```

Without `--project "Timesheets"` the issue never reaches the board. Check that it landed; this must print `Timesheets`:

```bash
gh issue view <issue> --repo glebfox/timesheets --json projectItems --jq '.projectItems[].title'
```

The board sets Status to `To triage` itself.

## Board fields

- **Jmix** — set it only when the issue is tied to a Jmix version, such as an upgrade: `bash .skills/issue-flow/scripts/set-board-field.sh <issue> Jmix 3.2`.
- **Priority**, **Size** — leave them to the user.
